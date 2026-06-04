package com.dipcoin.db.services;

import java.util.List;
import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import com.dipcoin.db.services.model.Recharge;


public abstract class RechargeDBService {
	
	abstract public List<Recharge> getRechargesWithComplaints();

	abstract public List<Recharge> getRechargeByRequestTypeAndStartTimeAndEndTime(Integer requestType, Long startTime,
			Long endTime);

	abstract public Recharge find(final Integer id);
	
	abstract public Recharge getTransactionStatus(final String ostaTransactionReferenceId);
	
	@Async
	public Future<Recharge> asyncGetRechargeStatus(final String ostaTransactionReferenceId) {
		return new AsyncResult<>(getTransactionStatus(ostaTransactionReferenceId));
	}

	abstract public Recharge getBillDetail(final String billPaymentToken);
	
	abstract public Recharge getByPartnerTxnRefId(final String partnerTransactionRefId);
	
	public abstract Recharge getByClientTransactionId(String clientTransactionId);
	
	abstract public Recharge updateRecharge(final Recharge recharge);

	@Async
	public Future<Recharge> asyncUpdateRecharge(final Recharge recharge) {
		return new AsyncResult<>(updateRecharge(recharge));
	}

	abstract public int markPaymentNotInitiatedIfPending(final Integer rechargeId,
			final Integer pendingStatus, final Integer failedStatus, final String responseCode,
			final String responseMessage, final String bbpsTxnStatus, final Boolean refundRequired,
			final String updateDateTime);


}
