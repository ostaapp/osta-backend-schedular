# Bill Reminder Scheduler Flow

## Purpose

This document explains the BBPS bill reminder scheduler flow in `osta-backend-schedular`, including trigger configuration, reminder event types, candidate selection, dedupe behavior, email handling, and expected final behavior.

## Module Scope

- Main app: `com.dipcoin.scheduler.OstaBackendSchedularApplication`
- Scheduler class: `com.dipcoin.scheduler.scheduled.BillReminderScheduler`
- Job interface: `com.dipcoin.scheduler.job.BillReminderJob`
- Job implementation: `com.dipcoin.scheduler.job.BbpsBillReminderJob`
- Log table/entity: `BbpsBillReminderLog`

## Configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `com.dipcoin.scheduler.enabled` | `true` | Enables/disables bill reminder scheduler execution. |
| `com.dipcoin.scheduler.zone-id` | `Asia/Kolkata` | Time zone used by schedule trigger and date calculation. |
| `com.dipcoin.scheduler.bill-reminder.cron` | `0 */5 * * * *` in module properties, `0 0 14 * * *` in production profile | Controls bill reminder run frequency. |

Production config currently schedules bill reminders at 2:00 PM IST.

## High-Level Flow

1. Scheduler runs on configured cron and zone.
2. If `com.dipcoin.scheduler.enabled=false`, execution is skipped.
3. Scheduler calls `BillReminderJob.runReminderCycle()`.
4. Job resolves `today` using configured scheduler zone.
5. Job processes reminder events for five calculated due dates.
6. Scheduler-level exceptions are caught and logged, so future cron executions continue.

## Reminder Events

| Event type | Due date checked | Meaning |
| --- | --- | --- |
| `DUE_IN_7_DAYS` | `today + 7 days` | Customer bill is due in 7 days. |
| `DUE_IN_3_DAYS` | `today + 3 days` | Customer bill is due in 3 days. |
| `DUE_TODAY` | `today` | Customer bill is due today. |
| `BILL_EXPIRED` | `today - 1 day` | Bill due date has passed. |
| `BILL_PAY_REMINDER_AFTER_EXPIRED` | `today - 2 days` | Reminder after expiry. |

Reminder processing is skipped when calculated due date is before `01/01/2026`.

## Candidate Selection

The job reads BBPS successful service transactions with a due date:

- `source = BBPS`
- `status = 0` / success
- `requestType = 1` / service
- `dueDate IS NOT NULL`

Then it filters:

- Only transactions updated in the last 5 months are considered.
- Only completed/successful bill payments are considered.
- Due date must parse successfully.
- Due date must match the event due date exactly.

Supported due date formats:

- `dd/MM/yyyy`
- `d/M/yyyy`
- `yyyy-MM-dd`

## Latest-Cycle Rule

For each unique customer + biller + consumer number, only the latest transaction is selected.

Key used:

- `customerId`
- resolved biller ID
- `consumerNo`

Latest transaction is decided by:

1. Higher `updateDateTime`
2. If tied, higher recharge ID

This avoids sending reminders based on an older paid bill cycle when a newer cycle exists for the same customer/biller/consumer.

## Reminder Dedupe Behavior

For each candidate, a SHA-256 dedupe key is built from:

- `customerId`
- `billerId`
- `consumerNo`
- formatted due date
- event type

Behavior:

- Duplicate keys inside the same run are skipped.
- If a log already exists in `BbpsBillReminderLog`, reminder is skipped.
- The DB table has a unique key on `dedupeKey`.

This means one customer/biller/consumer/due-date/event combination should be logged only once.

## Email Preparation

For each valid candidate:

1. Resolve biller ID from `billPaymentsInfo`.
2. If missing, fallback to `spCode`.
3. If still missing, use `UNKNOWN`.
4. Resolve biller name from `BillPaymentsInfo`.
5. If biller name is not found, fallback to recharge type or biller ID.
6. Resolve amount from `amountDue`, fallback to `amount`.
7. Resolve user by numeric `customerId`.
8. Use user email for sending.

## Email Behavior Scenarios

| Scenario | Behavior |
| --- | --- |
| User/email not found | Insert reminder log with `emailStatus = PENDING` and provider response `EMAIL_PENDING_NO_EMAIL`. No email is sent. |
| Email send returns true | Insert reminder log with `emailStatus = SENT` and provider response `EMAIL_SENT`. |
| Email send returns false | Insert reminder log with `emailStatus = FAILED` and provider response `EMAIL_FAILED`. |
| Candidate processing throws exception | Error is logged; scheduler continues with next candidate. |

## Email Content

Subject:

- `BILL_EXPIRED`: `Osta- BBPS bill expired`
- All other events: `Osta- BBPS bill reminder`

Message includes:

- Customer first name, fallback `Customer`
- Event-specific reminder line
- Biller name
- Masked consumer number
- Amount
- Due date
- Request to pay through Osta to avoid late fees or interruption

Consumer number masking:

- If length is 4 or less, value is shown as-is.
- If longer than 4, only last 4 digits are shown with `XXXX` prefix.

## Bill Reminder Log Table

Table: `BbpsBillReminderLog`

Important columns:

- `dedupeKey`
- `rechargeId`
- `customerId`
- `email`
- `billerId`
- `billerName`
- `consumerNo`
- `dueDate`
- `eventType`
- `emailStatus`
- `message`
- `providerResponse`
- `createdAt`

`dedupeKey` is unique and prevents repeat reminders for the same event.

## Operational Notes

- Scheduler catches/logs exceptions at entry point, so future cron runs continue.
- Candidate-level exceptions are caught, logged, and the job continues with the next candidate.
- `FAILED` email attempts are logged with a dedupe key, so the same event will not be retried automatically unless the log is manually handled.
- Missing customer email creates a `PENDING` log, but the same dedupe rule means the same reminder event will not be retried automatically after email is later added.
- The log table must exist before enabling the bill reminder job in a target DB.

