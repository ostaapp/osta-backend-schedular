package com.dipcoin.partner.recharge.comm;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.utils.RechargeUtils;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONRechargeResponse extends PartnerResponse {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LogManager.getLogger(JSONRechargeResponse.class);

  private static TypeReference<HashMap<String, Object>> typeRef =
      new TypeReference<HashMap<String, Object>>() {};

  private static ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean processResults(PartnerRequestContext requestContext, Protocol protocol,
      final RechargeRequest request, final byte[] rawData, RechargeResponse response)
      throws Exception {

    if (response == null)
      return false;

    String payload = new String(rawData, StandardCharsets.UTF_8);

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("JSON recharge response payload processed.")
        .data("payloadLength", payload.length()).format());

    HashMap<String, Object> data = objectMapper.readValue(payload, typeRef);

    Map<String, Object> flattenMap = RechargeUtils.flattenMap(data, ".");

    Iterator<String> it = flattenMap.keySet().iterator();

    while (it.hasNext()) {
      String key = it.next();
      response.put(key, flattenMap.get(key) != null ? flattenMap.get(key).toString() : null);
    }

    LOG.debug(
        LogFormatter.instance(requestContext.getTraceId()).message("JSON recharge response mapped.")
            .data("responseFieldCount", response.size()).format());

    String codeKey = RechargeResponse.Fields.RESPONSE_CODE;
    String descKey = RechargeResponse.Fields.RESPONSE_DESCRIPTION;
    String messageKey = RechargeResponse.Fields.RESPONSE_MESSAGE;
    String responseCode = null, responseDesc = null, responseMessage = null;

    final Map<String, String> fieldsMappings = getFieldsMapping();

    for (String key : fieldsMappings.keySet()) {
      String mappedKey = fieldsMappings.get(key);

      // response code & desc
      if (mappedKey.equals(RechargeResponse.Fields.RESPONSE_CODE)) {
        responseCode = response.get(key);
        response.remove(key);
        continue;
      }

      if (mappedKey.equals(RechargeResponse.Fields.RESPONSE_DESCRIPTION)) {
        responseDesc = response.get(key);
        response.remove(key);
        continue;
      }

      if (mappedKey.equals(RechargeResponse.Fields.RESPONSE_MESSAGE)) {
        responseMessage = response.get(key);
        response.remove(key);
        continue;
      }

      response.put(mappedKey, response.get(key));
      if (!mappedKey.equals(key))
        response.remove(key);
    }

    if (fieldsMappings.containsKey(RechargeResponse.Fields.RESPONSE_CODE))
      codeKey = fieldsMappings.get(RechargeResponse.Fields.RESPONSE_CODE);
    if (fieldsMappings.containsKey(RechargeResponse.Fields.RESPONSE_DESCRIPTION))
      descKey = fieldsMappings.get(RechargeResponse.Fields.RESPONSE_DESCRIPTION);

    if (responseCode == null)
      responseCode = response.get(RechargeResponse.Fields.RESPONSE_CODE);
    if (responseDesc == null)
      responseDesc = response.get(RechargeResponse.Fields.RESPONSE_DESCRIPTION);

    if (responseCode != null) {
      response.put(codeKey, responseCode);
      if (responseMessage != null)
        response.put(messageKey, responseMessage);
      if (responseDesc != null)
        response.put(descKey, responseDesc);
    }

    return true;
  }

  @Override
  public boolean processBillResults(PartnerRequestContext requestContext, Protocol protocol,
      BillRequest request, byte[] rawData, BillResponse response) throws Exception {

    if (response == null)
      return false;

    String payload = new String(rawData, StandardCharsets.UTF_8);

    HashMap<String, Object> data = objectMapper.readValue(payload, typeRef);

    Map<String, Object> flattenMap = RechargeUtils.flattenMap(data, ".");

    Iterator<String> it = flattenMap.keySet().iterator();

    while (it.hasNext()) {
      String key = it.next();
      response.put(key, flattenMap.get(key) != null ? flattenMap.get(key) : null);
    }

    LOG.debug(
        LogFormatter.instance(requestContext.getTraceId()).data("response", response.size()).format());

    String codeKey = RechargeResponse.Fields.RESPONSE_CODE;
    String descKey = RechargeResponse.Fields.RESPONSE_DESCRIPTION;
    String messageKey = RechargeResponse.Fields.RESPONSE_MESSAGE;
    String responseCode = null, responseDesc = null, responseMessage = null;

    final Map<String, String> fieldsMappings = getFieldsMapping();

    for (String key : fieldsMappings.keySet()) {
      String mappedKey = fieldsMappings.get(key);

      // response code & desc
      if (mappedKey.equals(RechargeResponse.Fields.RESPONSE_CODE)) {
        responseCode = (String) response.get(key);
        response.remove(key);
        continue;
      }

      if (mappedKey.equals(RechargeResponse.Fields.RESPONSE_DESCRIPTION)) {
        responseDesc = (String) response.get(key);
        response.remove(key);
        continue;
      }

      if (mappedKey.equals(RechargeResponse.Fields.RESPONSE_MESSAGE)) {
        responseMessage = (String) response.get(key);
        response.remove(key);
        continue;
      }

      response.put(mappedKey, response.get(key));
      if (!mappedKey.equals(key))
        response.remove(key);
    }

    if (fieldsMappings.containsKey(RechargeResponse.Fields.RESPONSE_CODE))
      codeKey = fieldsMappings.get(RechargeResponse.Fields.RESPONSE_CODE);
    if (fieldsMappings.containsKey(RechargeResponse.Fields.RESPONSE_DESCRIPTION))
      descKey = fieldsMappings.get(RechargeResponse.Fields.RESPONSE_DESCRIPTION);

    if (responseCode == null)
      responseCode = (String) response.get(RechargeResponse.Fields.RESPONSE_CODE);
    if (responseMessage == null)
      responseMessage = (String) response.get(RechargeResponse.Fields.RESPONSE_MESSAGE);
    if (responseDesc == null)
      responseDesc = (String) response.get(RechargeResponse.Fields.RESPONSE_DESCRIPTION);

    if (responseCode != null)
      response.put(codeKey, responseCode);
    if (responseMessage != null)
      response.put(messageKey, responseMessage);
    if (responseDesc != null)
      response.put(descKey, responseDesc);
    return true;
  }
}
