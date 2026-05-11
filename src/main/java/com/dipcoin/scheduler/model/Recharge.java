package com.dipcoin.scheduler.model;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "Recharge")
@NamedQuery(name = "Recharge.findAll", query = "SELECT r FROM Recharge r")
public class Recharge implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private BigDecimal amount;

  private BigDecimal amountDue;

  private String consumerNo;

  private String customerId;

  private String dueDate;

  private String rechargeType;

  private Integer requestType;

  private String source;

  private String spCode;

  private Integer status;

  @Column(name = "BillProviderId")
  private String billPaymentsInfo;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public String getConsumerNo() {
    return consumerNo;
  }

  public void setConsumerNo(String consumerNo) {
    this.consumerNo = consumerNo;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }

  public String getRechargeType() {
    return rechargeType;
  }

  public void setRechargeType(String rechargeType) {
    this.rechargeType = rechargeType;
  }

  public Integer getRequestType() {
    return requestType;
  }

  public void setRequestType(Integer requestType) {
    this.requestType = requestType;
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

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getBillPaymentsInfo() {
    return billPaymentsInfo;
  }

  public void setBillPaymentsInfo(String billPaymentsInfo) {
    this.billPaymentsInfo = billPaymentsInfo;
  }
}
