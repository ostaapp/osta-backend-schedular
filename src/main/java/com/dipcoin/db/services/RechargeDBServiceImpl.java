package com.dipcoin.db.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.db.services.dao.RechargeDao;
import com.dipcoin.db.services.model.Recharge;

@Component("rechargeDBService")
public class RechargeDBServiceImpl extends RechargeDBService {

	@Autowired
	private RechargeDao rechargeDao;
	
	@Override
	public List<Recharge> getRechargeByRequestTypeAndStartTimeAndEndTime(Integer requestType, Long startTime,
			Long endTime) {

		return rechargeDao.findRechargeByRequestTypeAndStartTimeAndEndTime(requestType, startTime, endTime);
	}
	
	@Override
	@Transactional
	public Recharge find(Integer id) {
		return rechargeDao.find(id);
	}
	
	@Override
	public Recharge getTransactionStatus(String ostaTransactionReferenceId) {
		return this.rechargeDao.findTransactionStatus(ostaTransactionReferenceId);
	}
	
	@Override
	public Recharge getBillDetail(String billPaymentToken) {
		return this.rechargeDao.findBillDetail(billPaymentToken);
	}
	
	@Override
	public Recharge getByPartnerTxnRefId(String partnerTransactionRefId) {
		return this.rechargeDao.findByPartnerTxnRefId(partnerTransactionRefId);
	}
	
	@Override
	public Recharge getByClientTransactionId(String clientTransactionId) {
		return rechargeDao.findByClientTransactionId(clientTransactionId);
	}
	
	@Override
	@Transactional
	public Recharge updateRecharge(Recharge recharge) {
		if (recharge != null && recharge.getId() <= 0 && recharge.getClientTransactionId() != null
				&& !recharge.getClientTransactionId().trim().isEmpty()) {
			Recharge existingRecharge = rechargeDao.findByClientTransactionId(recharge.getClientTransactionId());
			if (existingRecharge != null) {
				recharge.setId(existingRecharge.getId());
			}
		}
		return rechargeDao.update(recharge);
	}

	@Override
	@Transactional
	public int markPaymentNotInitiatedIfPending(Integer rechargeId, Integer pendingStatus, Integer failedStatus,
			String responseCode, String responseMessage, String bbpsTxnStatus, Boolean refundRequired,
			String updateDateTime) {
		return rechargeDao.markPaymentNotInitiatedIfPending(rechargeId, pendingStatus, failedStatus,
				responseCode, responseMessage, bbpsTxnStatus, refundRequired, updateDateTime);
	}
}
