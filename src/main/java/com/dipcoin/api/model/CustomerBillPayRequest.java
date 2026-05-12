package com.dipcoin.api.model;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class CustomerBillPayRequest extends CustomerBillFetchRequest {

  @ApiModelProperty(required = true)
  private BigDecimal amount;
  @ApiModelProperty(required = true)
  private String osta;
  @ApiModelProperty(required = false)
  private Integer cardId;
  @ApiModelProperty(required = false)
  private Integer usageType = DipcoinUsageType.GENERIC.value();
  @ApiModelProperty(required = false)
  private String usageCategoryId;
  @ApiModelProperty(required = false)
  private String authorizationPin;
  @ApiModelProperty(required = false)
  private String currency;
  @ApiModelProperty(required = false)
  private PaymentInformation paymentInformation;
  @ApiModelProperty(required = false)
  private BillDetailInformation billDetail;
  @ApiModelProperty(required = false)
  private List<FieldInfoRequestMapper> additionalInformation;
  @ApiModelProperty(required = false)
  private String billPaymentToken;
  @ApiModelProperty(required = false)
  private String partnerTransactionReferenceId;
  @ApiModelProperty(required = false)
  private BigDecimal convenienceFee;
  @ApiModelProperty(required = false)
  private String customerAccountNumber;
  @ApiModelProperty(required = false)
  private String bbpsBankAccountNumber;
  @ApiModelProperty(required = false)
  private Integer fundTransferType;
  @ApiModelProperty(required = false)
  private String bankTransactionReferenceId;
  @ApiModelProperty(required = false)
  private String orderId;  //this parameter is added for rhythmFLows (primeFlix)
  @ApiModelProperty(required = false)
  private String finacusReferenceId;
  @ApiModelProperty(required = false)
  private String finacusPaymentReferenceId;
  @ApiModelProperty(required = false)
  private String remitterName;
  @ApiModelProperty(required = false)
  private String remitterAddress;

  @NoArgsConstructor
  @Accessors(chain = true)
  @Setter
  @Getter
  public class PaymentInformation {
    private String paymentMode;
    private List<FieldInfoRequestMapper> paymentParams;
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (CollectionUtils.isEmpty(getSubscriptionDetails())) {
      this.setErrorCode(HeaderCode.INVALID_MISSING_RECHARGE_BILL_INFORMATION);
      return false;
    }
    
    if (this.getCardId() == null || this.getCardId() <= 0) {
      this.setErrorCode(HeaderCode.INVALID_CARD_ID);
      return false;
    }

    if (this.getAmount() == null || BigDecimal.ZERO.compareTo(this.getAmount()) > 0) {
      this.setErrorCode(HeaderCode.MISSING_PAYMENT_AMOUNT);
      return false;
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub

  }

}
