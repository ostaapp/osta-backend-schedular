package com.dipcoin.scheduler.scheduled;

import com.dipcoin.scheduler.config.SchedulerProperties;
import com.dipcoin.scheduler.job.BillReminderJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BillReminderVerificationScheduler {

  private static final Logger LOG =
      LoggerFactory.getLogger(BillReminderVerificationScheduler.class);

  private final SchedulerProperties schedulerProperties;

  private final BillReminderJob billReminderJob;

  @Value("${com.dipcoin.bbps.billReminder.liveCheck.enabled:true}")
  private boolean liveCheckEnabled;

  public BillReminderVerificationScheduler(SchedulerProperties schedulerProperties,
      BillReminderJob billReminderJob) {
    this.schedulerProperties = schedulerProperties;
    this.billReminderJob = billReminderJob;
  }

  @Scheduled(
      cron = "${com.dipcoin.scheduler.bill-reminder.verification-cron:0 */10 14-18 * * *}",
      zone = "${com.dipcoin.scheduler.zone-id:Asia/Kolkata}")
  public void runBillReminderVerificationScheduler() {
    if (!schedulerProperties.isEnabled()) {
      LOG.debug("Bill reminder verification skipped because com.dipcoin.scheduler.enabled=false.");
      return;
    }

    if (!liveCheckEnabled) {
      LOG.debug("Bill reminder verification skipped because live bill check is disabled.");
      return;
    }

    LOG.info("Bill reminder verification scheduler started.");
    try {
      billReminderJob.processPendingVerifications();
    } catch (Exception e) {
      LOG.error("Bill reminder verification scheduler failed.", e);
    }
  }
}
