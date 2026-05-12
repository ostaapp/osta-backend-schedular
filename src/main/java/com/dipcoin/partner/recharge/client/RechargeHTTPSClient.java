package com.dipcoin.partner.recharge.client;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONObject;
import org.json.XML;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.comm.BillRequest;
import com.dipcoin.partner.recharge.comm.BillResponse;
import com.dipcoin.partner.recharge.comm.PartnerResponse;
import com.dipcoin.partner.recharge.comm.RechargePlanResponse;
import com.dipcoin.partner.recharge.comm.RechargeRequest;
import com.dipcoin.partner.recharge.comm.RechargeResponse;
import com.dipcoin.partner.recharge.utils.RechargeConfigurations;
import com.dipcoin.partner.recharge.utils.RechargeServiceException;
import com.dipcoin.partner.recharge.utils.RechargeUtils;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.dipcoin.partner.utils.PartnerTrustStoreManager;
import com.dipcoin.scheduler.constants.RechargeConstants;
import com.dipcoin.scheduler.constants.RechargeConstants.RechargeErrorResponse;
import com.dipcoin.scheduler.constants.RechargeConstants.RechargeResponseStatus;
import com.dipcoin.scheduler.constants.RechargeConstants.SupportedHttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;

public class RechargeHTTPSClient extends RechargeClient {

  private static final Logger LOG = LogManager.getLogger(RechargeHTTPSClient.class);
  private SupportedHttpMethod httpMethod = SupportedHttpMethod.POST;


  @Autowired
  private PartnerTrustStoreManager partnerTrustStore;
  
  @Autowired
  private RechargeConfigurations rechargeConfigurations;

  private String uri;

  public RechargeHTTPSClient(String host) {
    super(RechargeClient.Protocol.HTTPS, host);
  }
  
  private boolean fetchPlanRequest;

  public boolean isFetchPlanRequest() {
    return fetchPlanRequest;
  }

  public void setFetchPlanRequest(boolean fetchPlanRequest) {
    this.fetchPlanRequest = fetchPlanRequest;
  }
  
  private boolean httpGetMethod;
  
  public boolean isHttpGetMethod() {
    return httpGetMethod;
  }

  public void setHttpGetMethod(boolean httpGetMethod) {
    this.httpGetMethod = httpGetMethod;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  /*
   * Method to execute the HTTPS request.
   */
  @Override
  protected void executeRequest(PartnerRequestContext requestContext, RechargeRequest rcRequest,
      RechargeResponse rcResponse) throws RechargeServiceException, IOException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Processing recharge request")
        .data("merchantReferenceId", rcRequest.getMerchantReferenceId()).format());

    StringBuffer uriBuf = new StringBuffer("https://");
    uriBuf.append(this.getHost());

    String uri =
        RechargeUtils.getRequestURI(requestContext, getProtocol(), this.uri, getPartnerRequest());
    if (uri != null)
      uriBuf.append("/").append(uri);

    RequestConfig config =
        RequestConfig.custom().setConnectTimeout(rechargeConfigurations.getRequestTimeOut())
            .setConnectionRequestTimeout(rechargeConfigurations.getRequestTimeOut()).build();

    int retries = 0;
    CloseableHttpClient httpClient = null;
    CloseableHttpResponse httpResponse = null;
    
    boolean isResponseError = true;
    
