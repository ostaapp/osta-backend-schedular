package com.dipcoin.db.services;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.dipcoin.db.services.model.RechargePlan;
import org.springframework.transaction.annotation.Transactional;

public interface RechargePlanRepository extends JpaRepository<RechargePlan, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE RechargePlan r SET r.status = 0 WHERE r.billerId = :billerId AND r.circleCode = :circleCode")
    int deactivateByBillerAndCircle(String billerId, String circleCode);

    @Query("SELECT r FROM RechargePlan r WHERE r.billerId = :billerId AND r.circleCode = :circleCode AND r.planId = :planId")
    Optional<RechargePlan> findByBillerIdAndCircleCodeAndPlanId(String billerId, String circleCode, String planId);

}
