package com.dipcoin.db.services;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.dipcoin.db.services.dao.RechargePlanDetailsDao;
import com.dipcoin.db.services.model.RechargePlanDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
@Component("rechargePlanDBService")
public class RechargePlanDetailsDBServiceImpl extends RechargePlanDetailsDBService{


    private final RechargePlanDetailsDao rechargePlanDetailsDao;

    @Override
    public void saveOrUpdate(RechargePlanDetails plan) {
        rechargePlanDetailsDao.saveOrUpdate(plan);
    }

    @Override
    public RechargePlanDetails findByPlanId(String planId) {
        return rechargePlanDetailsDao.findByPlanId(planId);
    }

    @Override
    public List<RechargePlanDetails> findAll() {
        return rechargePlanDetailsDao.findAll();
    }

	@Override
	public List<RechargePlanDetails> getPlans(String billerId, String zone) {
		return rechargePlanDetailsDao.getPlans(billerId, zone);
	}

	@Override
	public List<RechargePlanDetails> getZones(String billerId) {
		return rechargePlanDetailsDao.getZones(billerId);
	}

	@Override
	public Optional<RechargePlanDetails> findByBillerAndPlanId(String billerId, String planId) {
		return rechargePlanDetailsDao.findByBillerAndPlanId(billerId, planId);
	}

	@Override
	public void Update(RechargePlanDetails existingPlan) {
		rechargePlanDetailsDao.update(existingPlan);
	}
//    @Override
//    public void saveValidationRegex(String billerId, String regex) {
//        rechargePlanDetailsDao.saveRegex(billerId, regex);
//    }

}
