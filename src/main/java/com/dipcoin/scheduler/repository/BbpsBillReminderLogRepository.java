package com.dipcoin.scheduler.repository;

import com.dipcoin.scheduler.model.BbpsBillReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BbpsBillReminderLogRepository extends JpaRepository<BbpsBillReminderLog, Integer> {

  BbpsBillReminderLog findFirstByDedupeKey(String dedupeKey);
}
