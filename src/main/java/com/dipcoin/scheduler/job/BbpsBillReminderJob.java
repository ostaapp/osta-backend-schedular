package com.dipcoin.scheduler.job;

import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.commons.EmailClient;
import com.dipcoin.scheduler.config.SchedulerProperties;
import com.dipcoin.scheduler.constants.RechargeConstants;
import com.dipcoin.scheduler.model.BbpsBillReminderLog;
import com.dipcoin.scheduler.model.BillPaymentsInfo;
import com.dipcoin.scheduler.model.Recharge;
import com.dipcoin.scheduler.model.User;
import com.dipcoin.scheduler.repository.BbpsBillReminderLogRepository;
import com.dipcoin.scheduler.repository.BillPaymentsInfoRepository;
import com.dipcoin.scheduler.repository.RechargeRepository;
import com.dipcoin.scheduler.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class BbpsBillReminderJob implements BillReminderJob {

  private static final Logger LOG = LoggerFactory.getLogger(BbpsBillReminderJob.class);

  private static final String EVENT_DUE_IN_7_DAYS = "DUE_IN_7_DAYS";
  private static final String EVENT_DUE_IN_3_DAYS = "DUE_IN_3_DAYS";
  private static final String EVENT_DUE_TODAY = "DUE_TODAY";
  private static final String EVENT_BILL_EXPIRED = "BILL_EXPIRED";
  private static final String EVENT_BILL_PAY_REMINDER_AFTER_EXPIRED =
      "BILL_PAY_REMINDER_AFTER_EXPIRED";

  private static final String EMAIL_SENT = "SENT";
  private static final String EMAIL_FAILED = "FAILED";
  private static final String EMAIL_PENDING = "PENDING";

  private static final LocalDate MIN_VALID_DUE_DATE = LocalDate.of(2026, 1, 1);
  private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter D_M_YYYY = DateTimeFormatter.ofPattern("d/M/yyyy");
  private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final int LOOKBACK_MONTHS = 5;

  private final RechargeRepository rechargeRepository;

  private final BbpsBillReminderLogRepository reminderLogRepository;

  private final BillPaymentsInfoRepository billPaymentsInfoRepository;

  private final UserRepository userRepository;

  private final EmailClient emailClient;

  private final ApplicationProperties applicationProperties;

  private final SchedulerProperties schedulerProperties;

  public BbpsBillReminderJob(RechargeRepository rechargeRepository,
      BbpsBillReminderLogRepository reminderLogRepository,
      BillPaymentsInfoRepository billPaymentsInfoRepository, UserRepository userRepository,
      EmailClient emailClient, ApplicationProperties applicationProperties,
      SchedulerProperties schedulerProperties) {
    this.rechargeRepository = rechargeRepository;
    this.reminderLogRepository = reminderLogRepository;
    this.billPaymentsInfoRepository = billPaymentsInfoRepository;
    this.userRepository = userRepository;
    this.emailClient = emailClient;
    this.applicationProperties = applicationProperties;
    this.schedulerProperties = schedulerProperties;
  }

  @Override
  @Transactional
  public void runReminderCycle() {
    runReminderCycle(LocalDate.now(schedulerProperties.resolveZoneId()));
  }

  @Override
  @Transactional
  public void runReminderCycle(LocalDate today) {
    LocalDate runDate = today != null ? today : LocalDate.now(schedulerProperties.resolveZoneId());
    processEventForDueDate(runDate, runDate.plusDays(7), EVENT_DUE_IN_7_DAYS);
    processEventForDueDate(runDate, runDate.plusDays(3), EVENT_DUE_IN_3_DAYS);
    processEventForDueDate(runDate, runDate, EVENT_DUE_TODAY);
    processEventForDueDate(runDate, runDate.minusDays(1), EVENT_BILL_EXPIRED);
    processEventForDueDate(runDate, runDate.minusDays(2), EVENT_BILL_PAY_REMINDER_AFTER_EXPIRED);
  }

  private void processEventForDueDate(LocalDate runDate, LocalDate dueDate, String eventType) {
    if (dueDate == null || StringUtils.isBlank(eventType) || dueDate.isBefore(MIN_VALID_DUE_DATE)) {
      return;
    }

    List<Recharge> candidates = findReminderCandidates(runDate, dueDate);
    if (CollectionUtils.isEmpty(candidates)) {
      LOG.info("BBPS bill reminder found no candidates. eventType={}, dueDate={}", eventType,
          formatDueDate(dueDate));
      return;
    }

    LOG.info("BBPS bill reminder candidates found. eventType={}, dueDate={}, count={}",
        eventType, formatDueDate(dueDate), candidates.size());
    Set<String> processedKeys = new LinkedHashSet<>();
    for (Recharge recharge : candidates) {
      try {
        processRecharge(recharge, dueDate, eventType, processedKeys);
      } catch (Exception e) {
        LOG.error("Failed to process BBPS bill reminder. rechargeId={}, eventType={}",
            recharge != null ? recharge.getId() : null, eventType, e);
      }
    }
  }

  private List<Recharge> findReminderCandidates(LocalDate runDate, LocalDate dueDate) {
    ZoneId zoneId = schedulerProperties.resolveZoneId();
    LocalDate effectiveRunDate = runDate != null ? runDate : LocalDate.now(zoneId);
    long cutoffEpochMs = effectiveRunDate.minusMonths(LOOKBACK_MONTHS).atStartOfDay(zoneId).toInstant()
        .toEpochMilli();
    List<Recharge> recentTransactions =
        rechargeRepository.findBySourceAndStatusAndRequestTypeAndDueDateIsNotNull(
            RechargeConstants.BBPS_SOURCE, RechargeConstants.SUCCESS_STATUS,
            RechargeConstants.SERVICE_REQUEST_TYPE);
    if (CollectionUtils.isEmpty(recentTransactions)) {
      return Collections.emptyList();
    }

    Map<String, Recharge> latestByCustomerBillerConsumer = new HashMap<>();
    for (Recharge recharge : recentTransactions) {
      if (!isRecentBbpsTransaction(recharge, cutoffEpochMs)) {
        continue;
      }
      String latestCycleKey = buildLatestCycleKey(recharge);
      Recharge existing = latestByCustomerBillerConsumer.get(latestCycleKey);
      if (existing == null || compareLatestTransaction(recharge, existing) > 0) {
        latestByCustomerBillerConsumer.put(latestCycleKey, recharge);
      }
    }

    Set<Recharge> candidates = new LinkedHashSet<>();
    for (Recharge latestRecharge : latestByCustomerBillerConsumer.values()) {
      if (isValidDueDate(latestRecharge.getDueDate(), dueDate)) {
        candidates.add(latestRecharge);
      }
    }
    return Arrays.asList(candidates.toArray(new Recharge[0]));
  }

  private void processRecharge(Recharge recharge, LocalDate expectedDueDate, String eventType,
      Set<String> processedKeys) {
    if (recharge == null || !isCompletedBillPayment(recharge)
        || !isValidDueDate(recharge.getDueDate(), expectedDueDate)) {
      return;
    }

    String billerId = resolveBillerId(recharge);
    String consumerNo = StringUtils.trimToEmpty(recharge.getConsumerNo());
    String customerId = StringUtils.trimToEmpty(recharge.getCustomerId());
    String dueDate = formatDueDate(expectedDueDate);
    String dedupeKey = buildDedupeKey(customerId, billerId, consumerNo, dueDate, eventType);

    if (!processedKeys.add(dedupeKey)) {
      return;
    }
    if (reminderLogRepository.findFirstByDedupeKey(dedupeKey) != null) {
      LOG.info("BBPS bill reminder skipped because reminder is already logged."
          + " rechargeId={}, eventType={}, dueDate={}", recharge.getId(), eventType, dueDate);
      return;
    }

    String billerName = resolveBillerName(billerId, recharge);
    BigDecimal amount = resolveAmount(recharge);
    User user = resolveUser(customerId);
    String email = user != null ? StringUtils.trimToNull(user.getEmail()) : null;
    if (StringUtils.isBlank(email)) {
      String message = "Email reminder pending because customer email is not available.";
      addReminderLog(dedupeKey, recharge, customerId, billerId, billerName, consumerNo, dueDate,
          eventType, null, EMAIL_PENDING, message, "EMAIL_PENDING_NO_EMAIL");
      LOG.warn("BBPS bill reminder email pending because email could not be resolved."
          + " rechargeId={}, customerId={}", recharge.getId(), customerId);
      return;
    }

    String subject = buildEmailSubject(eventType);
    String message = buildEmailMessage(user, eventType, billerName, consumerNo, amount, dueDate);
    boolean sent = emailClient.sendEmail(applicationProperties.getSenderEmail(),
        Collections.singletonList(email), subject, message, false, null, null);

    addReminderLog(dedupeKey, recharge, customerId, billerId, billerName, consumerNo, dueDate,
        eventType, email, sent ? EMAIL_SENT : EMAIL_FAILED, message,
        sent ? "EMAIL_SENT" : "EMAIL_FAILED");

    LOG.info("BBPS bill reminder email processed. rechargeId={}, eventType={}, sent={}",
        recharge.getId(), eventType, sent);
  }

  private void addReminderLog(String dedupeKey, Recharge recharge, String customerId,
      String billerId, String billerName, String consumerNo, String dueDate, String eventType,
      String email, String status, String message, String providerResponse) {
    BbpsBillReminderLog log = new BbpsBillReminderLog();
    log.setDedupeKey(dedupeKey);
    log.setRechargeId(recharge.getId());
    log.setCustomerId(customerId);
    log.setEmail(email);
    log.setBillerId(billerId);
    log.setBillerName(billerName);
    log.setConsumerNo(consumerNo);
    log.setDueDate(dueDate);
    log.setEventType(eventType);
    log.setEmailStatus(status);
    log.setMessage(message);
    log.setProviderResponse(providerResponse);
    log.setCreatedAt(System.currentTimeMillis());
    reminderLogRepository.save(log);
  }

  private boolean isValidDueDate(String rawDueDate, LocalDate expectedDueDate) {
    LocalDate parsedDueDate = parseDueDate(rawDueDate);
    return parsedDueDate != null && !parsedDueDate.isBefore(MIN_VALID_DUE_DATE)
        && parsedDueDate.equals(expectedDueDate);
  }

  private boolean isCompletedBillPayment(Recharge recharge) {
    return Integer.valueOf(RechargeConstants.SUCCESS_STATUS).equals(recharge.getStatus());
  }

  private boolean isRecentBbpsTransaction(Recharge recharge, long cutoffEpochMs) {
    if (recharge == null || !isCompletedBillPayment(recharge)
        || StringUtils.isBlank(recharge.getDueDate())) {
      return false;
    }
    Long updateEpochMs = parseEpochMillis(recharge.getUpdateDateTime());
    return updateEpochMs != null && updateEpochMs >= cutoffEpochMs;
  }

  private int compareLatestTransaction(Recharge left, Recharge right) {
    Long leftUpdateTime = parseEpochMillis(left != null ? left.getUpdateDateTime() : null);
    Long rightUpdateTime = parseEpochMillis(right != null ? right.getUpdateDateTime() : null);
    int updateTimeCompare = Long.compare(leftUpdateTime != null ? leftUpdateTime : 0L,
        rightUpdateTime != null ? rightUpdateTime : 0L);
    if (updateTimeCompare != 0) {
      return updateTimeCompare;
    }
    return Integer.compare(
    	    left != null ? left.getId() : 0,
    	    right != null ? right.getId() : 0
    	);
  }

  private Long parseEpochMillis(String value) {
    String trimmedValue = StringUtils.trimToNull(value);
    if (trimmedValue == null || !NumberUtils.isDigits(trimmedValue)) {
      return null;
    }
    try {
      return Long.valueOf(trimmedValue);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private LocalDate parseDueDate(String rawDueDate) {
    String value = StringUtils.trimToNull(rawDueDate);
    if (value == null) {
      return null;
    }
    for (DateTimeFormatter formatter : Arrays.asList(DD_MM_YYYY, D_M_YYYY, YYYY_MM_DD)) {
      try {
        return LocalDate.parse(value, formatter);
      } catch (DateTimeParseException ignored) {
        // Try the next supported provider format.
      }
    }
    return null;
  }

  private String resolveBillerId(Recharge recharge) {
    for (String candidate : Arrays.asList(recharge.getBillPaymentsInfo(), recharge.getSpCode())) {
      if (StringUtils.isNotBlank(candidate)) {
        return StringUtils.trim(candidate);
      }
    }
    return "UNKNOWN";
  }

  private String resolveBillerName(String billerId, Recharge recharge) {
    if (StringUtils.isNotBlank(billerId) && !"UNKNOWN".equals(billerId)) {
      try {
        BillPaymentsInfo biller = billPaymentsInfoRepository.findFirstByBillerId(billerId);
        if (biller != null && StringUtils.isNotBlank(biller.getBillerName())) {
          return StringUtils.trim(biller.getBillerName());
        }
      } catch (Exception e) {
        LOG.warn("Unable to resolve BBPS biller name. billerId={}", billerId, e);
      }
    }
    return StringUtils.defaultIfBlank(recharge.getRechargeType(), billerId);
  }

  private User resolveUser(String customerId) {
    Integer parsedCustomerId = parseInteger(customerId);
    if (parsedCustomerId != null) {
      Optional<User> user = userRepository.findById(parsedCustomerId);
      return user.orElse(null);
    }
    return null;
  }

  private Integer parseInteger(String value) {
    if (!NumberUtils.isDigits(StringUtils.trimToEmpty(value))) {
      return null;
    }
    try {
      return Integer.valueOf(StringUtils.trim(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String buildEmailSubject(String eventType) {
    if (EVENT_BILL_EXPIRED.equals(eventType)) {
      return "Osta- BBPS bill expired";
    }
    return "Osta- BBPS bill reminder";
  }

  private String buildEmailMessage(User user, String eventType, String billerName, String consumerNo,
      BigDecimal amount, String dueDate) {
    String customerName = StringUtils.defaultIfBlank(user.getFirstName(), "Customer");
    String amountText = amount != null ? "Rs. " + amount.toPlainString() : "the pending amount";
    String maskedConsumerNo = maskConsumerNo(consumerNo);
    String reminderLine = buildReminderLine(eventType, dueDate);

    StringBuilder message = new StringBuilder();
    message.append("Dear ").append(customerName).append(",\n\n");
    message.append(reminderLine).append("\n\n");
    message.append("Biller: ").append(StringUtils.defaultIfBlank(billerName, "BBPS biller"))
        .append("\n");
    message.append("Consumer No: ").append(StringUtils.defaultIfBlank(maskedConsumerNo, "N/A"))
        .append("\n");
    message.append("Amount: ").append(amountText).append("\n");
    message.append("Due Date: ").append(dueDate).append("\n\n");
    message.append("Please pay the bill through Osta to avoid late fees or service interruption.")
        .append("\n\n");
    message.append("Regards,\nOsta Team");
    return message.toString();
  }

  private String buildReminderLine(String eventType, String dueDate) {
    if (EVENT_DUE_IN_7_DAYS.equals(eventType)) {
      return "Your BBPS bill is due in 7 days on " + dueDate + ".";
    }
    if (EVENT_DUE_IN_3_DAYS.equals(eventType)) {
      return "Your BBPS bill is due in 3 days on " + dueDate + ".";
    }
    if (EVENT_DUE_TODAY.equals(eventType)) {
      return "Your BBPS bill is due today.";
    }
    if (EVENT_BILL_EXPIRED.equals(eventType)) {
      return "Your BBPS bill due date has passed.";
    }
    if (EVENT_BILL_PAY_REMINDER_AFTER_EXPIRED.equals(eventType)) {
      return "Your BBPS bill is still pending after the due date.";
    }
    return "This is a reminder for your BBPS bill due on " + dueDate + ".";
  }

  private BigDecimal resolveAmount(Recharge recharge) {
    return recharge.getAmountDue() != null ? recharge.getAmountDue() : recharge.getAmount();
  }

  private String buildDedupeKey(String customerId, String billerId, String consumerNo, String dueDate,
      String eventType) {
    String rawKey = StringUtils.join(Arrays.asList(StringUtils.defaultString(customerId),
        StringUtils.defaultString(billerId), StringUtils.defaultString(consumerNo),
        StringUtils.defaultString(dueDate), StringUtils.defaultString(eventType)), "|");
    return sha256(rawKey);
  }

  private String buildLatestCycleKey(Recharge recharge) {
    return StringUtils.join(Arrays.asList(StringUtils.defaultString(recharge.getCustomerId()),
        StringUtils.defaultString(resolveBillerId(recharge)),
        StringUtils.defaultString(recharge.getConsumerNo())), "|");
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private String maskConsumerNo(String consumerNo) {
    String value = StringUtils.trimToEmpty(consumerNo);
    if (value.length() <= 4) {
      return value;
    }
    return "XXXX" + value.substring(value.length() - 4);
  }

  private String formatDueDate(LocalDate dueDate) {
    return dueDate.format(DD_MM_YYYY);
  }
}
