package com.dipcoin.partner.paymentGateway.model;

import lombok.Data;

@Data
public class InternalAggrepayRefundStatusResponse {
  private String status;
  private String orderId;
  private String refundId;
  private String refundReferenceNo;
  private String responseCode;
  private String message;
}
