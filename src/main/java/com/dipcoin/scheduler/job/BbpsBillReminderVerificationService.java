package com.dipcoin.scheduler.job;

import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.model.Recharge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class BbpsBillReminderVerificationService {

  public static final String STATUS_VERIFIED_DUE = "VERIFIED_DUE";
  public static final String STATUS_SKIPPED_PAID_OR_NOT_DUE = "SKIPPED_PAID_OR_NOT_DUE";
  public static final String STATUS_SKIPPED_DUE_DATE_CHANGED = "SKIPPED_DUE_DATE_CHANGED";
  public static final String STATUS_SKIPPED_FETCH_FAILED = "SKIPPED_FETCH_FAILED";
  public static final String STATUS_BILLER_UNSUPPORTED = "BILLER_UNSUPPORTED";

  private static final Logger LOG =
      LoggerFactory.getLogger(BbpsBillReminderVerificationService.class);

  private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter D_M_YYYY = DateTimeFormatter.ofPattern("d/M/yyyy");
  private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final FinacusHttpClient finacusHttpClient;

  private final ObjectMapper objectMapper;

  public BbpsBillReminderVerificationService(FinacusHttpClient finacusHttpClient,
      ObjectMapper objectMapper) {
    this.finacusHttpClient = finacusHttpClient;
    this.objectMapper = objectMapper;
  }

  public BillReminderVerificationResult verify(Recharge recharge, LocalDate expectedDueDate) {
    BillReminderVerificationResult result = new BillReminderVerificationResult();
    if (recharge == null) {
      result.setStatus(STATUS_SKIPPED_FETCH_FAILED);
      result.setReason("Recharge record is not available for reminder verification.");
      return result;
    }

    String billerId = StringUtils.trimToNull(recharge.getBillPaymentsInfo());
    if (billerId == null) {
      billerId = StringUtils.trimToNull(recharge.getSpCode());
    }
    String customerParams = resolveCustomerParams(recharge);
    if (StringUtils.isBlank(billerId) || StringUtils.isBlank(customerParams)) {
      result.setStatus(STATUS_BILLER_UNSUPPORTED);
      result.setReason("Reminder verification skipped because billerId or customer params are missing.");
      return result;
    }

    try {
      JsonNode rawResponse = finacusHttpClient.billFetchForReminder(billerId, customerParams);
      result.setRawResponse(truncate(rawResponse != null ? rawResponse.toString() : null, 4000));

      String responseCode = readText(rawResponse, "ResponseCode");
      String responseMessage = readText(rawResponse, "ResponseMessage");
      if (!"000".equals(responseCode)) {
        result.setStatus(STATUS_SKIPPED_FETCH_FAILED);
        result.setReason(StringUtils.defaultIfBlank(responseMessage, "Bill fetch did not return success."));
        return result;
      }

      JsonNode billNode = resolveBillNode(rawResponse);
      BigDecimal latestAmount = parseAmount(readText(billNode, "BillAmount"));
      String rawDueDate = readText(billNode, "BillDueDate");
      LocalDate latestDueDate = parseDueDate(rawDueDate);

      result.setLatestAmount(latestAmount);
      result.setLatestDueDate(latestDueDate != null ? latestDueDate.format(DD_MM_YYYY)
          : StringUtils.trimToNull(rawDueDate));

      if (latestAmount == null || latestAmount.compareTo(BigDecimal.ZERO) <= 0) {
        result.setStatus(STATUS_SKIPPED_PAID_OR_NOT_DUE);
        result.setReason("Latest bill fetch returned no outstanding amount.");
        return result;
      }

      if (latestDueDate != null && expectedDueDate != null && latestDueDate.isAfter(expectedDueDate)) {
        result.setStatus(STATUS_SKIPPED_DUE_DATE_CHANGED);
        result.setReason("Latest bill due date is after the reminder event due date.");
        return result;
      }

      result.setSendReminder(true);
      result.setStatus(STATUS_VERIFIED_DUE);
      result.setReason("Latest bill fetch confirms the bill is still due.");
      return result;
    } catch (Exception e) {
      LOG.warn("BBPS bill reminder live verification failed. rechargeId={}",
          recharge.getId(), e);
      result.setStatus(STATUS_SKIPPED_FETCH_FAILED);
      result.setReason("Bill fetch failed during reminder verification: " + e.getMessage());
      return result;
    }
  }

  private String resolveCustomerParams(Recharge recharge) {
    List<String> values = parseSubscriptionValues(recharge.getSubscriptionDetails());
    if (!CollectionUtils.isEmpty(values)) {
      return StringUtils.join(values, "|");
    }
    return StringUtils.trimToNull(recharge.getConsumerNo());
  }

  private List<String> parseSubscriptionValues(String subscriptionDetails) {
    List<String> values = new LinkedList<>();
    String raw = StringUtils.trimToNull(subscriptionDetails);
    if (raw == null) {
      return values;
    }
    try {
      JsonNode root = objectMapper.readTree(raw);
      if (root != null && root.isArray()) {
        for (JsonNode item : root) {
          String value = readText(item, "value");
          if (StringUtils.isBlank(value)) {
            value = readText(item, "Value");
          }
          if (StringUtils.isNotBlank(value)) {
            values.add(StringUtils.trim(value));
          }
        }
      }
    } catch (Exception ignored) {
      // Older rows may have non-JSON subscriptionDetails; fallback to consumerNo.
    }
    return values;
  }

  private JsonNode resolveBillNode(JsonNode root) {
    if (root == null) {
      return null;
    }
    if (StringUtils.isNotBlank(readText(root, "BillAmount"))
        || StringUtils.isNotBlank(readText(root, "BillDueDate"))) {
      return root;
    }
    JsonNode billerResponse = root.get("BillerResponse");
    if (billerResponse != null && billerResponse.isArray() && billerResponse.size() > 0) {
      return billerResponse.get(0);
    }
    return root;
  }

  private String readText(JsonNode node, String fieldName) {
    if (node == null || StringUtils.isBlank(fieldName)) {
      return null;
    }
    JsonNode value = node.get(fieldName);
    return value != null && !value.isNull() ? StringUtils.trimToNull(value.asText()) : null;
  }

  private BigDecimal parseAmount(String value) {
    String trimmed = StringUtils.trimToNull(value);
    if (trimmed == null) {
      return null;
    }
    try {
      return new BigDecimal(trimmed);
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

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
