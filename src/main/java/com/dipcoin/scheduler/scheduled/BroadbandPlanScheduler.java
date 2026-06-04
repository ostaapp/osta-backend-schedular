package com.dipcoin.scheduler.scheduled;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dipcoin.api.model.ZoneItem;
import com.dipcoin.api.model.ZoneResponse;
import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.BillPaymentsInfoDBService;
import com.dipcoin.db.services.RechargePlanDetailsDBService;
import com.dipcoin.db.services.model.BillPaymentsInfo;
import com.dipcoin.db.services.model.RechargePlanDetails;
import com.dipcoin.partner.recharge.utils.RechargeConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@EnableScheduling
public class BroadbandPlanScheduler {

    private static final Logger LOG = LogManager.getLogger(BroadbandPlanScheduler.class);

    @Autowired
    private FinacusHttpClient finacusHttpClient;

    @Autowired
    private BillPaymentsInfoDBService billPaymentsInfoDBService;

    @Autowired
    private RechargePlanDetailsDBService rechargePlanDetailsDBService;

    @Autowired
    private ObjectMapper objectMapper;

    
//    @Scheduled(cron = "0 0 * * * ?")
    public void syncBroadbandPlans() {

        LOG.info("=== Broadband Plan Sync Started ===");

        try {

            // Fetch all broadband billers
            List<BillPaymentsInfo> broadbandBillers =
                    billPaymentsInfoDBService.getProviders(
                            "Broadband Postpaid",
                            RechargeConstants.BillerStatus.ACTIVE.value(),
                            null,
                            null
                    );

            if (broadbandBillers == null || broadbandBillers.isEmpty()) {
                LOG.info("No broadband billers found");
                return;
            }

            LOG.info("Total broadband billers found: {}", broadbandBillers.size());

            for (BillPaymentsInfo biller : broadbandBillers) {

                String billerId = biller.getBillerId();

                try {

                    LOG.info("Fetching zones for biller {}", billerId);

                    // Fetch zones
                    ZoneResponse zoneResponse = finacusHttpClient.getZoneByBillerId(billerId);

//                    if (zoneResponse == null ||
//                        zoneResponse.getZoneList() == null ||
//                        zoneResponse.getZoneList().isEmpty()) {
//
//                        LOG.info("No zones found for biller {}", billerId);
//                        continue;
//                    }
                 // Check if API returned error
                    if (zoneResponse == null) {
                        LOG.warn("Zone response is null for biller {}", billerId);
                        continue;
                    }

                    if (!"000".equals(zoneResponse.getResponseCode())) {

                        LOG.warn("Finacus returned error for biller {} : {}",
                                billerId,
                                zoneResponse.getResponseMessage());

                        continue;
                    }

                    // Check if zone list exists
                    if (zoneResponse.getZoneList() == null ||
                        zoneResponse.getZoneList().isEmpty()) {

                        LOG.info("No zones found for biller {}", billerId);
                        continue;
                    }

                    // Iterate zones
                    for (ZoneItem zoneItem : zoneResponse.getZoneList()) {

                        String zone = zoneItem.getZone();

                        try {

                            LOG.info("Fetching plans for biller {}, zone {}", billerId, zone);
                            
                            int insertedCount = 0;
                            int updatedCount = 0;

                            // Step 4: Fetch plans
                            String planResponse =
                                    finacusHttpClient.fetchRechargePlans(billerId, zone);

                            if (planResponse == null || planResponse.isEmpty()) {

                                LOG.info("No plan response for biller {}, zone {}", billerId, zone);
                                continue;
                            }

                            // Convert JSON → List<RechargePlanDetails>
                            List<RechargePlanDetails> plans =
                                    objectMapper.readValue(
                                            planResponse,
                                            new TypeReference<List<RechargePlanDetails>>() {}
                                    );

                            if (plans == null || plans.isEmpty()) {

                                LOG.info("No plans found for biller {}, zone {}", billerId, zone);
                                continue;
                            }

                            // Process plans
                            for (RechargePlanDetails plan : plans) {

                                Optional<RechargePlanDetails> existingPlan =
                                        rechargePlanDetailsDBService
                                                .findByBillerAndPlanId(
                                                        plan.getBillerId(),
                                                        plan.getPlanId()
                                                );

                                if (existingPlan.isPresent()) {

                                    RechargePlanDetails existing = existingPlan.get();

                                    updatePlan(existing, plan);

                                     rechargePlanDetailsDBService.saveOrUpdate(existing);
                                    updatedCount++;

                                    LOG.info("Updated plan: {}", plan.getPlanId());

                                } else {

                                    plan.setId(null);

                                     rechargePlanDetailsDBService.saveOrUpdate(plan);
                                    insertedCount++;

                                    LOG.info("Inserted new plan: {}", plan.getPlanId());
                                }

                            }
                            
                            LOG.info("BroadbandPlan Sync Summary for zone {} - Total Inserted: {} | Total Updated: {}", zone, insertedCount, updatedCount);

                        } catch (Exception zoneEx) {

                            LOG.error("Error fetching plans for biller {} zone {}", billerId, zone, zoneEx);

                        }

                    }

                } catch (Exception billerEx) {

                    LOG.error("Error processing biller {}", billerId, billerEx);

                }

            }

            LOG.info("=== Broadband Plan Sync Completed ===");

        } catch (Exception e) {

            LOG.error("Broadband Plan Sync Failed", e);

        }

    }

    private void updatePlan(RechargePlanDetails target,
                            RechargePlanDetails source) {

        target.setAmount(source.getAmount());
        target.setPlanDescription(source.getPlanDescription());
        target.setType(source.getType());
        target.setValidity(source.getValidity());
        target.setData(source.getData());
        target.setTalktime(source.getTalktime());
        target.setZone(source.getZone());
        target.setCategoryType(source.getCategoryType());
        target.setStatus(source.getStatus());

    }
}