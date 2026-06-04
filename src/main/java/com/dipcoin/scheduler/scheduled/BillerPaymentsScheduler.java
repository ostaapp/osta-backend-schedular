package com.dipcoin.scheduler.scheduled;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.dipcoin.api.model.BillerItem;
import com.dipcoin.api.model.BillersByCategoryResponse;
import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.BillPaymentsInfoDBService;
import com.dipcoin.db.services.BillerCategoriesDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.model.BillPaymentsInfo;
import com.dipcoin.db.services.model.BillerCategories;
import com.dipcoin.db.services.model.Merchant;

import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class BillerPaymentsScheduler {

    private final MerchantDBService merchantDBService;
    private final BillerCategoriesDBService billerCategoriesDBService;
    private final BillPaymentsInfoDBService billPaymentsInfoDBService;
    private final FinacusHttpClient finacusHttpClient;

    private static final Logger LOG = LogManager.getLogger(BillerPaymentsScheduler.class);
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_FETCH_REQUIREMENT = "1";
    private static final String DEFAULT_SUPPORT_BILL_VALIDATION = "0";
    private static final String DEFAULT_PAYMENT_CHANNELS = "AGT";
    private static final String DEFAULT_PAYMENT_MODES =
            "Cash,InternetBanking,DebitCard,CreditCard,PrepaidCard,IMPS,UPI,Wallet,NEFT,AEPS,AccountTransfer,BharatQR,USSD";

//    @Scheduled(cron = "* * 0 * * ?")
    public void syncBillPaymentsInfo() {

        LOG.info("=== BillPaymentsInfo Sync Started ===");

        List<Merchant> merchants;
        try {
            merchants = merchantDBService
                    .asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value())
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to fetch merchants: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            LOG.error("Failed to fetch merchants: {}", e.getMessage(), e);
            return;
        }

        if (merchants == null || merchants.isEmpty() || !StringUtils.hasText(merchants.get(0).getReferenceId())) {
            LOG.error("No merchant found for RECHARGE_BILLPAYMENTS");
            return;
        }

        String partnerReferenceId = merchants.get(0).getReferenceId();
        LOG.info("Using partnerReferenceId = {}", partnerReferenceId);

        List<BillerCategories> categories = billerCategoriesDBService.getAllBillerCategories();
        if (categories == null || categories.isEmpty()) {
            LOG.warn("No biller categories found in DB");
            return;
        }

        LOG.info("Total categories found in DB: {}", categories.size());

        Set<String> processedCategoryIds = new HashSet<>();
        for (BillerCategories category : categories) {
            if (category == null || !StringUtils.hasText(category.getCategoryId())) {
                LOG.warn("Skipping category with blank categoryId");
                continue;
            }

            String categoryId = category.getCategoryId();
            if (!processedCategoryIds.add(categoryId)) {
                LOG.info("Skipping duplicate Category ID = {}", categoryId);
                continue;
            }

            Long categoryIdValue;
            try {
                categoryIdValue = Long.valueOf(categoryId);
            } catch (NumberFormatException e) {
                LOG.error("Invalid categoryId '{}' for category '{}'. Skipping category.", categoryId, category.getName());
                continue;
            }

            LOG.info("Fetching billers for Category ID = {}", categoryId);

            try {
                BillersByCategoryResponse response = finacusHttpClient.getBillersByCategory(categoryId, "");
                List<BillerItem> billers = response != null ? response.getResponse() : null;

                if (billers == null || billers.isEmpty()) {
                    LOG.warn("No billers fetched for Category ID = {}", categoryId);
                    continue;
                }

                LOG.info("Billers fetched: {}", billers.size());

                for (BillerItem biller : billers) {
                    try {
                        if (biller == null || !StringUtils.hasText(biller.getBillerId())) {
                            LOG.warn("Skipping biller with blank billerId for Category ID = {}", categoryId);
                            continue;
                        }

                        LOG.info("BillerId: {} | CustomerParams: {}", biller.getBillerId(), biller.getBillerInputParams());

                        BillPaymentsInfo billPaymentsInfo = findExistingBiller(
                                biller.getBillerId(), partnerReferenceId);

                        if (!StringUtils.hasText(billPaymentsInfo.getStatus())) {
                            billPaymentsInfo.setStatus(DEFAULT_STATUS);
                        }
                        billPaymentsInfo.setPartnerReferenceId(partnerReferenceId);
                        billPaymentsInfo.setBillerId(biller.getBillerId());
                        billPaymentsInfo.setBillerName(defaultIfBlank(biller.getBillerName(), billPaymentsInfo.getBillerName()));
                        billPaymentsInfo.setBillerAliasName(
                                defaultIfBlank(biller.getBillerName(), billPaymentsInfo.getBillerAliasName()));
                        billPaymentsInfo.setBillerCategoryId(categoryIdValue);
                        billPaymentsInfo.setBillerCategoryName(
                                defaultIfBlank(category.getName(), billPaymentsInfo.getBillerCategoryName()));
                        billPaymentsInfo.setBillerMode(defaultIfBlank(biller.getBillerMode(), billPaymentsInfo.getBillerMode()));
                        billPaymentsInfo.setBillerAcceptsAdhoc(
                                biller.getAcceptAdHocPayment() != null && biller.getAcceptAdHocPayment() ? "1" : "0");
                        billPaymentsInfo.setTjsbId(
                                biller.getId() != null ? biller.getId().intValue() : billPaymentsInfo.getTjsbId());
                        billPaymentsInfo.setBillerDescription(
                                defaultIfBlank(biller.getBillerDescription(), billPaymentsInfo.getBillerDescription()));
                        billPaymentsInfo.setPaymentAmountExactness(defaultIfBlank(
                                biller.getPaymentAmtExactness(),
                                defaultIfBlank(billPaymentsInfo.getPaymentAmountExactness(), "EXACT")));

                        billPaymentsInfo.setFetchRequirement(
                                defaultIfBlank(billPaymentsInfo.getFetchRequirement(), DEFAULT_FETCH_REQUIREMENT));
                        billPaymentsInfo.setSupportBillValidation(
                                defaultIfBlank(billPaymentsInfo.getSupportBillValidation(), DEFAULT_SUPPORT_BILL_VALIDATION));
                        billPaymentsInfo.setBillerPaymentChannels(
                                defaultIfBlank(billPaymentsInfo.getBillerPaymentChannels(), DEFAULT_PAYMENT_CHANNELS));
                        billPaymentsInfo.setBillerPaymentModes(
                                defaultIfBlank(billPaymentsInfo.getBillerPaymentModes(), DEFAULT_PAYMENT_MODES));

                        if (billPaymentsInfo.getId() == null) {
                            billPaymentsInfo.setChargeCode(-1);
                        }
                        if (!StringUtils.hasText(billPaymentsInfo.getBillerEffctvFrom())) {
                            billPaymentsInfo.setBillerEffctvFrom(LocalDate.now().toString());
                        }
                        if (!StringUtils.hasText(billPaymentsInfo.getBillerEffctvTo())) {
                            billPaymentsInfo.setBillerEffctvTo("2099-12-31");
                        }

                        LOG.debug("Saving biller -> billerId={}, name={}, category={}, acceptsAdhoc={}, tjsbId={}",
                                billPaymentsInfo.getBillerId(),
                                billPaymentsInfo.getBillerName(),
                                billPaymentsInfo.getBillerCategoryId(),
                                billPaymentsInfo.getBillerAcceptsAdhoc(),
                                billPaymentsInfo.getTjsbId());

                        billPaymentsInfoDBService.save(billPaymentsInfo);

                        LOG.info("Saved biller: {} ({})", billPaymentsInfo.getBillerId(), billPaymentsInfo.getBillerName());
                    } catch (Exception billerEx) {
                        LOG.error("Failed to save biller: {} | Error: {}", biller != null ? biller.getBillerId() : null,
                                billerEx.getMessage(), billerEx);
                    }
                }
                LOG.info("Completed storing billers for Category ID = {}", categoryId);
            } catch (Exception ex) {
                LOG.error("Error for Category {} : {}", categoryId, ex.getMessage(), ex);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void syncBillerCustomerParams() {

        LOG.info("=== Biller Customer Params Sync Started ===");

        List<BillPaymentsInfo> billers = billPaymentsInfoDBService.getAllBillers();

        if (billers == null || billers.isEmpty()) {
            LOG.warn("No billers found for customer param sync");
            return;
        }

        for (BillPaymentsInfo biller : billers) {

            try {
                String billerId = biller.getBillerId();
                if (!StringUtils.hasText(billerId)) {
                    LOG.warn("Skipping biller with blank billerId during customer param sync");
                    continue;
                }

                LOG.info("Fetching customer params for biller: {}", billerId);

                String responseText = finacusHttpClient.fetchCustomerParams(billerId);

                if (!StringUtils.hasText(responseText)) {
                    LOG.warn("Null response for billerId {}", billerId);
                    continue;
                }

                LOG.info("Raw customer params response for {} = {}", billerId, responseText);

                JSONObject fullJson;
                if (responseText.trim().startsWith("<")) {
                    JSONObject xmlObj = XML.toJSONObject(responseText);
                    String innerJson = xmlObj.getJSONObject("string").getString("#text");
                    fullJson = new JSONObject(innerJson);
                } else {
                    fullJson = new JSONObject(responseText);
                }

                JSONArray params = fullJson.optJSONArray("Response");
                if (params == null) {
                    LOG.warn("No customer params returned for biller {}", billerId);
                    continue;
                }

                JSONArray normalized = new JSONArray();

                for (int i = 0; i < params.length(); i++) {
                    JSONObject p = params.getJSONObject(i);
                    JSONObject np = new JSONObject();

                    String paramName = p.optString("name", p.optString("Name", null));
                    String dataType = p.optString("FieldType", p.optString("fieldType", null));

                    boolean isMandatory = p.optBoolean("IsMandatory")
                            || "true".equalsIgnoreCase(p.optString("IsMandatory"));

                    np.put("paramName", paramName);
                    np.put("dataType", dataType);
                    np.put("optional", !isMandatory);
                    np.put("minLength", p.opt("MinLength"));
                    np.put("maxLength", p.opt("MaxLength"));
                    np.put("minValue", JSONObject.NULL);
                    np.put("maxValue", JSONObject.NULL);
                    np.put("visibility", true);
                    np.put("unique", false);
                    np.put("regex", p.optString("Regex", p.optString("regex", null)));
                    np.put("values", JSONObject.NULL);

                    normalized.put(np);
                }

                String normalizedJson = normalized.toString();
                if (normalizedJson.length() > 2000) {
                    LOG.warn("Skipping customer params save for biller {} because payload length {} exceeds DB limit",
                            billerId, normalizedJson.length());
                    continue;
                }

                biller.setBillerCustomerParams(normalizedJson);
                billPaymentsInfoDBService.save(biller);

                LOG.info("Saved NORMALIZED customer params for biller {}", billerId);
            } catch (Exception ex) {
                LOG.error("Error syncing customer params for biller {} : {}", biller.getBillerId(), ex.getMessage(), ex);
            }
        }

        LOG.info("=== Biller Customer Params Sync Completed ===");
    }

    private BillPaymentsInfo findExistingBiller(String billerId, String partnerReferenceId) {
        BillPaymentsInfo existing = billPaymentsInfoDBService
                .getBillerByBillerIdAndPartnerReferenceId(billerId, partnerReferenceId);
        if (existing != null) {
            return existing;
        }

        List<BillPaymentsInfo> existingList = billPaymentsInfoDBService.getBillPaymentsInfoByBillerId(billerId);
        if (existingList != null && !existingList.isEmpty()) {
            return existingList.get(0);
        }

        return new BillPaymentsInfo();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
