package com.dipcoin.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.model.CustomerBillPayRequest;
import com.dipcoin.api.model.PartnerRefundSyncRequest;
import com.dipcoin.api.resource.MerchantSettlementResource;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.BbpsRefundCaseDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.RechargeDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.paymentGateway.PartnerInternalServices;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayPaymentStatusResponse;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayRefundResponse;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayRefundStatusResponse;
import com.dipcoin.partner.paymentGateway.model.InternalCapturedPgTransactionResponse;
import com.dipcoin.partner.utils.ChecksumUtil;
import com.dipcoin.scheduler.constants.BbpsRefundConstants;
import com.dipcoin.scheduler.constants.RechargeConstants;
import com.dipcoin.db.services.model.BbpsRefundCase;
import com.dipcoin.db.services.model.Recharge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("bbpsRefundService")
public class BbpsRefundServiceImpl implements BbpsRefundService {

  private static final Logger LOG = LogManager.getLogger(BbpsRefundServiceImpl.class);
  private static final int DEFAULT_BATCH_SIZE = 200;
  private static final String FINACUS_STATUS_BY_REFERENCE_ID = "REFERENCE_ID";
  private static final String FINACUS_STATUS_BY_TRANSACTION_ID = "TRANSACTION_ID";
  private static final String PAYMENT_NOT_INITIATED_CODE = "PNI";
  private static final String AG_DEBIT_FAILED_CODE = "AGF";
  private static final String PAYMENT_NOT_INITIATED_STATUS = "NOT_INITIATED";
  private static final String PAYMENT_PROCESS_PENDING_MESSAGE = "PAYMENT_PROCESS_PENDING";
  private static final String PAYMENT_NOT_INITIATED_MESSAGE =
      "Customer abandoned payment before Aggrepay initiation";
  private static final String AG_DEBIT_FAILED_MESSAGE =
      "Aggrepay debit failed and bill payment was not called";

  @Value("${com.dipcoin.reconciliation.refundScheduler.orphanGraceMs:600000}")
  private long orphanGraceMs;

  @Value("${com.dipcoin.reconciliation.refundScheduler.noAgEvidenceCloseMs:1800000}")
  private long noAgEvidenceCloseMs;

  @Autowired
  private RechargeDBService rechargeDBService;

  @Autowired
  private BbpsRefundCaseDBService bbpsRefundCaseDBService;

  @Autowired
  private DipcoinDBService coinDBService;

  @Autowired
  private CustomerDBService customerDBService;

  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private PartnerInternalServices partnerInternalServices;

  @Autowired
  private MerchantSettlementResource merchantSettlementResource;

  @Autowired
  private FinacusHttpClient finacusHttpClient;

  @Autowired
  private ChecksumUtil checksumUtil;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  @Lazy
  private BbpsRefundService bbpsRefundServiceProxy;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BbpsRefundCase syncRechargeState(Recharge recharge, String source) {
    return syncRechargeState(recharge, null, source);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BbpsRefundCase syncRechargeState(Recharge recharge, CustomerBillPayRequest billPayRequest,
      String source) {
    if (recharge == null) {
      return null;
    }

    try {
      BbpsRefundCase refundCase = resolveRefundCaseEntity(recharge, billPayRequest);
      if (refundCase == null) {
        return null;
      }
      String originalState = refundCase.getId() != null ? toJson(refundCase) : null;
      mergeRechargeIntoCase(refundCase, recharge, billPayRequest);
      String paymentKey = buildPaymentKey(refundCase);
      if (!StringUtils.equals(refundCase.getPaymentKey(), paymentKey)) {
        refundCase.setPaymentKey(paymentKey);
      }
      
      // Check if a record with this paymentKey already exists (prevent duplicates)
      if (refundCase.getId() == null && StringUtils.isNotBlank(paymentKey)) {
        BbpsRefundCase existingCase = bbpsRefundCaseDBService.getByPaymentKey(paymentKey);
        if (existingCase != null) {
          refundCase = existingCase;
          mergeRechargeIntoCase(refundCase, recharge, billPayRequest);
        }
      }
      
      if (StringUtils.isBlank(refundCase.getRefundStatus())) {
        refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.NEW);
      }
      return saveRefundCaseIfChanged(refundCase, originalState);
    } catch (Exception e) {
      LOG.error(LogFormatter.instance()
          .message("Failed to sync BBPS refund case state")
          .data("rechargeId", recharge.getId())
          .data("dipcoinTransRefId", recharge.getDipcoinTransRefId())
          .data("partnerTransRefId", recharge.getPartnerTransRefId())
          .data("source", source).format(), e);
      return null;
    }
  }

