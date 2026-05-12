package com.dipcoin.db.services.dao;

import java.util.Collections;
import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.dipcoin.db.services.model.BbpsRefundCase;

@Component("bbpsRefundCaseDao")
public class BbpsRefundCaseDaoJpa extends GenericDaoImpl<BbpsRefundCase>
    implements BbpsRefundCaseDao {

  private static final String FIND_BY_PAYMENT_KEY =
      "SELECT c FROM BbpsRefundCase c WHERE c.paymentKey = :paymentKey ORDER BY c.id DESC";
  private static final String FIND_BY_RECHARGE_ID =
      "SELECT c FROM BbpsRefundCase c WHERE c.rechargeId = :rechargeId ORDER BY c.id DESC";
  private static final String FIND_BY_DIPCOIN_TXN_REF_ID =
      "SELECT c FROM BbpsRefundCase c WHERE c.dipcoinTransRefId = :dipcoinTransRefId ORDER BY c.id DESC";
  private static final String FIND_RECENT_CASES =
      "SELECT c FROM BbpsRefundCase c WHERE c.updateDateTime BETWEEN :startTime AND :endTime ORDER BY c.updateDateTime DESC, c.id DESC";
  private static final String ADMIN_REFUND_HISTORY_FILTER =
      " AND (c.refundStatus IS NULL OR c.refundStatus <> 'NOT_REQUIRED')";
  private static final String ADMIN_REFUND_HISTORY_ORDER =
      " ORDER BY CASE WHEN c.refundStatus = 'MANUAL_REVIEW' THEN 0 ELSE 1 END,"
          + " c.updateDateTime DESC, c.id DESC";
  private static final int MAX_HISTORY_RECORDS = 100;

  @Override
  public BbpsRefundCase findByPaymentKey(String paymentKey) {
    if (StringUtils.isBlank(paymentKey)) {
      return null;
    }
    TypedQuery<BbpsRefundCase> query =
        getEm().createQuery(FIND_BY_PAYMENT_KEY, BbpsRefundCase.class);
    query.setParameter("paymentKey", StringUtils.trim(paymentKey));
    query.setMaxResults(1);
    List<BbpsRefundCase> cases = query.getResultList();
    return CollectionUtils.isEmpty(cases) ? null : cases.get(0);
  }

  @Override
  public BbpsRefundCase findByRechargeId(Integer rechargeId) {
    if (rechargeId == null || rechargeId <= 0) {
      return null;
    }
    TypedQuery<BbpsRefundCase> query =
        getEm().createQuery(FIND_BY_RECHARGE_ID, BbpsRefundCase.class);
    query.setParameter("rechargeId", rechargeId);
    query.setMaxResults(1);
    List<BbpsRefundCase> cases = query.getResultList();
    return CollectionUtils.isEmpty(cases) ? null : cases.get(0);
  }

  @Override
  public BbpsRefundCase findByDipcoinTransRefId(String dipcoinTransRefId) {
    if (StringUtils.isBlank(dipcoinTransRefId)) {
      return null;
    }
    TypedQuery<BbpsRefundCase> query =
        getEm().createQuery(FIND_BY_DIPCOIN_TXN_REF_ID, BbpsRefundCase.class);
    query.setParameter("dipcoinTransRefId", StringUtils.trim(dipcoinTransRefId));
    query.setMaxResults(1);
    List<BbpsRefundCase> cases = query.getResultList();
    return CollectionUtils.isEmpty(cases) ? null : cases.get(0);
  }

  @Override
  public List<BbpsRefundCase> findByIdentifiers(String paymentKey, Integer rechargeId,
      String clientTransactionId, String dipcoinTransRefId, String orderId, Integer start,
      Integer count) {
    StringBuilder queryStr = new StringBuilder("SELECT c FROM BbpsRefundCase c WHERE 1=1");
    boolean hasFilter = false;

    if (StringUtils.isNotBlank(paymentKey)) {
      queryStr.append(" AND c.paymentKey = :paymentKey");
      hasFilter = true;
    }
    if (rechargeId != null && rechargeId > 0) {
      queryStr.append(" AND c.rechargeId = :rechargeId");
      hasFilter = true;
    }
    if (StringUtils.isNotBlank(clientTransactionId)) {
      queryStr.append(" AND c.clientTransactionId = :clientTransactionId");
      hasFilter = true;
    }
    if (StringUtils.isNotBlank(dipcoinTransRefId)) {
      queryStr.append(" AND c.dipcoinTransRefId = :dipcoinTransRefId");
      hasFilter = true;
    }
    if (StringUtils.isNotBlank(orderId)) {
      queryStr.append(" AND c.orderId = :orderId");
      hasFilter = true;
    }

    if (!hasFilter) {
      return Collections.emptyList();
    }

    queryStr.append(" ORDER BY c.updateDateTime DESC, c.id DESC");
    TypedQuery<BbpsRefundCase> query =
        getEm().createQuery(queryStr.toString(), BbpsRefundCase.class);

    if (StringUtils.isNotBlank(paymentKey)) {
      query.setParameter("paymentKey", StringUtils.trim(paymentKey));
    }
    if (rechargeId != null && rechargeId > 0) {
      query.setParameter("rechargeId", rechargeId);
    }
    if (StringUtils.isNotBlank(clientTransactionId)) {
      query.setParameter("clientTransactionId", StringUtils.trim(clientTransactionId));
    }
    if (StringUtils.isNotBlank(dipcoinTransRefId)) {
      query.setParameter("dipcoinTransRefId", StringUtils.trim(dipcoinTransRefId));
    }
    if (StringUtils.isNotBlank(orderId)) {
      query.setParameter("orderId", StringUtils.trim(orderId));
    }

    if (start != null) {
      query.setFirstResult(start);
    }
    if (count != null) {
      query.setMaxResults(count);
    }
    return query.getResultList();
  }

  @Override
  public List<BbpsRefundCase> findRecentCases(Long startTime, Long endTime, Integer start,
      Integer count) {
    if (startTime == null || endTime == null) {
      return Collections.emptyList();
    }
    TypedQuery<BbpsRefundCase> query =
        getEm().createQuery(FIND_RECENT_CASES, BbpsRefundCase.class);
    query.setParameter("startTime", startTime);
    query.setParameter("endTime", endTime);
    if (start != null) {
      query.setFirstResult(start);
    }
    if (count != null) {
      query.setMaxResults(count);
    }
    return query.getResultList();
  }

  @Override
  public List<BbpsRefundCase> findRefundHistoryAdmin(String mobileNo, Long startTime, Long endTime,
      Integer start, Integer count) {
    if (startTime == null || endTime == null) {
      return Collections.emptyList();
    }

    StringBuilder queryStr = new StringBuilder(
        "SELECT c FROM BbpsRefundCase c WHERE c.updateDateTime BETWEEN :startTime AND :endTime");
    queryStr.append(ADMIN_REFUND_HISTORY_FILTER);
    if (StringUtils.isNotBlank(mobileNo)) {
      queryStr.append(" AND c.customerMobile = :mobileNo");
    }
    queryStr.append(ADMIN_REFUND_HISTORY_ORDER);

    TypedQuery<BbpsRefundCase> query =
        getEm().createQuery(queryStr.toString(), BbpsRefundCase.class);
    query.setParameter("startTime", startTime);
    query.setParameter("endTime", endTime);
    if (StringUtils.isNotBlank(mobileNo)) {
      query.setParameter("mobileNo", StringUtils.trim(mobileNo));
    }

    int safeStart = start != null ? Math.max(0, start.intValue()) : 0;
    int safeCount = count != null ? Math.max(0, count.intValue()) : 10;

    if (safeStart >= MAX_HISTORY_RECORDS) {
      return Collections.emptyList();
    }
    if (safeStart + safeCount > MAX_HISTORY_RECORDS) {
      safeCount = MAX_HISTORY_RECORDS - safeStart;
    }

    query.setFirstResult(safeStart);
    query.setMaxResults(safeCount);

    List<BbpsRefundCase> results = query.getResultList();
    return CollectionUtils.isEmpty(results) ? Collections.emptyList() : results;
  }

  @Override
  public Long findRefundHistoryCountAdmin(String mobileNo, Long startTime, Long endTime) {
    if (startTime == null || endTime == null) {
      return 0L;
    }

    StringBuilder queryStr = new StringBuilder(
        "SELECT COUNT(c) FROM BbpsRefundCase c WHERE c.updateDateTime BETWEEN :startTime AND :endTime");
    queryStr.append(ADMIN_REFUND_HISTORY_FILTER);
    if (StringUtils.isNotBlank(mobileNo)) {
      queryStr.append(" AND c.customerMobile = :mobileNo");
    }

    TypedQuery<Long> query = getEm().createQuery(queryStr.toString(), Long.class);
    query.setParameter("startTime", startTime);
    query.setParameter("endTime", endTime);
    if (StringUtils.isNotBlank(mobileNo)) {
      query.setParameter("mobileNo", StringUtils.trim(mobileNo));
    }

    Long countResult = query.getSingleResult();
    if (countResult == null) {
      return 0L;
    }
    return countResult.longValue() > MAX_HISTORY_RECORDS ? Long.valueOf(MAX_HISTORY_RECORDS) : countResult;
  }
}
