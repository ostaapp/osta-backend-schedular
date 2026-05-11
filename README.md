# OSTA Backend Schedular

Standalone Spring Boot service for new backend scheduler jobs.

## Current Scope

- Keep new scheduler logic outside `osta-api-bbps-module`.
- Add new jobs here first, starting with bill reminders.
- Existing reviewed BBPS schedulers stay in BBPS until a separate migration is approved.

## Run

```bash
mvn spring-boot:run
```

The service starts with an in-memory H2 datasource by default so the scheduler shell can boot locally without real DB credentials.
Override these values in deployment or a local profile when jobs need production data:

```properties
OSTA_SCHEDULER_DB_URL=jdbc:mysql://host:3306/dipcoin
OSTA_SCHEDULER_DB_USERNAME=dipcoindb
OSTA_SCHEDULER_DB_PASSWORD=...
```

## Scheduler Properties

```properties
com.dipcoin.scheduler.enabled=true
com.dipcoin.scheduler.zone-id=Asia/Kolkata
com.dipcoin.scheduler.bill-reminder.cron=*/30 * * * * *
```

Email uses the same `osta-api-common` `EmailClient` flow as BBPS:
`dev`, `stage`, `test`, and `unit` use `PostfixEmailClient`; `uat` and `prod` use `SesEmailClient`.

## Database

Run `src/main/resources/db/create_bbps_bill_reminder_log.sql` once in the target DB before enabling the bill reminder job.
