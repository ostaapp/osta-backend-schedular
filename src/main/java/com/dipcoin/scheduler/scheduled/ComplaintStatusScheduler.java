package com.dipcoin.scheduler.scheduled;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dipcoin.api.service.BbpsRefundService;
import com.dipcoin.db.services.RechargeDBService;
import com.dipcoin.db.services.commons.BbpsRefundConstants;
import com.dipcoin.db.services.model.Recharge;
import com.dipcoin.db.service.client.FinacusHttpClient;
import org.json.JSONObject;

@Component
@EnableScheduling
public class ComplaintStatusScheduler {

    private static final Logger LOG = LogManager.getLogger(ComplaintStatusScheduler.class);

    @Autowired
    private RechargeDBService rechargeDBService;

    @Autowired
    private FinacusHttpClient finacusHttpClient;

    @Autowired
    private BbpsRefundService bbpsRefundService;

//    @Scheduled(cron = "0 34 12 * * ?")
//    @Scheduled(cron = "0 0 0 * * ?")
    public void updateComplaintStatuses() {
        LOG.info("=== Complaint Status Scheduler Started ===");

        try {
            List<Recharge> complaints = rechargeDBService.getRechargesWithComplaints();
            if (complaints == null || complaints.isEmpty()) {
                LOG.info("No complaints found to update.");
                return;
            }

            LOG.info("Found {} complaints to process.", complaints.size());

            for (Recharge recharge : complaints) {
                try {
                    processStatusUpdate(recharge);
                } catch (Exception e) {
                    LOG.error("Error processing status update for complaintId {}: {}", recharge.getComplaintId(),
                            e.getMessage());
                }
            }

        } catch (Exception e) {
            LOG.error("Complaint Status Scheduler FAILED: {}", e.getMessage(), e);
        }

        LOG.info("=== Complaint Status Scheduler Completed ===");
    }

    private void processStatusUpdate(Recharge recharge) throws Exception {
        String complaintId = recharge.getComplaintId();
        if (StringUtils.isBlank(complaintId)) {
            return;
        }

        LOG.info("Updating status for complaintId: {}", complaintId);

        // Call Finacus SOAP client (returns JSON string)
        String finacusJson = finacusHttpClient.sendComplaintStatusRequest("Transaction", complaintId);

        if (StringUtils.isBlank(finacusJson)) {
            LOG.warn("Finacus API returned empty response for complaintId: {}", complaintId);
            return;
        }

        // Parse the JSON result
        JSONObject json = new JSONObject(finacusJson);
        String responseCode = json.optString("ResponseCode", "");

        if ("000".equals(responseCode)) {
            String newStatus = json.optString("ComplaintStatus", "");
            if (StringUtils.isBlank(newStatus)) {
                newStatus = json.optString("Status", "");
            }

            String remarks = json.optString("Remarks", "");
            if (StringUtils.isBlank(remarks)) {
                remarks = json.optString("StatusDetails", "");
            }

            if (StringUtils.isNotBlank(newStatus)) {
                recharge.setComplaintStatus(newStatus);
                recharge.setComplaintDescription(remarks);
                rechargeDBService.updateRecharge(recharge);
                if (bbpsRefundService != null) {
                    bbpsRefundService.syncRechargeState(recharge, BbpsRefundConstants.StatusSource.FINACUS);
                    bbpsRefundService.triggerRefundAfterCommit(recharge.getId(),
                            BbpsRefundConstants.StatusSource.FINACUS);
                }
                LOG.info("Updated complaintId {} to status: {} with remarks: {}", complaintId, newStatus, remarks);
            }
        } else {
            LOG.warn("Finacus API returned failure for complaintId {}: {}", complaintId,
                    json.optString("ResponseMessage", "NULL RESPONSE"));
        }
    }
}
