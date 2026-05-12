package com.dipcoin.partner.paymentGateway.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class InternalAggrepayRefundRequest {
  private String orderId;
  private BigDecimal amount;
  private String reason;
}
