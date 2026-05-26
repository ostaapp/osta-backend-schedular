# Refund Reconciliation Scheduler Flow

## Purpose

This document explains the BBPS refund scheduler in a simple way for TL review.

The scheduler checks recent BBPS bill payment transactions and decides:

- Refund required
- Refund not required
- Wait and check again later
- Move to manual review

The main decision is based on two things:

1. Did Aggrepay debit happen?
2. What happened to bill payment?

## Main Classes

| Area | Class |
| --- | --- |
| Scheduler trigger | `RechargeRefundReconciliationScheduler` |
| Refund decision logic | `BbpsRefundServiceImpl` |
| Refund case table | `BbpsRefundCase` |

## Scheduler Timing

| Property | Meaning |
| --- | --- |
| `com.dipcoin.reconciliation.refundScheduler.reconcileCron` | When scheduler runs. Default is every 5 minutes. Production profile uses every 10 minutes. |
| `com.dipcoin.reconciliation.refundScheduler.lookbackMs` | How much old data scheduler checks. Default is last 24 hours. |
| `com.dipcoin.reconciliation.refundScheduler.orphanGraceMs` | Wait time before refunding AG debit when billpay was not called. Default is 10 minutes. |
| `com.dipcoin.reconciliation.refundScheduler.noAgEvidenceCloseMs` | Wait time before closing cases where no AG debit evidence is found. Default is 30 minutes. |

## Simple Flow

1. Scheduler runs by cron.
2. It checks recent `Recharge` records from the last configured lookback period.
3. It creates or updates a `BbpsRefundCase`.
4. It refreshes latest payment status from Aggrepay, Finacus/BBPS, recharge, and Dipcoin data.
5. It decides the refund case status.
6. If refund is initiated, scheduler checks refund status in later runs.
7. If Dipcoin was used, scheduler also handles Dipcoin cancellation after refund is accepted.

## Important Status Meaning

### Aggrepay / AG Status

| AG status | Simple meaning |
| --- | --- |
| `SUCCESS` / `DEBITED` | Customer money was debited / payment collection happened. Refund may be needed if bill payment did not complete. |
| `FAILED` | Customer money was not debited successfully. Refund is not required. |
| `NOT_FOUND` | System could not find debit evidence yet. Scheduler waits first, then may close as no refund required. |
| `PENDING` / `UNKNOWN` | Payment status is not clear yet. Scheduler waits or moves to manual review based on case. |

### Billpay Status

| Billpay status | Simple meaning |
| --- | --- |
| `SUCCESS` | Bill payment completed successfully. Refund is not required. |
| `FAILED` | Bill payment failed. Refund is required only if AG debit happened. |
| `NOT_CALLED` | Money may have been debited, but bill payment API was not called. Refund is required if AG debit is confirmed and grace time is over. |
| `PENDING` | Bill payment status is still not final. Scheduler waits unless AG debit failed or no evidence timeout is over. |
| `UNKNOWN` | Not enough clear information. Scheduler may wait or move to manual review. |

## Main Scenario Matrix

This is the main TL-friendly decision table.

