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
import lombok.Data;

@Entity
@Table(name = "BbpsRefundCase")
@NamedQuery(name = "BbpsRefundCase.findAll", query = "SELECT c FROM BbpsRefundCase c")
@Data
public class BbpsRefundCase implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "paymentKey", unique = true, length = 160)
  private String paymentKey;

  @Column(name = "rechargeId")
  private Integer rechargeId;

  @Column(name = "dipcoinId")
  private Integer dipcoinId;

  @Column(name = "dipcoinTransRefId")
  private String dipcoinTransRefId;

  @Column(name = "partnerTransRefId")
  private String partnerTransRefId;

  @Column(name = "orderId")
  private String orderId;

  @Column(name = "clientTransactionId")
  private String clientTransactionId;

  @Column(name = "billPaymentToken")
  private String billPaymentToken;

  @Column(name = "paymentRefNo")
  private String paymentRefNo;

  @Column(name = "capturedPgTransactionId")
  private String capturedPgTransactionId;

  @Column(name = "capturedPgPaymentMode")
  private String capturedPgPaymentMode;

  @Column(name = "customerId")
  private String customerId;

  @Column(name = "customerMobile")
  private String customerMobile;

  @Column(name = "customerAccountNumber")
  private String customerAccountNumber;

  @Column(name = "billerId")
  private String billerId;

  @Column(name = "txnAmount")
  private BigDecimal txnAmount;

  @Column(name = "refundAmount")
  private BigDecimal refundAmount;

  @Column(name = "agEvidenceFound")
  private Boolean agEvidenceFound = Boolean.FALSE;

  @Column(name = "agStatus")
  private String agStatus;

  @Column(name = "agStatusSource")
  private String agStatusSource;

  @Column(name = "agLastCheckedAt")
  private Long agLastCheckedAt;

  @Column(name = "billpayCalled")
  private Boolean billpayCalled = Boolean.FALSE;

  @Column(name = "billpayStatus")
  private String billpayStatus;

  @Column(name = "billpayStatusSource")
  private String billpayStatusSource;

  @Column(name = "billpayLastCheckedAt")
  private Long billpayLastCheckedAt;

  @Column(name = "refundRequired")
  private Boolean refundRequired = Boolean.FALSE;

  @Column(name = "refundReason")
  private String refundReason;

  @Column(name = "refundRawReason", length = 1000)
  private String refundRawReason;

  @Column(name = "refundStatus")
  private String refundStatus;

  @Column(name = "refundReferenceId")
  private String refundReferenceId;

  @Column(name = "refundResponseCode")
  private String refundResponseCode;

  @Column(name = "refundResponseMessage", length = 500)
  private String refundResponseMessage;

  @Column(name = "providerRefundStatus")
  private String providerRefundStatus;

  @Column(name = "providerRefundCheckedAt")
  private Long providerRefundCheckedAt;

  @Column(name = "refundInitiatedAt")
  private Long refundInitiatedAt;

  @Column(name = "refundCompletedAt")
  private Long refundCompletedAt;

  @Column(name = "dipcoinCancelRequired")
  private Boolean dipcoinCancelRequired = Boolean.FALSE;

  @Column(name = "dipcoinCancelStatus")
  private String dipcoinCancelStatus;

  @Column(name = "dipcoinCancelReason")
  private String dipcoinCancelReason;

  @Column(name = "dipcoinCancelInitiatedAt")
  private Long dipcoinCancelInitiatedAt;

  @Column(name = "dipcoinCancelCompletedAt")
  private Long dipcoinCancelCompletedAt;

  @Column(name = "dipcoinCancelTxnRefId")
  private String dipcoinCancelTxnRefId;

  @Column(name = "lastDipcoinCancelError", length = 1000)
  private String lastDipcoinCancelError;

  @Column(name = "manualReviewRequired")
  private Boolean manualReviewRequired = Boolean.FALSE;

  @Column(name = "manualReviewReason")
  private String manualReviewReason;

  @Column(name = "adminTriggerRequired")
  private Boolean adminTriggerRequired = Boolean.FALSE;

  @Column(name = "adminActionBy")
  private String adminActionBy;

  @Column(name = "adminActionAt")
  private Long adminActionAt;

  @Column(name = "lastError", length = 1000)
  private String lastError;

  @Column(name = "lastAgResponse", length = 4000)
  private String lastAgResponse;

  @Column(name = "lastBbpsResponse", length = 4000)
  private String lastBbpsResponse;

  @Column(name = "lastRefundResponse", length = 4000)
  private String lastRefundResponse;

  @Column(name = "createDateTime")
  private Long createDateTime;

  @Column(name = "updateDateTime")
  private Long updateDateTime;
}