  @Override
  public void triggerRefundAfterCommit(final Integer rechargeId, final String source) {
    if (rechargeId == null || rechargeId <= 0) {
      return;
    }

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          BbpsRefundCase refundCase = getRefundCase(rechargeId, null, null);
          if (refundCase != null) {
            bbpsRefundServiceProxy.processRefundCase(refundCase.getId(), source);
          }
        }
      });
      return;
    }

    BbpsRefundCase refundCase = getRefundCase(rechargeId, null, null);
    if (refundCase != null) {
      bbpsRefundServiceProxy.processRefundCase(refundCase.getId(), source);
    }
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BbpsRefundCase processRefundCase(Integer refundCaseId, String source) {
    if (refundCaseId == null || refundCaseId <= 0) {
      return null;
    }

    BbpsRefundCase refundCase = bbpsRefundCaseDBService.find(refundCaseId);
    if (refundCase == null) {
      return null;
    }

    String originalState = toJson(refundCase);
    try {
      Recharge recharge = resolveRecharge(refundCase);
      if (recharge != null) {
        mergeRechargeIntoCase(refundCase, recharge, null);
      }
      if (StringUtils.isBlank(refundCase.getPaymentKey())) {
        refundCase.setPaymentKey(buildPaymentKey(refundCase));
      }

      refreshCaseSignals(refundCase, recharge);
      String paymentKey = buildPaymentKey(refundCase);
      if (!StringUtils.equals(refundCase.getPaymentKey(), paymentKey)) {
        refundCase.setPaymentKey(paymentKey);
      }

      if (shouldPollRefundStatus(refundCase)) {
        pollAggrepayRefundStatus(refundCase);
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (shouldResetInvalidComplaintManualReview(refundCase, recharge)) {
        resetInvalidComplaintManualReview(refundCase);
      }

      if (shouldMoveToComplaintManualReview(refundCase, recharge)) {
        refundCase.setRefundRequired(Boolean.TRUE);
        moveToManualReview(refundCase, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT,
            buildComplaintManualReviewMessage(recharge));
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isFinalState(refundCase.getRefundStatus())) {
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isLaterDuplicateCase(refundCase)) {
        refundCase.setRefundRequired(Boolean.TRUE);
        refundCase.setRefundReason(BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT);
        if (isAggrepaySettled(refundCase)) {
          initiateAggrepayRefund(refundCase);
        } else if (isAggrepayFailed(refundCase)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED,
              "Duplicate payment was detected, but Aggrepay debit did not succeed");
        } else if (shouldAutoCloseWithoutRefund(refundCase, recharge, source)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE,
              "Duplicate payment was detected, but no Aggrepay debit evidence was found within the scheduler observation window");
        } else {
          refundCase.setRefundRequired(Boolean.FALSE);
          refundCase.setRefundReason(BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT);
          markPending(refundCase, "Duplicate payment detected; waiting for Aggrepay debit confirmation");
        }
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isBillpaySuccess(refundCase)) {
        markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.BILLPAY_SUCCESS,
            "Bill payment successful - no refund required");
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isBillpayFailed(refundCase)) {
        refundCase.setRefundReason(BbpsRefundConstants.RefundReason.BILLPAY_FAILED);
        if (isAggrepaySettled(refundCase)) {
          refundCase.setRefundRequired(Boolean.TRUE);
          initiateAggrepayRefund(refundCase);
        } else if (isAggrepayFailed(refundCase)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED,
              "Bill payment failed, but Aggrepay debit did not succeed");
        } else {
          refundCase.setRefundRequired(Boolean.FALSE);
          if (shouldAutoCloseWithoutRefund(refundCase, recharge, source)) {
            markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE,
                "Bill payment failed, but no Aggrepay debit evidence was found within the scheduler observation window");
          } else {
            markPending(refundCase, "Bill payment failed; waiting for Aggrepay debit confirmation");
          }
        }
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isBillpayNotCalled(refundCase)) {
        if (isAggrepayFailed(refundCase)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED,
              "Aggrepay debit failed and bill payment was not called");
        } else if (isAggrepaySettled(refundCase) && isOrphanWindowElapsed(recharge, refundCase)) {
          refundCase.setRefundRequired(Boolean.TRUE);
          refundCase.setRefundReason(BbpsRefundConstants.RefundReason.ORPHAN_AG_DEBIT);
          initiateAggrepayRefund(refundCase);
        } else if (isAggrepaySettled(refundCase)) {
          markPending(refundCase, "Waiting to confirm whether bill payment was submitted");
        } else if (shouldAutoCloseWithoutRefund(refundCase, recharge, source)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE,
              "No Aggrepay debit evidence was found and bill payment was not submitted within the scheduler observation window");
        } else {
          markPending(refundCase, "Waiting for add-money confirmation");
        }
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isBillpayPending(refundCase)) {
        if (isAggrepayFailed(refundCase)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED,
              "Aggrepay debit failed while bill payment remained pending");
        } else if (shouldAutoCloseWithoutRefund(refundCase, recharge, source)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE,
              "No Aggrepay debit evidence was found while bill payment remained pending within the scheduler observation window");
        } else {
          markPending(refundCase, "Waiting for Finacus transaction status");
        }
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (isAggrepayFailed(refundCase)) {
        markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED,
            "Aggrepay debit failed");
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      if (!hasAggrepayDebitEvidence(refundCase)) {
        if (shouldAutoCloseWithoutRefund(refundCase, recharge, source)) {
          markResolvedWithoutRefund(refundCase, BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE,
              "No Aggrepay debit or bill payment evidence was found within the scheduler observation window");
          return finalizeRefundCase(refundCase, recharge, source, originalState);
        }
        markPending(refundCase, "Waiting for partner transaction confirmation");
        return finalizeRefundCase(refundCase, recharge, source, originalState);
      }

      moveToManualReview(refundCase, BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED,
          "Unable to classify refund case from current AG and Finacus states");
      return finalizeRefundCase(refundCase, recharge, source, originalState);
    } catch (Exception e) {
      LOG.error(LogFormatter.instance()
          .message("Failed to process BBPS refund case")
          .data("refundCaseId", refundCaseId)
          .data("source", source).format(), e);
      moveToManualReview(refundCase, BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED,
          e.getMessage());
      return saveRefundCaseIfChanged(refundCase, originalState);
    }
  }

  @Override
  public void runSchedulerCycle(Long startTime, Long endTime) {
    Set<Integer> processedCaseIds = new HashSet<>();

    List<Recharge> recharges = rechargeDBService.getRechargeByRequestTypeAndStartTimeAndEndTime(
        RechargeConstants.RequestType.SERVICE.value(), startTime, endTime);
    if (CollectionUtils.isNotEmpty(recharges)) {
      for (Recharge recharge : recharges) {
        if (recharge == null) {
          continue;
        }
        BbpsRefundCase refundCase =
            syncRechargeState(recharge, BbpsRefundConstants.StatusSource.SCHEDULER);
        if (refundCase != null && refundCase.getId() != null
            && processedCaseIds.add(refundCase.getId())) {
          bbpsRefundServiceProxy.processRefundCase(refundCase.getId(),
              BbpsRefundConstants.StatusSource.SCHEDULER);
        }
      }
    }

    List<BbpsRefundCase> recentCases =
        bbpsRefundCaseDBService.findRecentCases(startTime, endTime, 0, DEFAULT_BATCH_SIZE);
    if (CollectionUtils.isEmpty(recentCases)) {
      return;
    }

    for (BbpsRefundCase refundCase : recentCases) {
      if (refundCase == null || refundCase.getId() == null
          || !processedCaseIds.add(refundCase.getId())
          || (isFinalState(refundCase.getRefundStatus())
              && !requiresDipcoinCancelFollowUp(refundCase))) {
        continue;
      }
      bbpsRefundServiceProxy.processRefundCase(refundCase.getId(),
          BbpsRefundConstants.StatusSource.SCHEDULER);
    }
  }

  @Override
  public BbpsRefundCase getRefundCase(Integer rechargeId, String clientTransactionId,
      String dipcoinTransRefId) {
    if (rechargeId != null && rechargeId > 0) {
      BbpsRefundCase refundCase = bbpsRefundCaseDBService.getByRechargeId(rechargeId);
      if (refundCase != null) {
        return refundCase;
      }
    }

    if (StringUtils.isNotBlank(dipcoinTransRefId)) {
      BbpsRefundCase refundCase =
          bbpsRefundCaseDBService.getByDipcoinTransRefId(StringUtils.trim(dipcoinTransRefId));
      if (refundCase != null) {
        return refundCase;
      }
    }

    if (StringUtils.isNotBlank(clientTransactionId)) {
      List<BbpsRefundCase> refundCases =
          bbpsRefundCaseDBService.findByIdentifiers(null, null,
              StringUtils.trim(clientTransactionId), null, null, 0, 1);
      if (CollectionUtils.isNotEmpty(refundCases)) {
        return refundCases.get(0);
      }
    }

    return null;
  }

  private BbpsRefundCase resolveRefundCaseEntity(Recharge recharge,
      CustomerBillPayRequest billPayRequest) {
    BbpsRefundCase refundCase = null;
    String resolvedClientTransactionId = resolveClientTransactionId(recharge, billPayRequest);
    String resolvedPartnerTransactionRefId =
        resolvePartnerTransactionReferenceId(recharge, billPayRequest);
    Integer rechargeId = recharge != null ? positiveInteger(recharge.getId()) : null;

    if (rechargeId != null) {
      refundCase = bbpsRefundCaseDBService.getByRechargeId(rechargeId);
    }

    if (refundCase == null && recharge != null && StringUtils.isNotBlank(recharge.getDipcoinTransRefId())) {
      refundCase =
          bbpsRefundCaseDBService.getByDipcoinTransRefId(StringUtils.trim(recharge.getDipcoinTransRefId()));
    }

    if (refundCase == null && StringUtils.isNotBlank(resolvedPartnerTransactionRefId)) {
      refundCase = bbpsRefundCaseDBService.getByPaymentKey(
          "PARTNER:" + StringUtils.trim(resolvedPartnerTransactionRefId));
    }

    if (refundCase == null) {
      refundCase = getRefundCase(null, resolvedClientTransactionId,
          recharge != null ? recharge.getDipcoinTransRefId() : null);
    }

    if (refundCase == null) {
      refundCase = new BbpsRefundCase();
      refundCase.setCreateDateTime(now());
      refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.NEW);
    }

    refundCase.setUpdateDateTime(now());
    return refundCase;
  }


  private void mergeRechargeIntoCase(BbpsRefundCase refundCase, Recharge recharge,
      CustomerBillPayRequest billPayRequest) {
    if (refundCase == null) {
      return;
    }

    if (recharge != null) {
      Integer rechargeId = positiveInteger(recharge.getId());
      if (rechargeId != null) {
        refundCase.setRechargeId(rechargeId);
      }

      Integer rechargeDipcoinId = positiveInteger(recharge.getDipcoinId());
      if (rechargeDipcoinId != null) {
        refundCase.setDipcoinId(rechargeDipcoinId);
      }

      refundCase.setDipcoinTransRefId(
          firstNonBlank(refundCase.getDipcoinTransRefId(), recharge.getDipcoinTransRefId()));
      refundCase.setPartnerTransRefId(
          firstNonBlank(refundCase.getPartnerTransRefId(), recharge.getPartnerTransRefId()));
      refundCase.setClientTransactionId(
          firstNonBlank(refundCase.getClientTransactionId(), recharge.getClientTransactionId()));
      refundCase.setBillPaymentToken(
          firstNonBlank(refundCase.getBillPaymentToken(), recharge.getBillPaymentToken()));
      refundCase.setPaymentRefNo(
          firstNonBlank(refundCase.getPaymentRefNo(), recharge.getPaymentRefNo()));
      refundCase.setCustomerId(firstNonBlank(refundCase.getCustomerId(), recharge.getCustomerId()));
      refundCase.setCustomerMobile(
          firstNonBlank(refundCase.getCustomerMobile(), recharge.getConsumerNo()));
      refundCase.setCustomerAccountNumber(firstNonBlank(refundCase.getCustomerAccountNumber(),
          recharge.getCustomerAccountNumber()));
      refundCase.setBillerId(firstNonBlank(refundCase.getBillerId(), recharge.getBillPaymentsInfo()));
      refundCase.setTxnAmount(firstNonNull(refundCase.getTxnAmount(), recharge.getAmount()));
      refundCase.setRefundAmount(firstNonNull(refundCase.getRefundAmount(), recharge.getAmount()));
      refundCase.setLastBbpsResponse(firstNonBlank(refundCase.getLastBbpsResponse(),
          truncateLargeText(recharge.getRawBBpsTxnStatus())));
    }

    if (billPayRequest != null) {
      refundCase.setBillPaymentToken(firstNonBlank(refundCase.getBillPaymentToken(),
          billPayRequest.getBillPaymentToken()));
      refundCase.setPartnerTransRefId(firstNonBlank(refundCase.getPartnerTransRefId(),
          billPayRequest.getPartnerTransactionReferenceId()));
      refundCase.setCustomerMobile(firstNonBlank(refundCase.getCustomerMobile(),
          billPayRequest.getCustomerMobileNumber()));
      refundCase.setCustomerAccountNumber(firstNonBlank(refundCase.getCustomerAccountNumber(),
          billPayRequest.getCustomerAccountNumber()));
      refundCase.setTxnAmount(firstNonNull(refundCase.getTxnAmount(), billPayRequest.getAmount()));
      refundCase.setRefundAmount(firstNonNull(refundCase.getRefundAmount(), billPayRequest.getAmount()));
      refundCase.setBillerId(firstNonBlank(refundCase.getBillerId(), billPayRequest.getBillerId()));
    }

    try {
      DipcoinTransaction dipcoinTransaction = resolveDipcoinTransaction(recharge, refundCase);
      mergeDipcoinTransactionIntoCase(refundCase, dipcoinTransaction);
      mergeAuthoritativeCapturedPgDataIntoCase(refundCase,
          resolveCapturedPgDipcoinTransaction(recharge, refundCase, dipcoinTransaction));
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Unable to enrich BBPS refund case with DipcoinTransaction captured PG data during recharge sync")
          .data("rechargeId", recharge != null ? recharge.getId() : null)
          .data("refundCaseId", refundCase.getId())
          .data("refundCaseRechargeId", refundCase.getRechargeId())
          .data("refundCaseDipcoinId", refundCase.getDipcoinId())
          .data("refundCaseDipcoinTransRefId", refundCase.getDipcoinTransRefId())
          .data("refundCasePartnerTransRefId", refundCase.getPartnerTransRefId())
          .format(), e);
    }
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BbpsRefundCase syncPartnerRefundUpdate(PartnerRefundSyncRequest request) {
    if (request == null || !request.isValid()) {
      return null;
    }

    List<BbpsRefundCase> refundCases =
        bbpsRefundCaseDBService.findByIdentifiers(null, null, null, null,
            StringUtils.trim(request.getOrderId()), 0, 10);
    if (CollectionUtils.isEmpty(refundCases)) {
      return null;
    }

    BbpsRefundCase refundCase = selectPartnerRefundSyncTarget(refundCases);
    if (refundCase == null) {
      return null;
    }

    String originalState = toJson(refundCase);
    applyPartnerRefundSync(refundCase, request);
    return finalizeRefundCase(refundCase, resolveRecharge(refundCase),
        BbpsRefundConstants.StatusSource.ADMIN, originalState);
  }

  private void mergeDipcoinTransactionIntoCase(BbpsRefundCase refundCase,
      DipcoinTransaction dipcoinTransaction) {
    if (refundCase == null || dipcoinTransaction == null) {
      return;
    }

    Integer dipcoinId = positiveInteger(dipcoinTransaction.getDipcoinId());
    if (dipcoinId != null) {
      refundCase.setDipcoinId(dipcoinId);
    }
    refundCase.setDipcoinTransRefId(firstNonBlank(refundCase.getDipcoinTransRefId(),
        dipcoinTransaction.getDipcoinTransactionRefId()));
    refundCase.setPartnerTransRefId(firstNonBlank(refundCase.getPartnerTransRefId(),
        dipcoinTransaction.getPartnerTransactionReferenceId()));
    refundCase.setCapturedPgTransactionId(firstNonBlank(refundCase.getCapturedPgTransactionId(),
        dipcoinTransaction.getCapturedPgTransactionId()));
    refundCase.setCapturedPgPaymentMode(firstNonBlank(refundCase.getCapturedPgPaymentMode(),
        dipcoinTransaction.getCapturedPgPaymentMode()));
    refundCase.setTxnAmount(firstNonNull(refundCase.getTxnAmount(), dipcoinTransaction.getAmount()));
    refundCase.setRefundAmount(firstNonNull(refundCase.getRefundAmount(), dipcoinTransaction.getAmount()));
    if (StringUtils.isBlank(refundCase.getCustomerMobile())) {
      refundCase.setCustomerMobile(resolveCustomerMobileFromDipcoinTransaction(dipcoinTransaction));
    }
    Integer customerAccountId = positiveInteger(dipcoinTransaction.getCustomerAccountId());
    if (StringUtils.isBlank(refundCase.getCustomerAccountNumber())
        && customerAccountId != null) {
      CustomerAccount customerAccount =
          customerDBService.getAccountById(customerAccountId);
      if (customerAccount != null) {
        refundCase.setCustomerAccountNumber(firstNonBlank(refundCase.getCustomerAccountNumber(),
            customerAccount.getAccountNumber()));
      }
    }
  }

  private Recharge resolveRecharge(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return null;
    }

    if (refundCase.getRechargeId() != null && refundCase.getRechargeId() > 0) {
      Recharge recharge = rechargeDBService.find(refundCase.getRechargeId());
      if (recharge != null) {
        return recharge;
      }
    }

    if (StringUtils.isNotBlank(refundCase.getDipcoinTransRefId())) {
      Recharge recharge =
          rechargeDBService.getTransactionStatus(StringUtils.trim(refundCase.getDipcoinTransRefId()));
      if (recharge != null) {
        return recharge;
      }
    }

    if (StringUtils.isNotBlank(refundCase.getBillPaymentToken())) {
      Recharge recharge =
          rechargeDBService.getBillDetail(StringUtils.trim(refundCase.getBillPaymentToken()));
      if (recharge != null) {
        return recharge;
      }
    }

    if (StringUtils.isNotBlank(refundCase.getPartnerTransRefId())) {
      Recharge recharge =
          rechargeDBService.getByPartnerTxnRefId(StringUtils.trim(refundCase.getPartnerTransRefId()));
      if (recharge != null) {
        return recharge;
      }
    }

    if (StringUtils.isNotBlank(refundCase.getClientTransactionId())) {
      return rechargeDBService.getByClientTransactionId(StringUtils.trim(refundCase.getClientTransactionId()));
    }

    return null;
  }

  private DipcoinTransaction resolveDipcoinTransaction(Recharge recharge, BbpsRefundCase refundCase) {
    List<DipcoinTransaction> candidateTransactions = new ArrayList<>();

    if (recharge != null && StringUtils.isNotBlank(recharge.getDipcoinTransRefId())) {
      addDipcoinTransactionCandidate(candidateTransactions,
          coinDBService.getTransactionByOstaTransactionRefId(recharge.getDipcoinTransRefId()));
    }

    if (refundCase != null && StringUtils.isNotBlank(refundCase.getDipcoinTransRefId())) {
      addDipcoinTransactionCandidate(candidateTransactions,
          coinDBService.getTransactionByOstaTransactionRefId(refundCase.getDipcoinTransRefId()));
    }

    List<String> partnerTransactionReferenceIds =
        collectPartnerTransactionReferenceIds(recharge, refundCase);
    if (CollectionUtils.isNotEmpty(partnerTransactionReferenceIds)) {
      addDipcoinTransactionCandidates(candidateTransactions,
          coinDBService.findDipcoinTransactionByPartnerTransactionReferenceId(
              partnerTransactionReferenceIds));
    }

    DipcoinTransaction bestMatch =
        selectBestOriginalDipcoinTransaction(candidateTransactions, recharge, refundCase);
    if (isValidDipcoinCancellationSource(bestMatch)) {
      return bestMatch;
    }

    Integer dipcoinId = resolveDipcoinId(recharge, refundCase, bestMatch);
    if (dipcoinId != null) {
      addDipcoinTransactionCandidates(candidateTransactions,
          coinDBService.getTransactions(Collections.singletonList(dipcoinId), null, 0, 100));
    }

    return selectBestOriginalDipcoinTransaction(candidateTransactions, recharge, refundCase);
  }

  private void addDipcoinTransactionCandidate(List<DipcoinTransaction> candidates,
      DipcoinTransaction candidate) {
    if (candidate != null) {
      candidates.add(candidate);
    }
  }

  private void addDipcoinTransactionCandidates(List<DipcoinTransaction> candidates,
      List<DipcoinTransaction> newCandidates) {
    if (CollectionUtils.isEmpty(newCandidates)) {
      return;
    }
    for (DipcoinTransaction candidate : newCandidates) {
      addDipcoinTransactionCandidate(candidates, candidate);
    }
  }

  private List<String> collectPartnerTransactionReferenceIds(Recharge recharge,
      BbpsRefundCase refundCase) {
    Set<String> partnerTransactionReferenceIds = new LinkedHashSet<>();
    addPartnerTransactionReferenceId(partnerTransactionReferenceIds,
        extractPartnerTransactionReferenceIdFromRecharge(recharge));
    addPartnerTransactionReferenceId(partnerTransactionReferenceIds,
        recharge != null ? recharge.getPartnerTransRefId() : null);
    addPartnerTransactionReferenceId(partnerTransactionReferenceIds,
        refundCase != null ? refundCase.getPartnerTransRefId() : null);
    return new ArrayList<>(partnerTransactionReferenceIds);
  }

  private void addPartnerTransactionReferenceId(Set<String> partnerTransactionReferenceIds,
      String partnerTransactionReferenceId) {
    String normalizedValue = StringUtils.trimToNull(partnerTransactionReferenceId);
    if (normalizedValue != null) {
      partnerTransactionReferenceIds.add(normalizedValue);
    }
  }

  private String extractPartnerTransactionReferenceIdFromRecharge(Recharge recharge) {
    if (recharge == null) {
      return null;
    }

    JsonNode rawRequestNode = readJsonNodeQuietly(recharge.getRawRequest());
    if (rawRequestNode == null || !rawRequestNode.hasNonNull("partnerTransactionReferenceId")) {
      return null;
    }

    return StringUtils.trimToNull(rawRequestNode.path("partnerTransactionReferenceId").asText(null));
  }

  private Integer resolveDipcoinId(Recharge recharge, BbpsRefundCase refundCase,
      DipcoinTransaction bestMatch) {
    Integer dipcoinId = recharge != null ? positiveInteger(recharge.getDipcoinId()) : null;
    if (dipcoinId == null && refundCase != null && refundCase.getDipcoinId() != null
        && refundCase.getDipcoinId() > 0) {
      dipcoinId = refundCase.getDipcoinId();
    }
    if (dipcoinId == null && bestMatch != null) {
      dipcoinId = positiveInteger(bestMatch.getDipcoinId());
    }
    return dipcoinId;
  }

  @Override
  public String getRefundRawReason(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return null;
    }
    String storedReason = StringUtils.trimToNull(refundCase.getRefundRawReason());
    if (StringUtils.isBlank(storedReason)) {
      storedReason = truncateRawReasonText(buildRefundRawReasonText(refundCase));
    }
    return appendDipcoinCancelContext(storedReason, refundCase);
  }

  private DipcoinTransaction selectBestOriginalDipcoinTransaction(
      List<DipcoinTransaction> dipcoinTransactions, Recharge recharge, BbpsRefundCase refundCase) {
    if (CollectionUtils.isEmpty(dipcoinTransactions)) {
      return null;
    }

    String expectedOrderId = StringUtils.trimToNull(refundCase != null ? refundCase.getOrderId() : null);
    String expectedPartnerReferenceId =
        StringUtils.trimToNull(recharge != null ? recharge.getPartnerReferenceId() : null);

    DipcoinTransaction bestMatch = null;
    int bestScore = Integer.MIN_VALUE;
    for (DipcoinTransaction dipcoinTransaction : dipcoinTransactions) {
      if (dipcoinTransaction == null) {
        continue;
      }

      int score = 0;
      if (isValidDipcoinCancellationSource(dipcoinTransaction)) {
        score += 4;
      }
      if (StringUtils.isNotBlank(expectedOrderId)
          && StringUtils.equals(StringUtils.trimToEmpty(dipcoinTransaction.getOrderId()),
              expectedOrderId)) {
        score += 2;
      }
      if (StringUtils.isNotBlank(expectedPartnerReferenceId)
          && StringUtils.equals(StringUtils.trimToEmpty(dipcoinTransaction.getPartnerReferenceId()),
              expectedPartnerReferenceId)) {
        score += 1;
      }

      if (bestMatch == null || score > bestScore
          || (score == bestScore && dipcoinTransaction.getId() > bestMatch.getId())) {
        bestMatch = dipcoinTransaction;
        bestScore = score;
      }
    }

    return bestMatch;
  }

  private void refreshCaseSignals(BbpsRefundCase refundCase, Recharge recharge) {
    if (refundCase == null) {
      return;
    }

    enrichCapturedPgData(refundCase);
    refreshAggrepayPaymentStatus(refundCase);

    boolean billpayCalled = determineBillpayCalled(refundCase, recharge);
    refundCase.setBillpayCalled(Boolean.valueOf(billpayCalled));
    if (!billpayCalled) {
      refundCase.setBillpayStatus(BbpsRefundConstants.BillpayStatus.NOT_CALLED);
      refundCase.setBillpayStatusSource(BbpsRefundConstants.StatusSource.INTERNAL);
    } else {
      refreshBillpayStatus(refundCase, recharge);
      if (StringUtils.isBlank(refundCase.getBillpayStatus())) {
        refundCase.setBillpayStatus(deriveBillpayStatus(recharge, refundCase));
      }
    }

    if (hasAggrepayDebitEvidence(refundCase)
        && StringUtils.isBlank(refundCase.getAgStatus())) {
      refundCase.setAgStatus(BbpsRefundConstants.AgStatus.DEBITED);
    } else if (!hasAggrepayDebitEvidence(refundCase)
        && StringUtils.isBlank(refundCase.getAgStatus())) {
      refundCase.setAgStatus(BbpsRefundConstants.AgStatus.NOT_FOUND);
    }
  }

  private JsonNode readJsonNodeQuietly(String payload) {
    if (StringUtils.isBlank(payload)) {
      return null;
    }
    try {
      return objectMapper.readTree(payload);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private String jsonText(JsonNode jsonNode, String fieldName) {
    if (jsonNode == null || StringUtils.isBlank(fieldName) || !jsonNode.has(fieldName)
        || jsonNode.get(fieldName).isNull()) {
      return null;
    }
    return StringUtils.trimToNull(jsonNode.get(fieldName).asText());
  }

  private boolean enrichCapturedPgData(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return false;
    }

    Recharge recharge = resolveRecharge(refundCase);
    DipcoinTransaction dipcoinTransaction = resolveDipcoinTransaction(recharge, refundCase);
    boolean changed = mergeAuthoritativeCapturedPgDataIntoCase(refundCase,
        resolveCapturedPgDipcoinTransaction(recharge, refundCase, dipcoinTransaction));

    if (hasAggrepayDebitEvidence(refundCase)
        && StringUtils.isNotBlank(refundCase.getOrderId())
        && StringUtils.isNotBlank(refundCase.getLastAgResponse())) {
      return changed;
    }
    if (StringUtils.isBlank(refundCase.getPartnerTransRefId())) {
      return changed;
    }

    InternalCapturedPgTransactionResponse capturedResponse =
        partnerInternalServices.getCapturedPgTransactionDetails(refundCase.getPartnerTransRefId(),
            null);
    if (capturedResponse == null) {
      return changed;
    }

    String capturedPgTransactionId = StringUtils.trimToNull(capturedResponse.getCapturedTransactionId());
    if (StringUtils.isBlank(capturedPgTransactionId)) {
      capturedPgTransactionId = refundCase.getCapturedPgTransactionId();
    }
    if (!StringUtils.equals(refundCase.getCapturedPgTransactionId(), capturedPgTransactionId)) {
      refundCase.setCapturedPgTransactionId(capturedPgTransactionId);
      changed = true;
    }

    String paymentMode = StringUtils.trimToNull(capturedResponse.getPaymentMode());
    if (StringUtils.isBlank(paymentMode)) {
      paymentMode = refundCase.getCapturedPgPaymentMode();
    }
    if (!StringUtils.equals(refundCase.getCapturedPgPaymentMode(), paymentMode)) {
      refundCase.setCapturedPgPaymentMode(paymentMode);
      changed = true;
    }

    String partnerOrderId = StringUtils.trimToNull(capturedResponse.getOrderId());
    if (!StringUtils.equals(refundCase.getOrderId(), partnerOrderId)) {
      refundCase.setOrderId(partnerOrderId);
      changed = true;
    }

    if (changed) {
      refundCase.setAgEvidenceFound(Boolean.TRUE);
      refundCase.setLastAgResponse(truncateLargeText(toJson(capturedResponse)));
      refundCase.setAgLastCheckedAt(now());
    }
    return changed;
  }

  private DipcoinTransaction resolveCapturedPgDipcoinTransaction(Recharge recharge,
      BbpsRefundCase refundCase, DipcoinTransaction anchorTransaction) {
    if (hasCapturedPgData(anchorTransaction)) {
      return anchorTransaction;
    }

    DipcoinTransaction walletScopedCapturedPgTransaction =
        resolveCapturedPgDipcoinTransactionByWalletContext(anchorTransaction, recharge, refundCase);
    if (walletScopedCapturedPgTransaction != null) {
      return walletScopedCapturedPgTransaction;
    }

    DipcoinTransaction rechargeScopedCapturedPgTransaction =
        resolveCapturedPgDipcoinTransactionByRechargeContext(recharge, refundCase);
    if (rechargeScopedCapturedPgTransaction != null) {
      return rechargeScopedCapturedPgTransaction;
    }

    for (String partnerTransactionReferenceId : new String[] {
        anchorTransaction != null
            ? StringUtils.trimToNull(anchorTransaction.getPartnerTransactionReferenceId())
            : null,
        recharge != null ? StringUtils.trimToNull(recharge.getPartnerTransRefId()) : null,
        refundCase != null ? StringUtils.trimToNull(refundCase.getPartnerTransRefId()) : null}) {
      if (StringUtils.isBlank(partnerTransactionReferenceId)) {
        continue;
      }
      try {
        List<DipcoinTransaction> dipcoinTransactions = coinDBService
            .findDipcoinTransactionByPartnerTransactionReferenceId(
                Collections.singletonList(partnerTransactionReferenceId));
        DipcoinTransaction capturedPgDipcoinTransaction =
            selectBestCapturedPgDipcoinTransaction(dipcoinTransactions, recharge, refundCase);
        if (capturedPgDipcoinTransaction != null) {
          return capturedPgDipcoinTransaction;
        }
      } catch (Exception e) {
        LOG.warn(LogFormatter.instance()
            .message("Unable to resolve DipcoinTransaction captured PG data by partnerTransactionReferenceId for BBPS refund flow")
            .data("partnerTransactionReferenceId", partnerTransactionReferenceId).format(), e);
      }
    }

    return null;
  }

  private BbpsRefundCase selectPartnerRefundSyncTarget(List<BbpsRefundCase> refundCases) {
    if (CollectionUtils.isEmpty(refundCases)) {
      return null;
    }

    for (BbpsRefundCase refundCase : refundCases) {
      if (refundCase != null && (Boolean.TRUE.equals(refundCase.getManualReviewRequired())
          || Boolean.TRUE.equals(refundCase.getAdminTriggerRequired()))) {
        return refundCase;
      }
    }

    for (BbpsRefundCase refundCase : refundCases) {
      if (refundCase != null && Boolean.TRUE.equals(refundCase.getRefundRequired())
          && !StringUtils.equals(refundCase.getRefundStatus(),
              BbpsRefundConstants.RefundStatus.SUCCESS)) {
        return refundCase;
      }
    }

    return refundCases.get(0);
  }

  private void applyPartnerRefundSync(BbpsRefundCase refundCase, PartnerRefundSyncRequest request) {
    if (refundCase == null || request == null) {
      return;
    }

    Long actionTime = request.getAdminActionAt() != null && request.getAdminActionAt().longValue() > 0L
        ? request.getAdminActionAt() : now();
    refundCase.setRefundRequired(Boolean.TRUE);
    if (StringUtils.isBlank(refundCase.getRefundReason())) {
      refundCase.setRefundReason(BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT);
    }
    ensureRefundRawReasonForRefundDecision(refundCase);
    refundCase.setAdminActionBy(firstNonBlank(request.getAdminActionBy(), refundCase.getAdminActionBy(),
        BbpsRefundConstants.StatusSource.ADMIN));
    refundCase.setAdminActionAt(actionTime);
    refundCase.setRefundReferenceId(firstNonBlank(request.getRefundReferenceId(),
        refundCase.getRefundReferenceId()));
    refundCase.setRefundResponseCode(firstNonBlank(request.getRefundResponseCode(),
        refundCase.getRefundResponseCode()));
    refundCase.setRefundResponseMessage(truncateShortText(firstNonBlank(
        request.getRefundResponseMessage(), refundCase.getRefundResponseMessage())));
    if (StringUtils.isNotBlank(request.getProviderRefundStatus())) {
      syncProviderRefundStatus(refundCase, request.getProviderRefundStatus());
    }
    if (StringUtils.isNotBlank(request.getRawResponse())) {
      refundCase.setLastRefundResponse(truncateLargeText(request.getRawResponse()));
    }

    if (StringUtils.equalsIgnoreCase(request.getSyncType(), "STATUS")) {
      refundCase.setProviderRefundCheckedAt(actionTime);
    } else if (refundCase.getRefundInitiatedAt() == null) {
      refundCase.setRefundInitiatedAt(actionTime);
    }

    String effectiveStatus =
        firstNonBlank(request.getRefundStatus(), request.getProviderRefundStatus(),
            StringUtils.equalsIgnoreCase(request.getSyncType(), "INITIATE")
                ? BbpsRefundConstants.RefundStatus.PENDING : null);
    String mappedStatus = mapRefundStatus(effectiveStatus);
    String message = firstNonBlank(request.getRefundResponseMessage(),
        "Partner refund state synced");

    if (BbpsRefundConstants.RefundStatus.SUCCESS.equals(mappedStatus)) {
      markRefundSuccess(refundCase, message);
      refundCase.setRefundCompletedAt(actionTime);
      return;
    }
    if (BbpsRefundConstants.RefundStatus.PENDING.equals(mappedStatus)) {
      markRefundPending(refundCase, message);
      if (refundCase.getRefundInitiatedAt() == null) {
        refundCase.setRefundInitiatedAt(actionTime);
      }
      return;
    }

    moveToManualReview(refundCase,
        StringUtils.defaultIfBlank(refundCase.getRefundReason(),
            BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT),
        message);
  }

  private DipcoinTransaction resolveCapturedPgDipcoinTransactionByWalletContext(
      DipcoinTransaction anchorTransaction, Recharge recharge, BbpsRefundCase refundCase) {
    Integer dipcoinId = anchorTransaction != null ? positiveInteger(anchorTransaction.getDipcoinId()) : null;
    if (dipcoinId == null) {
      return null;
    }

    try {
      List<DipcoinTransaction> dipcoinTransactions = coinDBService
          .getTransactions(Collections.singletonList(dipcoinId), null, 0, 100);
      if (CollectionUtils.isEmpty(dipcoinTransactions)) {
        return null;
      }

      DipcoinTransaction walletScopedCapturedPgTransaction = dipcoinTransactions.stream()
          .filter(this::hasCapturedPgData)
          .filter(dipcoinTransaction ->
              dipcoinTransaction.getCustomerAccountId() == anchorTransaction.getCustomerAccountId())
          .filter(dipcoinTransaction -> matchesRefundCaseCustomer(dipcoinTransaction, recharge, refundCase))
          .filter(dipcoinTransaction -> isTransactionAtOrBeforeAnchor(dipcoinTransaction, anchorTransaction))
          .max(this::compareCapturedPgDipcoinTransactions).orElse(null);
      if (walletScopedCapturedPgTransaction != null) {
        return walletScopedCapturedPgTransaction;
      }

      return dipcoinTransactions.stream().filter(this::hasCapturedPgData)
          .filter(dipcoinTransaction ->
              dipcoinTransaction.getCustomerAccountId() == anchorTransaction.getCustomerAccountId())
          .filter(dipcoinTransaction -> isTransactionAtOrBeforeAnchor(dipcoinTransaction, anchorTransaction))
          .max(this::compareCapturedPgDipcoinTransactions).orElse(null);
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Unable to resolve DipcoinTransaction captured PG data by wallet context for BBPS refund flow")
          .data("anchorDipcoinTransactionId", anchorTransaction.getId())
          .data("anchorDipcoinTransactionRefId", anchorTransaction.getDipcoinTransactionRefId())
          .data("anchorDipcoinId", anchorTransaction.getDipcoinId())
          .data("anchorCustomerAccountId", anchorTransaction.getCustomerAccountId()).format(), e);
      return null;
    }
  }

  private DipcoinTransaction resolveCapturedPgDipcoinTransactionByRechargeContext(Recharge recharge,
      BbpsRefundCase refundCase) {
    Integer dipcoinId = null;
    if (recharge != null) {
      dipcoinId = positiveInteger(recharge.getDipcoinId());
    } else if (refundCase != null && refundCase.getDipcoinId() != null && refundCase.getDipcoinId() > 0) {
      dipcoinId = refundCase.getDipcoinId();
    }
    if (dipcoinId == null || dipcoinId.intValue() <= 0) {
      return null;
    }

    try {
      List<DipcoinTransaction> dipcoinTransactions =
          coinDBService.getTransactions(Collections.singletonList(dipcoinId), null, 0, 100);
      if (CollectionUtils.isEmpty(dipcoinTransactions)) {
        return null;
      }

      DipcoinTransaction rechargeScopedCapturedPgTransaction = dipcoinTransactions.stream()
          .filter(this::hasCapturedPgData)
          .filter(dipcoinTransaction -> matchesRefundCaseCustomer(dipcoinTransaction, recharge, refundCase))
          .max(this::compareCapturedPgDipcoinTransactions).orElse(null);
      if (rechargeScopedCapturedPgTransaction != null) {
        return rechargeScopedCapturedPgTransaction;
      }

      return dipcoinTransactions.stream().filter(this::hasCapturedPgData)
          .max(this::compareCapturedPgDipcoinTransactions).orElse(null);
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Unable to resolve DipcoinTransaction captured PG data by recharge context for BBPS refund flow")
          .data("dipcoinId", dipcoinId)
          .data("customerId", recharge != null ? recharge.getCustomerId()
              : refundCase != null ? refundCase.getCustomerId() : null).format(), e);
      return null;
    }
  }

  private DipcoinTransaction selectBestCapturedPgDipcoinTransaction(
      List<DipcoinTransaction> dipcoinTransactions, Recharge recharge, BbpsRefundCase refundCase) {
    if (CollectionUtils.isEmpty(dipcoinTransactions)) {
      return null;
    }

    DipcoinTransaction sameCustomerTransaction = dipcoinTransactions.stream()
        .filter(this::hasCapturedPgData)
        .filter(dipcoinTransaction -> matchesRefundCaseCustomer(dipcoinTransaction, recharge, refundCase))
        .max(this::compareCapturedPgDipcoinTransactions).orElse(null);
    if (sameCustomerTransaction != null) {
      return sameCustomerTransaction;
    }

    return dipcoinTransactions.stream().filter(this::hasCapturedPgData)
        .max(this::compareCapturedPgDipcoinTransactions).orElse(null);
  }

  private boolean mergeAuthoritativeCapturedPgDataIntoCase(BbpsRefundCase refundCase,
      DipcoinTransaction capturedPgDipcoinTransaction) {
    if (refundCase == null || !hasCapturedPgData(capturedPgDipcoinTransaction)) {
      return false;
    }

    boolean changed = false;
    String partnerTransactionReferenceId = StringUtils.trimToNull(
        capturedPgDipcoinTransaction.getPartnerTransactionReferenceId());
    if (!StringUtils.equals(refundCase.getPartnerTransRefId(), partnerTransactionReferenceId)
        && StringUtils.isNotBlank(partnerTransactionReferenceId)) {
      refundCase.setPartnerTransRefId(partnerTransactionReferenceId);
      changed = true;
    }

    String partnerOrderId = StringUtils.trimToNull(capturedPgDipcoinTransaction.getOrderId());
    if (!StringUtils.equals(refundCase.getOrderId(), partnerOrderId)
        && StringUtils.isNotBlank(partnerOrderId)) {
      refundCase.setOrderId(partnerOrderId);
      changed = true;
    }

    String capturedPgTransactionId = StringUtils.trimToNull(
        capturedPgDipcoinTransaction.getCapturedPgTransactionId());
    if (!StringUtils.equals(refundCase.getCapturedPgTransactionId(), capturedPgTransactionId)
        && StringUtils.isNotBlank(capturedPgTransactionId)) {
      refundCase.setCapturedPgTransactionId(capturedPgTransactionId);
      changed = true;
    }

    String capturedPgPaymentMode = StringUtils.trimToNull(
        capturedPgDipcoinTransaction.getCapturedPgPaymentMode());
    if (!StringUtils.equals(refundCase.getCapturedPgPaymentMode(), capturedPgPaymentMode)
        && StringUtils.isNotBlank(capturedPgPaymentMode)) {
      refundCase.setCapturedPgPaymentMode(capturedPgPaymentMode);
      changed = true;
    }

    if (changed || Boolean.FALSE.equals(refundCase.getAgEvidenceFound())
        || refundCase.getAgEvidenceFound() == null) {
      refundCase.setAgEvidenceFound(Boolean.TRUE);
      if (StringUtils.isBlank(refundCase.getAgStatus())) {
        refundCase.setAgStatus(BbpsRefundConstants.AgStatus.DEBITED);
      }
    }

    return changed;
  }

  private void refreshAggrepayPaymentStatus(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return;
    }

    if (StringUtils.isBlank(refundCase.getOrderId())) {
      enrichCapturedPgData(refundCase);
    }
    if (StringUtils.isBlank(refundCase.getOrderId())) {
      return;
    }

    InternalAggrepayPaymentStatusResponse paymentStatusResponse =
        partnerInternalServices.getAggrepayPaymentStatus(refundCase.getOrderId());
    if (paymentStatusResponse == null) {
      if (!hasAggrepayDebitEvidence(refundCase)
          && StringUtils.isBlank(refundCase.getAgStatus())) {
        refundCase.setAgStatus(BbpsRefundConstants.AgStatus.NOT_FOUND);
      }
      return;
    }

    refundCase.setAgStatus(mapAggrepayPaymentStatus(paymentStatusResponse.getStatus()));
    refundCase.setAgEvidenceFound(Boolean.valueOf(
        hasAggrepayDebitEvidence(refundCase)
            || isAggrepaySettled(paymentStatusResponse.getStatus())));
    refundCase.setAgStatusSource(BbpsRefundConstants.StatusSource.AGGREPAY);
    refundCase.setAgLastCheckedAt(now());
    refundCase.setLastAgResponse(truncateLargeText(toJson(paymentStatusResponse)));
    refundCase.setCapturedPgTransactionId(firstNonBlank(refundCase.getCapturedPgTransactionId(),
        paymentStatusResponse.getCapturedPgTransactionId()));
  }

  private void refreshBillpayStatus(BbpsRefundCase refundCase, Recharge recharge) {
    if (refundCase == null) {
      return;
    }

    if (isBillpayFailureIndicatedByRecharge(recharge)) {
      applyInternalBillpayFailure(refundCase, recharge);
      return;
    }

    String currentStatus = deriveBillpayStatus(recharge, refundCase);
    if (!BbpsRefundConstants.BillpayStatus.PENDING.equals(currentStatus)
        && !BbpsRefundConstants.BillpayStatus.UNKNOWN.equals(currentStatus)) {
      refundCase.setBillpayStatus(currentStatus);
      refundCase.setBillpayStatusSource(BbpsRefundConstants.StatusSource.INTERNAL);
      return;
    }

    FinacusStatus finacusStatus = fetchFinacusStatus(refundCase, recharge);
    if (finacusStatus == null) {
      refundCase.setBillpayStatus(currentStatus);
      return;
    }

    String resolvedStatus = finacusStatus.status;
    String resolvedSource = BbpsRefundConstants.StatusSource.FINACUS;
    if (BbpsRefundConstants.BillpayStatus.UNKNOWN.equals(resolvedStatus)
        && isNoTransactionFoundFinacusResponse(finacusStatus.rawResponse)
        && determineBillpayCalled(refundCase, recharge)) {
      resolvedStatus = BbpsRefundConstants.BillpayStatus.FAILED;
    }

    refundCase.setBillpayStatus(resolvedStatus);
    refundCase.setBillpayStatusSource(resolvedSource);
    refundCase.setBillpayLastCheckedAt(now());
    refundCase.setLastBbpsResponse(truncateLargeText(finacusStatus.rawResponse));

    if (recharge != null && StringUtils.isNotBlank(finacusStatus.rawResponse)) {
      recharge.setBbpsTxnStatus(resolvedStatus);
      recharge.setRawBBpsTxnStatus(finacusStatus.rawResponse);
      recharge.setUpdateDateTime(String.valueOf(now()));
      rechargeDBService.updateRecharge(recharge);
    }
  }

  private void applyInternalBillpayFailure(BbpsRefundCase refundCase, Recharge recharge) {
    long checkedAt = now();
    String failureEvidence = buildInternalBillpayFailureEvidence(recharge);

    refundCase.setBillpayStatus(BbpsRefundConstants.BillpayStatus.FAILED);
    refundCase.setBillpayStatusSource(BbpsRefundConstants.StatusSource.INTERNAL);
    refundCase.setBillpayLastCheckedAt(checkedAt);
    if (StringUtils.isNotBlank(failureEvidence)) {
      refundCase.setLastBbpsResponse(truncateLargeText(failureEvidence));
    }

    if (recharge == null) {
      return;
    }

    boolean rechargeChanged = false;
    if (!StringUtils.equals(recharge.getBbpsTxnStatus(), BbpsRefundConstants.BillpayStatus.FAILED)) {
      recharge.setBbpsTxnStatus(BbpsRefundConstants.BillpayStatus.FAILED);
      rechargeChanged = true;
    }
    if (StringUtils.isNotBlank(failureEvidence)
        && !StringUtils.equals(recharge.getRawBBpsTxnStatus(), failureEvidence)) {
      recharge.setRawBBpsTxnStatus(failureEvidence);
      rechargeChanged = true;
    }
    if (rechargeChanged) {
      recharge.setUpdateDateTime(String.valueOf(checkedAt));
      rechargeDBService.updateRecharge(recharge);
    }
  }

  private String buildInternalBillpayFailureEvidence(Recharge recharge) {
    if (recharge == null) {
      return null;
    }
    if (StringUtils.isNotBlank(recharge.getRawBBpsTxnStatus())) {
      return recharge.getRawBBpsTxnStatus();
    }
    if (StringUtils.isNotBlank(recharge.getRawResponse())) {
      return recharge.getRawResponse();
    }

    Map<String, Object> failureEvidence = new LinkedHashMap<>();
    failureEvidence.put("statusSource", BbpsRefundConstants.StatusSource.INTERNAL);
    failureEvidence.put("status", recharge.getStatus());
    if (StringUtils.isNotBlank(recharge.getResponseCode())) {
      failureEvidence.put("responseCode", recharge.getResponseCode());
    }
    if (StringUtils.isNotBlank(recharge.getResponseMessage())) {
      failureEvidence.put("responseMessage", recharge.getResponseMessage());
    }
    if (failureEvidence.size() == 2) {
      return null;
    }
    return toJson(failureEvidence);
  }

  private FinacusStatus fetchFinacusStatus(BbpsRefundCase refundCase, Recharge recharge) {
    String statusBy = resolveFinacusStatusBy(refundCase, recharge);
    String statusParam = resolveFinacusStatusParam(refundCase, recharge);
    if (StringUtils.isAnyBlank(statusBy, statusParam)) {
      return null;
    }

    try {
      String checksum = checksumUtil.generateChecksum(statusBy + "|" + statusParam);
      String rawResponse = finacusHttpClient.getTransactionStatus(statusBy, statusParam, checksum);
      String finacusStatus = mapFinacusStatus(extractFinacusStatus(rawResponse));
      if (StringUtils.isBlank(finacusStatus)) {
        finacusStatus = BbpsRefundConstants.BillpayStatus.UNKNOWN;
      }
      return new FinacusStatus(finacusStatus, rawResponse);
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to fetch Finacus BBPS status for refund decision")
          .data("refundCaseId", refundCase != null ? refundCase.getId() : null)
          .data("statusBy", statusBy)
          .data("statusParam", statusParam).format(), e);
      return null;
    }
  }

  private void initiateAggrepayRefund(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return;
    }
    if (!isAggrepaySettled(refundCase)) {
      moveToManualReview(refundCase, StringUtils.defaultIfBlank(refundCase.getRefundReason(),
          BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED),
          "Refund requested before add-money success was confirmed");
      return;
    }
    if (StringUtils.isBlank(refundCase.getOrderId()) || refundCase.getRefundAmount() == null) {
      moveToManualReview(refundCase, StringUtils.defaultIfBlank(refundCase.getRefundReason(),
          BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED),
          "Aggrepay refund requires partner orderId and amount");
      return;
    }

    ensureRefundRawReasonForRefundDecision(refundCase);
    InternalAggrepayRefundResponse refundResponse = partnerInternalServices.initiateAggrepayRefund(
        refundCase.getOrderId(), refundCase.getRefundAmount(), refundCase.getRefundReason());
    refundCase.setLastRefundResponse(truncateLargeText(toJson(refundResponse)));

    if (refundResponse == null) {
      moveToManualReview(refundCase, StringUtils.defaultIfBlank(refundCase.getRefundReason(),
          BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED),
          "Aggrepay refund initiation returned no response");
      return;
    }

    refundCase.setRefundRequired(Boolean.TRUE);
    refundCase.setRefundReferenceId(firstNonBlank(refundCase.getRefundReferenceId(),
        refundResponse.getRefundId(), refundResponse.getRefundReferenceNo()));
    syncProviderRefundStatus(refundCase, refundResponse.getStatus());
    boolean alreadyInitiated = isRefundAlreadyInitiatedResponse(refundResponse.getStatus(),
        refundResponse.getResponseCode(), refundResponse.getMessage());
    refundCase.setRefundResponseCode(firstNonBlank(refundCase.getRefundResponseCode(),
        refundResponse.getResponseCode()));
    if (!alreadyInitiated || StringUtils.isBlank(refundCase.getRefundResponseMessage())) {
      refundCase.setRefundResponseMessage(truncateShortText(refundResponse.getMessage()));
    }
    refundCase.setRefundInitiatedAt(
        refundCase.getRefundInitiatedAt() != null ? refundCase.getRefundInitiatedAt() : now());

    String mappedStatus = mapRefundStatus(refundResponse.getStatus());
    if (BbpsRefundConstants.RefundStatus.SUCCESS.equals(mappedStatus)) {
      markRefundSuccess(refundCase, refundResponse.getMessage());
      return;
    }
    if (BbpsRefundConstants.RefundStatus.PENDING.equals(mappedStatus)) {
      markRefundPending(refundCase, refundResponse.getMessage());
      return;
    }
    if (alreadyInitiated) {
      markRefundPending(refundCase, firstNonBlank(refundResponse.getMessage(),
          "Aggrepay refund already initiated; waiting for refund status"));
      return;
    }

    moveToManualReview(refundCase, StringUtils.defaultIfBlank(refundCase.getRefundReason(),
        BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED),
        firstNonBlank(refundResponse.getMessage(), "Aggrepay refund initiation failed"));
  }

  private void pollAggrepayRefundStatus(BbpsRefundCase refundCase) {
    if (refundCase == null || StringUtils.isBlank(refundCase.getOrderId())) {
      moveToManualReview(refundCase, StringUtils.defaultIfBlank(
          refundCase != null ? refundCase.getRefundReason() : null,
          BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT),
          "Aggrepay refund status lookup requires partner orderId");
      return;
    }

    InternalAggrepayRefundStatusResponse refundStatusResponse =
        partnerInternalServices.getAggrepayRefundStatus(refundCase.getOrderId(),
            refundCase.getRefundReferenceId());

    if (refundStatusResponse == null) {
      keepRefundPendingForStatusRetry(refundCase,
          "Aggrepay refund status lookup returned no response; scheduler will retry");
      return;
    }

    refundCase.setLastRefundResponse(truncateLargeText(toJson(refundStatusResponse)));
    refundCase.setRefundReferenceId(firstNonBlank(refundCase.getRefundReferenceId(),
        refundStatusResponse.getRefundId(), refundStatusResponse.getRefundReferenceNo()));
    syncProviderRefundStatus(refundCase, refundStatusResponse.getStatus());

    String mappedStatus = mapRefundStatus(refundStatusResponse.getStatus());
    if (BbpsRefundConstants.RefundStatus.SUCCESS.equals(mappedStatus)) {
      markRefundSuccess(refundCase, refundStatusResponse.getMessage());
      return;
    }
    if (BbpsRefundConstants.RefundStatus.PENDING.equals(mappedStatus)) {
      markRefundPending(refundCase, refundStatusResponse.getMessage());
      return;
    }

    moveToManualReview(refundCase, StringUtils.defaultIfBlank(refundCase.getRefundReason(),
        BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT),
        firstNonBlank(refundStatusResponse.getMessage(), "Aggrepay refund status is not successful"));
  }

  private void markResolvedWithoutRefund(BbpsRefundCase refundCase, String reason, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRequired(Boolean.FALSE);
    refundCase.setRefundReason(reason);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.NOT_REQUIRED);
    refundCase.setRefundReferenceId(null);
    refundCase.setRefundResponseCode(null);
    refundCase.setProviderRefundStatus(null);
    refundCase.setProviderRefundCheckedAt(null);
    refundCase.setRefundInitiatedAt(null);
    refundCase.setRefundCompletedAt(null);
    refundCase.setRefundResponseMessage(truncateShortText(message));
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    refundCase.setLastError(null);
    replaceRefundRawReason(refundCase,
        appendReasonContext(buildNoRefundRequiredRawReason(refundCase), message));
  }

  private void markClosedFailure(BbpsRefundCase refundCase, String reason, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRequired(Boolean.FALSE);
    refundCase.setRefundReason(reason);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.FAILED);
    refundCase.setRefundResponseMessage(truncateShortText(message));
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    refundCase.setLastError(truncateMediumText(message));
    replaceRefundRawReason(refundCase,
        appendReasonContext(buildRefundFailureRawReason(reason), message));
  }

  private void markPending(BbpsRefundCase refundCase, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRequired(Boolean.FALSE);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.PENDING);
    refundCase.setRefundResponseMessage(truncateShortText(message));
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    if (StringUtils.isNotBlank(refundCase.getRefundReason())) {
      ensureRefundRawReason(refundCase,
          appendReasonContext(buildDecisionPendingRawReason(refundCase.getRefundReason()), message));
    }
  }

  private void markRefundPending(BbpsRefundCase refundCase, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRequired(Boolean.TRUE);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.PENDING);
    refundCase.setRefundResponseMessage(truncateShortText(message));
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    refundCase.setLastError(null);
    ensureRefundRawReasonForRefundDecision(refundCase);
  }

  private void keepRefundPendingForStatusRetry(BbpsRefundCase refundCase, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRequired(Boolean.TRUE);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.PENDING);
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    refundCase.setLastError(null);
    if (refundCase.getRefundInitiatedAt() == null) {
      refundCase.setRefundInitiatedAt(now());
    }
    if (StringUtils.isBlank(refundCase.getRefundResponseMessage())) {
      refundCase.setRefundResponseMessage(truncateShortText(message));
    }
    ensureRefundRawReasonForRefundDecision(refundCase);
  }

  private void moveToManualReview(BbpsRefundCase refundCase, String reason, String message) {
    if (refundCase == null) {
      return;
    }
    String existingReason = StringUtils.trimToNull(refundCase.getRefundReason());
    String requestedReason = StringUtils.trimToNull(reason);
    String resolvedReason = resolveManualReviewReason(existingReason, requestedReason);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.MANUAL_REVIEW);
    refundCase.setManualReviewRequired(Boolean.TRUE);
    refundCase.setAdminTriggerRequired(Boolean.TRUE);
    refundCase.setManualReviewReason(buildDetailedReason(firstNonBlank(requestedReason, resolvedReason), message));
    refundCase.setRefundReason(resolvedReason);
    refundCase.setLastError(truncateMediumText(message));
    refundCase.setRefundResponseMessage(truncateShortText(message));
    updateRefundRawReasonForManualReview(refundCase, existingReason, requestedReason, resolvedReason, message);
  }

  private void markRefundSuccess(BbpsRefundCase refundCase, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRequired(Boolean.TRUE);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.SUCCESS);
    refundCase.setRefundCompletedAt(now());
    refundCase.setRefundResponseMessage(truncateShortText(message));
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    refundCase.setLastError(null);
    ensureRefundRawReasonForRefundDecision(refundCase);
  }

  private BbpsRefundCase finalizeRefundCase(BbpsRefundCase refundCase, Recharge recharge,
      String source, String originalState) {
    syncDipcoinCancellationState(refundCase, recharge, source);
    markPendingRechargeFailureIfEligible(refundCase, recharge, source);
    return saveRefundCaseIfChanged(refundCase, originalState);
  }

  private void markPendingRechargeFailureIfEligible(BbpsRefundCase refundCase,
      Recharge recharge, String source) {
    if (!shouldMarkPendingRechargeFailed(refundCase, recharge, source)) {
      return;
    }

    try {
      String responseCode = resolvePendingRechargeFailureCode(refundCase);
      String responseMessage = resolvePendingRechargeFailureMessage(refundCase);
      String updateDateTime = String.valueOf(now());
      int updated = rechargeDBService.markPaymentNotInitiatedIfPending(recharge.getId(),
          RechargeConstants.RechargeStatus.PENDING.value(),
          RechargeConstants.RechargeStatus.FAILED.value(), responseCode,
          responseMessage, BbpsRefundConstants.BillpayStatus.FAILED, Boolean.FALSE,
          updateDateTime);
      if (updated <= 0) {
        LOG.debug(LogFormatter.instance()
            .message("Skipped BBPS recharge failure update because recharge is no longer pending")
            .data("rechargeId", recharge.getId())
            .data("clientTransactionId", recharge.getClientTransactionId()).format());
        return;
      }

      recharge.setStatus(RechargeConstants.RechargeStatus.FAILED.value());
      recharge.setResponseCode(responseCode);
      recharge.setResponseMessage(responseMessage);
      recharge.setBbpsTxnStatus(BbpsRefundConstants.BillpayStatus.FAILED);
      recharge.setIsRefundRequired(Boolean.FALSE);
      recharge.setUpdateDateTime(updateDateTime);

      LOG.info(LogFormatter.instance()
          .message("Closed BBPS pending recharge without refund")
          .data("rechargeId", recharge.getId())
          .data("clientTransactionId", recharge.getClientTransactionId())
          .data("billPaymentToken", recharge.getBillPaymentToken())
          .data("partnerTransRefId", recharge.getPartnerTransRefId())
          .data("responseCode", responseCode)
          .format());
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to close BBPS pending recharge")
          .data("rechargeId", recharge != null ? recharge.getId() : null)
          .data("refundCaseId", refundCase != null ? refundCase.getId() : null)
          .format(), e);
    }
  }

  private boolean shouldMarkPendingRechargeFailed(BbpsRefundCase refundCase,
      Recharge recharge, String source) {
    return isSchedulerSource(source)
        && isPendingRecharge(recharge)
        && !hasRefundActivity(refundCase)
        && !hasProviderBillpaySubmissionEvidence(refundCase, recharge)
        && (isNoAggrepayDebitEvidenceResolution(refundCase)
            || isAggrepayDebitFailedResolution(refundCase));
  }

  private boolean isNoAggrepayDebitEvidenceResolution(BbpsRefundCase refundCase) {
    return refundCase != null
        && !Boolean.TRUE.equals(refundCase.getRefundRequired())
        && StringUtils.equals(refundCase.getRefundStatus(),
            BbpsRefundConstants.RefundStatus.NOT_REQUIRED)
        && StringUtils.equals(refundCase.getRefundReason(),
            BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE)
        && !hasAggrepayDebitEvidence(refundCase);
  }

  private boolean isAggrepayDebitFailedResolution(BbpsRefundCase refundCase) {
    return refundCase != null
        && !Boolean.TRUE.equals(refundCase.getRefundRequired())
        && StringUtils.equals(refundCase.getRefundStatus(),
            BbpsRefundConstants.RefundStatus.NOT_REQUIRED)
        && StringUtils.equals(refundCase.getRefundReason(),
            BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED)
        && isAggrepayFailed(refundCase);
  }

  private String resolvePendingRechargeFailureCode(BbpsRefundCase refundCase) {
    if (isAggrepayDebitFailedResolution(refundCase)) {
      return AG_DEBIT_FAILED_CODE;
    }
    return PAYMENT_NOT_INITIATED_CODE;
  }

  private String resolvePendingRechargeFailureMessage(BbpsRefundCase refundCase) {
    if (isAggrepayDebitFailedResolution(refundCase)) {
      return firstNonBlank(refundCase.getRefundResponseMessage(), AG_DEBIT_FAILED_MESSAGE);
    }
    return PAYMENT_NOT_INITIATED_MESSAGE;
  }

  private boolean isPendingRecharge(Recharge recharge) {
    return recharge != null
        && RechargeConstants.RechargeStatus.PENDING.value().equals(recharge.getStatus());
  }

  private boolean hasRefundActivity(BbpsRefundCase refundCase) {
    return refundCase != null
        && (StringUtils.isNotBlank(refundCase.getRefundReferenceId())
            || refundCase.getRefundInitiatedAt() != null
            || refundCase.getRefundCompletedAt() != null
            || isRefundInFlight(refundCase));
  }

  private boolean hasProviderBillpaySubmissionEvidence(BbpsRefundCase refundCase,
      Recharge recharge) {
    if (refundCase != null
        && (StringUtils.isNotBlank(refundCase.getPaymentRefNo())
            || StringUtils.isNotBlank(refundCase.getLastBbpsResponse())
            || Boolean.TRUE.equals(refundCase.getBillpayCalled()))) {
      return true;
    }

    if (recharge == null) {
      return false;
    }

    if (StringUtils.isNotBlank(recharge.getPaymentRefNo())
        || StringUtils.isNotBlank(recharge.getRawResponse())
        || StringUtils.isNotBlank(recharge.getRawBBpsTxnStatus())) {
      return true;
    }

    if (StringUtils.isNotBlank(recharge.getBbpsTxnStatus())
        && !StringUtils.equalsIgnoreCase(recharge.getBbpsTxnStatus(),
            PAYMENT_NOT_INITIATED_STATUS)) {
      return true;
    }

    if (StringUtils.isNotBlank(recharge.getResponseCode())
        && !StringUtils.equalsIgnoreCase(recharge.getResponseCode(),
            PAYMENT_NOT_INITIATED_CODE)) {
      return true;
    }

    String responseMessage = StringUtils.trimToEmpty(recharge.getResponseMessage());
    return StringUtils.isNotBlank(responseMessage)
        && !StringUtils.equalsIgnoreCase(responseMessage, PAYMENT_PROCESS_PENDING_MESSAGE)
        && !StringUtils.equalsIgnoreCase(responseMessage, PAYMENT_NOT_INITIATED_MESSAGE);
  }

  private BbpsRefundCase saveRefundCaseIfChanged(BbpsRefundCase refundCase, String originalState) {
    if (refundCase == null) {
      return null;
    }
    if (refundCase.getId() != null && StringUtils.equals(originalState, toJson(refundCase))) {
      return refundCase;
    }
    return saveRefundCase(refundCase);
  }

  private void syncDipcoinCancellationState(BbpsRefundCase refundCase, Recharge recharge,
      String source) {
    if (refundCase == null) {
      return;
    }

    if (isDipcoinCancelSuccessful(refundCase)) {
      return;
    }

    if (!shouldRequireDipcoinCancellation(refundCase)) {
      if (!Boolean.TRUE.equals(refundCase.getRefundRequired())
          || StringUtils.equals(refundCase.getRefundStatus(),
              BbpsRefundConstants.RefundStatus.NOT_REQUIRED)) {
        markDipcoinCancelNotRequired(refundCase);
      }
      return;
    }

    refundCase.setDipcoinCancelRequired(Boolean.TRUE);
    refundCase.setDipcoinCancelReason(firstNonBlank(refundCase.getDipcoinCancelReason(),
        refundCase.getRefundReason()));

    if (StringUtils.equals(refundCase.getDipcoinCancelStatus(),
        BbpsRefundConstants.DipcoinCancelStatus.MANUAL_REVIEW)
        && isSchedulerSource(source)) {
      return;
    }

    DipcoinTransaction originalDipcoinTransaction = resolveDipcoinTransaction(recharge, refundCase);
    if (!isValidDipcoinCancellationSource(originalDipcoinTransaction)) {
      if (hasDipcoinContext(refundCase)) {
        moveDipcoinCancelToManualReview(refundCase,
            "Unable to resolve the original used Dipcoin transaction for cancellation");
      } else {
        markDipcoinCancelNotRequired(refundCase);
      }
      return;
    }

    DipcoinCancellationSnapshot existingSnapshot =
        resolveDipcoinCancellationSnapshot(originalDipcoinTransaction);
    if (existingSnapshot.confirmed()) {
      markDipcoinCancelSuccess(refundCase, existingSnapshot.cancelTxnRefId, now());
      return;
    }

    MerchantCancellationContext merchantContext =
        resolveMerchantCancellationContext(recharge, originalDipcoinTransaction);
    if (merchantContext == null) {
      moveDipcoinCancelToManualReview(refundCase,
          "Unable to resolve merchant context required for Dipcoin cancellation");
      return;
    }

    markDipcoinCancelInitiated(refundCase);

    String orderId = StringUtils.trimToNull(originalDipcoinTransaction.getOrderId());
    if (StringUtils.isBlank(orderId)) {
      moveDipcoinCancelToManualReview(refundCase,
          "Dipcoin cancellation requires the original Dipcoin transaction orderId");
      return;
    }

    String merchantTransactionRefId = firstNonBlank(
        originalDipcoinTransaction.getPartnerTransactionReferenceId(),
        refundCase.getPartnerTransRefId());
    if (StringUtils.isBlank(merchantTransactionRefId)) {
      moveDipcoinCancelToManualReview(refundCase,
          "Dipcoin cancellation requires the original merchant transaction reference");
      return;
    }

    try {
      ResponseEntity response = merchantSettlementResource.cancelTransactions(
          merchantContext.user, merchantContext.merchant, merchantTransactionRefId,
          originalDipcoinTransaction.getDipcoinTransactionRefId(), orderId,
          buildDipcoinCancelComment(refundCase),
          firstNonNull(refundCase.getRefundAmount(), originalDipcoinTransaction.getAmount()), null);

      String responseCode = extractApiResponseHeaderCode(response != null ? response.getBody() : null);
      String responseMessage = extractApiResponseHeaderMessage(response != null ? response.getBody() : null);

      if (response != null && response.getStatusCode() == HttpStatus.NO_CONTENT) {
        confirmDipcoinCancellation(refundCase, originalDipcoinTransaction,
            firstNonBlank(responseMessage, "Dipcoin cancellation completed"));
        return;
      }

      if (StringUtils.equals(responseCode, HeaderCode.TX_ALREADY_CANCELED.code())) {
        confirmDipcoinCancellation(refundCase, originalDipcoinTransaction,
            firstNonBlank(responseMessage, "Dipcoin transaction already cancelled"));
        return;
      }

      if (response != null && response.getStatusCode().is4xxClientError()) {
        moveDipcoinCancelToManualReview(refundCase,
            firstNonBlank(responseMessage, "Dipcoin cancellation rejected"));
        return;
      }

      markDipcoinCancelPending(refundCase,
          firstNonBlank(responseMessage, "Dipcoin cancellation could not be confirmed"));
    } catch (APIException e) {
      String responseCode = extractApiResponseHeaderCode(e.getResponse());
      String responseMessage = extractApiResponseHeaderMessage(e.getResponse());
      if (StringUtils.equals(responseCode, HeaderCode.TX_ALREADY_CANCELED.code())) {
        confirmDipcoinCancellation(refundCase, originalDipcoinTransaction,
            firstNonBlank(responseMessage, "Dipcoin transaction already cancelled"));
        return;
      }

      if (e.getCode() != null && e.getCode().is4xxClientError()) {
        moveDipcoinCancelToManualReview(refundCase,
            firstNonBlank(responseMessage, e.getMessage()));
        return;
      }

      markDipcoinCancelPending(refundCase,
          firstNonBlank(responseMessage, e.getMessage()));
    } catch (Exception e) {
      markDipcoinCancelPending(refundCase,
          firstNonBlank(e.getMessage(), "Dipcoin cancellation failed"));
    }
  }

  private boolean shouldRequireDipcoinCancellation(BbpsRefundCase refundCase) {
    return refundCase != null
        && hasDipcoinContext(refundCase)
        && isAcceptedRefundState(refundCase);
  }

  private boolean hasDipcoinContext(BbpsRefundCase refundCase) {
    return refundCase != null
        && (positiveInteger(refundCase.getDipcoinId()) != null
            || StringUtils.isNotBlank(refundCase.getDipcoinTransRefId()));
  }

  private boolean isAcceptedRefundState(BbpsRefundCase refundCase) {
    if (refundCase == null || !Boolean.TRUE.equals(refundCase.getRefundRequired())) {
      return false;
    }

    if (StringUtils.equalsAny(refundCase.getRefundStatus(),
        BbpsRefundConstants.RefundStatus.PENDING,
        BbpsRefundConstants.RefundStatus.SUCCESS)) {
      return true;
    }

    if (StringUtils.isBlank(refundCase.getProviderRefundStatus())) {
      return false;
    }

    String mappedProviderStatus = mapRefundStatus(refundCase.getProviderRefundStatus());
    return StringUtils.equalsAny(mappedProviderStatus,
        BbpsRefundConstants.RefundStatus.PENDING,
        BbpsRefundConstants.RefundStatus.SUCCESS);
  }

  private boolean isDipcoinCancelSuccessful(BbpsRefundCase refundCase) {
    return refundCase != null
        && StringUtils.equals(refundCase.getDipcoinCancelStatus(),
            BbpsRefundConstants.DipcoinCancelStatus.SUCCESS);
  }

  private boolean isValidDipcoinCancellationSource(DipcoinTransaction dipcoinTransaction) {
    if (dipcoinTransaction == null) {
      return false;
    }

    return dipcoinTransaction.getType() == DBConstants.DipcoinTransactionType.COMPLETELY_USED.value()
        || dipcoinTransaction.getType() == DBConstants.DipcoinTransactionType.PARTIALLY_USED.value()
        || dipcoinTransaction.getType() == DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED.value()
        || dipcoinTransaction.getType()
            == DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED_PROCESSED.value();
  }

  private DipcoinCancellationSnapshot resolveDipcoinCancellationSnapshot(
      DipcoinTransaction originalDipcoinTransaction) {
    if (originalDipcoinTransaction == null || originalDipcoinTransaction.getDipcoinId() <= 0) {
      return DipcoinCancellationSnapshot.empty();
    }

    List<DipcoinTransaction> cancelTransactions = coinDBService.getTransactions(
        Collections.singletonList(Integer.valueOf(originalDipcoinTransaction.getDipcoinId())),
        Arrays.asList(
            Integer.valueOf(DBConstants.DipcoinTransactionType.CANCELLED_BY_MERCHANT.value()),
            Integer.valueOf(DBConstants.DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT.value())),
        0, 100);

    DipcoinTransaction bestMatch = null;
    if (CollectionUtils.isNotEmpty(cancelTransactions)) {
      for (DipcoinTransaction dipcoinTransaction : cancelTransactions) {
        if (dipcoinTransaction == null) {
          continue;
        }
        if (!StringUtils.equals(StringUtils.trimToEmpty(dipcoinTransaction.getOrderId()),
            StringUtils.trimToEmpty(originalDipcoinTransaction.getOrderId()))) {
          continue;
        }
        if (bestMatch == null || dipcoinTransaction.getId() > bestMatch.getId()) {
          bestMatch = dipcoinTransaction;
        }
      }
    }

    Dipcoin dipcoin = coinDBService.getById(Integer.valueOf(originalDipcoinTransaction.getDipcoinId()), false);
    boolean dipcoinUpdated = dipcoin != null
        && dipcoin.getStatus() == DBConstants.DipcoinStatus.PROCESSED_DISPUTED.value();

    if (bestMatch == null || !dipcoinUpdated) {
      return DipcoinCancellationSnapshot.empty();
    }

    return new DipcoinCancellationSnapshot(bestMatch.getDipcoinTransactionRefId(), dipcoinUpdated);
  }

  private MerchantCancellationContext resolveMerchantCancellationContext(Recharge recharge,
      DipcoinTransaction originalDipcoinTransaction) {
    String partnerReferenceId = StringUtils.trimToNull(recharge != null
        ? recharge.getPartnerReferenceId() : null);
    if (StringUtils.isBlank(partnerReferenceId) && originalDipcoinTransaction != null) {
      partnerReferenceId =
          StringUtils.trimToNull(originalDipcoinTransaction.getPartnerReferenceId());
    }

    if (StringUtils.isBlank(partnerReferenceId)) {
      return null;
    }

    Merchant merchant = merchantDBService.getMerchant(partnerReferenceId);
    if (merchant == null) {
      return null;
    }

    String email = partnerReferenceId + APIConstants.INTERNAL_PARTNER_EMAIL_SUFFIX;
    User merchantUser = userDBService.getUser(email);
    if (merchantUser == null || !userDBService.isActive(merchantUser)
        || merchantUser.getBankMerchantId() != merchant.getId()) {
      return null;
    }

    return new MerchantCancellationContext(merchant, merchantUser);
  }

  private void confirmDipcoinCancellation(BbpsRefundCase refundCase,
      DipcoinTransaction originalDipcoinTransaction, String fallbackMessage) {
    DipcoinCancellationSnapshot snapshot =
        resolveDipcoinCancellationSnapshot(originalDipcoinTransaction);
    if (snapshot.confirmed()) {
      markDipcoinCancelSuccess(refundCase, snapshot.cancelTxnRefId, now());
      return;
    }

    markDipcoinCancelPending(refundCase,
        firstNonBlank(fallbackMessage,
            "Dipcoin cancellation acknowledged but confirmation is pending"));
  }

  private void markDipcoinCancelNotRequired(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return;
    }
    refundCase.setDipcoinCancelRequired(Boolean.FALSE);
    refundCase.setDipcoinCancelStatus(BbpsRefundConstants.DipcoinCancelStatus.NOT_REQUIRED);
    refundCase.setDipcoinCancelReason(null);
    refundCase.setDipcoinCancelInitiatedAt(null);
    refundCase.setDipcoinCancelCompletedAt(null);
    refundCase.setDipcoinCancelTxnRefId(null);
    refundCase.setLastDipcoinCancelError(null);
  }

  private void markDipcoinCancelInitiated(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return;
    }
    refundCase.setDipcoinCancelRequired(Boolean.TRUE);
    refundCase.setDipcoinCancelReason(firstNonBlank(refundCase.getDipcoinCancelReason(),
        refundCase.getRefundReason()));
    if (refundCase.getDipcoinCancelInitiatedAt() == null) {
      refundCase.setDipcoinCancelInitiatedAt(now());
    }
    refundCase.setDipcoinCancelStatus(BbpsRefundConstants.DipcoinCancelStatus.NEW);
    refundCase.setLastDipcoinCancelError(null);
  }

  private void markDipcoinCancelPending(BbpsRefundCase refundCase, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setDipcoinCancelRequired(Boolean.TRUE);
    refundCase.setDipcoinCancelReason(firstNonBlank(refundCase.getDipcoinCancelReason(),
        refundCase.getRefundReason()));
    if (refundCase.getDipcoinCancelInitiatedAt() == null) {
      refundCase.setDipcoinCancelInitiatedAt(now());
    }
    refundCase.setDipcoinCancelStatus(BbpsRefundConstants.DipcoinCancelStatus.PENDING);
    refundCase.setLastDipcoinCancelError(truncateMediumText(message));
  }

  private void markDipcoinCancelSuccess(BbpsRefundCase refundCase, String cancelTxnRefId,
      Long completionTime) {
    if (refundCase == null) {
      return;
    }
    refundCase.setDipcoinCancelRequired(Boolean.TRUE);
    refundCase.setDipcoinCancelReason(firstNonBlank(refundCase.getDipcoinCancelReason(),
        refundCase.getRefundReason()));
    refundCase.setDipcoinCancelStatus(BbpsRefundConstants.DipcoinCancelStatus.SUCCESS);
    refundCase.setDipcoinCancelTxnRefId(firstNonBlank(cancelTxnRefId,
        refundCase.getDipcoinCancelTxnRefId()));
    if (refundCase.getDipcoinCancelInitiatedAt() == null) {
      refundCase.setDipcoinCancelInitiatedAt(
          completionTime != null && completionTime.longValue() > 0L ? completionTime : now());
    }
    refundCase.setDipcoinCancelCompletedAt(
        completionTime != null && completionTime.longValue() > 0L ? completionTime : now());
    refundCase.setLastDipcoinCancelError(null);
  }

  private void moveDipcoinCancelToManualReview(BbpsRefundCase refundCase, String message) {
    if (refundCase == null) {
      return;
    }
    refundCase.setDipcoinCancelRequired(Boolean.TRUE);
    refundCase.setDipcoinCancelReason(firstNonBlank(refundCase.getDipcoinCancelReason(),
        refundCase.getRefundReason()));
    refundCase.setDipcoinCancelStatus(BbpsRefundConstants.DipcoinCancelStatus.MANUAL_REVIEW);
    if (refundCase.getDipcoinCancelInitiatedAt() == null) {
      refundCase.setDipcoinCancelInitiatedAt(now());
    }
    refundCase.setLastDipcoinCancelError(truncateMediumText(message));
  }

  private String buildDipcoinCancelComment(BbpsRefundCase refundCase) {
    return firstNonBlank(describeRefundReason(refundCase != null ? refundCase.getRefundReason() : null),
        "Refund compensation triggered Dipcoin cancellation");
  }

  private String extractApiResponseHeaderCode(Object body) {
    if (!(body instanceof APIResponse)) {
      return null;
    }
    APIResponse response = (APIResponse) body;
    return CollectionUtils.isEmpty(response.getCodes()) ? null : response.getCodes().get(0).getCode();
  }

  private String extractApiResponseHeaderMessage(Object body) {
    if (!(body instanceof APIResponse)) {
      return null;
    }
    APIResponse response = (APIResponse) body;
    return CollectionUtils.isEmpty(response.getCodes()) ? null : response.getCodes().get(0).getMessage();
  }

  private BbpsRefundCase saveRefundCase(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return null;
    }
    refundCase.setUpdateDateTime(now());
    if (refundCase.getCreateDateTime() == null) {
      refundCase.setCreateDateTime(refundCase.getUpdateDateTime());
    }
    if (refundCase.getId() == null || refundCase.getId() <= 0) {
      return bbpsRefundCaseDBService.addCase(refundCase);
    }
    return bbpsRefundCaseDBService.updateCase(refundCase);
  }

  private String resolveClientTransactionId(Recharge recharge, CustomerBillPayRequest billPayRequest) {
    if (recharge != null && StringUtils.isNotBlank(recharge.getClientTransactionId())) {
      return StringUtils.trim(recharge.getClientTransactionId());
    }
    return null;
  }

  private String resolvePartnerTransactionReferenceId(Recharge recharge,
      CustomerBillPayRequest billPayRequest) {
    if (billPayRequest != null
        && StringUtils.isNotBlank(billPayRequest.getPartnerTransactionReferenceId())) {
      return StringUtils.trim(billPayRequest.getPartnerTransactionReferenceId());
    }
    if (recharge != null && StringUtils.isNotBlank(recharge.getPartnerTransRefId())) {
      return StringUtils.trim(recharge.getPartnerTransRefId());
    }
    return null;
  }

  private String buildPaymentKey(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return "REFUND:" + now();
    }

    if (StringUtils.isNotBlank(refundCase.getPartnerTransRefId())) {
      return "PARTNER:" + StringUtils.trim(refundCase.getPartnerTransRefId());
    }
    if (StringUtils.isNotBlank(refundCase.getClientTransactionId())) {
      return "CLIENT:" + StringUtils.trim(refundCase.getClientTransactionId());
    }
    if (StringUtils.isNotBlank(refundCase.getDipcoinTransRefId())) {
      return "DIPCOIN:" + StringUtils.trim(refundCase.getDipcoinTransRefId());
    }
    if (StringUtils.isNotBlank(refundCase.getBillPaymentToken())) {
      return "TOKEN:" + StringUtils.trim(refundCase.getBillPaymentToken());
    }
    if (refundCase.getRechargeId() != null && refundCase.getRechargeId() > 0) {
      return "RECHARGE:" + refundCase.getRechargeId();
    }
    return "REFUND:" + now();
  }

  private boolean determineBillpayCalled(BbpsRefundCase refundCase, Recharge recharge) {
    if (refundCase != null && Boolean.TRUE.equals(refundCase.getBillpayCalled())) {
      return true;
    }
    if (StringUtils.isNotBlank(refundCase != null ? refundCase.getPaymentRefNo() : null)
        || StringUtils.isNotBlank(refundCase != null ? refundCase.getLastBbpsResponse() : null)) {
      return true;
    }
    if (recharge == null) {
      return false;
    }
    if (StringUtils.isNotBlank(recharge.getPaymentRefNo())
        || StringUtils.isNotBlank(recharge.getRawResponse())
        || StringUtils.isNotBlank(recharge.getResponseCode())
        || StringUtils.isNotBlank(recharge.getBbpsTxnStatus())) {
      return true;
    }
    return !StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(recharge.getResponseMessage()),
        "PAYMENT_PROCESS_PENDING");
  }

  private String deriveBillpayStatus(Recharge recharge, BbpsRefundCase refundCase) {
    String existingStatus =
        StringUtils.trimToNull(refundCase != null ? refundCase.getBillpayStatus() : null);

    if (recharge == null) {
      return StringUtils.defaultIfBlank(existingStatus, BbpsRefundConstants.BillpayStatus.UNKNOWN);
    }

    String derivedStatus = deriveBillpayStatusFromRecharge(recharge);
    if (!BbpsRefundConstants.BillpayStatus.UNKNOWN.equals(derivedStatus)) {
      return derivedStatus;
    }

    if (StringUtils.isNotBlank(existingStatus)
        && !BbpsRefundConstants.BillpayStatus.UNKNOWN.equals(existingStatus)) {
      return existingStatus;
    }

    return BbpsRefundConstants.BillpayStatus.UNKNOWN;
  }

  private String deriveBillpayStatusFromRecharge(Recharge recharge) {
    if (RechargeConstants.RechargeStatus.SUCCESS.value().equals(recharge.getStatus())) {
      return BbpsRefundConstants.BillpayStatus.SUCCESS;
    }
    if (RechargeConstants.RechargeStatus.FAILED.value().equals(recharge.getStatus())) {
      return BbpsRefundConstants.BillpayStatus.FAILED;
    }
    if (isBillpayFailureIndicatedByRecharge(recharge)) {
      return BbpsRefundConstants.BillpayStatus.FAILED;
    }

    if (StringUtils.isNotBlank(recharge.getBbpsTxnStatus())) {
      return mapFinacusStatus(recharge.getBbpsTxnStatus());
    }
    if (RechargeConstants.RechargeSuccessResponseCode.validCode(recharge.getResponseCode())) {
      return BbpsRefundConstants.BillpayStatus.SUCCESS;
    }
    if (RechargeConstants.RechargeFailureResponseCode.validCode(recharge.getResponseCode())) {
      return BbpsRefundConstants.BillpayStatus.FAILED;
    }
    if (RechargeConstants.RechargePendingResponseCode.validCode(recharge.getResponseCode())
        || RechargeConstants.RechargeStatus.PENDING.value().equals(recharge.getStatus())) {
      return BbpsRefundConstants.BillpayStatus.PENDING;
    }

    return BbpsRefundConstants.BillpayStatus.UNKNOWN;
  }

  private boolean isBillpayFailureIndicatedByRecharge(Recharge recharge) {
    if (recharge == null) {
      return false;
    }

    if (RechargeConstants.RechargeStatus.FAILED.value().equals(recharge.getStatus())) {
      return true;
    }

    String responseCode = StringUtils.trimToEmpty(recharge.getResponseCode());
    if (RechargeConstants.RechargeFailureResponseCode.validCode(responseCode)
        || StringUtils.startsWithIgnoreCase(responseCode, "FA")) {
      return true;
    }

    String responseMessage = StringUtils.trimToEmpty(recharge.getResponseMessage());
    return StringUtils.containsIgnoreCase(responseMessage, "invalid payment amount")
        || StringUtils.containsIgnoreCase(responseMessage, "plan id")
        || StringUtils.containsIgnoreCase(responseMessage, "invalid data");
  }

  private String resolveFinacusStatusParam(BbpsRefundCase refundCase, Recharge recharge) {
    if (StringUtils.isNotBlank(refundCase != null ? refundCase.getPaymentRefNo() : null)) {
      return StringUtils.trim(refundCase.getPaymentRefNo());
    }
    if (recharge != null && StringUtils.isNotBlank(recharge.getPaymentRefNo())) {
      return StringUtils.trim(recharge.getPaymentRefNo());
    }
    if (StringUtils.isNotBlank(refundCase != null ? refundCase.getBillPaymentToken() : null)) {
      return StringUtils.trim(refundCase.getBillPaymentToken());
    }
    if (recharge != null && StringUtils.isNotBlank(recharge.getBillPaymentToken())) {
      return StringUtils.trim(recharge.getBillPaymentToken());
    }
    if (StringUtils.isNotBlank(refundCase != null ? refundCase.getPartnerTransRefId() : null)) {
      return StringUtils.trim(refundCase.getPartnerTransRefId());
    }
    if (recharge != null && StringUtils.isNotBlank(recharge.getPartnerTransRefId())) {
      return StringUtils.trim(recharge.getPartnerTransRefId());
    }
    return null;
  }

  private String resolveFinacusStatusBy(BbpsRefundCase refundCase, Recharge recharge) {
    if (StringUtils.isNotBlank(refundCase != null ? refundCase.getPaymentRefNo() : null)
        || (recharge != null && StringUtils.isNotBlank(recharge.getPaymentRefNo()))) {
      return FINACUS_STATUS_BY_REFERENCE_ID;
    }
    if (StringUtils.isNotBlank(refundCase != null ? refundCase.getBillPaymentToken() : null)
        || (recharge != null && StringUtils.isNotBlank(recharge.getBillPaymentToken()))
        || StringUtils.isNotBlank(refundCase != null ? refundCase.getPartnerTransRefId() : null)
        || (recharge != null && StringUtils.isNotBlank(recharge.getPartnerTransRefId()))) {
      return FINACUS_STATUS_BY_TRANSACTION_ID;
    }
    return null;
  }

  private String extractFinacusStatus(String rawResponse) {
    if (StringUtils.isBlank(rawResponse)) {
      return null;
    }

    try {
      JsonNode root = objectMapper.readTree(rawResponse);
      JsonNode responseNode = root.path("Response");

      if (responseNode.isArray()) {
        for (JsonNode item : responseNode) {
          String status = item.path("Status").asText(null);
          if (StringUtils.isNotBlank(status)) {
            return StringUtils.trim(status);
          }
        }
      }

      if (responseNode.isObject()) {
        String status = responseNode.path("Status").asText(null);
        if (StringUtils.isNotBlank(status)) {
          return StringUtils.trim(status);
        }
      }

      String topLevelStatus = root.path("Status").asText(null);
      if (StringUtils.isNotBlank(topLevelStatus)) {
        return StringUtils.trim(topLevelStatus);
      }
    } catch (Exception e) {
      LOG.warn("Failed to parse Finacus status response for BBPS refund flow", e);
    }

    return null;
  }

  private String mapFinacusStatus(String status) {
    String normalizedStatus = StringUtils.upperCase(StringUtils.trimToEmpty(status));
    if (StringUtils.equalsAny(normalizedStatus, "SUCCESS", "SUCCESSFUL")) {
      return BbpsRefundConstants.BillpayStatus.SUCCESS;
    }
    if (StringUtils.equalsAny(normalizedStatus, "FAILED", "FAILURE")) {
      return BbpsRefundConstants.BillpayStatus.FAILED;
    }
    if (StringUtils.equalsAny(normalizedStatus, "PENDING", "PROCESSING", "IN_PROGRESS")) {
      return BbpsRefundConstants.BillpayStatus.PENDING;
    }
    if (StringUtils.isBlank(normalizedStatus)) {
      return BbpsRefundConstants.BillpayStatus.UNKNOWN;
    }
    return BbpsRefundConstants.BillpayStatus.UNKNOWN;
  }

  private boolean isNoTransactionFoundFinacusResponse(String rawResponse) {
    JsonNode root = readJsonNodeQuietly(rawResponse);
    if (root == null) {
      return false;
    }

    String responseCode = StringUtils.trimToEmpty(root.path("ResponseCode").asText(null));
    String responseMessage = StringUtils.trimToEmpty(root.path("ResponseMessage").asText(null));
    return StringUtils.equals(responseCode, "001")
        && StringUtils.containsIgnoreCase(responseMessage, "no transaction found");
  }

  private String mapAggrepayPaymentStatus(String status) {
    String normalizedStatus = StringUtils.upperCase(StringUtils.trimToEmpty(status));
    if (StringUtils.equalsAny(normalizedStatus, "SUCCESS", "DEBITED", "CAPTURED")) {
      return StringUtils.equals(normalizedStatus, "SUCCESS")
          ? BbpsRefundConstants.AgStatus.SUCCESS : BbpsRefundConstants.AgStatus.DEBITED;
    }
    if (StringUtils.equalsAny(normalizedStatus, "FAILED", "FAILURE", "REJECTED")) {
      return BbpsRefundConstants.AgStatus.FAILED;
    }
    if (StringUtils.equalsAny(normalizedStatus, "PENDING", "PROCESSING", "INITIATED")) {
      return BbpsRefundConstants.AgStatus.PENDING;
    }
    if (StringUtils.isBlank(normalizedStatus)) {
      return BbpsRefundConstants.AgStatus.NOT_FOUND;
    }
    return BbpsRefundConstants.AgStatus.UNKNOWN;
  }

  private String mapRefundStatus(String status) {
    String normalizedStatus = StringUtils.upperCase(StringUtils.trimToEmpty(status));
    if (StringUtils.equalsAny(normalizedStatus, "SUCCESS", "PROCESSED", "COMPLETED", "REFUNDED",
        "CUSTOMER REFUNDED", "ALREADY_REFUNDED")) {
      return BbpsRefundConstants.RefundStatus.SUCCESS;
    }
    if (StringUtils.equalsAny(normalizedStatus, "PENDING", "PROCESSING", "INITIATED", "CREATED",
        "ALREADY_INITIATED", "IN_PROGRESS")) {
      return BbpsRefundConstants.RefundStatus.PENDING;
    }
    if (StringUtils.equalsAny(normalizedStatus, "FAILED", "REJECTED", "CANCELLED", "ERROR")) {
      return BbpsRefundConstants.RefundStatus.FAILED;
    }
    return BbpsRefundConstants.RefundStatus.FAILED;
  }

  private boolean hasCapturedPgData(DipcoinTransaction dipcoinTransaction) {
    return dipcoinTransaction != null
        && StringUtils.isNotBlank(dipcoinTransaction.getCapturedPgTransactionId())
        && StringUtils.isNotBlank(dipcoinTransaction.getCapturedPgPaymentMode());
  }

  private boolean matchesRefundCaseCustomer(DipcoinTransaction dipcoinTransaction, Recharge recharge,
      BbpsRefundCase refundCase) {
    if (dipcoinTransaction == null || dipcoinTransaction.getUser() == null) {
      return false;
    }

    Integer customerId = null;
    if (recharge != null && StringUtils.isNumeric(recharge.getCustomerId())) {
      customerId = Integer.valueOf(recharge.getCustomerId());
    } else if (refundCase != null && StringUtils.isNumeric(refundCase.getCustomerId())) {
      customerId = Integer.valueOf(refundCase.getCustomerId());
    }
    return customerId != null && dipcoinTransaction.getUser().getId() == customerId.intValue();
  }

  private int compareCapturedPgDipcoinTransactions(DipcoinTransaction left,
      DipcoinTransaction right) {
    int requestTimeCompare = Long.compare(
        NumberUtils.toLong(StringUtils.trimToEmpty(left.getRequestTime()), 0L),
        NumberUtils.toLong(StringUtils.trimToEmpty(right.getRequestTime()), 0L));
    if (requestTimeCompare != 0) {
      return requestTimeCompare;
    }
    return Integer.compare(left.getId(), right.getId());
  }

  private boolean isTransactionAtOrBeforeAnchor(DipcoinTransaction dipcoinTransaction,
      DipcoinTransaction anchorTransaction) {
    if (dipcoinTransaction == null || anchorTransaction == null) {
      return false;
    }
    long anchorRequestTime =
        NumberUtils.toLong(StringUtils.trimToEmpty(anchorTransaction.getRequestTime()), Long.MAX_VALUE);
    long candidateRequestTime =
        NumberUtils.toLong(StringUtils.trimToEmpty(dipcoinTransaction.getRequestTime()), 0L);
    return candidateRequestTime <= anchorRequestTime;
  }

  private boolean hasAggrepayDebitEvidence(BbpsRefundCase refundCase) {
    return refundCase != null && (Boolean.TRUE.equals(refundCase.getAgEvidenceFound())
        || StringUtils.isNotBlank(refundCase.getCapturedPgTransactionId())
        || StringUtils.isNotBlank(refundCase.getCapturedPgPaymentMode())
        || isAggrepaySettled(refundCase.getAgStatus()));
  }

  private boolean isAggrepaySettled(BbpsRefundCase refundCase) {
    return refundCase != null && isAggrepaySettled(refundCase.getAgStatus());
  }

  private boolean isAggrepaySettled(String agStatus) {
    return StringUtils.equalsAny(agStatus, BbpsRefundConstants.AgStatus.SUCCESS,
        BbpsRefundConstants.AgStatus.DEBITED);
  }

  private boolean isAggrepayFailed(BbpsRefundCase refundCase) {
    return refundCase != null && StringUtils.equals(refundCase.getAgStatus(),
        BbpsRefundConstants.AgStatus.FAILED);
  }

  private boolean isBillpaySuccess(BbpsRefundCase refundCase) {
    return refundCase != null && StringUtils.equals(refundCase.getBillpayStatus(),
        BbpsRefundConstants.BillpayStatus.SUCCESS);
  }

  private boolean isBillpayFailed(BbpsRefundCase refundCase) {
    return refundCase != null && StringUtils.equals(refundCase.getBillpayStatus(),
        BbpsRefundConstants.BillpayStatus.FAILED);
  }

  private boolean isBillpayPending(BbpsRefundCase refundCase) {
    return refundCase != null && StringUtils.equals(refundCase.getBillpayStatus(),
        BbpsRefundConstants.BillpayStatus.PENDING);
  }

  private boolean isBillpayNotCalled(BbpsRefundCase refundCase) {
    return refundCase != null && StringUtils.equals(refundCase.getBillpayStatus(),
        BbpsRefundConstants.BillpayStatus.NOT_CALLED);
  }

  private boolean isRefundInFlight(BbpsRefundCase refundCase) {
    return refundCase != null && Boolean.TRUE.equals(refundCase.getRefundRequired())
        && StringUtils.equals(refundCase.getRefundStatus(), BbpsRefundConstants.RefundStatus.PENDING);
  }

  private boolean shouldPollRefundStatus(BbpsRefundCase refundCase) {
    return refundCase != null
        && !StringUtils.equalsAny(refundCase.getRefundStatus(),
            BbpsRefundConstants.RefundStatus.SUCCESS,
            BbpsRefundConstants.RefundStatus.NOT_REQUIRED)
        && Boolean.TRUE.equals(refundCase.getRefundRequired())
        && StringUtils.isNotBlank(refundCase.getOrderId())
        && (isRefundInFlight(refundCase)
            || refundCase.getRefundInitiatedAt() != null
            || StringUtils.isNotBlank(refundCase.getRefundReferenceId())
            || isRefundAlreadyInitiatedResponse(refundCase.getProviderRefundStatus(),
                refundCase.getRefundResponseCode(), refundCase.getRefundResponseMessage()));
  }

  private boolean isFinalState(String refundStatus) {
    return StringUtils.equalsAny(refundStatus, BbpsRefundConstants.RefundStatus.SUCCESS,
        BbpsRefundConstants.RefundStatus.FAILED,
        BbpsRefundConstants.RefundStatus.MANUAL_REVIEW,
        BbpsRefundConstants.RefundStatus.NOT_REQUIRED);
  }

  private boolean requiresDipcoinCancelFollowUp(BbpsRefundCase refundCase) {
    return refundCase != null
        && Boolean.TRUE.equals(refundCase.getDipcoinCancelRequired())
        && StringUtils.equalsAny(refundCase.getDipcoinCancelStatus(),
            BbpsRefundConstants.DipcoinCancelStatus.NEW,
            BbpsRefundConstants.DipcoinCancelStatus.PENDING);
  }

  private boolean isOrphanWindowElapsed(Recharge recharge, BbpsRefundCase refundCase) {
    Long referenceTime = resolveReferenceTime(recharge, refundCase);
    return referenceTime != null && referenceTime.longValue() + effectiveOrphanGraceMs() <= now();
  }

  private boolean hasComplaintRaised(Recharge recharge) {
    if (recharge == null) {
      return false;
    }
    return StringUtils.isNotBlank(recharge.getComplaintId())
        || StringUtils.isNotBlank(recharge.getComplaintStatus())
        || StringUtils.isNotBlank(recharge.getComplaintDisposition())
        || StringUtils.isNotBlank(recharge.getComplaintDescription());
  }

  private boolean shouldMoveToComplaintManualReview(BbpsRefundCase refundCase, Recharge recharge) {
    if (refundCase == null || !isComplaintRefundEligible(refundCase, recharge)) {
      return false;
    }

    if (Boolean.TRUE.equals(refundCase.getManualReviewRequired())
        || Boolean.TRUE.equals(refundCase.getAdminTriggerRequired())
        || StringUtils.equals(refundCase.getRefundStatus(),
            BbpsRefundConstants.RefundStatus.MANUAL_REVIEW)) {
      return false;
    }

    if (Boolean.TRUE.equals(refundCase.getRefundRequired())
        && (StringUtils.isNotBlank(refundCase.getRefundReferenceId())
            || refundCase.getRefundInitiatedAt() != null
            || isRefundInFlight(refundCase)
            || StringUtils.equals(refundCase.getRefundStatus(),
                BbpsRefundConstants.RefundStatus.SUCCESS))) {
      return false;
    }

    return true;
  }

  private boolean shouldAutoCloseWithoutRefund(BbpsRefundCase refundCase, Recharge recharge, String source) {
    if (!isSchedulerSource(source) || refundCase == null || Boolean.TRUE.equals(refundCase.getRefundRequired())
        || isFinalState(refundCase.getRefundStatus()) || isAggrepaySettled(refundCase)
        || isAggrepayFailed(refundCase) || isRefundInFlight(refundCase)
        || StringUtils.isNotBlank(refundCase.getRefundReferenceId())
        || refundCase.getRefundInitiatedAt() != null) {
      return false;
    }

    Long observationStartTime = resolveSchedulerObservationStartTime(recharge, refundCase);
    return observationStartTime != null
        && observationStartTime.longValue() + effectiveNoAgEvidenceCloseMs() <= now();
  }

  private boolean shouldResetInvalidComplaintManualReview(BbpsRefundCase refundCase, Recharge recharge) {
    return isComplaintGeneratedManualReview(refundCase)
        && !isComplaintRefundEligible(refundCase, recharge);
  }

  private boolean isComplaintGeneratedManualReview(BbpsRefundCase refundCase) {
    return refundCase != null
        && StringUtils.equals(refundCase.getRefundStatus(), BbpsRefundConstants.RefundStatus.MANUAL_REVIEW)
        && StringUtils.equals(refundCase.getRefundReason(),
            BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT);
  }

  private boolean isComplaintRefundEligible(BbpsRefundCase refundCase, Recharge recharge) {
    return refundCase != null
        && hasComplaintRaised(recharge)
        && isBillpaySuccess(refundCase)
        && isAggrepaySettled(refundCase);
  }

  private void resetInvalidComplaintManualReview(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return;
    }

    refundCase.setRefundRequired(Boolean.FALSE);
    refundCase.setRefundStatus(BbpsRefundConstants.RefundStatus.NEW);
    refundCase.setManualReviewRequired(Boolean.FALSE);
    refundCase.setAdminTriggerRequired(Boolean.FALSE);
    refundCase.setRefundReason(null);
    refundCase.setRefundRawReason(null);
    refundCase.setManualReviewReason(null);
    refundCase.setRefundReferenceId(null);
    refundCase.setRefundResponseCode(null);
    refundCase.setProviderRefundStatus(null);
    refundCase.setProviderRefundCheckedAt(null);
    refundCase.setRefundInitiatedAt(null);
    refundCase.setRefundCompletedAt(null);
    refundCase.setRefundResponseMessage(truncateShortText(
        "Complaint does not require refund handling because Aggrepay debit is not settled or bill payment is not successful"));
    refundCase.setLastError(null);
  }

  private String buildComplaintManualReviewMessage(Recharge recharge) {
    if (recharge == null) {
      return "Complaint raised for successful bill payment; admin review required";
    }

    String complaintStatus = StringUtils.trimToNull(recharge.getComplaintStatus());
    String complaintDescription = StringUtils.trimToNull(recharge.getComplaintDescription());
    String complaintId = StringUtils.trimToNull(recharge.getComplaintId());

    if (StringUtils.isNotBlank(complaintStatus) && StringUtils.isNotBlank(complaintDescription)) {
      return String.format("Complaint raised (%s): %s", complaintStatus, complaintDescription);
    }
    if (StringUtils.isNotBlank(complaintStatus)) {
      return String.format("Complaint raised with status %s; admin review required",
          complaintStatus);
    }
    if (StringUtils.isNotBlank(complaintDescription)) {
      return String.format("Complaint raised: %s", complaintDescription);
    }
    if (StringUtils.isNotBlank(complaintId)) {
      return String.format("Complaint %s raised for successful bill payment; admin review required",
          complaintId);
    }

    return "Complaint raised for successful bill payment; admin review required";
  }

  private boolean isLaterDuplicateCase(BbpsRefundCase refundCase) {
    if (refundCase == null || refundCase.getId() == null
        || StringUtils.isBlank(refundCase.getOrderId())) {
      return false;
    }

    List<BbpsRefundCase> sameOrderCases = bbpsRefundCaseDBService.findByIdentifiers(null, null, null,
        null, refundCase.getOrderId(), 0, 50);
    if (CollectionUtils.isEmpty(sameOrderCases) || sameOrderCases.size() <= 1) {
      return false;
    }

    Integer earliestCaseId = null;
    for (BbpsRefundCase candidate : sameOrderCases) {
      if (candidate == null || candidate.getId() == null) {
        continue;
      }
      if (earliestCaseId == null || candidate.getId().intValue() < earliestCaseId.intValue()) {
        earliestCaseId = candidate.getId();
      }
    }
    return earliestCaseId != null && !Objects.equals(refundCase.getId(), earliestCaseId);
  }

  private Long resolveReferenceTime(Recharge recharge, BbpsRefundCase refundCase) {
    if (recharge != null && StringUtils.isNumeric(recharge.getUpdateDateTime())) {
      return Long.valueOf(recharge.getUpdateDateTime());
    }

    if (refundCase != null && refundCase.getUpdateDateTime() != null) {
      return refundCase.getUpdateDateTime();
    }

    return null;
  }

  private Long resolveSchedulerObservationStartTime(Recharge recharge, BbpsRefundCase refundCase) {
    if (refundCase != null && refundCase.getCreateDateTime() != null
        && refundCase.getCreateDateTime().longValue() > 0L) {
      return refundCase.getCreateDateTime();
    }

    if (recharge != null && StringUtils.isNumeric(recharge.getUpdateDateTime())) {
      return Long.valueOf(recharge.getUpdateDateTime());
    }

    if (refundCase != null && refundCase.getUpdateDateTime() != null
        && refundCase.getUpdateDateTime().longValue() > 0L) {
      return refundCase.getUpdateDateTime();
    }

    return null;
  }

  private String resolveCustomerMobileFromDipcoinTransaction(DipcoinTransaction dipcoinTransaction) {
    Integer customerAccountId =
        dipcoinTransaction != null ? positiveInteger(dipcoinTransaction.getCustomerAccountId()) : null;
    if (customerAccountId == null) {
      return null;
    }

    CustomerAccount customerAccount = customerDBService.getAccountById(customerAccountId);
    if (customerAccount != null && StringUtils.isNotBlank(customerAccount.getMobileNumber())) {
      return customerAccount.getMobileNumber();
    }

    return null;
  }

  private Integer positiveInteger(Number value) {
    if (value == null) {
      return null;
    }
    int normalized = value.intValue();
    return normalized > 0 ? Integer.valueOf(normalized) : null;
  }

  private boolean containsIgnoreCase(String value, String... tokens) {
    if (StringUtils.isBlank(value) || tokens == null) {
      return false;
    }
    for (String token : tokens) {
      if (StringUtils.isNotBlank(token) && StringUtils.containsIgnoreCase(value, token)) {
        return true;
      }
    }
    return false;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.isNotBlank(value)) {
        return StringUtils.trim(value);
      }
    }
    return null;
  }

  private <T> T firstNonNull(T primary, T fallback) {
    return primary != null ? primary : fallback;
  }

  private void syncProviderRefundStatus(BbpsRefundCase refundCase, String providerRefundStatus) {
    if (refundCase == null) {
      return;
    }
    refundCase.setProviderRefundStatus(StringUtils.trimToNull(providerRefundStatus));
    refundCase.setProviderRefundCheckedAt(now());
  }

  private boolean isRefundAlreadyInitiatedResponse(String providerRefundStatus,
      String responseCode, String responseMessage) {
    String normalizedProviderRefundStatus =
        StringUtils.upperCase(StringUtils.trimToEmpty(providerRefundStatus));
    return StringUtils.equals(normalizedProviderRefundStatus, "ALREADY_INITIATED")
        || (StringUtils.equals(StringUtils.trimToEmpty(responseCode), "400")
            && containsIgnoreCase(responseMessage, "already initiated"));
  }

  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return String.valueOf(value);
    }
  }

  private String buildDetailedReason(String reason, String message) {
    String normalizedReason = StringUtils.trimToNull(reason);
    String normalizedMessage = StringUtils.trimToNull(message);
    String baseReason = buildManualReviewRawReason(normalizedReason);
    if (StringUtils.isBlank(baseReason)) {
      return truncateReasonText(normalizeStoredReasonText(normalizedMessage));
    }
    return truncateReasonText(appendReasonContext(baseReason, normalizedMessage));
  }

  private void replaceRefundRawReason(BbpsRefundCase refundCase, String rawReason) {
    if (refundCase == null) {
      return;
    }
    refundCase.setRefundRawReason(truncateRawReasonText(StringUtils.trimToNull(rawReason)));
  }

  private void ensureRefundRawReason(BbpsRefundCase refundCase, String rawReason) {
    if (refundCase == null || StringUtils.isNotBlank(refundCase.getRefundRawReason())) {
      return;
    }
    replaceRefundRawReason(refundCase, rawReason);
  }

  private void ensureRefundRawReasonForRefundDecision(BbpsRefundCase refundCase) {
    if (refundCase == null || StringUtils.isBlank(refundCase.getRefundReason())) {
      return;
    }
    ensureRefundRawReason(refundCase, buildRefundRequiredRawReason(refundCase.getRefundReason()));
  }

  private String resolveManualReviewReason(String existingReason, String requestedReason) {
    if (StringUtils.isBlank(requestedReason)) {
      return StringUtils.trimToNull(existingReason);
    }
    if (StringUtils.equals(requestedReason, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT)) {
      return requestedReason;
    }
    return firstNonBlank(existingReason, requestedReason);
  }

  private void updateRefundRawReasonForManualReview(BbpsRefundCase refundCase, String existingReason,
      String requestedReason, String resolvedReason, String message) {
    if (refundCase == null) {
      return;
    }

    String manualReviewContext =
        buildDetailedReason(firstNonBlank(requestedReason, resolvedReason), message);
    boolean shouldReplace = StringUtils.isBlank(refundCase.getRefundRawReason())
        || !StringUtils.equals(StringUtils.trimToNull(existingReason), resolvedReason)
        || StringUtils.equals(requestedReason, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT);

    if (shouldReplace) {
      replaceRefundRawReason(refundCase,
          appendReasonContext(buildRefundRequiredRawReason(resolvedReason), manualReviewContext));
      return;
    }

    replaceRefundRawReason(refundCase,
        appendReasonContext(refundCase.getRefundRawReason(), manualReviewContext));
  }

  private String buildRefundRawReasonText(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return null;
    }

    String refundStatus = StringUtils.trimToEmpty(refundCase.getRefundStatus());
    String refundReason = StringUtils.trimToEmpty(refundCase.getRefundReason());
    String responseMessage = StringUtils.trimToNull(refundCase.getRefundResponseMessage());

    String baseReason;
    if (StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.MANUAL_REVIEW)) {
      baseReason = buildManualReviewRawReason(refundReason);
      baseReason = appendReasonContext(baseReason,
          normalizeStoredReasonText(refundCase.getManualReviewReason()));
    } else if (shouldTreatAsNoRefundRequired(refundCase)) {
      baseReason = appendReasonContext(buildNoRefundRequiredRawReason(refundCase), responseMessage);
    } else if (StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.SUCCESS)) {
      baseReason = appendReasonContext(buildRefundSuccessRawReason(refundReason), responseMessage);
    } else if (StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.FAILED)) {
      baseReason = appendReasonContext(buildRefundFailureRawReason(refundReason), responseMessage);
    } else if (StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.PENDING)
        && Boolean.TRUE.equals(refundCase.getRefundRequired())) {
      baseReason = appendReasonContext(buildRefundPendingRawReason(refundReason), responseMessage);
    } else if (Boolean.TRUE.equals(refundCase.getRefundRequired())) {
      baseReason = appendReasonContext(buildRefundRequiredRawReason(refundReason), responseMessage);
    } else if (StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.PENDING)) {
      baseReason = appendReasonContext(buildDecisionPendingRawReason(refundReason), responseMessage);
    } else {
      baseReason = appendReasonContext(buildGenericRefundRawReason(refundReason), responseMessage);
    }

    return appendDipcoinCancelContext(baseReason, refundCase);
  }

  private boolean shouldTreatAsNoRefundRequired(BbpsRefundCase refundCase) {
    if (refundCase == null) {
      return false;
    }

    String refundStatus = StringUtils.trimToEmpty(refundCase.getRefundStatus());
    String refundReason = StringUtils.trimToEmpty(refundCase.getRefundReason());

    if (StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.NOT_REQUIRED)) {
      return true;
    }

    if (Boolean.TRUE.equals(refundCase.getRefundRequired())) {
      return false;
    }

    return StringUtils.equals(refundStatus, BbpsRefundConstants.RefundStatus.SUCCESS)
        || StringUtils.equalsAny(refundReason,
            BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED,
            BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE,
            BbpsRefundConstants.RefundReason.BILLPAY_SUCCESS);
  }

  private String buildNoRefundRequiredRawReason(BbpsRefundCase refundCase) {
    String refundReason = StringUtils.trimToEmpty(refundCase != null ? refundCase.getRefundReason() : null);
    String billpayStatus = StringUtils.trimToEmpty(refundCase != null ? refundCase.getBillpayStatus() : null);

    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.BILLPAY_SUCCESS)) {
      return "Refund is not required because the bill payment completed successfully.";
    }

    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED)) {
      if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.NOT_CALLED)) {
        return "Refund is not required because the payment was not collected successfully and the bill payment was not submitted.";
      }
      if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.FAILED)) {
        return "Refund is not required because the payment was not collected successfully and the bill payment failed.";
      }
      if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.SUCCESS)) {
        return "Refund is not required because the bill payment succeeded, but the payment was not collected successfully.";
      }
      if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.PENDING)) {
        return "Refund is not required because the payment was not collected successfully while the bill payment is still pending confirmation.";
      }
      return "Refund is not required because the payment was not collected successfully.";
    }

    if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.NOT_CALLED)) {
      return "Refund is not required because no successful payment collection was found and the bill payment was not submitted.";
    }
    if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.PENDING)) {
      return "Refund is not required because no successful payment collection was found while the bill payment is still pending confirmation.";
    }
    if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.SUCCESS)) {
      return "Refund is not required because the bill payment succeeded and no successful payment collection was found.";
    }
    if (StringUtils.equals(billpayStatus, BbpsRefundConstants.BillpayStatus.FAILED)) {
      return "Refund is not required because the bill payment failed and no successful payment collection was found.";
    }
    return "Refund is not required because no successful payment collection was found for this case.";
  }

  private String buildRefundRequiredRawReason(String refundReason) {
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.BILLPAY_FAILED)) {
      return "Refund is required because the payment was collected successfully, but the bill payment failed.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.ORPHAN_AG_DEBIT)) {
      return "Refund is required because the payment was collected successfully, but the bill payment was not submitted within the allowed time.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.ADD_MONEY_FAILED)) {
      return "Refund is required because the payment was collected successfully, but add-money settlement failed.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT)) {
      return "Refund is required because a duplicate payment was detected for the same bill.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT)) {
      return "Refund is required because the bill payment was successful earlier, but a later complaint or biller rejection now requires reversal.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED)) {
      return "Refund is required, but the system could not initiate it automatically.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT)) {
      return "Refund was initiated for this case, but the final refund status is still awaiting confirmation.";
    }
    return buildGenericReasonSentence("Refund is required because", refundReason,
        "Refund is required for this case based on the latest payment and bill status.");
  }

  private String buildRefundSuccessRawReason(String refundReason) {
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.BILLPAY_FAILED)) {
      return "Refund was completed because the payment was collected successfully, but the bill payment failed.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.ORPHAN_AG_DEBIT)) {
      return "Refund was completed because the payment was collected successfully, but the bill payment was not submitted within the allowed time.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.ADD_MONEY_FAILED)) {
      return "Refund was completed because the payment was collected successfully, but add-money settlement failed.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT)) {
      return "Refund was completed because a duplicate payment was detected for the same bill.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT)) {
      return "Refund was completed because the bill payment was successful earlier, but a later complaint or biller rejection required reversal.";
    }
    return buildGenericReasonSentence("Refund was completed because", refundReason,
        "Refund was completed successfully for this case.");
  }

  private String buildRefundPendingRawReason(String refundReason) {
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.BILLPAY_FAILED)) {
      return "Refund is in progress because the payment was collected successfully, but the bill payment failed.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.ORPHAN_AG_DEBIT)) {
      return "Refund is in progress because the payment was collected successfully, but the bill payment was not submitted within the allowed time.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.ADD_MONEY_FAILED)) {
      return "Refund is in progress because the payment was collected successfully, but add-money settlement failed.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT)) {
      return "Refund is in progress because a duplicate payment was detected for the same bill.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT)) {
      return "Refund is in progress because the bill payment was successful earlier, but a later complaint or biller rejection now requires reversal.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT)) {
      return "Refund is in progress and the system is waiting for the provider to confirm the final refund status.";
    }
    return buildGenericReasonSentence("Refund is in progress because", refundReason,
        "Refund is currently in progress for this case.");
  }

  private String buildRefundFailureRawReason(String refundReason) {
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED)) {
      return "Refund could not be initiated automatically for this case.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT)) {
      return "Refund could not be closed automatically because the final provider refund status was not confirmed.";
    }
    return buildGenericReasonSentence("Refund could not be completed because", refundReason,
        "Refund could not be completed automatically for this case.");
  }

  private String buildDecisionPendingRawReason(String refundReason) {
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT)) {
      return "A duplicate payment was detected for the same bill, but the system is still waiting to confirm whether the payment was actually collected before deciding on refund.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.BILLPAY_FAILED)) {
      return "The bill payment failed, but the system is still waiting to confirm whether the payment was actually collected before deciding on refund.";
    }
    return "Refund decision is pending while the system verifies the payment and bill status.";
  }

  private String buildManualReviewRawReason(String refundReason) {
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT)) {
      return "Refund needs manual review because the bill payment was successful earlier, but a later complaint or biller rejection now requires admin action.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED)) {
      return "Refund needs manual review because the system could not initiate the refund automatically.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT)) {
      return "Refund needs manual review because the final provider refund status could not be confirmed automatically.";
    }
    if (StringUtils.equals(refundReason, BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT)) {
      return "Refund needs manual review because a duplicate payment was detected and the automatic refund flow could not finish.";
    }
    return buildGenericReasonSentence("Refund needs manual review because", refundReason,
        "Refund needs manual review for this case.");
  }

  private String buildGenericRefundRawReason(String refundReason) {
    String description = describeRefundReason(refundReason);
    if (StringUtils.isBlank(description)) {
      return "Refund case is being evaluated based on the latest payment and bill status.";
    }
    return ensureSentence(description);
  }

  private String buildGenericReasonSentence(String prefix, String refundReason,
      String defaultSentence) {
    String description = describeRefundReason(refundReason);
    if (StringUtils.isBlank(description)) {
      return defaultSentence;
    }
    return prefix + " " + lowercaseFirstCharacter(trimTrailingPunctuation(description)) + ".";
  }

  private String appendDipcoinCancelContext(String baseReason, BbpsRefundCase refundCase) {
    if (refundCase == null || !Boolean.TRUE.equals(refundCase.getDipcoinCancelRequired())) {
      return baseReason;
    }

    String dipcoinStatus = StringUtils.trimToEmpty(refundCase.getDipcoinCancelStatus());
    String dipcoinSentence;
    if (StringUtils.equals(dipcoinStatus, BbpsRefundConstants.DipcoinCancelStatus.SUCCESS)) {
      dipcoinSentence = "Dipcoin cancellation was completed for this case.";
    } else if (StringUtils.equals(dipcoinStatus, BbpsRefundConstants.DipcoinCancelStatus.MANUAL_REVIEW)) {
      dipcoinSentence = "Dipcoin cancellation also needs manual review.";
    } else if (StringUtils.equalsAny(dipcoinStatus,
        BbpsRefundConstants.DipcoinCancelStatus.NEW,
        BbpsRefundConstants.DipcoinCancelStatus.PENDING)) {
      dipcoinSentence = "Dipcoin cancellation is also in progress for this case.";
    } else {
      dipcoinSentence = "Dipcoin cancellation is also required for this case.";
    }

    String combinedReason = appendSentence(baseReason, dipcoinSentence);
    return appendReasonContext(combinedReason, refundCase.getLastDipcoinCancelError());
  }

  private String appendReasonContext(String baseReason, String contextMessage) {
    String normalizedBase = StringUtils.trimToNull(baseReason);
    String normalizedContext = normalizeStoredReasonText(contextMessage);
    if (normalizedContext == null) {
      return normalizedBase;
    }
    if (normalizedBase == null) {
      return ensureSentence(normalizedContext);
    }

    String comparableBase = trimTrailingPunctuation(normalizedBase);
    String comparableContext = trimTrailingPunctuation(normalizedContext);
    if (StringUtils.equalsIgnoreCase(comparableBase, comparableContext)
        || containsIgnoreCase(comparableBase, comparableContext)
        || containsIgnoreCase(comparableContext, comparableBase)) {
      return ensureSentence(normalizedBase);
    }

    return appendSentence(normalizedBase, "Latest update: " + comparableContext);
  }

  private String appendSentence(String baseReason, String addition) {
    String normalizedBase = StringUtils.trimToNull(baseReason);
    String normalizedAddition = StringUtils.trimToNull(addition);
    if (normalizedBase == null) {
      return ensureSentence(normalizedAddition);
    }
    if (normalizedAddition == null) {
      return ensureSentence(normalizedBase);
    }

    String comparableBase = trimTrailingPunctuation(normalizedBase);
    String comparableAddition = trimTrailingPunctuation(normalizedAddition);
    if (StringUtils.equalsIgnoreCase(comparableBase, comparableAddition)
        || containsIgnoreCase(comparableBase, comparableAddition)
        || containsIgnoreCase(comparableAddition, comparableBase)) {
      return ensureSentence(normalizedBase);
    }

    return ensureSentence(normalizedBase) + " " + ensureSentence(normalizedAddition);
  }

  private String normalizeStoredReasonText(String value) {
    String normalized = StringUtils.trimToNull(value);
    if (normalized == null) {
      return null;
    }

    int separatorIndex = normalized.indexOf(':');
    if (separatorIndex > 0) {
      String possibleCode = normalized.substring(0, separatorIndex).trim();
      if (possibleCode.matches("[A-Z0-9_]+")) {
        normalized = StringUtils.trimToNull(normalized.substring(separatorIndex + 1));
      }
    }
    return normalized;
  }

  private String ensureSentence(String value) {
    String normalized = StringUtils.trimToNull(value);
    if (normalized == null) {
      return null;
    }
    return StringUtils.endsWithAny(normalized, ".", "!", "?") ? normalized : normalized + ".";
  }

  private String trimTrailingPunctuation(String value) {
    String normalized = StringUtils.trimToEmpty(value);
    while (StringUtils.endsWithAny(normalized, ".", "!", "?")) {
      normalized = normalized.substring(0, normalized.length() - 1).trim();
    }
    return normalized;
  }

  private String lowercaseFirstCharacter(String value) {
    String normalized = StringUtils.trimToEmpty(value);
    if (normalized.isEmpty()) {
      return normalized;
    }

    char firstChar = normalized.charAt(0);
    if (!Character.isLetter(firstChar)) {
      return normalized;
    }
    return Character.toLowerCase(firstChar) + normalized.substring(1);
  }

  private String describeRefundReason(String reason) {
    if (StringUtils.isBlank(reason)) {
      return null;
    }

    switch (StringUtils.trim(reason)) {
      case BbpsRefundConstants.RefundReason.BILLPAY_FAILED:
        return "Bill payment was attempted but failed after the add-money leg succeeded";
      case BbpsRefundConstants.RefundReason.ORPHAN_AG_DEBIT:
        return "Aggrepay debit succeeded, but bill payment was not submitted within the grace window";
      case BbpsRefundConstants.RefundReason.AG_DEBIT_FAILED:
        return "Aggrepay payment/debit failed before add-money settlement could continue";
      case BbpsRefundConstants.RefundReason.NO_AG_DEBIT_EVIDENCE:
        return "No Aggrepay debit evidence was found within the scheduler observation window";
      case BbpsRefundConstants.RefundReason.ADD_MONEY_FAILED:
        return "Aggrepay debit succeeded, but the downstream add-money settlement failed";
      case BbpsRefundConstants.RefundReason.DUPLICATE_DEBIT:
        return "A duplicate debit/payment was detected for the same billing flow";
      case BbpsRefundConstants.RefundReason.LATE_BILLER_REJECT:
        return "A complaint or late biller rejection requires admin refund handling";
      case BbpsRefundConstants.RefundReason.BILLPAY_SUCCESS:
        return "Bill payment completed successfully, so refund is not required";
      case BbpsRefundConstants.RefundReason.REFUND_INITIATION_FAILED:
        return "Refund was required, but initiation could not be completed";
      case BbpsRefundConstants.RefundReason.REFUND_STATUS_TIMEOUT:
        return "Refund was initiated, but the final provider status could not be confirmed";
      default:
        return null;
    }
  }

  private String truncateLargeText(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > 3900 ? value.substring(0, 3900) : value;
  }

  private String truncateReasonText(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > 240 ? value.substring(0, 240) : value;
  }

  private String truncateRawReasonText(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > 950 ? value.substring(0, 950) : value;
  }

  private String truncateMediumText(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > 950 ? value.substring(0, 950) : value;
  }

  private String truncateShortText(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > 480 ? value.substring(0, 480) : value;
  }

  private long now() {
    return DateTime.now(DateTimeZone.UTC).getMillis();
  }

  private long effectiveOrphanGraceMs() {
    return orphanGraceMs > 0 ? orphanGraceMs
        : BbpsRefundConstants.Scheduler.DEFAULT_ORPHAN_GRACE_MS;
  }

  private long effectiveNoAgEvidenceCloseMs() {
    return noAgEvidenceCloseMs > 0 ? noAgEvidenceCloseMs
        : BbpsRefundConstants.Scheduler.DEFAULT_NO_AG_EVIDENCE_CLOSE_MS;
  }

  private boolean isSchedulerSource(String source) {
    return StringUtils.equals(source, BbpsRefundConstants.StatusSource.SCHEDULER);
  }

  private static final class MerchantCancellationContext {
    private final Merchant merchant;
    private final User user;

    private MerchantCancellationContext(Merchant merchant, User user) {
      this.merchant = merchant;
      this.user = user;
    }
  }

  private static final class DipcoinCancellationSnapshot {
    private final String cancelTxnRefId;
    private final boolean dipcoinUpdated;

    private DipcoinCancellationSnapshot(String cancelTxnRefId, boolean dipcoinUpdated) {
      this.cancelTxnRefId = cancelTxnRefId;
      this.dipcoinUpdated = dipcoinUpdated;
    }

    private static DipcoinCancellationSnapshot empty() {
      return new DipcoinCancellationSnapshot(null, false);
    }

    private boolean confirmed() {
      return StringUtils.isNotBlank(cancelTxnRefId) && dipcoinUpdated;
    }
  }

  private static final class FinacusStatus {
    private final String status;
    private final String rawResponse;

    private FinacusStatus(String status, String rawResponse) {
      this.status = status;
      this.rawResponse = rawResponse;
    }
  }
}
