package com.dipcoin.db.services.dao;

import java.util.List;

import com.dipcoin.db.services.model.BbpsRefundCase;

public interface BbpsRefundCaseDao extends GenericDao<BbpsRefundCase> {

  BbpsRefundCase findByPaymentKey(String paymentKey);

  BbpsRefundCase findByRechargeId(Integer rechargeId);

  BbpsRefundCase findByDipcoinTransRefId(String dipcoinTransRefId);

  List<BbpsRefundCase> findByIdentifiers(String paymentKey, Integer rechargeId,
      String clientTransactionId, String dipcoinTransRefId, String orderId, Integer start,
      Integer count);

  List<BbpsRefundCase> findRecentCases(Long startTime, Long endTime, Integer start, Integer count);

  List<BbpsRefundCase> findRefundHistoryAdmin(String mobileNo, Long startTime, Long endTime,
      Integer start, Integer count);

  Long findRefundHistoryCountAdmin(String mobileNo, Long startTime, Long endTime);
}
