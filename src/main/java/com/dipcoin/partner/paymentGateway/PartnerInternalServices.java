package com.dipcoin.partner.paymentGateway;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.api.config.ApplicationProperties;
//import com.dipcoin.partner.paymentGateway.model.CheckTransactionStatusResponse;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayPaymentStatusResponse;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayRefundRequest;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayRefundResponse;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayRefundStatusRequest;
import com.dipcoin.partner.paymentGateway.model.InternalAggrepayRefundStatusResponse;
import com.dipcoin.partner.paymentGateway.model.InternalCapturedPgTransactionResponse;
import com.dipcoin.partner.paymentGateway.model.PartnerRefundRequest;
import com.dipcoin.partner.paymentGateway.model.PartnerRefundResponse;
//import com.dipcoin.partner.paymentGateway.model.PartnerTxnStatusResponse;
//import com.dipcoin.partner.paymentGateway.model.WalletResponse;
import com.dipcoin.partner.paymentGateway.model.PaymentGatewayProperties;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.dipcoin.partner.paymentGateway.model.PartnerUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("partnerInternalServices")
public class PartnerInternalServices {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger LOG = LogManager.getLogger(PartnerInternalServices.class);
  private static final String INTERNAL_SIGNATURE_DELIMITER = "|";
  private static final String PARTNER_TXN_STATUS_PATH = "/oauth2/transactions/status";
  private static final String INTERNAL_CAPTURED_PG_STATUS_PATH =
      "/jwt/bbpsPartner/customer/partner/pg/captured-status";
  private static final String INTERNAL_AGGREPAY_PAYMENT_STATUS_PATH =
      "/jwt/bbpsPartner/customer/partner/aggrepay/payment-status";
  private static final String INTERNAL_AGGREPAY_REFUND_PATH =
      "/jwt/bbpsPartner/customer/partner/aggrepay/refund";
  private static final String INTERNAL_AGGREPAY_REFUND_STATUS_PATH =
      "/jwt/bbpsPartner/customer/partner/aggrepay/refund-status";
  private static final String API_V1_PREFIX = "/api/v1";
  private static final int LOCAL_PARTNER_PORT = 9443;
  private static final String PROD_INTERNAL_CAPTURED_PG_STATUS_URL =
      "https://apiprod.ostaapp.com/api/v1/jwt/bbpsPartner/customer/partner/pg/captured-status";
  private static final String PROD_INTERNAL_AGGREPAY_PAYMENT_STATUS_URL =
      "https://apiprod.ostaapp.com/api/v1/jwt/bbpsPartner/customer/agreePay/statusCheck";
  private static final String PROD_INTERNAL_AGGREPAY_REFUND_URL =
      "https://apiprod.ostaapp.com/api/v1/jwt/bbpsPartner/customer/agreePay/refund";
  private static final String PROD_INTERNAL_AGGREPAY_REFUND_STATUS_URL =
      "https://apiprod.ostaapp.com/api/v1/jwt/bbpsPartner/customer/partner/aggrepay/refund-status";

  @Autowired
  private PaymentGatewayProperties paymentGatewayProperties;
//
  @Autowired
  private PartnerUtils partnerUtils;

  @Autowired
  private ApplicationProperties applicationProperties;

  @Value("${spring.profiles.active:dev}")
  private String activeProfile;


  public InternalCapturedPgTransactionResponse getCapturedPgTransactionDetails(
      final String partnerTransactionRefId, final String orderId) {

    if (StringUtils.isBlank(partnerTransactionRefId)) {
      return null;
    }

    HttpClientBuilder builder = partnerUtils.getSocketBuilder();
    if (builder == null) {
      return null;
    }

    String resolvedCapturedPgStatusUrl;
    try {
      resolvedCapturedPgStatusUrl =
          buildInternalCapturedPgStatusUrl(partnerTransactionRefId, orderId);
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to resolve partner internal captured PG status URL")
          .data("partnerTransactionReferenceId", partnerTransactionRefId)
          .data("orderId", orderId).format(), e);
      return null;
    }

    applyConfiguredProxyIfRequired(builder, resolvedCapturedPgStatusUrl,
        "capturedPgStatusLookup");

