package com.dipcoin.db.services.model;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(name = "RechargePlan")
@NamedQuery(name = "RechargePlan.findAll", query = "SELECT r FROM RechargePlan r")
public class RechargePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    // PRIMARY KEY
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "PlanId")
    private String planId;

    
    public String getPlanId() {
		return planId;
	}



	public void setPlanId(String planId) {
		this.planId = planId;
	}

	// BillerId from BillPaymentsInfo
    @Column(name = "BillerId", length = 50)
    private String billerId;


    // Circle (Zone)
    @Column(name = "CircleCode", length = 64)
    private String circleCode;

    @Column(name = "CircleMaster", length = 64)
    private String circleMaster;

    // Hardcoded: MOBILE_RECHARGE
    @Column(name = "Service", length = 10)
    private String service;

    // From recharge.json
    @Column(name = "ServiceProviderCode", length = 10)
    private String serviceProviderCode;

    // Default = 0
    @Column(name = "ChargeCode")
    private Integer chargeCode;

    // BillerName
    @Column(name = "OperatorMaster", length = 64)
    private String operatorMaster;

    // AMOUNT (Finacus) — DB is INT but entity stores BigDecimal safely
    @Column(name = "RechargeValue")
    private BigDecimal rechargeValue;

    // TALKTIME (Finacus) — string like "84 Days"
    @Column(name = "RechargeTalkTime")
    private BigDecimal rechargeTalkTime;

    @Column(name = "RechargeValidity", length = 30)
    private String rechargeValidity;

    @Column(name = "EuronetPlanType", length = 30)
    private String euronetPlanType;

    @Column(name = "EuronetRechargeType", length = 30)
    private String euronetRechargeType;

    @Column(name = "RechargeShortDescription", length = 500)
    private String rechargeShortDescription;

    @Column(name = "RechargeDescription", length = 2000)
    private String rechargeDescription;

    @Column(name = "ProductType", length = 100)
    private String productType;

    @Column(name = "RechargeMaster", length = 2000)
    private String rechargeMaster;

    @Column(name = "AddedDeleted", length = 5)
    private String addedDeleted;

    @Column(name = "TarrifId")
    private Integer tarrifId;

    @Column(name = "PartnerReferenceId", length = 100)
    private String partnerReferenceId;

    @Column(name = "Status")
    private Integer status;



    // ----------- Constructors -------------

    public RechargePlan() {}



    // ----------- Getters & Setters -------------

    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public String getBillerId() { return billerId; }

    public void setBillerId(String billerId) { this.billerId = billerId; }

    public String getCircleCode() { return circleCode; }

    public void setCircleCode(String circleCode) { this.circleCode = circleCode; }

    public String getCircleMaster() { return circleMaster; }

    public void setCircleMaster(String circleMaster) { this.circleMaster = circleMaster; }

    public String getService() { return service; }

    public void setService(String service) { this.service = service; }

    public String getServiceProviderCode() { return serviceProviderCode; }

    public void setServiceProviderCode(String serviceProviderCode) { this.serviceProviderCode = serviceProviderCode; }

    public Integer getChargeCode() { return chargeCode; }

    public void setChargeCode(Integer chargeCode) { this.chargeCode = chargeCode; }

    public String getOperatorMaster() { return operatorMaster; }

    public void setOperatorMaster(String operatorMaster) { this.operatorMaster = operatorMaster; }

    public BigDecimal getRechargeValue() { return rechargeValue; }

    public void setRechargeValue(BigDecimal rechargeValue) { this.rechargeValue = rechargeValue; }


    public BigDecimal getRechargeTalkTime() {
		return rechargeTalkTime;
	}



	public void setRechargeTalkTime(BigDecimal rechargeTalkTime) {
		this.rechargeTalkTime = rechargeTalkTime;
	}



	public String getRechargeValidity() { return rechargeValidity; }

    public void setRechargeValidity(String rechargeValidity) { this.rechargeValidity = rechargeValidity; }

    public String getEuronetPlanType() { return euronetPlanType; }

    public void setEuronetPlanType(String euronetPlanType) { this.euronetPlanType = euronetPlanType; }

    public String getEuronetRechargeType() { return euronetRechargeType; }

    public void setEuronetRechargeType(String euronetRechargeType) { this.euronetRechargeType = euronetRechargeType; }

    public String getRechargeShortDescription() { return rechargeShortDescription; }

    public void setRechargeShortDescription(String rechargeShortDescription) { this.rechargeShortDescription = rechargeShortDescription; }

    public String getRechargeDescription() { return rechargeDescription; }

    public void setRechargeDescription(String rechargeDescription) { this.rechargeDescription = rechargeDescription; }

    public String getProductType() { return productType; }

    public void setProductType(String productType) { this.productType = productType; }

    public String getRechargeMaster() { return rechargeMaster; }

    public void setRechargeMaster(String rechargeMaster) { this.rechargeMaster = rechargeMaster; }

    public String getAddedDeleted() { return addedDeleted; }

    public void setAddedDeleted(String addedDeleted) { this.addedDeleted = addedDeleted; }

    public Integer getTarrifId() { return tarrifId; }

    public void setTarrifId(Integer tarrifId) { this.tarrifId = tarrifId; }

    public String getPartnerReferenceId() { return partnerReferenceId; }

    public void setPartnerReferenceId(String partnerReferenceId) { this.partnerReferenceId = partnerReferenceId; }

    public Integer getStatus() { return status; }

    public void setStatus(Integer status) { this.status = status; }

}
