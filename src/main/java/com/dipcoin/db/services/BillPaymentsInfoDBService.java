package com.dipcoin.db.services;

import java.util.List;
import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import com.dipcoin.db.services.model.BillPaymentsInfo;

public abstract class BillPaymentsInfoDBService {
	
	 abstract public List<BillPaymentsInfo> getProviders(final String billerCategory, final String status, final Integer start, final Integer count);

	  @Async
	  public Future<List<BillPaymentsInfo>> asyncGetProviders(String billerCategory, String status, Integer start,  Integer count) {
	    return new AsyncResult<>(getProviders(billerCategory, status, start, count));
	  }
	
	  abstract public BillPaymentsInfo getBillerByBillerIdAndPartnerReferenceId(String billerId, String partnerReferenceId);

	  @Async
	  public Future<BillPaymentsInfo> asyncGetBillerByBillerIdAndPartnerReferenceId(String billerId, String partnerReferenceId) {
	    return new AsyncResult<>(getBillerByBillerIdAndPartnerReferenceId(billerId, partnerReferenceId));
	  }
	
      public abstract List<BillPaymentsInfo> getBillPaymentsInfoByBillerId(String billerId);

      public abstract void save(BillPaymentsInfo billPaymentsInfo);

      public abstract List<BillPaymentsInfo> getAllBillers();

}