    try {
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Request Time : " + String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()))
          .format());

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("User requested downstream path")
          .data("requestPath", safeUrlForLog(uriBuf.toString())).format());

      HttpClientBuilder builder = HttpClients.custom()
          .setSSLSocketFactory(partnerTrustStore.getSSLConnectionSocketFactory(requestContext,
              rcRequest.getMerchantReferenceId()))
          .disableAutomaticRetries()
          .setConnectionTimeToLive(rechargeConfigurations.getRequestTimeOut(), TimeUnit.MILLISECONDS)
          .setDefaultRequestConfig(config);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Proxy settings")
          .data("Host", getProxy().getHost()).data("Port", getProxy().getPort())
          .data("Enabled", getProxy().isEnabled()).format());

      if (getProxy().isEnabled()) {
        builder.setProxy(new HttpHost(getProxy().getHost(), getProxy().getPort()));
      }

      httpClient = builder.build();

      HttpUriRequest request = null;
      if (SupportedHttpMethod.GET == httpMethod) {
        request = new HttpGet(uriBuf.toString());
      } else if (SupportedHttpMethod.POST == httpMethod) {
        request = new HttpPost(uriBuf.toString());

        byte[] postData = this.getPartnerRequest().getPayload(requestContext, getProtocol(),
            rcRequest.deepCopy(), retries);

        ((HttpPost) request).setEntity(new ByteArrayEntity(postData));
      }

      if (request == null) {
        throw new Exception("Unsupported Request Type");
      }

      request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
      request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
      request.addHeader(RechargeConstants.HEADER_TRACEID, requestContext.getTraceId());
      
      // process response
      httpResponse = httpClient.execute(request);

      isResponseError = false;
      
      // @TODO - Need to handle REDIRECTS explicitly if not
      int status = httpResponse.getStatusLine().getStatusCode();
      
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
              .message("Response Status Code: "+status).format());

      byte[] rawData = EntityUtils.toByteArray(httpResponse.getEntity());
      String payload = new String(rawData, StandardCharsets.UTF_8);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Response data length from server " + payload.length()).format());
      this.getPartnerResponse().processResults(requestContext, getProtocol(), rcRequest, rawData,
          rcResponse);

      if (status != HttpsURLConnection.HTTP_OK) {
        rcResponse.setErrorMsg(payload);
      } else {
        rcResponse.setRawData(payload);
      }
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Mapped downstream response to recharge response")
          .data("responseCode", rcResponse.getResponseCode())
          .data("responseError", rcResponse.getResponseError()).format());

    } catch (Exception e) {
      
      if(isResponseError) {
    	 rcResponse.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    	 rcResponse.setResponseError(RechargeErrorResponse.YES.value());
      }
      
      String desc = "[" + retries + "]: Failed to process recharge request";
      LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc)
          .data("requestPath", safeUrlForLog(uriBuf.toString())).format(), e);

    } finally {
      httpResponse.close();
      httpClient.close();

    }
  }

  @Override
  protected void executeBillRequest(PartnerRequestContext requestContext, BillRequest rcRequest,
      BillResponse rcResponse) throws RechargeServiceException, IOException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Processing bill request")
        .data("merchantReferenceId", rcRequest.getMerchantReferenceId()).format());

    StringBuffer uriBuf = new StringBuffer("https://");
    uriBuf.append(this.getHost());

    String uri = RechargeUtils.getRequestedURI(requestContext, getProtocol(), this.uri, rcRequest,
        this.getPartnerRequest());
    if (uri != null)
      uriBuf.append("/").append(uri);

    RequestConfig config =
        RequestConfig.custom().setConnectTimeout(rechargeConfigurations.getRequestTimeOut())
            .setConnectionRequestTimeout(rechargeConfigurations.getRequestTimeOut()).build();

    int retries = 0;
    CloseableHttpResponse httpResponse = null;
    CloseableHttpClient httpClient = null;
    
    boolean isResponseError = true;
    
    try {
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Request Time : " + String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()))
          .format());

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("User requested downstream path")
          .data("requestPath", safeUrlForLog(uriBuf.toString())).format());

      HttpClientBuilder builder = HttpClients.custom()
          .setSSLSocketFactory(partnerTrustStore.getSSLConnectionSocketFactory(requestContext,
              rcRequest.getMerchantReferenceId()))
          .disableAutomaticRetries()
          .setConnectionTimeToLive(rechargeConfigurations.getRequestTimeOut(), TimeUnit.MILLISECONDS)
          .setDefaultRequestConfig(config);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Proxy settings")
          .data("Host", getProxy().getHost()).data("Port", getProxy().getHost())
          .data("Enabled", getProxy().isEnabled()).format());

      if (getProxy().isEnabled()) {
        builder.setProxy(new HttpHost(getProxy().getHost(), getProxy().getPort()));
      }

      httpClient = builder.build();

      HttpUriRequest request = null;
      if (this.isHttpGetMethod()) {
        request = new HttpGet(uriBuf.toString());
      } else if (SupportedHttpMethod.POST == httpMethod) {
        request = new HttpPost(uriBuf.toString());

        byte[] postData = this.getPartnerRequest().getBillPayload(requestContext, getProtocol(),
            rcRequest, retries);

        ((HttpPost) request).setEntity(new ByteArrayEntity(postData));
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
        request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader(RechargeConstants.HEADER_TRACEID, requestContext.getTraceId());
      }

      if (request == null) {
        throw new Exception("Unsupported Request Type");
      }
      
      // process response
      httpResponse = httpClient.execute(request);
      
      isResponseError = false;  // if controll came here that means we got response res from euronet without any exception

      // @TODO - Need to handle REDIRECTS explicitly if not
      int status = httpResponse.getStatusLine().getStatusCode();

      byte[] rawData = EntityUtils.toByteArray(httpResponse.getEntity());
      String payload = new String(rawData, StandardCharsets.UTF_8);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Response data from server " + payload.length()).format());
      
      if (this.isFetchPlanRequest()) {
        
        rcResponse.put(RechargePlanResponse.Fields.RESPONSE, payload);
        LOG.debug(LogFormatter.instance(requestContext.getTraceId())
            .data("RechargePlanResponse", rcResponse.size()).format());
      } else {
        this.getPartnerResponse().processBillResults(requestContext, getProtocol(), rcRequest,
            rawData, rcResponse);
      }
      
      if (status != HttpsURLConnection.HTTP_OK) {
        rcResponse.setErrorMsg(payload);
      } else {
        rcResponse.setRawData(payload);
      }
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Mapped downstream response to bill response")
          .data("responseFields", rcResponse.size()).format());
      
    } catch (Exception e) {

		if (isResponseError) {
			rcResponse.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
			rcResponse.setResponseError(RechargeErrorResponse.YES.value());
		}

      String desc =
          "Failed to process bill request because no response was found";
      LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc)
          .data("requestPath", safeUrlForLog(uriBuf.toString())).format(), e);

    } finally {
      httpClient.close();
      httpResponse.close();
    }
  }

  private static String safeUrlForLog(String rawUrl) {
    if (rawUrl == null || rawUrl.trim().isEmpty()) {
      return "unknown";
    }
    try {
      String path = URI.create(rawUrl).getPath();
      return path == null || path.trim().isEmpty() ? "unknown" : path;
    } catch (IllegalArgumentException e) {
      int queryIndex = rawUrl.indexOf('?');
      String value = queryIndex >= 0 ? rawUrl.substring(0, queryIndex) : rawUrl;
      int fragmentIndex = value.indexOf('#');
      value = fragmentIndex >= 0 ? value.substring(0, fragmentIndex) : value;
      int schemeIndex = value.indexOf("://");
      if (schemeIndex >= 0) {
        int pathIndex = value.indexOf('/', schemeIndex + 3);
        return pathIndex >= 0 ? value.substring(pathIndex) : "unknown";
      }
      return value.trim().isEmpty() ? "unknown" : value;
    }
  }
}
