package com.dipcoin.scheduler.scheduled;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
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
    private static final int COMPLAINT_DESCRIPTION_LIMIT = 240;

    @Autowired
    private RechargeDBService rechargeDBService;

    @Autowired
    private FinacusHttpClient finacusHttpClient;

    @Autowired
    private BbpsRefundService bbpsRefundService;

    @Scheduled(cron = "0 0 0 * * ?")
//    @Scheduled(cron = "0 */1 * * * ?")
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
                    if (isFinacusConnectivityFailure(e)) {
                        LOG.warn("Finacus ticket status endpoint unreachable for complaintId {}: {}. Stopping this scheduler run.",
                                recharge.getComplaintId(), e.getMessage());
                        break;
                    }
                    LOG.error("Error processing status update for complaintId {}: {}", recharge.getComplaintId(),
                            e.getMessage());
                }
            }

        } catch (Exception e) {
            LOG.error("Complaint Status Scheduler FAILED: {}", e.getMessage(), e);
        }

        LOG.info("=== Complaint Status Scheduler Completed ===");
    }

    private boolean isFinacusConnectivityFailure(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }

        String message = StringUtils.defaultString(e.getMessage()).toLowerCase();
        return message.contains("connect timed out") || message.contains("connection timed out");
    }

    private void processStatusUpdate(Recharge recharge) throws Exception {
        String complaintId = recharge.getComplaintId();
        if (StringUtils.isBlank(complaintId)) {
            return;
        }

        String ticketId = complaintId.trim();
        LOG.info("Updating BBPS ticket status for ticketId: {}", ticketId);

        String finacusJson = finacusHttpClient.bbpsTicketStatus(ticketId);

        if (StringUtils.isBlank(finacusJson)) {
            LOG.warn("Finacus API returned empty response for ticketId: {}", ticketId);
            return;
        }

        JSONObject json = new JSONObject(finacusJson);
        String responseCode = json.optString("ResponseCode", "");
        String finacusTicketStatus = json.optString("TicketStatus", "");
        String responseMessage = json.optString("ResponseMessage", "NULL RESPONSE");
        String remarks = json.optString("description", "");
        if (StringUtils.isBlank(remarks)) {
            remarks = responseMessage;
        }

        if ("000".equals(responseCode)) {
            if (StringUtils.isNotBlank(finacusTicketStatus)) {
                recharge.setComplaintStatus(finacusTicketStatus);
                recharge.setComplaintDescription(remarks);
                rechargeDBService.updateRecharge(recharge);
                if (bbpsRefundService != null) {
                    bbpsRefundService.syncRechargeState(recharge, BbpsRefundConstants.StatusSource.FINACUS);
                    bbpsRefundService.triggerRefundAfterCommit(recharge.getId(),
                            BbpsRefundConstants.StatusSource.FINACUS);
                }
                LOG.info("Updated ticketId {} to status: {} with remarks: {}", ticketId, finacusTicketStatus, remarks);
            }
        } else {
            String failedStatus = StringUtils.defaultIfBlank(finacusTicketStatus, "FAILED");
            recharge.setComplaintStatus(failedStatus);
            recharge.setComplaintDescription(limitDescription("Finacus ResponseCode: " + responseCode
                    + ", ResponseMessage: " + remarks));
            rechargeDBService.updateRecharge(recharge);
            LOG.warn("Finacus API returned failure for ticketId {}: code={}, status={}, message={}", ticketId,
                    responseCode, failedStatus, responseMessage);
        }
    }

    private String limitDescription(String description) {
        if (description == null || description.length() <= COMPLAINT_DESCRIPTION_LIMIT) {
            return description;
        }
        return description.substring(0, COMPLAINT_DESCRIPTION_LIMIT);
    }
}
