package com.dipcoin.scheduler.constants;

public final class BbpsRefundConstants {

  private BbpsRefundConstants() {}

  public static final class Scheduler {
    public static final long DEFAULT_LOOKBACK_MS = 30L * 24L * 60L * 60L * 1000L;
    public static final long DEFAULT_ORPHAN_GRACE_MS = 10L * 60L * 1000L;
    public static final long DEFAULT_NO_AG_EVIDENCE_CLOSE_MS = 30L * 60L * 1000L;

    private Scheduler() {}
  }

  public static final class AgStatus {
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String FAILED = "FAILED";
    public static final String PENDING = "PENDING";
    public static final String DEBITED = "DEBITED";
    public static final String SUCCESS = "SUCCESS";
    public static final String UNKNOWN = "UNKNOWN";

    private AgStatus() {}
  }

  public static final class BillpayStatus {
    public static final String NOT_CALLED = "NOT_CALLED";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String UNKNOWN = "UNKNOWN";

    private BillpayStatus() {}
  }

  public static final class RefundStatus {
    public static final String NEW = "NEW";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String MANUAL_REVIEW = "MANUAL_REVIEW";
    public static final String NOT_REQUIRED = "NOT_REQUIRED";

    private RefundStatus() {}
  }

  public static final class DipcoinCancelStatus {
    public static final String NEW = "NEW";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String MANUAL_REVIEW = "MANUAL_REVIEW";
    public static final String NOT_REQUIRED = "NOT_REQUIRED";

    private DipcoinCancelStatus() {}
  }

  public static final class RefundReason {
    public static final String BILLPAY_FAILED = "BILLPAY_FAILED";
    public static final String ORPHAN_AG_DEBIT = "ORPHAN_AG_DEBIT";
    public static final String AG_DEBIT_FAILED = "AG_DEBIT_FAILED";
    public static final String NO_AG_DEBIT_EVIDENCE = "NO_AG_DEBIT_EVIDENCE";
    public static final String ADD_MONEY_FAILED = "ADD_MONEY_FAILED";
    public static final String DUPLICATE_DEBIT = "DUPLICATE_DEBIT";
    public static final String LATE_BILLER_REJECT = "LATE_BILLER_REJECT";
    public static final String BILLPAY_SUCCESS = "BILLPAY_SUCCESS";
    public static final String REFUND_INITIATION_FAILED = "REFUND_INITIATION_FAILED";
    public static final String REFUND_STATUS_TIMEOUT = "REFUND_STATUS_TIMEOUT";

    private RefundReason() {}
  }

  public static final class StatusSource {
    public static final String INLINE = "INLINE";
    public static final String SCHEDULER = "SCHEDULER";
    public static final String FINACUS = "FINACUS";
    public static final String AGGREPAY = "AGGREPAY";
    public static final String INTERNAL = "INTERNAL";
    public static final String ADMIN = "ADMIN";

    private StatusSource() {}
  }
}
