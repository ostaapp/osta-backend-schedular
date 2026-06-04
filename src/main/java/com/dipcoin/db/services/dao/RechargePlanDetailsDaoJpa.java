package com.dipcoin.db.services.dao;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import com.dipcoin.db.services.model.RechargePlanDetails;

@Component("rechargePlanDetailsDao")
public class RechargePlanDetailsDaoJpa extends GenericDaoImpl<RechargePlanDetails> implements RechargePlanDetailsDao {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public RechargePlanDetails findByPlanId(String planId) {
		try {
			return entityManager
					.createQuery("SELECT r FROM RechargePlanDetails r WHERE r.planId = :pid", RechargePlanDetails.class)
					.setParameter("pid", planId).getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@Transactional
	public void saveOrUpdate(RechargePlanDetails plan) {
		// Check if a plan with same billerId + planId already exists
		RechargePlanDetails existing = null;
		try {
			existing = entityManager
					.createQuery(
							"SELECT p FROM RechargePlanDetails p WHERE p.billerId = :billerId AND p.planId = :planId",
							RechargePlanDetails.class)
					.setParameter("billerId", plan.getBillerId()).setParameter("planId", plan.getPlanId())
					.getResultStream().findFirst().orElse(null);
		} catch (Exception e) {
			// Optional: log if needed
		}

		if (existing != null) {
			// Copy all fields to existing entity
			existing.setAmount(plan.getAmount());
			existing.setPlanDescription(plan.getPlanDescription());
			existing.setType(plan.getType());
			existing.setValidity(plan.getValidity());
			existing.setData(plan.getData());
			existing.setTalktime(plan.getTalktime());
			existing.setStatus(plan.getStatus());
			existing.setCategoryType(plan.getCategoryType());
			existing.setZone(plan.getZone());
			existing.setAddedDeleted(plan.getAddedDeleted());
			existing.setPartnerReferenceId(plan.getPartnerReferenceId());

			entityManager.merge(existing); // update existing
		} else {
			entityManager.persist(plan); // insert new
		}
	}

	@Override
	public List<RechargePlanDetails> findAll() {
		return entityManager.createQuery("SELECT r FROM RechargePlanDetails r", RechargePlanDetails.class)
				.getResultList();
	}

	@Override
	public List<RechargePlanDetails> getPlans(String billerId, String zone) {
		return entityManager
				.createQuery("SELECT r FROM RechargePlanDetails r WHERE r.billerId = :billerId AND r.zone = :zone",
						RechargePlanDetails.class)
				.setParameter("billerId", billerId).setParameter("zone", zone).getResultList();
	}

	@Override
	public List<RechargePlanDetails> getZones(String billerId) {
		return entityManager.createQuery("SELECT r FROM RechargePlanDetails r WHERE r.billerId = :billerId",
				RechargePlanDetails.class).setParameter("billerId", billerId).getResultList();
	}

	@Override
	public Optional<RechargePlanDetails> findByBillerAndPlanId(String billerId, String planId) {
		RechargePlanDetails plan = entityManager
				.createQuery("SELECT r FROM RechargePlanDetails r WHERE r.billerId = :billerId AND r.planId = :planId",
						RechargePlanDetails.class)
				.setParameter("billerId", billerId).setParameter("planId", planId).getResultStream().findFirst()
				.orElse(null);

		return Optional.ofNullable(plan);
	}

//    @Override
//    @Transactional
//    public void saveRegex(String billerId, String regex) {
//        // Option: store regex in RechargePlanDetails rows for this biller (if you want single row per biller)
//        // Simpler: upsert into a small table "BillerValidation" — but since we're in quick fix,
//        // we'll persist a simple native upsert into a separate table (create table if not exist earlier).
//        entityManager.createNativeQuery(
//            "INSERT INTO BillerValidation (BillerId, ValidationRegex) " +
//            "VALUES (:billerId, :regex) " +
//            "ON DUPLICATE KEY UPDATE ValidationRegex = :regex")
//            .setParameter("billerId", billerId)
//            .setParameter("regex", regex)
//            .executeUpdate();
//    }
}
