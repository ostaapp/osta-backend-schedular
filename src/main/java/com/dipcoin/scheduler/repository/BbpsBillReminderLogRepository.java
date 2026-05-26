package com.dipcoin.scheduler.repository;

import com.dipcoin.db.services.model.BbpsBillReminderLog;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BbpsBillReminderLogRepository extends JpaRepository<BbpsBillReminderLog, Integer> {

  BbpsBillReminderLog findFirstByDedupeKey(String dedupeKey);

  List<BbpsBillReminderLog>
      findTop100ByVerificationStatusInAndVerificationStartedAtLessThanEqualOrderByCreatedAtAsc(
          Collection<String> verificationStatuses, Long verificationStartedAt);
}
