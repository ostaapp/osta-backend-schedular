package com.dipcoin.partner.paymentGateway.model;

public class InternalAggrepayPaymentStatusResponse {

  private String status;
  private String orderId;
  private String capturedPgTransactionId;
  private String message;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getCapturedPgTransactionId() {
    return capturedPgTransactionId;
  }

  public void setCapturedPgTransactionId(String capturedPgTransactionId) {
    this.capturedPgTransactionId = capturedPgTransactionId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
