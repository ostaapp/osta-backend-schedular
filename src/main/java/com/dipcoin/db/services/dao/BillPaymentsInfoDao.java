package com.dipcoin.db.services.dao;

import java.util.List;
import com.dipcoin.db.services.model.BillPaymentsInfo;

public interface BillPaymentsInfoDao extends GenericDao<BillPaymentsInfo> {
	
	public List<BillPaymentsInfo> findProviders(String billerCategoryName, String status, Integer start, Integer count);
	
    public BillPaymentsInfo findByBillerIdAndPartnerReferenceId(String billerId, String partnerReferenceId);

    public List<BillPaymentsInfo> findByBillerId(String billerId);

    public void save(BillPaymentsInfo billPaymentsInfo);

    public List<BillPaymentsInfo> findAllBiller();

}
