package com.dipcoin.scheduler.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "BbpsBillReminderLog")
@NamedQuery(name = "BbpsBillReminderLog.findAll", query = "SELECT l FROM BbpsBillReminderLog l")
public class BbpsBillReminderLog implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "dedupeKey", unique = true, nullable = false, length = 255)
  private String dedupeKey;

  @Column(name = "rechargeId")
  private Integer rechargeId;

  @Column(name = "customerId")
  private String customerId;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "billerId", length = 64)
  private String billerId;

  @Column(name = "billerName", length = 255)
  private String billerName;

  @Column(name = "consumerNo", length = 128)
  private String consumerNo;

  @Column(name = "dueDate", length = 32)
  private String dueDate;

  @Column(name = "eventType", length = 64)
  private String eventType;

  @Column(name = "emailStatus", length = 32)
  private String emailStatus;

  @Column(name = "message", length = 1000)
  private String message;

  @Column(name = "providerResponse", length = 1000)
  private String providerResponse;

  @Column(name = "createdAt")
  private Long createdAt;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getDedupeKey() {
    return dedupeKey;
  }

  public void setDedupeKey(String dedupeKey) {
    this.dedupeKey = dedupeKey;
  }

  public Integer getRechargeId() {
    return rechargeId;
  }

  public void setRechargeId(Integer rechargeId) {
    this.rechargeId = rechargeId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getBillerId() {
    return billerId;
  }

  public void setBillerId(String billerId) {
    this.billerId = billerId;
  }

  public String getBillerName() {
    return billerName;
  }

  public void setBillerName(String billerName) {
    this.billerName = billerName;
  }

  public String getConsumerNo() {
    return consumerNo;
  }

  public void setConsumerNo(String consumerNo) {
    this.consumerNo = consumerNo;
  }

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getEmailStatus() {
    return emailStatus;
  }

  public void setEmailStatus(String emailStatus) {
    this.emailStatus = emailStatus;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getProviderResponse() {
    return providerResponse;
  }

  public void setProviderResponse(String providerResponse) {
    this.providerResponse = providerResponse;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }
}