| AG debit/payment collection | Billpay status | Scheduler behavior | Final/next status |
| --- | --- | --- | --- |
| AG debit success | Billpay success | No refund needed because bill payment is complete. | `NOT_REQUIRED`, reason `BILLPAY_SUCCESS` |
| AG debit success | Billpay failed | Refund is needed because money was debited but bill payment failed. | Initiate refund, reason `BILLPAY_FAILED` |
| AG debit success | Billpay not called | Wait for 10-minute orphan grace period. If still not called, refund is needed. | `PENDING`, then initiate refund with reason `ORPHAN_AG_DEBIT` |
| AG debit success | Billpay pending | Wait for final billpay/Finacus status. | `PENDING` |
| AG debit failed | Billpay success | Refund not needed because AG debit failed. This is an inconsistent case, but system closes no refund. | `NOT_REQUIRED`, reason `AG_DEBIT_FAILED` |
| AG debit failed | Billpay failed | Refund not needed because customer debit did not succeed. | `NOT_REQUIRED`, reason `AG_DEBIT_FAILED` |
| AG debit failed | Billpay not called | Refund not needed because customer debit did not succeed and billpay was not called. | `NOT_REQUIRED`, reason `AG_DEBIT_FAILED` |
| AG debit failed | Billpay pending | Refund not needed because customer debit did not succeed. | `NOT_REQUIRED`, reason `AG_DEBIT_FAILED` |
| No AG debit evidence | Billpay failed | Wait up to 30 minutes for AG evidence. If still no evidence, close without refund. | `PENDING`, then `NOT_REQUIRED`, reason `NO_AG_DEBIT_EVIDENCE` |
| No AG debit evidence | Billpay not called | Wait up to 30 minutes for AG evidence. If still no evidence, close without refund. | `PENDING`, then `NOT_REQUIRED`, reason `NO_AG_DEBIT_EVIDENCE` |
| No AG debit evidence | Billpay pending | Wait up to 30 minutes. If still no evidence, close without refund. | `PENDING`, then `NOT_REQUIRED`, reason `NO_AG_DEBIT_EVIDENCE` |
| No AG debit evidence | Billpay success | Refund not required because bill payment succeeded. | `NOT_REQUIRED`, reason `BILLPAY_SUCCESS` |
| AG status unknown/pending | Any non-final billpay status | Wait for clearer status. | `PENDING` |
| AG and billpay states cannot be classified | Any | Move to manual review. | `MANUAL_REVIEW`, reason `REFUND_INITIATION_FAILED` |

## Detailed Case Scenarios

### Case 1: AG Debit Success + Billpay Failed

Meaning:

- Customer amount was debited successfully.
- BBPS bill payment failed.

Behavior:

- Refund is required.
- Scheduler initiates Aggrepay refund.
- Refund reason is `BILLPAY_FAILED`.
- If refund initiation succeeds/pends, case becomes `PENDING` or `SUCCESS`.
- If refund initiation fails or required data is missing, case goes to `MANUAL_REVIEW`.

### Case 2: AG Debit Success + Billpay Not Called

Meaning:

- Customer amount was debited successfully.
- Bill payment API was not called.

Behavior:

- Scheduler waits for orphan grace period, default 10 minutes.
- During grace period, case stays `PENDING`.
- After grace period, refund is required.
- Refund reason is `ORPHAN_AG_DEBIT`.
- Scheduler initiates Aggrepay refund.

### Case 3: AG Debit Success + Billpay Success

Meaning:

- Customer amount was debited.
- Bill payment also completed successfully.

Behavior:

- Refund is not required.
- Case becomes `NOT_REQUIRED`.
- Refund reason is `BILLPAY_SUCCESS`.

### Case 4: AG Debit Success + Billpay Pending

Meaning:

- Customer amount was debited.
- Bill payment is not final yet.

Behavior:

- Scheduler waits.
- Case stays `PENDING`.
- Later scheduler runs check Finacus/BBPS again.
- Once billpay becomes success, refund is not required.
- Once billpay becomes failed, refund is initiated.

### Case 5: AG Debit Failed + Billpay Failed

Meaning:

- Customer debit failed.
- Bill payment also failed.

Behavior:

- Refund is not required because money was not collected successfully.
- Case becomes `NOT_REQUIRED`.
- Refund reason is `AG_DEBIT_FAILED`.

### Case 6: AG Debit Failed + Billpay Not Called

Meaning:

- Customer debit failed.
- Billpay API was not called.

Behavior:

- Refund is not required.
- Case becomes `NOT_REQUIRED`.
- Refund reason is `AG_DEBIT_FAILED`.

### Case 7: AG Debit Failed + Billpay Pending

Meaning:

- Customer debit failed.
- Billpay still shows pending or unclear.

Behavior:

