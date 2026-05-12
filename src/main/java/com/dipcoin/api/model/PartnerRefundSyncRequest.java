package com.dipcoin.api.model;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerRefundSyncRequest {

  private String orderId;
  private String syncType;
  private String refundStatus;
  private String providerRefundStatus;
  private String refundReferenceId;
  private String refundResponseCode;
  private String refundResponseMessage;
  private String adminActionBy;
  private Long adminActionAt;
  private String rawResponse;

  public boolean isValid() {
    return StringUtils.isNotBlank(orderId) && StringUtils.isNotBlank(syncType);
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getSyncType() {
    return syncType;
  }

  public void setSyncType(String syncType) {
    this.syncType = syncType;
  }

  public String getRefundStatus() {
    return refundStatus;
  }

  public void setRefundStatus(String refundStatus) {
    this.refundStatus = refundStatus;
  }

  public String getProviderRefundStatus() {
    return providerRefundStatus;
  }

  public void setProviderRefundStatus(String providerRefundStatus) {
    this.providerRefundStatus = providerRefundStatus;
  }

  public String getRefundReferenceId() {
    return refundReferenceId;
  }

  public void setRefundReferenceId(String refundReferenceId) {
    this.refundReferenceId = refundReferenceId;
  }

  public String getRefundResponseCode() {
    return refundResponseCode;
  }

  public void setRefundResponseCode(String refundResponseCode) {
    this.refundResponseCode = refundResponseCode;
  }

  public String getRefundResponseMessage() {
    return refundResponseMessage;
  }

  public void setRefundResponseMessage(String refundResponseMessage) {
    this.refundResponseMessage = refundResponseMessage;
  }

  public String getAdminActionBy() {
    return adminActionBy;
  }

  public void setAdminActionBy(String adminActionBy) {
    this.adminActionBy = adminActionBy;
  }

  public Long getAdminActionAt() {
    return adminActionAt;
  }

  public void setAdminActionAt(Long adminActionAt) {
    this.adminActionAt = adminActionAt;
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }
}
