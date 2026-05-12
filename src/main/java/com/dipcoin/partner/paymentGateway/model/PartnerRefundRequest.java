package com.dipcoin.partner.paymentGateway.model;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PartnerRefundRequest {
  private static ObjectMapper objectMapper = new ObjectMapper();


  private String orderId;

  private String partnerTransactionReferenceId;

  private String ostaTransactionReferenceId;

  private String comment;

  private BigDecimal amount;

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getPartnerTransactionReferenceId() {
    return partnerTransactionReferenceId;
  }

  public void setPartnerTransactionReferenceId(String partnerTransactionReferenceId) {
    this.partnerTransactionReferenceId = partnerTransactionReferenceId;
  }

  public String getOstaTransactionReferenceId() {
    return ostaTransactionReferenceId;
  }

  public void setOstaTransactionReferenceId(String ostaTransactionReferenceId) {
    this.ostaTransactionReferenceId = ostaTransactionReferenceId;
  }


  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

}
