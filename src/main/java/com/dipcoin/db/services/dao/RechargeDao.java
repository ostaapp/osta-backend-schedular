package com.dipcoin.db.services.dao;

import java.util.List;

import com.dipcoin.db.services.model.Recharge;

public interface RechargeDao extends GenericDao<Recharge> {

	public List<Recharge> findRechargeByRequestTypeAndStartTimeAndEndTime(Integer requestType,Long startTime,Long endTime);
	
	public Recharge findTransactionStatus(final String ostaTransactionReferenceId);
	
	public Recharge findBillDetail(final String billPaymentToken);
	
	public Recharge findByPartnerTxnRefId(String partnerTransactionRefId);
	
	public Recharge findByClientTransactionId(String clientTransactionId);

	public int markPaymentNotInitiatedIfPending(Integer rechargeId, Integer pendingStatus,
			Integer failedStatus, String responseCode, String responseMessage, String bbpsTxnStatus,
			Boolean refundRequired, String updateDateTime);

}


