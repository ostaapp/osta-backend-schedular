package com.dipcoin.scheduler.scheduled;

import com.dipcoin.scheduler.config.SchedulerProperties;
import com.dipcoin.scheduler.job.BillReminderJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BillReminderScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(BillReminderScheduler.class);

  private final SchedulerProperties schedulerProperties;

  private final BillReminderJob billReminderJob;

  public BillReminderScheduler(SchedulerProperties schedulerProperties, BillReminderJob billReminderJob) {
    this.schedulerProperties = schedulerProperties;
    this.billReminderJob = billReminderJob;
  }

  @Scheduled(
      cron = "${com.dipcoin.scheduler.bill-reminder.cron:0 0 14 * * *}",
      zone = "${com.dipcoin.scheduler.zone-id:Asia/Kolkata}")
  public void runBillReminderScheduler() {
    if (!schedulerProperties.isEnabled()) {
      LOG.debug("Scheduler execution skipped because com.dipcoin.scheduler.enabled=false.");
      return;
    }

    LOG.info("Bill reminder scheduler started.");
    try {
      billReminderJob.runReminderCycle();
    } catch (Exception e) {
      LOG.error("Bill reminder scheduler failed.", e);
    }
  }
}
