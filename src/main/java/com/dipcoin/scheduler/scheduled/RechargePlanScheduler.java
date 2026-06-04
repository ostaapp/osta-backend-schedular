package com.dipcoin.scheduler.scheduled;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dipcoin.api.resource.CustomerBillPaymentsInfoResource;
import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.RechargePlanDetailsDBService;
import com.dipcoin.db.services.model.RechargePlanDetails;

import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class RechargePlanScheduler {

	private static final Logger LOG = LogManager.getLogger(RechargePlanScheduler.class);

	@Autowired
	private CustomerBillPaymentsInfoResource customerBillPaymentsInfoResource;

	@Autowired
	private FinacusHttpClient finacusHttpClient;

	@Autowired
	private RechargePlanDetailsDBService rechargePlanDetailsDBService;

//	@Scheduled(cron = "0 0 * * * ?")
	public void syncRechargePlans() {
		LOG.info("=== Recharge Plan Sync Started ===");

		try {
			// Step 1: Get all unique biller IDs
			List<String> billerIds = finacusHttpClient.getAllBillerIds();
			LOG.info("Total unique billers found: {}", billerIds.size());

			// Step 2: Process each biller
			for (String billerId : billerIds) {
				try {
					List<String> zones = finacusHttpClient.getZonesForBiller(billerId);

					if (zones.isEmpty()) {
						LOG.info("No zones found for biller {}. Skipping...");
						continue;
					}

					// Step 3: Process each zone
					for (String zone : zones) {
						try {
							LOG.info("Fetching plans for biller {}, zone {}");
							List<RechargePlanDetails> plans = customerBillPaymentsInfoResource
									.getRechargePlans(billerId, zone);

							if (plans != null && !plans.isEmpty()) {

								// Step 4: Save or update each plan
								for (RechargePlanDetails plan : plans) {
									try {
										Optional<RechargePlanDetails> existingPlanOpt = rechargePlanDetailsDBService
												.findByBillerAndPlanId(plan.getBillerId(), plan.getPlanId());

										if (existingPlanOpt.isPresent()) {
											// Update existing plan - preserve the existing ID
											RechargePlanDetails existingPlan = existingPlanOpt.get();
											updatePlan(existingPlan, plan);
											rechargePlanDetailsDBService.saveOrUpdate(existingPlan);

											LOG.info("Updated existing plan {} for biller {}", plan.getPlanId(), plan.getBillerId());
										} else {
											// Insert new plan - ensure ID is null for new inserts
											plan.setId(null);
											rechargePlanDetailsDBService.saveOrUpdate(plan);
											LOG.info("Inserted new plan {} for biller {}", plan.getPlanId(),
													plan.getBillerId());
										}
									} catch (Exception planEx) {
										LOG.error("Error processing plan {} for biller {}: {}. Continuing with next plan...",
												plan != null ? plan.getPlanId() : "null", 
												plan != null ? plan.getBillerId() : "null", 
												planEx.getMessage());
									}
								}
							} else {
								LOG.info("No plans found for biller {}, zone {}", billerId, zone);
							}
						} catch (Exception zoneEx) {
							LOG.error("Error fetching plans for biller {}, zone {}: {}. Continuing with next zone...",
									billerId, zone, zoneEx.getMessage());
						}
					}
				} catch (Exception billerEx) {
					LOG.error("Error processing biller {}: {}. Continuing with next biller...",
							billerId, billerEx.getMessage());
				}
			}

			LOG.info("=== Recharge Plan Sync Completed ===");
		} catch (Exception e) {
			LOG.error("Recharge Plan Sync Failed: {}. Will retry in next scheduled run.", e.getMessage(), e);
		}
	}

	/**
	 * Copies all relevant fields from source plan to target plan for updating
	 */
	private void updatePlan(RechargePlanDetails target, RechargePlanDetails source) {
		target.setBillerId(source.getBillerId());
		target.setAmount(source.getAmount());
		target.setPlanDescription(source.getPlanDescription());
		target.setType(source.getType());
		target.setValidity(source.getValidity());
		target.setData(source.getData());
		target.setTalktime(source.getTalktime());
		target.setStatus(source.getStatus());
		target.setPlanId(source.getPlanId());
		target.setCategoryType(source.getCategoryType());
		target.setZone(source.getZone());
	}
}