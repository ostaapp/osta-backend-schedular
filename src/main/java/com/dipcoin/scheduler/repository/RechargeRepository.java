package com.dipcoin.scheduler.repository;

import com.dipcoin.db.services.model.Recharge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechargeRepository extends JpaRepository<Recharge, Integer> {

  List<Recharge> findBySourceAndStatusAndRequestTypeAndDueDateIsNotNull(String source,
      Integer status, Integer requestType);
}
