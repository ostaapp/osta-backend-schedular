package com.dipcoin.db.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.db.services.dao.BillPaymentsInfoDao;
import com.dipcoin.db.services.model.BillPaymentsInfo;
import com.dipcoin.db.services.model.Recharge;

@Component("billPaymentsInfoDBService")
public class BillPaymentsInfoDBServiceImpl extends BillPaymentsInfoDBService {

  @Autowired
  private BillPaymentsInfoDao billPaymentsInfoDao;

  @Override
  public List<BillPaymentsInfo> getProviders(String billerCategoryName, String status, Integer start, Integer count) {
    return this.billPaymentsInfoDao.findProviders(billerCategoryName, status, start, count);
  }

  @Override
  public BillPaymentsInfo getBillerByBillerIdAndPartnerReferenceId(String billerId,
      String partnerReferenceId) {
    return this.billPaymentsInfoDao.findByBillerIdAndPartnerReferenceId(billerId, partnerReferenceId);
  }

@Override
public List<BillPaymentsInfo> getBillPaymentsInfoByBillerId(String billerId) {
	return this.billPaymentsInfoDao.findByBillerId(billerId);
}

@Override
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void save(BillPaymentsInfo billPaymentsInfo) {
	    billPaymentsInfoDao.save(billPaymentsInfo);
	}

@Override
public List<BillPaymentsInfo> getAllBillers() {
    return this.billPaymentsInfoDao.findAllBiller();
}


}

