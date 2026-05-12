package com.dipcoin.db.services.dao;

import java.util.List;

import com.dipcoin.scheduler.model.Recharge;

public interface RechargeDao extends GenericDao<Recharge> {

	public List<Recharge> findRechargeByRequestTypeAndStartTimeAndEndTime(Integer requestType,Long startTime,Long endTime);
	
	public Recharge findTransactionStatus(final String ostaTransactionReferenceId);
	
	public Recharge findBillDetail(final String billPaymentToken);
	
	public Recharge findByPartnerTxnRefId(String partnerTransactionRefId);
	
	public Recharge findByClientTransactionId(String clientTransactionId);

}


