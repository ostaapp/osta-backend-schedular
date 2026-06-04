package com.dipcoin.db.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.db.services.model.RechargePlan;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RechargePlanService {

    private final RechargePlanRepository rechargePlanRepository;

    @Transactional
    public void deactivatePlansForBillerAndCircle(String billerId, String circle) {
        rechargePlanRepository.deactivateByBillerAndCircle(billerId, circle);
    }

    public Optional<RechargePlan> findExisting(String billerId, String circle, String planId) {
        return rechargePlanRepository.findByBillerIdAndCircleCodeAndPlanId(billerId, circle, planId);
    }

    @Transactional
    public RechargePlan save(RechargePlan plan) {
        return rechargePlanRepository.save(plan);
    }

    @Transactional
    public RechargePlan updateExisting(RechargePlan existing, RechargePlan incoming) {
        // update the required fields
        existing.setRechargeValue(incoming.getRechargeValue());
        existing.setRechargeTalkTime(incoming.getRechargeTalkTime());
        existing.setRechargeValidity(incoming.getRechargeValidity());
        existing.setEuronetPlanType(incoming.getEuronetPlanType());
        existing.setEuronetRechargeType(incoming.getEuronetRechargeType());
        existing.setRechargeShortDescription(incoming.getRechargeShortDescription());
        existing.setRechargeDescription(incoming.getRechargeDescription());

        // fields from requirements (refresh)
        existing.setService(incoming.getService());
        existing.setServiceProviderCode(incoming.getServiceProviderCode());
        existing.setChargeCode(incoming.getChargeCode());
        existing.setOperatorMaster(incoming.getOperatorMaster());
        existing.setPartnerReferenceId(incoming.getPartnerReferenceId());
        existing.setProductType(incoming.getProductType());
        existing.setRechargeMaster(incoming.getRechargeMaster());
        existing.setAddedDeleted(incoming.getAddedDeleted());
        existing.setTarrifId(incoming.getTarrifId());
        existing.setStatus(incoming.getStatus());

        return rechargePlanRepository.save(existing);
    }
}