- Refund is not required because debit failed.
- Case becomes `NOT_REQUIRED`.
- Refund reason is `AG_DEBIT_FAILED`.

### Case 8: No AG Debit Evidence + Billpay Failed

Meaning:

- Billpay failed.
- But system has not found proof that customer money was debited.

Behavior:

- Scheduler waits for configured observation window, default 30 minutes.
- If AG debit evidence appears, refund is initiated.
- If no AG debit evidence appears after timeout, refund is not required.
- Final reason is `NO_AG_DEBIT_EVIDENCE`.

### Case 9: No AG Debit Evidence + Billpay Not Called

Meaning:

- Billpay was not called.
- System has no proof of customer debit.

Behavior:

- Scheduler waits for AG evidence.
- If no evidence appears within 30 minutes, case closes as no refund required.
- Reason is `NO_AG_DEBIT_EVIDENCE`.

### Case 10: Duplicate Debit

Meaning:

- Multiple refund cases are found for the same partner `orderId`.
- Current case is not the earliest one.

Behavior:

| AG status | Behavior |
| --- | --- |
| AG debit success | Refund required. Reason `DUPLICATE_DEBIT`. |
| AG debit failed | Refund not required. Reason `AG_DEBIT_FAILED`. |
| No AG evidence after timeout | Refund not required. Reason `NO_AG_DEBIT_EVIDENCE`. |
| Still unclear | Keep `PENDING` and wait for AG confirmation. |

### Case 11: Complaint Raised After Successful Bill Payment

Meaning:

- Bill payment was successful.
- Later a complaint or biller rejection is present.
- AG debit is settled.

Behavior:

- Scheduler does not auto-refund directly.
- Case moves to `MANUAL_REVIEW`.
- Refund reason is `LATE_BILLER_REJECT`.
- Admin/TL review is required.

### Case 12: Refund Already Initiated

Meaning:

- Refund was already started earlier.

Behavior:

- Scheduler checks Aggrepay refund status.
- If provider says success, case becomes `SUCCESS`.
- If provider says pending, case remains `PENDING`.
- If provider gives failed/unknown final response, case moves to `MANUAL_REVIEW`.
- If status lookup fails temporarily, scheduler keeps the case pending for retry.

## Refund Initiation Rules

Scheduler initiates refund only when all required values are available:

- AG debit is successful/settled.
- Partner `orderId` is available.
- Refund amount is available.

If any required value is missing:

- Case moves to `MANUAL_REVIEW`.
- Reason is usually `REFUND_INITIATION_FAILED`.

## Final Refund Statuses

| Status | Meaning |
| --- | --- |
| `PENDING` | Scheduler is waiting for AG status, billpay status, or refund provider status. |
| `SUCCESS` | Refund completed successfully. |
| `NOT_REQUIRED` | Refund is not needed. |
| `MANUAL_REVIEW` | Admin/TL action is required. |
| `FAILED` | Refund flow is closed as failed. |

## Dipcoin Cancellation

After refund is accepted or in progress, scheduler checks whether Dipcoin cancellation is needed.

Dipcoin cancellation is required when:

- Refund is required.
- Refund is `PENDING` or `SUCCESS`.
- Case has Dipcoin details.

Behavior:

| Scenario | Behavior |
| --- | --- |
| No Dipcoin used | Cancellation not required. |
| Dipcoin cancellation already completed | Mark cancellation `SUCCESS`. |
| Required Dipcoin transaction/merchant data missing | Move Dipcoin cancellation to `MANUAL_REVIEW`. |
| Cancel API success or already cancelled | Confirm cancellation. |
| Cancel API temporary issue | Keep cancellation `PENDING` for retry. |
| Cancel API rejection | Move cancellation to `MANUAL_REVIEW`. |

## Operational Notes

- Scheduler processes each refund case only once per run.
- Scheduler catches and logs errors, so next cron run will continue.
- Auto-close for no AG evidence happens only in scheduler flow.
- Manual review means the scheduler could not safely complete the decision automatically.

