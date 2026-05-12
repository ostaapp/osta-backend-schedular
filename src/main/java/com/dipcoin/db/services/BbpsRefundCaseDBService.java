package com.dipcoin.db.services;

import java.util.List;

import com.dipcoin.db.services.model.BbpsRefundCase;

public abstract class BbpsRefundCaseDBService {

  abstract public BbpsRefundCase addCase(BbpsRefundCase refundCase);

  abstract public BbpsRefundCase updateCase(BbpsRefundCase refundCase);

  abstract public BbpsRefundCase find(Integer id);

  abstract public BbpsRefundCase getByPaymentKey(String paymentKey);

  abstract public BbpsRefundCase getByRechargeId(Integer rechargeId);

  abstract public BbpsRefundCase getByDipcoinTransRefId(String dipcoinTransRefId);

  abstract public List<BbpsRefundCase> findByIdentifiers(String paymentKey, Integer rechargeId,
      String clientTransactionId, String dipcoinTransRefId, String orderId, Integer start,
      Integer count);

  abstract public List<BbpsRefundCase> findRecentCases(Long startTime, Long endTime, Integer start,
      Integer count);

  abstract public List<BbpsRefundCase> getRefundHistoryAdmin(String mobileNo, Long startTime,
      Long endTime, Integer start, Integer count);

  abstract public Long getRefundHistoryCountAdmin(String mobileNo, Long startTime, Long endTime);
}
