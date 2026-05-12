package com.dipcoin.partner.paymentGateway.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.commons.LogFormatter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerRefundResponse {

  private static ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger LOG = LogManager.getLogger(PartnerRefundResponse.class);
  
  private String orderId;
  private String partnerTransactionReferenceId;
  private String ostaTransactionReferenceId;
  private Integer Status;
  private String message;
  private BigDecimal amount;
 
  private ArrayList<Code> codes;

  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      LOG.error(LogFormatter.instance().message("Exception caught").format(), e);
    }
    return null;
  }

}
