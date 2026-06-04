package com.dipcoin.db.services.dao;

import java.util.List;
import java.util.Optional;

import com.dipcoin.db.services.model.RechargePlanDetails;

public interface RechargePlanDetailsDao extends GenericDao<RechargePlanDetails> {
	RechargePlanDetails findByPlanId(String planId);

    void saveOrUpdate(RechargePlanDetails plan);

    List<RechargePlanDetails> findAll();

	List<RechargePlanDetails> getPlans(String billerId, String zone);

	List<RechargePlanDetails> getZones(String billerId);

	Optional<RechargePlanDetails> findByBillerAndPlanId(String billerId, String planId);
    
//    void saveRegex(String billerId, String regex);
}
