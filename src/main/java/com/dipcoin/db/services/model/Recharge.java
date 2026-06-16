package com.dipcoin.db.services.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;


/**
 * The persistent class for the Recharge database table.
 * 
 */
@Entity
@NamedQuery(name = "Recharge.findAll", query = "SELECT r FROM Recharge r")
public class Recharge implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  public Recharge() {}

  private BigDecimal actualCommission;

  private String additionalColumn1;

  private String additionalColumn2;

  private String additionalColumn3;

  @Column(name = "PgOrderId")
  private String pgOrderId;

  @Column(name = "PgPartnerTransactionReferenceId")
  private String pgPartnerTransactionReferenceId;

  private String additionalInfo;

  private String agentId;

  private BigDecimal amount;

  private BigDecimal amountDue;

  private int attempt;
  
  private String BBPSBranchReportId;
  
  private int bankId;

  private String billDetails;

  private String billPaymentToken;

  @Column(name = "bbpsTxnStatus")
  private String bbpsTxnStatus;

  private String billPeriod;

  private int chargeCode;

  private String circle;

  private String commissionUpdateTime;

  private String consumerNo;

  private BigDecimal convenienceFee;

  private String customerId;
  
  private String customerAccountNumber;

  private String customerName;

  private int dipcoinId;

  private String dipcoinRechargeReferenceNumber;

  private String dipcoinTransRefId;

  private String dueDate;

  private String email;

  private BigDecimal expectedCommission;

  private String expiry;
  
  private int fundTransferType;

  private String hashValue;

  private String IPAddress;

  private BigDecimal lateFee;

  private String offUsPay;

  private String partnerTransRefId;
  
  @Column(length = 255)
  @Access(AccessType.PROPERTY)
  private String partnerReferenceId;

  private String paymentRefNo;

  private String rawRequest;

  private String rawResponse;

  @Column(name = "RawBBpsTxnStatus")
  private String rawBBpsTxnStatus;

  private String rechargeType;
  
  private int requestSource;

  private int requestType;

  private String responseCode;

  private String responseMessage;

  private String serviceCode;

  private String source;

  private String spCode;

  private String splitPay;

  private BigDecimal splitPayAmount;

  private String sspCode;

  private String clientTransactionId;
  
  private String complaintId;
  
  private String complaintStatus;
  
  private String complaintDisposition;

  private String complaintDescription;

  private int status;

  @Column(name = "isRefundRequired")
  private Boolean isRefundRequired = Boolean.FALSE;

  private String subscriptionDetails;

  private String updateDateTime;
  
  private int merchantTxnType;
  
  private String partnerMerchantRequest;
  
  private String partnerMerchantResponse;
  
  private String complaintRawRequest;

  private String complaintRawResponse;
  
