package com.dipcoin.partner.paymentGateway.model;

import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PropertySource(
    value = {"classpath:dipcoin-partner-pg-services.properties",
        "classpath:dipcoin-partner-pg-services-${spring.profiles.active}.properties"},
    ignoreResourceNotFound = true)
@Configuration
@ComponentScan("com.dipcoin.partner.paymentGateway")
public class PaymentGatewayProperties {

  @JsonIgnore
  @Autowired
  private PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paytmMerchantId}")
  private String paytmMerchantId;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paytmMerchantKey}")
  private String paytmMerchantKey;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paytmRefundUrl}")
  private String paytmRefundUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paytmTransactionStatusUrl}")
  private String paytmTransactionStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paytmRefundStatusUrl}")
  private String paytmRefundStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paypalGetOauthTokenUrl}")
  private String paypalGetOauthTokenUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paypalClientId}")
  private String paypalClientId;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paypalSecret}")
  private String paypalSecret;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paypalCreateOrderReq}")
  private String paypalCreateOrder;
  // paypalSTCPutReq
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paypalSTCPutReq}")
  private String paypalSTCPutReq;
  // paypalTransactionStatusUrl
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.paypalTransactionStatusUrl}")
  private String paypalTransactionStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.ostaOauthUrl}")
  private String ostaOauthUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.ostaAddMoneyUrl}")
  private String ostaAddMoneyUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.isProxyEnabled}")
  private Boolean isProxyEnabled;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.port}")
  private int port;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.ip}")
  private String ip;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.getTxnStatusUrl}")
  private String getTxnStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.internalCapturedPgStatusUrl:}")
  private String internalCapturedPgStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.internalAggrepayPaymentStatusUrl:}")
  private String internalAggrepayPaymentStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.internalAggrepayRefundUrl:}")
  private String internalAggrepayRefundUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.internalAggrepayRefundStatusUrl:}")
  private String internalAggrepayRefundStatusUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepayPaymentSeamlessUrl}")
  private String aggrepayPaymentSeamlessUrl;

  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepayCheckTransactionStatusUrl}")
  private String aggrepayCheckTransactionStatusUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepayRefundUrl}")
  private String aggrepayRefundUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepayGenerateAccessTokenUrl}")
  private String aggrepayGenerateAccessTokenUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepaySaveCardDetailsUrl}")
  private String aggrepaySaveCardDetailsUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepayGetSavedCardSessionTokensUrl}")
  private String aggrepayGetSavedCardSessionTokensUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepayPaymentSavedCardRequestUrl}")
  private String aggrepayPaymentSavedCardRequestUrl;
  
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.bankCallBackUrl}")
  private String bankCallBackUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.aggrepaySettlementdetailsRequestUrl}")
  private String aggrepaySettlementdetailsRequestUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.partnerRefundInitiate}")
  private String partnerRefundInitiate;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.confirmPaymentRequestUrl}")
  private String confirmPaymentRequestUrl;
  
  @Value("${com.dipcoin.partner.paymentGateway.utils.PaymentGatewayProperties.primeFlixUrl}")
  private String primeFlixUrl;

  public String getPartnerRefundInitiate() {
    return partnerRefundInitiate;
  }


  public String getAggrepaySettlementdetailsRequestUrl() {
    return aggrepaySettlementdetailsRequestUrl;
  }

  public String getAggrepayGenerateAccessTokenUrl() {
    return aggrepayGenerateAccessTokenUrl;
  }

  public void setAggrepayGenerateAccessTokenUrl(String aggrepayGenerateAccessTokenUrl) {
    this.aggrepayGenerateAccessTokenUrl = aggrepayGenerateAccessTokenUrl;
  }

  public String getAggrepaySaveCardDetailsUrl() {
    return aggrepaySaveCardDetailsUrl;
  }

  public void setAggrepaySaveCardDetailsUrl(String aggrepaySaveCardDetailsUrl) {
    this.aggrepaySaveCardDetailsUrl = aggrepaySaveCardDetailsUrl;
  }

  public String getAggrepayGetSavedCardSessionTokensUrl() {
    return aggrepayGetSavedCardSessionTokensUrl;
  }

  public void setAggrepayGetSavedCardSessionTokensUrl(String aggrepayGetSavedCardSessionTokensUrl) {
    this.aggrepayGetSavedCardSessionTokensUrl = aggrepayGetSavedCardSessionTokensUrl;
  }

  public String getAggrepayPaymentSavedCardRequestUrl() {
    return aggrepayPaymentSavedCardRequestUrl;
  }

  public void setAggrepayPaymentSavedCardRequestUrl(String aggrepayPaymentSavedCardRequestUrl) {
    this.aggrepayPaymentSavedCardRequestUrl = aggrepayPaymentSavedCardRequestUrl;
  }

  public String getAggrepayRefundUrl() {
		return aggrepayRefundUrl;
  }

  public void setAggrepayRefundUrl(String aggrepayRefundUrl) {
		this.aggrepayRefundUrl = aggrepayRefundUrl;
  }


  public String getAggrepayPaymentSeamlessUrl() {
    return aggrepayPaymentSeamlessUrl;
  }

  public void setAggrepayPaymentSeamlessUrl(String aggrepayPaymentSeamlessUrl) {
    this.aggrepayPaymentSeamlessUrl = aggrepayPaymentSeamlessUrl;
  }

  public String getAggrepayCheckTransactionStatusUrl() {
    return aggrepayCheckTransactionStatusUrl;
  }

  public void setAggrepayCheckTransactionStatusUrl(String aggrepayCheckTransactionStatusUrl) {
    this.aggrepayCheckTransactionStatusUrl = aggrepayCheckTransactionStatusUrl;
  }
  
  public String getConfirmPaymentRequestUrl() {
	    return confirmPaymentRequestUrl;
  }

  public void setConfirmPaymentRequestUrl(String confirmPaymentRequestUrl) {
	    this.confirmPaymentRequestUrl = confirmPaymentRequestUrl;
   }
  
  public String getPrimeFlixUrl() {
	    return primeFlixUrl;
  }

  public void setPrimeFlixUrl(String primeFlixUrl) {
	    this.primeFlixUrl = primeFlixUrl;
  }


  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public Boolean getIsProxyEnabled() {
    return isProxyEnabled;
  }

  public void setIsProxyEnabled(Boolean isProxyEnabled) {
    this.isProxyEnabled = isProxyEnabled;
  }

  public String getPaypalTransactionStatusUrl() {
    return paypalTransactionStatusUrl;
  }

  public void setPaypalTransactionStatusUrl(String paypalTransactionStatusUrl) {
    this.paypalTransactionStatusUrl = paypalTransactionStatusUrl;
  }

  public String getPaypalSTCPutReq() {
    return paypalSTCPutReq;
  }

  public void setPaypalSTCPutReq(String paypalSTCPutReq) {
    this.paypalSTCPutReq = paypalSTCPutReq;
  }

  public String getPaytmMerchantId() {
    return paytmMerchantId;
  }

  public String getPaytmMerchantKey() {
    return paytmMerchantKey;
  }

  public String getPaytmRefundUrl() {
    return paytmRefundUrl;
  }

  public String getPaytmTransactionStatusUrl() {
    return paytmTransactionStatusUrl;
  }

  public String getPaytmRefundStatusUrl() {
    return paytmRefundStatusUrl;
  }

  public String getBankCallBackUrl() {
    return bankCallBackUrl;
  }

  public void setBankCallBackUrl(String bankCallBackUrl) {
    this.bankCallBackUrl = bankCallBackUrl;
  }

  public String getPayPalGetOauthTokenUrl() {
    return paypalGetOauthTokenUrl;
  }

  public String getPaypalClientId() {
    return paypalClientId;
  }

  public String getPaypalSecret() {
    return paypalSecret;
  }

  public String getPaypalOrderReq() {
    return paypalCreateOrder;
  }


  public String getOstaOauthUrl() {
    return ostaOauthUrl;
  }

  public void setOstaOauthUrl(String ostaOauthUrl) {
    this.ostaOauthUrl = ostaOauthUrl;
  }

  public String getOstaAddMoneyUrl() {
    return ostaAddMoneyUrl;
  }

  public void setOstaAddMoneyUrl(String ostaAddMoneyUrl) {
    this.ostaAddMoneyUrl = ostaAddMoneyUrl;
  }

  public String getGetTxnStatusUrl() {
    return getTxnStatusUrl;
  }

  public void setGetTxnStatusUrl(String getTxnStatusUrl) {
    this.getTxnStatusUrl = getTxnStatusUrl;
  }

  public String getInternalCapturedPgStatusUrl() {
    return internalCapturedPgStatusUrl;
  }

  public void setInternalCapturedPgStatusUrl(String internalCapturedPgStatusUrl) {
    this.internalCapturedPgStatusUrl = internalCapturedPgStatusUrl;
  }

  public String getInternalAggrepayPaymentStatusUrl() {
    return internalAggrepayPaymentStatusUrl;
  }

  public void setInternalAggrepayPaymentStatusUrl(String internalAggrepayPaymentStatusUrl) {
    this.internalAggrepayPaymentStatusUrl = internalAggrepayPaymentStatusUrl;
  }

  public String getInternalAggrepayRefundUrl() {
    return internalAggrepayRefundUrl;
  }

  public void setInternalAggrepayRefundUrl(String internalAggrepayRefundUrl) {
    this.internalAggrepayRefundUrl = internalAggrepayRefundUrl;
  }

  public String getInternalAggrepayRefundStatusUrl() {
    return internalAggrepayRefundStatusUrl;
  }

  public void setInternalAggrepayRefundStatusUrl(String internalAggrepayRefundStatusUrl) {
    this.internalAggrepayRefundStatusUrl = internalAggrepayRefundStatusUrl;
  }

  @PreDestroy
  public void cleanUp() throws Exception {
  }
}
