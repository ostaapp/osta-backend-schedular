package com.dipcoin.scheduler.repository;

import com.dipcoin.scheduler.model.BillPaymentsInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillPaymentsInfoRepository extends JpaRepository<BillPaymentsInfo, Integer> {

  BillPaymentsInfo findFirstByBillerId(String billerId);
}
