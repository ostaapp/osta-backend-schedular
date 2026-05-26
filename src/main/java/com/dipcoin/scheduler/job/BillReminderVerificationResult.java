package com.dipcoin.scheduler.job;

import java.math.BigDecimal;

public class BillReminderVerificationResult {

  private boolean sendReminder;

  private String status;

  private String reason;

  private String latestDueDate;

  private BigDecimal latestAmount;

  private String rawResponse;

  public boolean isSendReminder() {
    return sendReminder;
  }

  public void setSendReminder(boolean sendReminder) {
    this.sendReminder = sendReminder;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getLatestDueDate() {
    return latestDueDate;
  }

  public void setLatestDueDate(String latestDueDate) {
    this.latestDueDate = latestDueDate;
  }

  public BigDecimal getLatestAmount() {
    return latestAmount;
  }

  public void setLatestAmount(BigDecimal latestAmount) {
    this.latestAmount = latestAmount;
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }
}
