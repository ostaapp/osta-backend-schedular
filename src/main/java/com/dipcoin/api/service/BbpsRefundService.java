package com.dipcoin.api.service;

import com.dipcoin.api.model.CustomerBillPayRequest;
import com.dipcoin.api.model.PartnerRefundSyncRequest;
import com.dipcoin.scheduler.model.BbpsRefundCase;
import com.dipcoin.scheduler.model.Recharge;

public interface BbpsRefundService {

  BbpsRefundCase syncRechargeState(Recharge recharge, String source);

  BbpsRefundCase syncRechargeState(Recharge recharge, CustomerBillPayRequest billPayRequest,
      String source);

  void triggerRefundAfterCommit(Integer rechargeId, String source);

  BbpsRefundCase processRefundCase(Integer refundCaseId, String source);

  BbpsRefundCase syncPartnerRefundUpdate(PartnerRefundSyncRequest request);

  void runSchedulerCycle(Long startTime, Long endTime);

  BbpsRefundCase getRefundCase(Integer rechargeId, String clientTransactionId,
      String dipcoinTransRefId);

  String getRefundRawReason(BbpsRefundCase refundCase);
}
