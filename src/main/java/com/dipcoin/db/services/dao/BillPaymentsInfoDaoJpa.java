package com.dipcoin.db.services.dao;

import java.util.List;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.dipcoin.db.services.model.BillPaymentsInfo;

@Component("billpaymentsInfoDao")
public class BillPaymentsInfoDaoJpa extends GenericDaoImpl<BillPaymentsInfo>
    implements BillPaymentsInfoDao {

  private final static String BillPaymentsInfo_By_BillerIdAndPartnerReferenceId =
      "SELECT b FROM BillPaymentsInfo b where b.billerId = :billerId and b.partnerReferenceId = :partnerReferenceId";
  private final static String BillPaymentsInfo_By_BillerId =
	      "SELECT b FROM BillPaymentsInfo b WHERE b.billerId = :billerId";
  private final static String BillPaymentsInfo_AllBiller =
	        "SELECT b FROM BillPaymentsInfo b";
  private final static String BillPaymentsInfo_ByBillerCategoryName =
	      "SELECT b FROM BillPaymentsInfo b WHERE b.billerCategoryName = :billerCategoryName and b.status=:status";

  

  @Override
  public com.dipcoin.db.services.model.BillPaymentsInfo findByBillerIdAndPartnerReferenceId(
      String billerId, String partnerReferenceId) {
    TypedQuery<BillPaymentsInfo> query =
        getEm().createQuery(BillPaymentsInfo_By_BillerIdAndPartnerReferenceId, BillPaymentsInfo.class);
    query.setParameter("billerId", billerId);
    query.setParameter("partnerReferenceId", partnerReferenceId);

    List<BillPaymentsInfo> list = query.getResultList();
    if (list == null || list.size() == 0)
      return null;
    return list.get(0);
  }


@Override
public List<com.dipcoin.db.services.model.BillPaymentsInfo> findByBillerId(String billerId) {
	TypedQuery<BillPaymentsInfo> query =
	        getEm().createQuery(BillPaymentsInfo_By_BillerId, BillPaymentsInfo.class);
	    query.setParameter("billerId", billerId);

	    List<BillPaymentsInfo> list = query.getResultList();
	    if (list == null || list.size() == 0)
	      return java.util.Collections.emptyList();
	    return list;
}


public void save(BillPaymentsInfo billPaymentsInfo) {

	  if (billPaymentsInfo.getId() == null) {
		    getEm().persist(billPaymentsInfo);
		} else {
		    getEm().merge(billPaymentsInfo);   // UPDATE
		}

	  getEm().flush();
}

@Override
public List<BillPaymentsInfo> findAllBiller() {

    TypedQuery<BillPaymentsInfo> query =
            getEm().createQuery(BillPaymentsInfo_AllBiller, BillPaymentsInfo.class);

    return query.getResultList();  // always returns empty list if none
}

@Override
public List<BillPaymentsInfo> findProviders(String billerCategoryName, String status, Integer start, Integer count) {
  TypedQuery<BillPaymentsInfo> query =
      getEm().createQuery(BillPaymentsInfo_ByBillerCategoryName, BillPaymentsInfo.class);
  query.setParameter("billerCategoryName", billerCategoryName);
  query.setParameter("status", status);
  
  if (start != null)
    query.setFirstResult(start);
  if (count != null)
    query.setMaxResults(count);

  List<BillPaymentsInfo> list = query.getResultList();
  if (list == null || list.size() == 0)
    return null;
  return list;
}

}
