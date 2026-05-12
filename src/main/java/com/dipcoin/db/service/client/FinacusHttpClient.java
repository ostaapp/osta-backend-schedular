package com.dipcoin.db.service.client;

import com.dipcoin.api.model.*;
import com.dipcoin.db.services.model.RechargePlan;
import com.dipcoin.partner.utils.ChecksumUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
@PropertySource(
        value = {
                "classpath:dipcoin-finacus-api.properties",
                "classpath:dipcoin-finacus-api-${spring.profiles.active}.properties"
        },
        ignoreResourceNotFound = true)
public class FinacusHttpClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${bbps.api.base-url}")
    private String baseUrl;

    @Value("${finacus.biller.categories.url}")
    private String billerCategoryUrl;

    @Value("${finacus.biller.coverages.url}")
    private String billerCoveragesUrl;

    @Value("${finacus.biller.by.categories.url}")
    private String billerByCategoriesUrl;

    @Value("${finacus.get.bill.payment.url}")
    private String billpaymenturl;

    @Value("${finacus.get.customer.params.url}")
    private String customerParamsUrl;

    @Value("${finacus.get.plan.url}")
    private String planUrl;

    @Value("${finacus.complaint.url}")
    private String complaintUrl;
    @Value("${finacus.complaint.soapaction}")
    private String complaintSoapAction;

    @Value("${finacus.agent.id}")
    private String finacusAgentId;

    @Value("${finacus.init.channel}")
    private String finacusInitChannel;

    @Value("${finacus.payment.channel}")
    private String finacusPaymentChannel;

    @Value("${finacus.payment.mode}")
    private String finacusPaymentMode;
    
    @Value("${finacus.get.tickets.history.soapaction}")
    private String getTicketsHistorySoapAction;

    @Value("${finacus.payment.mti}")
    private String finacusPaymentMti;

    @Value("${finacus.get.params.url}")
    private String customerUrl;

    @Value("${finacus.raise.ticket.soapaction}")
    private String raiseTicketSoapAction;

    @Value("${finacus.ticket.status.soapaction}")
    private String ticketStatusSoapAction;
    
    @Value("${finacus.get.zone.url}")
    private String getZoneByBillerIdUrl;
    
    @Value("${finacus.get.plan.description.url}")
    private String getPlanDescriptionUrl;

    /**
     * Fetch transaction status from Finacus API
     * 
     * @param statusBy The type of status lookup (TRANSACTION_ID, REFERENCE_ID, MOBILE_NUMBER)
     * @param transactionStatusParam The parameter value for status lookup
     * @param checksum Generated checksum for the request
     * @return Finacus API response
     */
    public String getTransactionStatus(String statusBy, String transactionStatusParam, String checksum) throws Exception {
        
        log.info("Starting Finacus transaction status request");
        log.info("Request Parameters: statusBy=[{}], transactionStatusParam=[{}], checksum=[{}]", 
                statusBy, transactionStatusParam, checksum);

        // Build checksum input for validation (should match what was sent)
        String checksumInput = statusBy + "|" + transactionStatusParam;
        log.info("CHECKSUM INPUT = [{}]", checksumInput);
        log.info("GENERATED CHECKSUM = [{}]", checksum);

        // Prepare form data for the request
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("statusBy", encode(statusBy));
        form.add("transactionStatusParam", encode(transactionStatusParam));
        form.add("checksum", checksum);

        log.info("=== FINACUS TRANSACTION STATUS REQUEST ===");
        log.info("statusBy: {}", statusBy);
        log.info("transactionStatusParam: {}", transactionStatusParam);
        log.info("checksum: {}", checksum);
        log.info("=== END REQUEST PARAMETERS ===");

        try {
            // Build SOAP 1.1 request for transaction status
            String soapRequest = buildTransactionStatusSoapRequest(statusBy, transactionStatusParam, checksum);

            // Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "text/xml; charset=utf-8");
            headers.add("SOAPAction", "http://tempuri.org/SendTransactionStatusRequest");

            HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);

            // Determine the SOAP endpoint (you may need to configure this)
            String soapEndpoint = resolveSoapEndpoint(billpaymenturl); // Reusing existing URL or configure new one

            log.info("Calling Finacus Transaction Status SOAP URL: {}", soapEndpoint);
            log.info("=== FULL SOAP REQUEST TO FINACUS ===");
            log.info(soapRequest);
            log.info("=== END SOAP REQUEST ===");

            // Make the API call
            String soapResponse = restTemplate.postForObject(soapEndpoint, entity, String.class);

            log.info("=== FULL SOAP RESPONSE FROM FINACUS ===");
            log.info(soapResponse != null ? soapResponse : "NULL RESPONSE");
            log.info("=== END SOAP RESPONSE ===");

            if (StringUtils.isBlank(soapResponse)) {
                log.error("Received empty response from Finacus transaction status API");
                throw new Exception("Empty response from Finacus API");
            }

            // Extract JSON from SOAP response
            String jsonResponse = extractJsonFromTransactionStatusSoapResponse(soapResponse);
            
            log.info("=== EXTRACTED JSON RESPONSE ===");
            log.info(jsonResponse);
            log.info("=== END JSON RESPONSE ===");

            return jsonResponse;

        } catch (Exception e) {
            log.error("Exception in Finacus transaction status request", e);
            throw new Exception("Failed to get transaction status from Finacus: " + e.getMessage(), e);
        }
    }

    /**
     * Build SOAP 1.1 request for transaction status
     */
    private String buildTransactionStatusSoapRequest(String statusBy, String transactionStatusParam, String checksum) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
               "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
               "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
               "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
               "<soap:Body>" +
               "<SendTransactionStatusRequest xmlns=\"http://tempuri.org/\">" +  // FIXED METHOD NAME
               "<statusBy>" + encode(statusBy) + "</statusBy>" +
               "<transactionStatusParam>" + encode(transactionStatusParam) + "</transactionStatusParam>" +
               "<checksum>" + checksum + "</checksum>" +
               "</SendTransactionStatusRequest>" +  // FIXED CLOSING TAG
               "</soap:Body>" +
               "</soap:Envelope>";
    }
    
    // URL-ENCODING FIX
    private String encode(String value) {
        try {
            return value == null ? "" : URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * SOAP 1.1 requests must be posted to the base .asmx endpoint.
     * If the configured URL already targets a specific method (e.g.
     * .../SendBillPaymentRequestV2),
     * strip the suffix so the SOAPAction header selects the correct method
     * server-side.
     */
    private String resolveSoapEndpoint(String configuredUrl) {
        if (StringUtils.isBlank(configuredUrl)) {
            return configuredUrl;
        }
        String trimmed = configuredUrl.trim();
        String[] methodSuffixes = new String[] { "/SendBillPaymentRequestV2", "/SendBillPaymentRequest",
                "/SendBillPaymentRequestOffline" };
        for (String methodSuffix : methodSuffixes) {
            if (trimmed.endsWith(methodSuffix)) {
                return trimmed.substring(0, trimmed.length() - methodSuffix.length());
            }
        }
        return trimmed;
    }
    
    /**
     * Extract JSON from transaction status SOAP response
     */
	private String extractJsonFromTransactionStatusSoapResponse(String soapResponse) throws Exception {
		if (soapResponse == null || soapResponse.trim().isEmpty()) {
			throw new Exception("SOAP response is null or empty");
		}

		try {
			// Convert XML to JSON to extract content
			JSONObject xmlJson = XML.toJSONObject(soapResponse);

			// Navigate through SOAP envelope structure
			JSONObject envelope = xmlJson.optJSONObject("soap:Envelope") != null
					? xmlJson.optJSONObject("soap:Envelope")
					: xmlJson.optJSONObject("Envelope");

			if (envelope == null) {
				throw new Exception("Envelope tag not found");
			}

			JSONObject body = envelope.optJSONObject("soap:Body") != null ? envelope.optJSONObject("soap:Body")
					: envelope.optJSONObject("Body");

			if (body == null) {
				throw new Exception("Body tag not found");
			}

			// Look for SendTransactionStatusRequestResponse (FIXED)
			JSONObject response = body.optJSONObject("SendTransactionStatusRequestResponse");
			if (response == null) {
				throw new Exception("SendTransactionStatusRequestResponse tag not found");
			}

			// Extract the result (FIXED)
			String result = response.optString("SendTransactionStatusRequestResult");
			if (result == null || result.trim().isEmpty()) {
				throw new Exception("SendTransactionStatusRequestResult not found or empty");
			}

			return result.trim();

		} catch (Exception e) {
			log.warn("Error parsing transaction status SOAP response, attempting fallback extraction: {}",
					e.getMessage());

			// Fallback: Try to extract content between tags (FIXED)
			int startTag = soapResponse.indexOf("<SendTransactionStatusRequestResult>");
			int endTag = soapResponse.indexOf("</SendTransactionStatusRequestResult>");

			if (startTag != -1 && endTag != -1 && endTag > startTag) {
				startTag += "<SendTransactionStatusRequestResult>".length();
				return soapResponse.substring(startTag, endTag).trim();
			}

			throw new Exception("Unable to extract JSON from transaction status SOAP response: " + e.getMessage(), e);
		}
	}

}
