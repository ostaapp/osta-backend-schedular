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
