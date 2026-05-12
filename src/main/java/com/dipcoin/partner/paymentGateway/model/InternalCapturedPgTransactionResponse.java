package com.dipcoin.partner.paymentGateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalCapturedPgTransactionResponse {

  private String partnerTransactionRefId;
  private String orderId;
  private String capturedTransactionId;
  private String paymentMode;
  private String paymentInfoTagValue;

  public String getPartnerTransactionRefId() {
    return partnerTransactionRefId;
  }

  public void setPartnerTransactionRefId(String partnerTransactionRefId) {
    this.partnerTransactionRefId = partnerTransactionRefId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getCapturedTransactionId() {
    return capturedTransactionId;
  }

  public void setCapturedTransactionId(String capturedTransactionId) {
    this.capturedTransactionId = capturedTransactionId;
  }

  public String getPaymentMode() {
    return paymentMode;
  }

  public void setPaymentMode(String paymentMode) {
    this.paymentMode = paymentMode;
  }

  public String getPaymentInfoTagValue() {
    return paymentInfoTagValue;
  }

  public void setPaymentInfoTagValue(String paymentInfoTagValue) {
    this.paymentInfoTagValue = paymentInfoTagValue;
  }
}
