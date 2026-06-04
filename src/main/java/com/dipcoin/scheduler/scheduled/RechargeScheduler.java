package com.dipcoin.scheduler.scheduled;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.BillerCategoriesRepository;
import com.dipcoin.db.services.BillPaymentsInfoRepository;
import com.dipcoin.db.services.RechargePlanRepository;
import com.dipcoin.db.services.RechargePlanService;
import com.dipcoin.db.services.RechargeServiceProviderRepository;
import com.dipcoin.db.services.model.BillPaymentsInfo;
import com.dipcoin.db.services.model.RechargePlan;
import com.dipcoin.db.services.model.RechargeServiceProvider;

import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class RechargeScheduler {

    private static final Logger LOG = LogManager.getLogger(RechargeScheduler.class);

    private final RechargeServiceProviderRepository rechargeServiceProviderRepository;
    private final BillerCategoriesRepository categoriesRepo;
    private final BillPaymentsInfoRepository billPaymentsInfoRepo;
    private final RechargePlanRepository rechargePlanRepository;
    private final RechargePlanService rechargePlanService;
    private final FinacusHttpClient finacusHttpClient;

    private static final String MOBILE_RECHARGE = "MR";

//    @Scheduled(cron = "0 0 0 * * ?")
    public void syncRechargePlans() {

        LOG.info("=== Recharge Plan Sync Started ===");

        try {
            List<String> categoryIds = categoriesRepo.findMobilePrepaidCategoryIds();
            if (categoryIds == null || categoryIds.isEmpty()) {
                LOG.warn("No Mobile Prepaid categories found. Exiting.");
                return;
            }

            LOG.info("MobilePrepaid categoryIds (size={}): {}", categoryIds.size(), categoryIds);

            for (String catId : categoryIds) {

                List<BillPaymentsInfo> billers =
                        billPaymentsInfoRepo.findActiveByCategoryId(Long.parseLong(catId));

                LOG.info("Category {} -> {} billers", catId, billers.size());

                for (BillPaymentsInfo biller : billers) {

                    String billerId = biller.getBillerId();
                    String operatorMaster = biller.getBillerName();
                    String partnerRefId = biller.getPartnerReferenceId();

                    LOG.info("Processing biller={} operator={}", billerId, operatorMaster);

                    // Fetch provider details from DB based on billerName
                    List<RechargeServiceProvider> providers =
                            rechargeServiceProviderRepository.findByServiceProvider(biller.getBillerName());

                    if (providers.isEmpty()) {
                        LOG.warn("No RechargeServiceProvider found for billerName {}", biller.getBillerName());
                        continue;
                    }

                    for (RechargeServiceProvider provider : providers) {

                        // FROM DB table
                        String serviceProviderCode = provider.getServiceProviderCode();  
                        String dbCircleCode = provider.getCircleCode();                  
                        String dbCircleName = provider.getCircleName();                  

                        LOG.info("Matched Provider → billerId={} providerCode={} circleCode={} circleName={}",
                                billerId, serviceProviderCode, dbCircleCode, dbCircleName);

                        // Deactivate OLD plans (use circle_code)
                        rechargePlanService.deactivatePlansForBillerAndCircle(billerId, dbCircleCode);

                        // Fetch plans from Finacus using FULL CIRCLE NAME (circle_name)
                        List<RechargePlan> plans =
                                finacusHttpClient.fetchPlans(billerId, dbCircleName);

                        if (plans == null || plans.isEmpty()) {
                            LOG.info("No plans found for biller={} circle={}", billerId, dbCircleName);
                            continue;
                        }

                        LOG.info("Finacus returned {} plans for biller={} circleName={}",
                                plans.size(), billerId, dbCircleName);

                        // STEP 3: Insert or Update plans
                        for (RechargePlan incoming : plans) {

                            if (incoming.getPlanId() == null || incoming.getPlanId().isEmpty()) {
                                LOG.warn("Skipping plan with missing PLANID for biller={} circle={}",
                                        billerId, dbCircleName);
                                continue;
                            }

                            String planId = incoming.getPlanId();

                            LOG.info("Processing PLANID={} for biller={}", planId, billerId);

                            // Prepare RechargePlan object
                            incoming.setService(MOBILE_RECHARGE);
                            incoming.setServiceProviderCode(serviceProviderCode);    // from DB
                            incoming.setCircleCode(dbCircleCode);                    // short code
                            incoming.setCircleMaster(dbCircleName);                  // full name
                            incoming.setChargeCode(0);
                            incoming.setOperatorMaster(operatorMaster);
                            incoming.setPartnerReferenceId(partnerRefId);
                            incoming.setBillerId(billerId);
                            incoming.setStatus(1);
                            incoming.setAddedDeleted("ADD");

                            // Check if plan exists
                            Optional<RechargePlan> existing =
                                    rechargePlanRepository.findByBillerIdAndCircleCodeAndPlanId(
                                            billerId, dbCircleCode, planId);

                            if (existing.isPresent()) {
                                rechargePlanService.updateExisting(existing.get(), incoming);
                                LOG.info("Updated existing plan {} for biller={} circle={}",
                                        planId, billerId, dbCircleCode);
                            } else {
                                incoming.setId(null); // ensure insert
                                rechargePlanService.save(incoming);
                                LOG.info("Inserted new plan {} for biller={} circle={}",
                                        planId, billerId, dbCircleCode);
                            }
                        }
                    }
                }
            }

            LOG.info("=== Recharge Plan Sync Completed ===");

        } catch (Exception e) {
            LOG.error("Recharge Plan Sync FAILED: {}", e.getMessage(), e);
        }
    }
}
