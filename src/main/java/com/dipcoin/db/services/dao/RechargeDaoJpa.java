package com.dipcoin.db.services.dao;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.model.Recharge;

import io.micrometer.core.instrument.util.StringUtils;

@Component("rechargeDao")
public class RechargeDaoJpa extends GenericDaoImpl<Recharge> implements RechargeDao {
	
	private final static String RechargeDao_findByRequestTypeAndStartTimeAndEndTime = "SELECT r FROM Recharge r WHERE r.requestType = :requestType AND FROM_UNIXTIME(r.updateDateTime/1000) >=  FROM_UNIXTIME(:startTime) "
			+ " AND FROM_UNIXTIME(r.updateDateTime/1000) <=  FROM_UNIXTIME(:endTime) ";
	private final static String Customer_RechargeStatus = "SELECT r FROM Recharge r WHERE r.dipcoinTransRefId = :ostaTransactionReferenceId";
	private final static String Customer_BillDetail = "SELECT r FROM Recharge r WHERE r.billPaymentToken = :billPaymentToken ORDER BY r.updateDateTime DESC, r.id DESC";
	private final static String Find_ByPartnerTransRefId = "SELECT r FROM Recharge r WHERE r.partnerTransRefId = :partnerTransRefId ORDER BY r.updateDateTime DESC, r.id DESC";
	private final static String Find_ByClientTransactionId = "SELECT r FROM Recharge r WHERE r.clientTransactionId = :clientTransactionId ORDER BY CASE WHEN r.status = :pendingStatus THEN 1 ELSE 0 END, r.updateDateTime DESC, r.id DESC";
	
	@Override
	public List<Recharge> findRechargeByRequestTypeAndStartTimeAndEndTime(Integer requestType, Long startTime,
			Long endTime) {

		TypedQuery<Recharge> query = getEm().createQuery(RechargeDao_findByRequestTypeAndStartTimeAndEndTime,
				Recharge.class);
		query.setParameter("requestType", requestType);
		query.setParameter("startTime", startTime / 1000);
		query.setParameter("endTime", endTime / 1000);

		List<Recharge> list = query.getResultList();
		if (CollectionUtils.isEmpty(list))
			return null;

		return list;
	}
	
	@Override
	public List<Recharge> findRechargesWithComplaints() {
		String queryStr = "SELECT r FROM Recharge r WHERE r.complaintId IS NOT NULL";
		TypedQuery<Recharge> query = getEm().createQuery(queryStr, Recharge.class);
		return query.getResultList();
	}
	
	@Override
	public Recharge findTransactionStatus(String ostaTransactionReferenceId) {
		TypedQuery<Recharge> query = getEm().createQuery(Customer_RechargeStatus, Recharge.class);
		query.setParameter("ostaTransactionReferenceId", ostaTransactionReferenceId);

		List<Recharge> rechargeStatus = query.getResultList();
		if (CollectionUtils.isEmpty(rechargeStatus))
			return null;

		return rechargeStatus.get(0);
	}

	@Override
	public Recharge findBillDetail(String billPaymentToken) {
		TypedQuery<Recharge> query = getEm().createQuery(Customer_BillDetail, Recharge.class);
		query.setParameter("billPaymentToken", billPaymentToken);
		query.setMaxResults(1);

		List<Recharge> list = query.getResultList();

		if (list == null || list.size() == 0)
			return null;

		return list.get(0);

	}
	
	@Override
	public Recharge findByPartnerTxnRefId(String partnerTransRefId) {
		TypedQuery<Recharge> query = getEm().createQuery(Find_ByPartnerTransRefId, Recharge.class);
		query.setParameter("partnerTransRefId", partnerTransRefId);
		query.setMaxResults(1);

		List<Recharge> recharge = query.getResultList();
		if (recharge == null || recharge.size() == 0)
			return null;

		return recharge.get(0);
	}
	
	@Override
	public Recharge findByClientTransactionId(String clientTransactionId) {
		TypedQuery<Recharge> query = getEm().createQuery(Find_ByClientTransactionId, Recharge.class);
		query.setParameter("clientTransactionId", clientTransactionId);
		query.setParameter("pendingStatus", DBConstants.PartnerTransactionStatus.PENDING.value());
		query.setMaxResults(1);
		List<Recharge> list = query.getResultList();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	@Override
	public int markPaymentNotInitiatedIfPending(Integer rechargeId, Integer pendingStatus, Integer failedStatus,
			String responseCode, String responseMessage, String bbpsTxnStatus, Boolean refundRequired,
			String updateDateTime) {
		if (rechargeId == null || rechargeId <= 0) {
			return 0;
		}
		return getEm().createQuery("UPDATE Recharge r SET r.status = :failedStatus, "
				+ "r.responseCode = :responseCode, r.responseMessage = :responseMessage, "
				+ "r.bbpsTxnStatus = :bbpsTxnStatus, r.isRefundRequired = :refundRequired, "
				+ "r.updateDateTime = :updateDateTime WHERE r.id = :rechargeId "
				+ "AND r.status = :pendingStatus")
				.setParameter("failedStatus", failedStatus)
				.setParameter("responseCode", responseCode)
				.setParameter("responseMessage", responseMessage)
				.setParameter("bbpsTxnStatus", bbpsTxnStatus)
				.setParameter("refundRequired", refundRequired)
				.setParameter("updateDateTime", updateDateTime)
				.setParameter("rechargeId", rechargeId)
				.setParameter("pendingStatus", pendingStatus)
				.executeUpdate();
	}

}
