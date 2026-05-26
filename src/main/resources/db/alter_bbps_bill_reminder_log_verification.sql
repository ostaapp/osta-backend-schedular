ALTER TABLE BbpsBillReminderLog
  ADD COLUMN verificationStatus VARCHAR(64) NULL,
  ADD COLUMN verificationClientTransactionId VARCHAR(128) NULL,
  ADD COLUMN verificationStartedAt BIGINT NULL,
  ADD COLUMN verificationCompletedAt BIGINT NULL,
  ADD COLUMN verificationAttempts INT NULL DEFAULT 0,
  ADD COLUMN liveCheckStatus VARCHAR(64) NULL,
  ADD COLUMN liveCheckReason VARCHAR(500) NULL,
  ADD COLUMN liveCheckDueDate VARCHAR(32) NULL,
  ADD COLUMN liveCheckAmount DECIMAL(18,2) NULL,
  ADD COLUMN liveCheckRawResponse TEXT NULL,
  ADD COLUMN reminderSkippedReason VARCHAR(255) NULL,
  ADD COLUMN updatedAt BIGINT NULL;
