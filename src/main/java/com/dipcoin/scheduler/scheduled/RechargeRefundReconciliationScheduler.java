package com.dipcoin.scheduler.scheduled;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dipcoin.api.service.BbpsRefundService;

@Component
@EnableScheduling
public class RechargeRefundReconciliationScheduler {

  private static final Logger LOG =
      LogManager.getLogger(RechargeRefundReconciliationScheduler.class);
  private static final long DEFAULT_LOOKBACK_MS = 24L * 60L * 60L * 1000L;

  @Value("${com.dipcoin.reconciliation.refundScheduler.lookbackMs:86400000}")
  private long lookbackMs;

  @Autowired
  private BbpsRefundService bbpsRefundService;

  @Scheduled(cron = "${com.dipcoin.reconciliation.refundScheduler.reconcileCron:0 */5 * * * *}")
  public void runRefundScheduler() {
    long endTime = System.currentTimeMillis();
    long effectiveLookbackMs = effectiveLookbackMs();
    long startTime = endTime - effectiveLookbackMs;
    LOG.info("BBPS refund scheduler started. startTime={}, endTime={}, lookbackMs={}", startTime,
        endTime, effectiveLookbackMs);

    try {
      bbpsRefundService.runSchedulerCycle(startTime, endTime);
    } catch (Exception e) {
      LOG.error("BBPS refund scheduler failed.", e);
    }
  }

  private long effectiveLookbackMs() {
    return lookbackMs > 0 ? lookbackMs : DEFAULT_LOOKBACK_MS;
  }
}