    try (CloseableHttpClient httpClient = builder.build()) {
      String timestamp = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
      LOG.info(LogFormatter.instance()
          .message("Resolved partner internal captured PG status URL")
          .data("configuredInternalCapturedPgStatusUrl",
              StringUtils.trimToNull(paymentGatewayProperties.getInternalCapturedPgStatusUrl()))
          .data("configuredGetTxnStatusUrl",
              StringUtils.trimToNull(paymentGatewayProperties.getGetTxnStatusUrl()))
          .data("resolvedCapturedPgStatusUrl", resolvedCapturedPgStatusUrl)
          .data("partnerTransactionReferenceId", partnerTransactionRefId)
          .data("orderId", orderId).format());
      HttpGet getReq = new HttpGet(resolvedCapturedPgStatusUrl);
      getReq.addHeader("Accept", "application/json");
      getReq.addHeader("X-Internal-Timestamp", timestamp);
      getReq.addHeader("X-Internal-Signature",
          buildInternalSignature(partnerTransactionRefId, orderId, timestamp));

      try (CloseableHttpResponse response = httpClient.execute(getReq)) {
        int statusCode = response.getStatusLine().getStatusCode();
        byte[] rawData = EntityUtils.toByteArray(response.getEntity());
        String data = new String(rawData, StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
          return objectMapper.readValue(data, InternalCapturedPgTransactionResponse.class);
        }

        LOG.warn(LogFormatter.instance()
            .message("Partner internal service did not return captured PG details")
            .data("statusCode", statusCode)
            .data("partnerTransactionReferenceId", partnerTransactionRefId)
            .data("orderId", orderId)
            .data("response", data).format());
      }
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to fetch captured PG details from partner internal service")
          .data("partnerTransactionReferenceId", partnerTransactionRefId)
          .data("orderId", orderId).format(), e);
    }

    return null;
  }

  public InternalAggrepayPaymentStatusResponse getAggrepayPaymentStatus(final String orderId) {

    if (StringUtils.isBlank(orderId)) {
      return null;
    }

    HttpClientBuilder builder = partnerUtils.getSocketBuilder();
    if (builder == null) {
      return null;
    }

    String resolvedAggrepayPaymentStatusUrl;
    try {
      resolvedAggrepayPaymentStatusUrl = buildInternalAggrepayPaymentStatusUrl(orderId);
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to resolve partner internal Aggrepay payment status URL")
          .data("orderId", orderId).format(), e);
      return null;
    }

    LOG.info(LogFormatter.instance()
        .message("Resolved partner Aggrepay payment status URL")
        .data("configuredInternalAggrepayPaymentStatusUrl",
            StringUtils.trimToNull(paymentGatewayProperties.getInternalAggrepayPaymentStatusUrl()))
        .data("configuredGetTxnStatusUrl",
            StringUtils.trimToNull(paymentGatewayProperties.getGetTxnStatusUrl()))
        .data("orderId", orderId)
        .data("resolvedAggrepayPaymentStatusUrl", resolvedAggrepayPaymentStatusUrl)
        .format());

    applyConfiguredProxyIfRequired(builder, resolvedAggrepayPaymentStatusUrl,
        "aggrepayPaymentStatusLookup");

    try (CloseableHttpClient httpClient = builder.build()) {
      String timestamp = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
      HttpGet getReq = new HttpGet(resolvedAggrepayPaymentStatusUrl);
      getReq.addHeader("Accept", "application/json");
      getReq.addHeader("X-Internal-Timestamp", timestamp);
      getReq.addHeader("X-Internal-Signature",
          buildInternalSignature(orderId, null, timestamp));

      try (CloseableHttpResponse response = httpClient.execute(getReq)) {
        int statusCode = response.getStatusLine().getStatusCode();
        byte[] rawData = EntityUtils.toByteArray(response.getEntity());
        String data = new String(rawData, StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
          return objectMapper.readValue(data, InternalAggrepayPaymentStatusResponse.class);
        }

        LOG.warn(LogFormatter.instance()
            .message("Partner internal service did not return Aggrepay payment status")
            .data("statusCode", statusCode)
            .data("orderId", orderId)
            .data("response", data).format());
      }
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to fetch Aggrepay payment status from partner internal service")
          .data("orderId", orderId).format(), e);
    }

    return null;
  }

  public InternalAggrepayRefundResponse initiateAggrepayRefund(final String orderId,
      final java.math.BigDecimal amount, final String reason) {

    if (StringUtils.isBlank(orderId) || amount == null) {
      return null;
    }

    HttpClientBuilder builder = partnerUtils.getSocketBuilder();
    if (builder == null) {
      return null;
    }

    String resolvedAggrepayRefundUrl;
    try {
      resolvedAggrepayRefundUrl = buildInternalAggrepayRefundUrl();
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to resolve partner internal Aggrepay refund URL")
          .data("orderId", orderId)
          .data("amount", amount).format(), e);
      return null;
    }

    applyConfiguredProxyIfRequired(builder, resolvedAggrepayRefundUrl,
        "aggrepayRefundInitiation");

    try (CloseableHttpClient httpClient = builder.build()) {
      String timestamp = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
      HttpPost postReq = new HttpPost(resolvedAggrepayRefundUrl);
      postReq.addHeader("Accept", "application/json");
      postReq.addHeader("Content-Type", "application/json");
      postReq.addHeader("X-Internal-Timestamp", timestamp);
      postReq.addHeader("X-Internal-Signature",
          buildInternalSignature(orderId, amount.stripTrailingZeros().toPlainString(), timestamp));

      InternalAggrepayRefundRequest request = new InternalAggrepayRefundRequest();
      request.setOrderId(orderId);
      request.setAmount(amount);
      request.setReason(reason);
      postReq.setEntity(new StringEntity(objectMapper.writeValueAsString(request),
          StandardCharsets.UTF_8));

      try (CloseableHttpResponse response = httpClient.execute(postReq)) {
        int statusCode = response.getStatusLine().getStatusCode();
        byte[] rawData = EntityUtils.toByteArray(response.getEntity());
        String data = new String(rawData, StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
          return objectMapper.readValue(data, InternalAggrepayRefundResponse.class);
        }

        LOG.warn(LogFormatter.instance()
            .message("Partner internal service did not initiate Aggrepay refund")
            .data("statusCode", statusCode)
            .data("orderId", orderId)
            .data("amount", amount)
            .data("response", data).format());
      }
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to trigger Aggrepay refund from partner internal service")
          .data("orderId", orderId)
          .data("amount", amount).format(), e);
    }

    return null;
  }

  public InternalAggrepayRefundStatusResponse getAggrepayRefundStatus(final String orderId,
      final String refundId) {

    if (StringUtils.isBlank(orderId)) {
      return null;
    }

    HttpClientBuilder builder = partnerUtils.getSocketBuilder();
    if (builder == null) {
      return null;
    }

    String resolvedAggrepayRefundStatusUrl;
    try {
      resolvedAggrepayRefundStatusUrl = buildInternalAggrepayRefundStatusUrl();
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to resolve partner internal Aggrepay refund status URL")
          .data("orderId", orderId)
          .data("refundId", refundId).format(), e);
      return null;
    }

    applyConfiguredProxyIfRequired(builder, resolvedAggrepayRefundStatusUrl,
        "aggrepayRefundStatusLookup");

    try (CloseableHttpClient httpClient = builder.build()) {
      String normalizedRefundId = StringUtils.trimToNull(refundId);
      String timestamp = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
      HttpPost postReq = new HttpPost(resolvedAggrepayRefundStatusUrl);
      postReq.addHeader("Accept", "application/json");
      postReq.addHeader("Content-Type", "application/json");
      postReq.addHeader("X-Internal-Timestamp", timestamp);
      postReq.addHeader("X-Internal-Signature",
          buildInternalSignature(orderId, normalizedRefundId, timestamp));

      InternalAggrepayRefundStatusRequest request = new InternalAggrepayRefundStatusRequest();
      request.setOrderId(orderId);
      request.setRefundId(normalizedRefundId);
      postReq.setEntity(new StringEntity(objectMapper.writeValueAsString(request),
          StandardCharsets.UTF_8));

      try (CloseableHttpResponse response = httpClient.execute(postReq)) {
        int statusCode = response.getStatusLine().getStatusCode();
        byte[] rawData = EntityUtils.toByteArray(response.getEntity());
        String data = new String(rawData, StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
          return objectMapper.readValue(data, InternalAggrepayRefundStatusResponse.class);
        }

        LOG.warn(LogFormatter.instance()
            .message("Partner internal service did not return Aggrepay refund status")
            .data("statusCode", statusCode)
            .data("orderId", orderId)
            .data("refundId", normalizedRefundId)
            .data("response", data).format());
      }
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Failed to fetch Aggrepay refund status from partner internal service")
          .data("orderId", orderId)
          .data("refundId", StringUtils.trimToNull(refundId)).format(), e);
    }

    return null;
  }

  private String buildInternalCapturedPgStatusUrl(final String partnerTransactionRefId,
      final String orderId) throws Exception {
    if (isProdProfile()) {
      return appendCapturedPgStatusQuery(PROD_INTERNAL_CAPTURED_PG_STATUS_URL,
          partnerTransactionRefId, orderId);
    }

    String configuredUrl =
        StringUtils.trimToNull(paymentGatewayProperties.getInternalCapturedPgStatusUrl());
    boolean hasExplicitInternalUrl = StringUtils.isNotBlank(configuredUrl);
    URI statusUri = hasExplicitInternalUrl ? new URI(configuredUrl)
        : new URI(StringUtils.trim(paymentGatewayProperties.getGetTxnStatusUrl()));
    URI normalizedBaseUri =
        normalizeCapturedPgStatusBaseUri(statusUri, hasExplicitInternalUrl);
    String query = "partnerTransactionRefId="
        + URLEncoder.encode(partnerTransactionRefId, StandardCharsets.UTF_8.name());
    if (StringUtils.isNotBlank(orderId)) {
      query += "&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8.name());
    }

    URI internalUri =
        new URI(normalizedBaseUri.getScheme(), normalizedBaseUri.getUserInfo(),
            normalizedBaseUri.getHost(), normalizedBaseUri.getPort(),
            normalizedBaseUri.getPath(), query, null);
    return internalUri.toString();
  }

  private String appendCapturedPgStatusQuery(final String baseUrl,
      final String partnerTransactionRefId, final String orderId) throws Exception {
    StringBuilder urlBuilder = new StringBuilder(StringUtils.trim(baseUrl))
        .append("?partnerTransactionRefId=")
        .append(URLEncoder.encode(partnerTransactionRefId, StandardCharsets.UTF_8.name()));
    if (StringUtils.isNotBlank(orderId)) {
      urlBuilder.append("&orderId=")
          .append(URLEncoder.encode(orderId, StandardCharsets.UTF_8.name()));
    }
    return urlBuilder.toString();
  }

  private String buildInternalAggrepayPaymentStatusUrl(final String orderId) throws Exception {
    if (isProdProfile()) {
      return appendOrderIdQuery(PROD_INTERNAL_AGGREPAY_PAYMENT_STATUS_URL, orderId);
    }

    String configuredUrl =
        StringUtils.trimToNull(paymentGatewayProperties.getInternalAggrepayPaymentStatusUrl());
    if (StringUtils.isNotBlank(configuredUrl)) {
      return appendOrderIdQuery(configuredUrl, orderId);
    }

    URI statusUri = new URI(StringUtils.trim(paymentGatewayProperties.getGetTxnStatusUrl()));
    URI normalizedBaseUri = normalizeAggrepayPaymentStatusBaseUri(statusUri);
    return appendOrderIdQuery(normalizedBaseUri.toString(), orderId);
  }

  private String buildInternalAggrepayRefundUrl() throws Exception {
    if (isProdProfile()) {
      return PROD_INTERNAL_AGGREPAY_REFUND_URL;
    }

    String configuredUrl =
        StringUtils.trimToNull(paymentGatewayProperties.getInternalAggrepayRefundUrl());
    if (StringUtils.isNotBlank(configuredUrl)) {
      return configuredUrl;
    }

    URI statusUri = new URI(StringUtils.trim(paymentGatewayProperties.getGetTxnStatusUrl()));
    URI normalizedBaseUri =
        normalizeAggrepayInternalBaseUri(statusUri, INTERNAL_AGGREPAY_REFUND_PATH);
    return normalizedBaseUri.toString();
  }

  private String buildInternalAggrepayRefundStatusUrl() throws Exception {
    if (isProdProfile()) {
      return PROD_INTERNAL_AGGREPAY_REFUND_STATUS_URL;
    }

    String configuredUrl =
        StringUtils.trimToNull(paymentGatewayProperties.getInternalAggrepayRefundStatusUrl());
    if (StringUtils.isNotBlank(configuredUrl)) {
      return configuredUrl;
    }

    URI statusUri = new URI(StringUtils.trim(paymentGatewayProperties.getGetTxnStatusUrl()));
    URI normalizedBaseUri =
        normalizeAggrepayInternalBaseUri(statusUri, INTERNAL_AGGREPAY_REFUND_STATUS_PATH);
    return normalizedBaseUri.toString();
  }

  private String appendOrderIdQuery(final String baseUrl, final String orderId) throws Exception {
    if (StringUtils.isBlank(orderId)) {
      return StringUtils.trim(baseUrl);
    }
    return new StringBuilder(StringUtils.trim(baseUrl))
        .append("?orderId=")
        .append(URLEncoder.encode(orderId, StandardCharsets.UTF_8.name()))
        .toString();
  }

  private URI normalizeAggrepayPaymentStatusBaseUri(final URI statusUri) throws Exception {
    return normalizeAggrepayInternalBaseUri(statusUri, INTERNAL_AGGREPAY_PAYMENT_STATUS_PATH);
  }

  private URI normalizeAggrepayInternalBaseUri(final URI statusUri, final String internalPath)
      throws Exception {
    if (statusUri == null) {
      return null;
    }

    String scheme = StringUtils.defaultIfBlank(statusUri.getScheme(), "http");
    String host = statusUri.getHost();
    int port = statusUri.getPort();
    String path = resolveInternalAggrepayPath(statusUri.getPath(), internalPath);

    if (isLocalPartnerHost(host)) {
      scheme = "http";
      port = LOCAL_PARTNER_PORT;
      path = API_V1_PREFIX + internalPath;
    }

    return new URI(scheme, statusUri.getUserInfo(), host, port, path, null, null);
  }

  private String resolveInternalAggrepayPath(final String requestPath, final String internalPath) {
    if (StringUtils.isBlank(requestPath)) {
      return API_V1_PREFIX + internalPath;
    }

    String normalizedPath = StringUtils.removeEnd(StringUtils.trim(requestPath), "/");
    if (StringUtils.endsWith(normalizedPath, PARTNER_TXN_STATUS_PATH)) {
      return StringUtils.removeEnd(normalizedPath, PARTNER_TXN_STATUS_PATH)
          + internalPath;
    }

    return normalizedPath + internalPath;
  }

  private URI normalizeCapturedPgStatusBaseUri(final URI statusUri,
      final boolean hasExplicitInternalUrl) throws Exception {
    if (statusUri == null) {
      return null;
    }

    String scheme = StringUtils.defaultIfBlank(statusUri.getScheme(), "http");
    String host = statusUri.getHost();
    int port = statusUri.getPort();
    String path = hasExplicitInternalUrl ? statusUri.getPath()
        : resolveInternalCapturedPgStatusPath(statusUri.getPath());

    if (isLocalPartnerHost(host)) {
      scheme = "http";
      port = LOCAL_PARTNER_PORT;
      path = API_V1_PREFIX + INTERNAL_CAPTURED_PG_STATUS_PATH;
    }

    return new URI(scheme, statusUri.getUserInfo(), host, port, path, null, null);
  }

  private String resolveInternalCapturedPgStatusPath(final String requestPath) {
    if (StringUtils.isBlank(requestPath)) {
      return API_V1_PREFIX + INTERNAL_CAPTURED_PG_STATUS_PATH;
    }

    String normalizedPath = StringUtils.removeEnd(StringUtils.trim(requestPath), "/");
    if (StringUtils.endsWith(normalizedPath, PARTNER_TXN_STATUS_PATH)) {
      return StringUtils.removeEnd(normalizedPath, PARTNER_TXN_STATUS_PATH)
          + INTERNAL_CAPTURED_PG_STATUS_PATH;
    }

    return normalizedPath + INTERNAL_CAPTURED_PG_STATUS_PATH;
  }

  private boolean isLocalPartnerHost(final String host) {
    if (StringUtils.isBlank(host)) {
      return false;
    }

    return StringUtils.equalsAnyIgnoreCase(host, "localhost", "127.0.0.1", "0.0.0.0", "::1");
  }

  private boolean isProdProfile() {
    return StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(activeProfile), "prod");
  }

  private boolean shouldBypassProxyForUrl(final String targetUrl) {
    String normalizedTargetUrl = StringUtils.trimToNull(targetUrl);
    if (normalizedTargetUrl == null) {
      return false;
    }

    try {
      URI targetUri = new URI(normalizedTargetUrl);
      String host = StringUtils.trimToNull(targetUri.getHost());
      if (host == null) {
        return false;
      }

      return isLocalPartnerHost(host) || StringUtils.equalsAnyIgnoreCase(host, "ostaapp.com",
          "osta.in") || StringUtils.endsWithIgnoreCase(host, ".ostaapp.com")
          || StringUtils.endsWithIgnoreCase(host, ".osta.in");
    } catch (Exception e) {
      LOG.warn(LogFormatter.instance()
          .message("Unable to parse outbound URL for proxy evaluation")
          .data("targetUrl", normalizedTargetUrl).format(), e);
      return false;
    }
  }

  private void applyConfiguredProxyIfRequired(final HttpClientBuilder builder,
      final String targetUrl, final String operationName) {
    if (!paymentGatewayProperties.getIsProxyEnabled()) {
      return;
    }

    if (shouldBypassProxyForUrl(targetUrl)) {
      LOG.info(LogFormatter.instance()
          .message("Bypassing configured proxy for internal OSTA URL")
          .data("operation", operationName)
          .data("targetUrl", targetUrl).format());
      return;
    }

    builder.setProxy(
        new HttpHost(paymentGatewayProperties.getIp(), paymentGatewayProperties.getPort()));
    LOG.info(LogFormatter.instance().message("To enable proxy settings for prod")
        .data("Ip", paymentGatewayProperties.getIp())
        .data("Port", paymentGatewayProperties.getPort())
        .data("operation", operationName).format());
  }

  private String buildInternalSignature(final String primaryValue, final String secondaryValue,
      final String timestamp) throws Exception {
    return sha256Hex(StringUtils.defaultString(primaryValue)
        + INTERNAL_SIGNATURE_DELIMITER + StringUtils.defaultString(secondaryValue)
        + INTERNAL_SIGNATURE_DELIMITER + StringUtils.defaultString(timestamp)
        + INTERNAL_SIGNATURE_DELIMITER
        + StringUtils.defaultString(applicationProperties.getOauth2EncryptionKey()));
  }

  private String sha256Hex(final String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
    StringBuilder builder = new StringBuilder(hash.length * 2);
    for (byte current : hash) {
      builder.append(String.format("%02x", current));
    }
    return builder.toString();
  }
  
  public PartnerRefundResponse initiatePartnerRefund(final PartnerRefundRequest request, final String token, final String dclCookie)
      throws ClientProtocolException, IOException {

    HttpClientBuilder builder = partnerUtils.getSocketBuilder();
    PartnerRequestContext partnerRequestContext = PartnerRequestContext.instance();

    if (paymentGatewayProperties.getIsProxyEnabled()) {
      builder.setProxy(
          new HttpHost(paymentGatewayProperties.getIp(), paymentGatewayProperties.getPort()));
      LOG.info(LogFormatter.instance().message("To enable proxy settings for prod")
          .data("Ip", paymentGatewayProperties.getIp())
          .data("Port", paymentGatewayProperties.getPort()).format());
    }

    CloseableHttpClient httpClient = builder.build();
    CloseableHttpResponse response = null;
    PartnerRefundResponse partnerRefundResponse = new PartnerRefundResponse();
    try {
      HttpPost postRequest = new HttpPost(paymentGatewayProperties.getPartnerRefundInitiate());
      postRequest.addHeader("Accept", "application/json");
      postRequest.addHeader("Authorization", token);
      postRequest.addHeader("Content-Type", "application/json");
      postRequest.addHeader("cookie","dcl="+dclCookie);


      StringEntity input = new StringEntity(objectMapper.writeValueAsString(request));

      postRequest.setEntity(input);

      response = httpClient.execute(postRequest);
      int statusCode = response.getStatusLine().getStatusCode();
      byte[] rawData = EntityUtils.toByteArray(response.getEntity());

      LOG.info(LogFormatter.instance(partnerRequestContext.getTraceId())
          .data("Response from Partner To initiate refund at payment gateway", response.getEntity())
          .data("statusCode", statusCode).format());


      String data = new String(rawData, StandardCharsets.UTF_8);


      try {
        if (statusCode < HttpsURLConnection.HTTP_INTERNAL_ERROR) {
          partnerRefundResponse = objectMapper.readValue(data, PartnerRefundResponse.class);

        } else if (statusCode == HttpsURLConnection.HTTP_INTERNAL_ERROR) {
          partnerRefundResponse = null;

        }
      } catch (JsonMappingException e) {

        partnerRefundResponse = null;
      } catch (JsonProcessingException e) {

        partnerRefundResponse = null;
      }

      LOG.info(partnerRefundResponse);


    } finally {
      try {
        if (response != null)
          response.close();
        if (httpClient != null)
          httpClient.close();
      } catch (Exception e) {
        LOG.debug(LogFormatter.instance()
            .message("Exception Caught Duri.ng Closing connnection ").format(), e);
        return null;
      }

    }

    return partnerRefundResponse;

  }
}
