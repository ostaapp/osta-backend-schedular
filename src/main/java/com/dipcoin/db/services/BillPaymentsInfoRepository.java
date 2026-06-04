package com.dipcoin.db.services;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dipcoin.db.services.model.BillPaymentsInfo;

@Repository("dbBillPaymentsInfoRepository")
public interface BillPaymentsInfoRepository extends JpaRepository<BillPaymentsInfo, Long> {

    @Query("SELECT b FROM BillPaymentsInfo b WHERE b.status = 'ACTIVE' AND b.billerCategoryId = :categoryId")
    List<BillPaymentsInfo> findActiveByCategoryId(Long categoryId);
}
