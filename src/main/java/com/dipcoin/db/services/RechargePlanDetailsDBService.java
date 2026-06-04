package com.dipcoin.db.services;

import java.util.List;
import java.util.Optional;

import com.dipcoin.db.services.model.RechargePlanDetails;

public abstract class RechargePlanDetailsDBService {
    public abstract void saveOrUpdate(RechargePlanDetails plan);
    public abstract RechargePlanDetails findByPlanId(String planId);
    public abstract List<RechargePlanDetails> findAll();
	public abstract List<RechargePlanDetails> getPlans(String billerId, String zone);
	public abstract List<RechargePlanDetails> getZones(String billerId);
	public abstract Optional<RechargePlanDetails> findByBillerAndPlanId(String billerId, String planId);
	public abstract void Update(RechargePlanDetails existingPlan);
    

}
