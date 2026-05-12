package com.dipcoin.db.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.db.services.dao.BbpsRefundCaseDao;
import com.dipcoin.scheduler.model.BbpsRefundCase;

@Component("bbpsRefundCaseDBService")
public class BbpsRefundCaseDBServiceImpl extends BbpsRefundCaseDBService {

  @Autowired
  private BbpsRefundCaseDao bbpsRefundCaseDao;

  @Override
  @Transactional
  public BbpsRefundCase addCase(BbpsRefundCase refundCase) {
    return bbpsRefundCaseDao.create(refundCase);
  }

  @Override
  @Transactional
  public BbpsRefundCase updateCase(BbpsRefundCase refundCase) {
    return bbpsRefundCaseDao.update(refundCase);
  }

  @Override
  public BbpsRefundCase find(Integer id) {
    return bbpsRefundCaseDao.find(id);
  }

  @Override
  public BbpsRefundCase getByPaymentKey(String paymentKey) {
    return bbpsRefundCaseDao.findByPaymentKey(paymentKey);
  }

  @Override
  public BbpsRefundCase getByRechargeId(Integer rechargeId) {
    return bbpsRefundCaseDao.findByRechargeId(rechargeId);
  }

  @Override
  public BbpsRefundCase getByDipcoinTransRefId(String dipcoinTransRefId) {
    return bbpsRefundCaseDao.findByDipcoinTransRefId(dipcoinTransRefId);
  }

  @Override
  public List<BbpsRefundCase> findByIdentifiers(String paymentKey, Integer rechargeId,
      String clientTransactionId, String dipcoinTransRefId, String orderId, Integer start,
      Integer count) {
    return bbpsRefundCaseDao.findByIdentifiers(paymentKey, rechargeId, clientTransactionId,
        dipcoinTransRefId, orderId, start, count);
  }

  @Override
  public List<BbpsRefundCase> findRecentCases(Long startTime, Long endTime, Integer start,
      Integer count) {
    return bbpsRefundCaseDao.findRecentCases(startTime, endTime, start, count);
  }

  @Override
  public List<BbpsRefundCase> getRefundHistoryAdmin(String mobileNo, Long startTime, Long endTime,
      Integer start, Integer count) {
    return bbpsRefundCaseDao.findRefundHistoryAdmin(mobileNo, startTime, endTime, start, count);
  }

  @Override
  public Long getRefundHistoryCountAdmin(String mobileNo, Long startTime, Long endTime) {
    return bbpsRefundCaseDao.findRefundHistoryCountAdmin(mobileNo, startTime, endTime);
  }
}
