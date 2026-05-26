package com.dipcoin.scheduler.job;

import java.time.LocalDate;

public interface BillReminderJob {

  void runReminderCycle();

  void runReminderCycle(LocalDate today);

  void processPendingVerifications();
}
