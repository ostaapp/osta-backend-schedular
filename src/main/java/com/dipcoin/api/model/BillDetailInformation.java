package com.dipcoin.api.model;

import java.util.List;
import com.dipcoin.api.filter.HttpServletContext;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class BillDetailInformation extends APIRequest {

  @JsonAlias("consumerName")
  private String customerName;
  private String amount;
  private String billDate;
  private String billAmount;
  private String billerId;
  private String dueDate;
  private String custConvFee;
  private String custConvDesc;
  private String billPaymentToken;
  private String billNumber;
  private String billPeriod;
  private List<FieldInfoRequestMapper> additionalAmount;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    return false;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {

  }
}
