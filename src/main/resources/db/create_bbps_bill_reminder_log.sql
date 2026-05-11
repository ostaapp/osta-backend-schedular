CREATE TABLE IF NOT EXISTS BbpsBillReminderLog (
  id INT NOT NULL AUTO_INCREMENT,
  dedupeKey VARCHAR(255) NOT NULL,
  rechargeId INT NULL,
  customerId VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  billerId VARCHAR(64) NULL,
  billerName VARCHAR(255) NULL,
  consumerNo VARCHAR(128) NULL,
  dueDate VARCHAR(32) NULL,
  eventType VARCHAR(64) NULL,
  emailStatus VARCHAR(32) NULL,
  message VARCHAR(1000) NULL,
  providerResponse VARCHAR(1000) NULL,
  createdAt BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bbps_bill_reminder_log_dedupe_key (dedupeKey)
);
