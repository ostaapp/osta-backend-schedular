package com.dipcoin.db.services.model;

import java.io.Serializable;

import javax.persistence.Entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import javax.persistence.NamedQuery;
import javax.persistence.Table;


/**
 * The persistent class for the BillPaymentsInfo database table.
 * 
 */
@Entity
@Table(name = "BillPaymentsInfo")  
@NamedQuery(name = "BillPaymentsInfo.findAll", query = "SELECT b FROM BillPaymentsInfo b")
public class BillPaymentsInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  private Integer chargeCode;
  
  private Long billerCategoryId;
    
  private Integer TjsbId;
    
  private String BillerDescription;

  
  
  private String billerAcceptsAdhoc;

  private String billerAdditionalInfo;

  private String billerAliasName;

  private String billerCategoryName;

  private String billerCoverage;

  private String billerCustomerParams;

  private String billerEffctvFrom;

  private String billerEffctvTo;

  private String billerEndDownTime;

  private String billerId;

  private String billerMode;

  private String billerName;

  private String billerOwnerShp;

  private String billerPaymentChannels;

  private String billerPaymentModes;

  private String billerResponseParams;

  private String billerStartDownTime;

  private String billerTempDeactivationEnd;

  private String billerTempDeactivationStart;

  private String customizedAdditionalInformation;

  private String fetchRequirement;

  private String interchangeFee;

  private String interchangeFeeConf;

  private String parentBiller;

  private String parentBillerId;

  private String partnerReferenceId;

  private String paymentAmountExactness;

  private String status;

  private String supportBillValidation;
  
  //bi-directional many-to-one association to Recharge
	/*
	 * @OneToMany(mappedBy = "billPaymentsInfo", cascade = CascadeType.ALL)
	 * 
	 * @JsonManagedReference private List<Recharge> recharge;
	 */

  public String getBillerAcceptsAdhoc() {
    return this.billerAcceptsAdhoc;
  }

  public Integer getId() {
	return id;
}

  public void setId(Integer id) {
	this.id = id;
  }

  public Long getBillerCategoryId() {
	return billerCategoryId;
  }

  public void setBillerCategoryId(Long billerCategoryId) {
	this.billerCategoryId = billerCategoryId;
  }

  public Integer getTjsbId() {
	return TjsbId;
  }

  public void setTjsbId(Integer tjsbId) {
	TjsbId = tjsbId;
  }

  public String getBillerDescription() {
	return BillerDescription;
  }

  public void setBillerDescription(String billerDescription) {
	BillerDescription = billerDescription;
  }

  public void setChargeCode(Integer chargeCode) {
	this.chargeCode = chargeCode;
  }

  public void setBillerAcceptsAdhoc(String billerAcceptsAdhoc) {
    this.billerAcceptsAdhoc = billerAcceptsAdhoc;
  }

  public String getBillerAdditionalInfo() {
    return this.billerAdditionalInfo;
  }

  public void setBillerAdditionalInfo(String billerAdditionalInfo) {
    this.billerAdditionalInfo = billerAdditionalInfo;
  }

  public String getBillerAliasName() {
    return this.billerAliasName;
  }

  public void setBillerAliasName(String billerAliasName) {
    this.billerAliasName = billerAliasName;
  }

  public String getBillerCategoryName() {
    return this.billerCategoryName;
  }

  public void setBillerCategoryName(String billerCategoryName) {
    this.billerCategoryName = billerCategoryName;
  }

  public String getBillerCoverage() {
    return this.billerCoverage;
  }

  public void setBillerCoverage(String billerCoverage) {
    this.billerCoverage = billerCoverage;
  }

  public String getBillerCustomerParams() {
    return this.billerCustomerParams;
  }

  public void setBillerCustomerParams(String billerCustomerParams) {
    this.billerCustomerParams = billerCustomerParams;
  }

  public String getBillerEffctvFrom() {
    return this.billerEffctvFrom;
  }

  public void setBillerEffctvFrom(String billerEffctvFrom) {
    this.billerEffctvFrom = billerEffctvFrom;
  }

  public String getBillerEffctvTo() {
    return this.billerEffctvTo;
  }

  public void setBillerEffctvTo(String billerEffctvTo) {
    this.billerEffctvTo = billerEffctvTo;
  }

  public String getBillerEndDownTime() {
    return this.billerEndDownTime;
  }

  public void setBillerEndDownTime(String billerEndDownTime) {
    this.billerEndDownTime = billerEndDownTime;
  }

  public String getBillerMode() {
    return this.billerMode;
  }

  public void setBillerMode(String billerMode) {
    this.billerMode = billerMode;
  }

  public String getBillerName() {
    return this.billerName;
  }

  public void setBillerName(String billerName) {
    this.billerName = billerName;
  }

  public String getBillerOwnerShp() {
    return this.billerOwnerShp;
  }

  public void setBillerOwnerShp(String billerOwnerShp) {
    this.billerOwnerShp = billerOwnerShp;
  }

  public String getBillerPaymentChannels() {
    return this.billerPaymentChannels;
  }

  public void setBillerPaymentChannels(String billerPaymentChannels) {
    this.billerPaymentChannels = billerPaymentChannels;
  }

  public String getBillerPaymentModes() {
    return this.billerPaymentModes;
  }

  public void setBillerPaymentModes(String billerPaymentModes) {
    this.billerPaymentModes = billerPaymentModes;
  }

  public String getBillerResponseParams() {
    return this.billerResponseParams;
  }

  public void setBillerResponseParams(String billerResponseParams) {
    this.billerResponseParams = billerResponseParams;
  }

  public String getBillerStartDownTime() {
    return this.billerStartDownTime;
  }

  public void setBillerStartDownTime(String billerStartDownTime) {
    this.billerStartDownTime = billerStartDownTime;
  }

  public String getBillerTempDeactivationEnd() {
    return this.billerTempDeactivationEnd;
  }

  public void setBillerTempDeactivationEnd(String billerTempDeactivationEnd) {
    this.billerTempDeactivationEnd = billerTempDeactivationEnd;
  }

  public String getBillerTempDeactivationStart() {
    return this.billerTempDeactivationStart;
  }

  public void setBillerTempDeactivationStart(String billerTempDeactivationStart) {
    this.billerTempDeactivationStart = billerTempDeactivationStart;
  }

  public int getChargeCode() {
    return this.chargeCode;
  }

  public void setChargeCode(int chargeCode) {
    this.chargeCode = chargeCode;
  }

  public String getCustomizedAdditionalInformation() {
    return this.customizedAdditionalInformation;
  }

  public void setCustomizedAdditionalInformation(String customizedAdditionalInformation) {
    this.customizedAdditionalInformation = customizedAdditionalInformation;
  }

  public String getFetchRequirement() {
    return this.fetchRequirement;
  }

  public void setFetchRequirement(String fetchRequirement) {
    this.fetchRequirement = fetchRequirement;
  }

  public String getInterchangeFee() {
    return this.interchangeFee;
  }

  public void setInterchangeFee(String interchangeFee) {
    this.interchangeFee = interchangeFee;
  }

  public String getInterchangeFeeConf() {
    return this.interchangeFeeConf;
  }

  public void setInterchangeFeeConf(String interchangeFeeConf) {
    this.interchangeFeeConf = interchangeFeeConf;
  }

  public String getParentBiller() {
    return this.parentBiller;
  }

  public void setParentBiller(String parentBiller) {
    this.parentBiller = parentBiller;
  }

  public String getParentBillerId() {
    return this.parentBillerId;
  }

  public void setParentBillerId(String parentBillerId) {
    this.parentBillerId = parentBillerId;
  }

  public String getPartnerReferenceId() {
    return this.partnerReferenceId;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }

  public String getPaymentAmountExactness() {
    return this.paymentAmountExactness;
  }

  public void setPaymentAmountExactness(String paymentAmountExactness) {
    this.paymentAmountExactness = paymentAmountExactness;
  }

  public String getStatus() {
    return this.status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSupportBillValidation() {
    return this.supportBillValidation;
  }

  public void setSupportBillValidation(String supportBillValidation) {
    this.supportBillValidation = supportBillValidation;
  }
  
  public String getBillerId() {
	return billerId;
}

public void setBillerId(String billerId) {
	this.billerId = billerId;
}

}
