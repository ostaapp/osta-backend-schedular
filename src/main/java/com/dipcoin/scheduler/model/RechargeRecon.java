package com.dipcoin.scheduler.model;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

/**
 * The persistent class for the RechargeRecon database table.
 */
@Entity
@NamedQuery(name = "RechargeRecon.findAll", query = "SELECT m FROM RechargeRecon m")
public class RechargeRecon implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private BigDecimal amount;

  private String dipcoinTransactionRefId;

  private int issue;

  private String issueDate;

  private String merchantReferenceId;

  private String merchantTransactionRefId;

  private String requestTime;

  private int status;

  private String statusDate;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getDipcoinTransactionRefId() {
    return dipcoinTransactionRefId;
  }

  public void setDipcoinTransactionRefId(String dipcoinTransactionRefId) {
    this.dipcoinTransactionRefId = dipcoinTransactionRefId;
  }

  public int getIssue() {
    return issue;
  }

  public void setIssue(int issue) {
    this.issue = issue;
  }

  public String getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(String issueDate) {
    this.issueDate = issueDate;
  }

  public String getMerchantReferenceId() {
    return merchantReferenceId;
  }

  public void setMerchantReferenceId(String merchantReferenceId) {
    this.merchantReferenceId = merchantReferenceId;
  }

  public String getMerchantTransactionRefId() {
    return merchantTransactionRefId;
  }

  public void setMerchantTransactionRefId(String merchantTransactionRefId) {
    this.merchantTransactionRefId = merchantTransactionRefId;
  }

  public String getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(String requestTime) {
    this.requestTime = requestTime;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getStatusDate() {
    return statusDate;
  }

  public void setStatusDate(String statusDate) {
    this.statusDate = statusDate;
  }

}
