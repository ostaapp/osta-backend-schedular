package com.dipcoin.partner.recharge.comm;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.dipcoin.partner.recharge.utils.RechargeUtils;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONRechargeRequest extends PartnerRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LogManager.getLogger(JSONRechargeRequest.class);
  private static ObjectMapper objectMapper = new ObjectMapper();

  @SuppressWarnings("unchecked")
  @Override
  public byte[] getPayload(PartnerRequestContext requestContext, Protocol protocol,
      final RechargeRequest request, int attempt) throws Exception {

    RechargeRequest mappedRequest =
        this.mapAndResolveParams(requestContext, protocol, request, getPayloadFieldMapping());
    if (mappedRequest == null)
      return null;

    Map<String, Object> resolvedRequest = new HashMap<>();

    for (String key : mappedRequest.keySet()) {
      Map<String, Object> data =
          RechargeUtils.jsonNamespaceToMap(key, "\\.", mappedRequest.get(key));
      Entry<String, Object> entry = data.entrySet().iterator().next();
      if (resolvedRequest.containsKey(entry.getKey()) && entry.getValue() instanceof Map) {
        Map<String, Object> value = (Map<String, Object>) resolvedRequest.get(entry.getKey());
        value.putAll((Map<String, Object>) entry.getValue());

      } else {
        resolvedRequest.putAll(data);
      }
    }
    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Prepared JSON recharge request payload.")
        .data("fieldCount", resolvedRequest.size()).format());

    String rawPayload = objectMapper.writeValueAsString(resolvedRequest);
    String encyptedPayload = rawPayload;
    
    // in case of Euronet Wallet fetch api, it doesn't give proper format
    // so making it proper json
    if(request != null && StringUtils.isNotEmpty(request.getRequestType()) && request.getRequestType().equals(OperationType.BALANCE_RECHARGE.value())) {
    	// Replace unnecessary escape characters
    	encyptedPayload = encyptedPayload.replaceAll("\\\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
    }
    
    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("JSON recharge payload prepared.")
        .data("payloadLength", encyptedPayload.length()).format());

    return encyptedPayload.getBytes(StandardCharsets.UTF_8);
  }

  @SuppressWarnings("unchecked")
  @Override
  public byte[] getBillPayload(PartnerRequestContext requestContext, Protocol protocol,
      BillRequest request, int retries) throws Exception {

    BillRequest mappedBillRequest = this.mapAndResolveBillParams(requestContext, protocol, request,
        getBillPayloadFieldMapping(), getXyzFieldMapping());
    if (mappedBillRequest == null)
      return null;

    // handle json namespace in keys i.e bank.json.namespace => dipcoinKey
    Map<String, Object> resolvedRequest = new LinkedHashMap<>();

    for (String key : mappedBillRequest.keySet()) {
      Map<String, Object> data =
          RechargeUtils.jsonNamespaceToMap(key, "\\.", mappedBillRequest.get(key));
      Entry<String, Object> entry = data.entrySet().iterator().next();
      if (resolvedRequest.containsKey(entry.getKey()) && entry.getValue() instanceof Map) {
        Map<String, Object> value = (Map<String, Object>) resolvedRequest.get(entry.getKey());
        value.putAll((Map<String, Object>) entry.getValue());

      } else {
        resolvedRequest.putAll(data);
      }
    }
    LOG.debug(
        LogFormatter.instance(requestContext.getTraceId()).message("Prepared JSON bill payload.")
            .data("fieldCount", resolvedRequest.size()).format());

    if (resolvedRequest.get("RequestType").equals("GetMDMResponse")) {
      Map<String, Object> newRequest = new HashMap<>(resolvedRequest);
      newRequest.remove("GetMDMResponse");
      resolvedRequest.clear();
      resolvedRequest.put("RequestType", "GetMDMResponse");
      resolvedRequest.putAll(newRequest);
    }
    String rawPayload = objectMapper.writeValueAsString(resolvedRequest);
    String encyptedPayload = rawPayload;
    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("JSON bill payload prepared.")
        .data("payloadLength", encyptedPayload.length()).format());

    return encyptedPayload.getBytes(StandardCharsets.UTF_8);
  }
}