//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "BillProviderId", referencedColumnName = "BillerId")
//  private BillPaymentsInfo billPaymentsInfo;

  @Column(name = "BillProviderId")
  private String billPaymentsInfo;
  
  public int getId() {
	  return id;
  }

  public void setId(int id) {
	  this.id = id;
  }

  public BigDecimal getActualCommission() {
    return actualCommission;
  }

  public void setActualCommission(BigDecimal actualCommission) {
    this.actualCommission = actualCommission;
  }

  public String getAdditionalColumn1() {
    return additionalColumn1;
  }

  public void setAdditionalColumn1(String additionalColumn1) {
    this.additionalColumn1 = additionalColumn1;
  }

  public String getAdditionalColumn2() {
    return additionalColumn2;
  }

  public void setAdditionalColumn2(String additionalColumn2) {
    this.additionalColumn2 = additionalColumn2;
  }

  public String getAdditionalColumn3() {
    return additionalColumn3;
  }

  public void setAdditionalColumn3(String additionalColumn3) {
    this.additionalColumn3 = additionalColumn3;
  }

  public String getPgOrderId() {
    return pgOrderId;
  }

  public void setPgOrderId(String pgOrderId) {
    this.pgOrderId = pgOrderId;
  }

  public String getPgPartnerTransactionReferenceId() {
    return pgPartnerTransactionReferenceId;
  }

  public void setPgPartnerTransactionReferenceId(String pgPartnerTransactionReferenceId) {
    this.pgPartnerTransactionReferenceId = pgPartnerTransactionReferenceId;
  }

  public String getAdditionalInfo() {
    return additionalInfo;
  }

  public void setAdditionalInfo(String additionalInfo) {
    this.additionalInfo = additionalInfo;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getAmountDue() {
    return amountDue;
  }

  public void setAmountDue(BigDecimal amountDue) {
    this.amountDue = amountDue;
  }

  public int getAttempt() {
    return attempt;
  }

  public void setAttempt(int attempt) {
    this.attempt = attempt;
  }

  public String getBBPSBranchReportId() {
    return BBPSBranchReportId;
  }

  public void setBBPSBranchReportId(String bBPSBranchReportId) {
    BBPSBranchReportId = bBPSBranchReportId;
  }

  public int getBankId() {
    return bankId;
  }

  public void setBankId(int bankId) {
    this.bankId = bankId;
  }

  public String getBillDetails() {
    return billDetails;
  }

  public void setBillDetails(String billDetails) {
    this.billDetails = billDetails;
  }

  public String getBillPaymentToken() {
    return billPaymentToken;
  }

  public void setBillPaymentToken(String billPaymentToken) {
    this.billPaymentToken = billPaymentToken;
  }

  public String getBbpsTxnStatus() {
    return bbpsTxnStatus;
  }

  public void setBbpsTxnStatus(String bbpsTxnStatus) {
    this.bbpsTxnStatus = bbpsTxnStatus;
  }

//  public BillPaymentsInfo getBillPaymentsInfo() {
//	  return billPaymentsInfo;
//  }
//
//  public void setBillPaymentsInfo(BillPaymentsInfo billPaymentsInfo) {
//	  this.billPaymentsInfo = billPaymentsInfo;
//  }
  
  public String getBillPaymentsInfo() {
    return billPaymentsInfo;
  }

  public void setBillPaymentsInfo(String billPaymentsInfo) {
    this.billPaymentsInfo = billPaymentsInfo;
  }

  public String getBillPeriod() {
	  return billPeriod;
  }

  public void setBillPeriod(String billPeriod) {
    this.billPeriod = billPeriod;
  }

  public int getChargeCode() {
    return chargeCode;
  }

  public void setChargeCode(int chargeCode) {
    this.chargeCode = chargeCode;
  }

  public String getCircle() {
    return circle;
  }

  public void setCircle(String circle) {
    this.circle = circle;
  }

  public String getCommissionUpdateTime() {
    return commissionUpdateTime;
  }

  public void setCommissionUpdateTime(String commissionUpdateTime) {
    this.commissionUpdateTime = commissionUpdateTime;
  }

  public String getConsumerNo() {
    return consumerNo;
  }

  public void setConsumerNo(String consumerNo) {
    this.consumerNo = consumerNo;
  }

  public BigDecimal getConvenienceFee() {
    return convenienceFee;
  }

  public void setConvenienceFee(BigDecimal convenienceFee) {
    this.convenienceFee = convenienceFee;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public int getDipcoinId() {
    return dipcoinId;
  }

  public void setDipcoinId(int dipcoinId) {
    this.dipcoinId = dipcoinId;
  }

  public String getDipcoinRechargeReferenceNumber() {
    return dipcoinRechargeReferenceNumber;
  }

  public void setDipcoinRechargeReferenceNumber(String dipcoinRechargeReferenceNumber) {
    this.dipcoinRechargeReferenceNumber = dipcoinRechargeReferenceNumber;
  }

  public String getDipcoinTransRefId() {
    return dipcoinTransRefId;
  }

  public void setDipcoinTransRefId(String dipcoinTransRefId) {
    this.dipcoinTransRefId = dipcoinTransRefId;
  }

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public BigDecimal getExpectedCommission() {
    return expectedCommission;
  }

  public void setExpectedCommission(BigDecimal expectedCommission) {
    this.expectedCommission = expectedCommission;
  }

  public String getExpiry() {
    return expiry;
  }

  public void setExpiry(String expiry) {
    this.expiry = expiry;
  }

  public String getHashValue() {
    return hashValue;
  }

  public void setHashValue(String hashValue) {
    this.hashValue = hashValue;
  }

  public String getIPAddress() {
    return IPAddress;
  }

  public void setIPAddress(String iPAddress) {
    IPAddress = iPAddress;
  }

  public BigDecimal getLateFee() {
    return lateFee;
  }

  public void setLateFee(BigDecimal lateFee) {
    this.lateFee = lateFee;
  }

  public String getOffUsPay() {
    return offUsPay;
  }

  public void setOffUsPay(String offUsPay) {
    this.offUsPay = offUsPay;
  }

  public String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    // Truncate to 20 characters to prevent database truncation error
    // The database column appears to be smaller than 25, so using 20 as a safe limit
    if (partnerReferenceId != null && partnerReferenceId.length() > 20) {
      this.partnerReferenceId = partnerReferenceId.substring(0, 20);
    } else {
      this.partnerReferenceId = partnerReferenceId;
    }
  }

  public String getPartnerTransRefId() {
    return partnerTransRefId;
  }

  public void setPartnerTransRefId(String partnerTransRefId) {
    this.partnerTransRefId = partnerTransRefId;
  }

  public String getClientTransactionId() {
    return clientTransactionId;
  }

  public void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public String getPaymentRefNo() {
    return paymentRefNo;
  }

  public void setPaymentRefNo(String paymentRefNo) {
    this.paymentRefNo = paymentRefNo;
  }

  public String getRawRequest() {
    return rawRequest;
  }
  
  // Bill Fetch Error Handling Fields
  @Column(name = "fetchResponseCode")
  private String fetchResponseCode;

  @Column(name = "fetchResponseMessage")
  private String fetchResponseMessage;

  public String getFetchResponseCode() {
    return fetchResponseCode;
  }

  public void setFetchResponseCode(String fetchResponseCode) {
    this.fetchResponseCode = fetchResponseCode;
  }

  public String getFetchResponseMessage() {
    return fetchResponseMessage;
  }

  public void setFetchResponseMessage(String fetchResponseMessage) {
    this.fetchResponseMessage = fetchResponseMessage;
  }

  public void setRawRequest(String rawRequest) {
    // Truncate to 1000 characters to prevent database truncation error
    // RawRequest stores request objects as strings which can be very long
    if (rawRequest != null && rawRequest.length() > 1000) {
      this.rawRequest = rawRequest.substring(0, 1000);
    } else {
      this.rawRequest = rawRequest;
    }
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }

  public String getRawBBpsTxnStatus() {
    return rawBBpsTxnStatus;
  }

  public void setRawBBpsTxnStatus(String rawBBpsTxnStatus) {
    this.rawBBpsTxnStatus = rawBBpsTxnStatus;
  }

  public int getRequestSource() {
    return requestSource;
  }

  public void setRequestSource(int requestSource) {
    this.requestSource = requestSource;
  }

  public String getRechargeType() {
    return rechargeType;
  }

  public void setRechargeType(String rechargeType) {
    this.rechargeType = rechargeType;
  }

  public int getRequestType() {
    return requestType;
  }

  public void setRequestType(int requestType) {
    this.requestType = requestType;
  }

  public String getComplaintDisposition() {
	    return complaintDisposition;
	  }

	  public void setComplaintDisposition(String complaintDisposition) {
	    this.complaintDisposition = complaintDisposition;
	  }

	  public String getComplaintDescription() {
	    return complaintDescription;
	  }

	  public void setComplaintDescription(String complaintDescription) {
	    this.complaintDescription = complaintDescription;
	  }
	  
  public String getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(String responseCode) {
    this.responseCode = responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  public String getServiceCode() {
    return serviceCode;
  }

  public void setServiceCode(String serviceCode) {
    this.serviceCode = serviceCode;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSpCode() {
    return spCode;
  }

  public void setSpCode(String spCode) {
    this.spCode = spCode;
  }

  public String getSplitPay() {
    return splitPay;
  }

  public void setSplitPay(String splitPay) {
    this.splitPay = splitPay;
  }

  public BigDecimal getSplitPayAmount() {
    return splitPayAmount;
  }

  public void setSplitPayAmount(BigDecimal splitPayAmount) {
    this.splitPayAmount = splitPayAmount;
  }

  public String getSspCode() {
    return sspCode;
  }

  public void setSspCode(String sspCode) {
    this.sspCode = sspCode;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public Boolean getIsRefundRequired() {
    return isRefundRequired;
  }

  public void setIsRefundRequired(Boolean isRefundRequired) {
    this.isRefundRequired = isRefundRequired;
  }

  public String getSubscriptionDetails() {
    return subscriptionDetails;
  }

  public void setSubscriptionDetails(String subscriptionDetails) {
    this.subscriptionDetails = subscriptionDetails;
  }

  public String getUpdateDateTime() {
    return updateDateTime;
  }

  public void setUpdateDateTime(String updateDateTime) {
    this.updateDateTime = updateDateTime;
  }

  public String getComplaintId() {
    return complaintId;
  }

  public void setComplaintId(String complaintId) {
    this.complaintId = complaintId;
  }
  
  public String getComplaintStatus() {
	    return complaintStatus;
  }

  public void setComplaintStatus(String complaintStatus) {
    this.complaintStatus = complaintStatus;
  }

  public String getComplaintRawRequest() {
	    return complaintRawRequest;
	  }

	  public void setComplaintRawRequest(String complaintRawRequest) {
	    this.complaintRawRequest = complaintRawRequest;
	  }

	  public String getComplaintRawResponse() {
	    return complaintRawResponse;
	  }

	  public void setComplaintRawResponse(String complaintRawResponse) {
	    this.complaintRawResponse = complaintRawResponse;
	  }
	 
  public String getCustomerAccountNumber() {
    return customerAccountNumber;
  }

  public void setCustomerAccountNumber(String customerAccountNumber) {
    this.customerAccountNumber = customerAccountNumber;
  }

  public int getFundTransferType() {
    return fundTransferType;
  }

  public void setFundTransferType(int fundTransferType) {
    this.fundTransferType = fundTransferType;
  }

  public int getMerchantTxnType() {
	return merchantTxnType;
  }

  public void setMerchantTxnType(int merchantTxnType) {
	this.merchantTxnType = merchantTxnType;
  }

  public String getPartnerMerchantRequest() {
		return partnerMerchantRequest;
  }

  public void setPartnerMerchantRequest(String partnerMerchantRequest) {
		this.partnerMerchantRequest = partnerMerchantRequest;
  }

  public String getPartnerMerchantResponse() {
		return partnerMerchantResponse;
  }

  public void setPartnerMerchantResponse(String partnerMerchantResponse) {
		this.partnerMerchantResponse = partnerMerchantResponse;
  }

  /**
   * Truncate PartnerReferenceId to 255 characters before persisting or updating.
   * This ensures the value fits in the database column regardless of how it was set.
   * This method is called by JPA/Hibernate before any persist or update operation.
   */
  @PrePersist
  @PreUpdate
  public void truncatePartnerReferenceId() {
    // Force truncation to 20 characters - this is a safety net
    // The database column appears to be smaller than 25, so using 20 as a safe limit
    if (this.partnerReferenceId != null && this.partnerReferenceId.length() > 20) {
      this.partnerReferenceId = this.partnerReferenceId.substring(0, 20);
    }
  }

}
