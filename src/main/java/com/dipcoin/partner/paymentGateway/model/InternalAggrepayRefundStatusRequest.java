package com.dipcoin.partner.paymentGateway.model;

import lombok.Data;

@Data
public class InternalAggrepayRefundStatusRequest {
  private String orderId;
  private String refundId;
}
