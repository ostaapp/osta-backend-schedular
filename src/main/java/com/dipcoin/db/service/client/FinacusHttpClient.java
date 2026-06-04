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
    private ChecksumUtil checksumUtil;

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

    @Value("${finacus.request.mobile-number:}")
    private String finacusRequestMobileNumber;

    @Value("${finacus.request.ip-address:}")
    private String finacusRequestIpAddress;

    @Value("${finacus.request.mac-address:}")
    private String finacusRequestMacAddress;

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

    public JsonNode billFetchForReminder(String billerId, String customerParams) throws Exception {
        String dataToCalculate = String.join("|",
                StringUtils.defaultString(finacusAgentId),
                StringUtils.defaultString(billerId),
                StringUtils.defaultString(finacusInitChannel),
                StringUtils.defaultString(finacusRequestMobileNumber),
                StringUtils.defaultString(customerParams),
                StringUtils.defaultString(finacusRequestIpAddress),
                StringUtils.defaultString(finacusRequestMacAddress));

        String checksum = checksumUtil.generateChecksum(dataToCalculate);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("agentId", encode(finacusAgentId));
        form.add("billerId", encode(billerId));
        form.add("initChannel", encode(finacusInitChannel));
        form.add("mobileNumber", encode(finacusRequestMobileNumber));
        form.add("customerParams", customerParams);
        form.add("ip", encode(finacusRequestIpAddress));
        form.add("mac", encode(finacusRequestMacAddress));
        form.add("checksum", checksum);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        String apiUrl = baseUrl + "/SendBillFetchRequest";
        log.info("Calling Finacus bill fetch for BBPS reminder verification. billerId={}", billerId);
        String rawResponse = restTemplate.postForObject(apiUrl, entity, String.class);
        String json = extractJsonFromFinacusString(rawResponse);
        if (StringUtils.isBlank(json)) {
            throw new Exception("Empty bill fetch response from Finacus");
        }
        return objectMapper.readTree(json);
    }

    private String extractJsonFromFinacusString(String xml) {
        if (xml == null) {
            return null;
        }
        return xml.replaceAll("(?s).*<string[^>]*>(.*)</string>.*", "$1");
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
	
	private BillerCategoriesResponse fetchFromUrl(String apiUrl) throws Exception {
        log.info("Calling Finacus API - URL: {}", apiUrl);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apiUrl);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(new StringEntity("coverage=")); // body

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                log.info("Finacus API Response - URL: {} | Response Size: {} bytes", apiUrl, responseBody.length());
                log.debug("Finacus API Response Body: {}", responseBody);

                // Convert XML to JSON
                JSONObject xmlJson = XML.toJSONObject(responseBody);

                // Extract actual JSON content inside <string>...</string>
                String jsonContent = xmlJson.getJSONObject("string").getString("content");

                // Map JSON to POJO
                BillerCategoriesResponse result = objectMapper.readValue(jsonContent, BillerCategoriesResponse.class);
                
                log.info("Finacus API Response parsed successfully - URL: {} | ResponseCode: {} | Items: {}", 
                        apiUrl, 
                        result.getResponseCode(), 
                        result.getResponse() != null ? result.getResponse().size() : 0);
                
                return result;
            }
        } catch (Exception e) {
            log.error("Finacus API call failed - URL: {} | Error: {}", apiUrl, e.getMessage(), e);
            throw e;
        }
    }
	
	 public BillerCategoriesResponse fetchBillerCategories() throws Exception {
	        return fetchFromUrl(billerCategoryUrl);
	    }
	 
	 public BillersByCategoryResponse getBillersByCategory(String categoryId, String coverage) throws Exception {

	        if (coverage == null)
	            coverage = "";

	        String checksumInput = categoryId + "|" + coverage;
	        String checksum = checksumUtil.generateChecksum(checksumInput);

	        String body = "categoryId=" + categoryId +
	                "&checksum=" + checksum +
	                "&coverage=" + coverage;

	        HttpPost post = new HttpPost(billerByCategoriesUrl);
	        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        post.setEntity(new StringEntity(body));

	        try (CloseableHttpClient client = HttpClients.createDefault();
	                CloseableHttpResponse response = client.execute(post)) {

	            String xmlResponse = EntityUtils.toString(response.getEntity());
	            JSONObject jsonObj = XML.toJSONObject(xmlResponse);

	            // Extract <string><content> actual JSON
	            String contentJson = jsonObj.getJSONObject("string").getString("content");

	            return objectMapper.readValue(contentJson, BillersByCategoryResponse.class);
	        }
	    }
	    
	 public String fetchCustomerParams(String billerId) throws Exception {

	        // Generate checksum input format EXACTLY required by Finacus
	        String checksumInput = billerId;
	        String checksum = checksumUtil.generateChecksum(checksumInput);

	        // Build request body
	        String body = "billerId=" + billerId +
	                "&checksum=" + checksum;

	        // Get URL from properties file
	        HttpPost post = new HttpPost(customerUrl);
	        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        post.setEntity(new StringEntity(body));

	        try (CloseableHttpClient client = HttpClients.createDefault();
	                CloseableHttpResponse response = client.execute(post)) {

	            // Raw XML
	            String xmlResponse = EntityUtils.toString(response.getEntity());

	            // Convert XML → JSONObject
	            JSONObject jsonObj = XML.toJSONObject(xmlResponse);

	            // Extract pure JSON inside <string>content</string>
	            return jsonObj.getJSONObject("string").getString("content");
	        }
	    }

	 /**
	     * Send complaint status request to Finacus
	     * 
	     * @param complaintType Type of complaint (e.g., "TXN")
	     * @param complaintId   Complaint ID from registration
	     * @return JSON response from Finacus
	     * @throws Exception if SOAP call fails
	     */
	    public String sendComplaintStatusRequest(String complaintType, String complaintId) throws Exception {

	        if (complaintType == null || complaintId == null) {
	            throw new IllegalArgumentException("complaintType and complaintId cannot be null");
	        }

	        String safeComplaintType = safe(complaintType);
	        String safeComplaintId = safe(complaintId);

	        // 1) Build checksum string: complaintType|complaintId
	        String rawChecksumInput = String.join("|",
	                safeComplaintType,
	                safeComplaintId);

	        log.debug("Complaint Status Checksum Raw String = {}", rawChecksumInput);

	        // 2) Generate checksum
	        String checksum = checksumUtil.generateChecksum(rawChecksumInput);
	        log.debug("Generated Complaint Status Checksum = {}", checksum);

	        // 3) Build SOAP 1.1 Envelope
	        String soapRequest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
	                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
	                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
	                "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
	                "  <soap:Body>\n" +
	                "    <SendComplaintStatusRequest xmlns=\"http://tempuri.org/\">\n" +
	                "      <complaintType>" + escapeXml(safeComplaintType) + "</complaintType>\n" +
	                "      <complaintId>" + escapeXml(safeComplaintId) + "</complaintId>\n" +
	                "      <checksum>" + escapeXml(checksum) + "</checksum>\n" +
	                "    </SendComplaintStatusRequest>\n" +
	                "  </soap:Body>\n" +
	                "</soap:Envelope>";

	        HttpHeaders headers = new HttpHeaders();
	        headers.add("Content-Type", "text/xml; charset=utf-8");
	        headers.add("SOAPAction", "http://tempuri.org/SendComplaintStatusRequest");

	        HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);
	        String soapEndpoint = resolveSoapEndpoint(complaintUrl);

	        log.info("Calling Finacus Complaint Status SOAP 1.1 URL: {}", soapEndpoint);
	        log.debug("=== SOAP REQUEST ===\n{}", soapRequest);

	        String soapResponse = restTemplate.postForObject(soapEndpoint, entity, String.class);

	        log.debug("=== SOAP RESPONSE ===\n{}", soapResponse);

	        // Extract the actual JSON result
	        String json = extractComplaintStatusResult(soapResponse);

	        log.info("Finacus Complaint Status JSON = {}", json);

	        return json;
	    }
	    
	    /**
	     * Extract JSON result from SendComplaintStatusRequest SOAP response
	     */
	    private String extractComplaintStatusResult(String soapResponse) throws Exception {

	        if (soapResponse == null)
	            throw new Exception("SOAP response is NULL");

	        JSONObject xml = XML.toJSONObject(soapResponse);

	        JSONObject envelope = xml.optJSONObject("soap:Envelope") != null ? xml.optJSONObject("soap:Envelope")
	                : xml.optJSONObject("soapenv:Envelope") != null ? xml.optJSONObject("soapenv:Envelope")
	                        : xml.optJSONObject("Envelope");

	        if (envelope == null)
	            throw new Exception("SOAP Envelope not found");

	        JSONObject body = envelope.optJSONObject("soap:Body") != null ? envelope.optJSONObject("soap:Body")
	                : envelope.optJSONObject("soapenv:Body") != null ? envelope.optJSONObject("soapenv:Body")
	                        : envelope.optJSONObject("Body");

	        if (body == null)
	            throw new Exception("SOAP Body not found");

	        // 1) Main response wrapper
	        JSONObject resp = body.optJSONObject("SendComplaintStatusRequestResponse");

	        if (resp != null && resp.has("SendComplaintStatusRequestResult")) {
	            return resp.getString("SendComplaintStatusRequestResult");
	        }

	        // 2) Backup: Finacus sometimes returns <string>
	        if (body.has("string")) {
	            return body.getString("string");
	        }

	        throw new Exception("Unable to extract complaint status result from SOAP response");
	    }

	    private String escapeXml(String value) {
	        if (value == null) {
	            return "";
	        }
	        return value.replace("&", "&amp;")
	                .replace("<", "&lt;")
	                .replace(">", "&gt;")
	                .replace("\"", "&quot;")
	                .replace("'", "&apos;");
	    }
	    
	    private String safe(String s) {
	        return s == null ? "" : s;
	    }
	    
	    public List<String> getAllBillerIds() throws Exception {
	        
	        log.info("Calling Finacus API - getAllBillerIds | Starting to fetch all biller IDs");
	        
	        List<String> billerIds = new ArrayList<>();

	        try {
	            // Fetch all categories from Finacus
	            BillerCategoriesResponse categories = fetchBillerCategories();

	            if (categories != null && categories.getResponse() != null) {
	                int totalCategories = categories.getResponse().size();
	                log.info("Finacus API Response - getAllBillerIds | Total categories fetched: {}", totalCategories);
	                
	                // Iterate over each category
	                for (BillerCategoryItem cat : categories.getResponse()) {
	                    try {
	                        log.info("Fetching billers for category: {} ({})", cat.getId(), cat.getName());
	                        
	                        // Fetch billers in this category
	                        BillersByCategoryResponse billers = getBillersByCategory(String.valueOf(cat.getId()), null);

	                        if (billers != null && billers.getResponse() != null) {
	                            int billersInCategory = billers.getResponse().size();
	                            log.info("Finacus API Response - Category {} | Billers found: {}", cat.getId(), billersInCategory);
	                            
	                            for (BillerItem b : billers.getResponse()) {
	                                // Avoid duplicates
	                                if (!billerIds.contains(b.getBillerId())) {
	                                    billerIds.add(b.getBillerId());
	                                }
	                            }
	                        } else {
	                            log.warn("No billers found for category: {}", cat.getId());
	                        }
	                    } catch (Exception e) {
	                        log.error("Error fetching billers for category {}: {}", cat.getId(), e.getMessage(), e);
	                    }
	                }
	            } else {
	                log.warn("Finacus API returned null or empty categories response");
	            }

	            log.info("Finacus API - getAllBillerIds completed | Total unique biller IDs: {}", billerIds.size());
	            
	            return billerIds;
	            
	        } catch (Exception e) {
	            log.error("Finacus API call failed - getAllBillerIds | Error: {}", e.getMessage(), e);
	            throw e;
	        }
	    }
	    
	    public List<String> getZonesForBiller(String billerId) throws Exception {

	        String checksum = checksumUtil.generateChecksum(billerId);
	        String body = "billerId=" + billerId + "&checksum=" + checksum;

	        try {
	            String xml = sendPost(customerParamsUrl, body);

	            String json = XML.toJSONObject(xml)
	                    .getJSONObject("string")
	                    .getString("content");

	            JsonNode root = objectMapper.readTree(json);
	            JsonNode response = root.get("Response");

	            List<String> zones = new ArrayList<>();

	            if (response == null || !response.isArray()) {
	                return zones;
	            }

	            for (JsonNode field : response) {
	                if (field.has("name") && "Circle".equals(field.get("name").asText())) {

	                    if (field.has("Regex")) {
	                        String regex = field.get("Regex").asText();
	                        String[] parts = regex.split("\\|");

	                        for (String p : parts) {
	                            String zone = p.replace("^(", "").replace(")$", "");
	                            if (!zone.isEmpty()) {
	                                zones.add(zone);
	                            }
	                        }
	                    }
	                }
	            }

	            return zones;

	        } catch (Exception e) {
	            log.error("Error fetching zones for biller {}: {}", billerId, e.getMessage());
	            return new ArrayList<>();
	        }
	    }

	    public String sendPost(String url, String body) throws Exception {

	        HttpPost post = new HttpPost(url);
	        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        post.setEntity(new StringEntity(body));

	        try (CloseableHttpClient client = HttpClients.createDefault();
	                CloseableHttpResponse response = client.execute(post)) {

	            return EntityUtils.toString(response.getEntity());
	        }
	    }

	    public String fetchRechargePlans(String billerId, String zone) throws Exception {
	        
	        log.info("Calling Finacus API - fetchRechargePlans | URL: {} | BillerId: {} | Zone: {}", planUrl, billerId, zone);

	        String checksumInput = billerId + "|" + zone;
	        String checksum = checksumUtil.generateChecksum(checksumInput);

	        String body = "BillerId=" + billerId +
	                "&Zone=" + zone +
	                "&checksum=" + checksum;

	        HttpPost post = new HttpPost(planUrl);
	        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        post.setEntity(new StringEntity(body));

	        try (CloseableHttpClient client = HttpClients.createDefault();
	                CloseableHttpResponse response = client.execute(post)) {

	            String xmlResponse = EntityUtils.toString(response.getEntity());
	            
	            log.info("Finacus API Response - fetchRechargePlans | URL: {} | Response Size: {} bytes", planUrl, xmlResponse.length());
	            log.debug("Finacus API Response Body: {}", xmlResponse);

	            JSONObject jsonObj = XML.toJSONObject(xmlResponse);

	            String jsonContent = jsonObj.getJSONObject("string").getString("content");
	            
	            log.info("Finacus API Response parsed - fetchRechargePlans | URL: {} | JSON Size: {} bytes", planUrl, jsonContent.length());
	            
	            return jsonContent; // return pure JSON
	        } catch (Exception e) {
	            log.error("Finacus API call failed - fetchRechargePlans | URL: {} | BillerId: {} | Zone: {} | Error: {}", 
	                    planUrl, billerId, zone, e.getMessage(), e);
	            throw e;
	        }
	    }

	    public List<RechargePlan> fetchPlans(String billerId, String circleName) {
	        
	        log.info("Calling Finacus API - fetchPlans | URL: {} | BillerId: {} | CircleName: {}", planUrl, billerId, circleName);
	        
	        try {
	            // Build checksum
	            String checksumInput = billerId + "|" + circleName;
	            String checksum = checksumUtil.generateChecksum(checksumInput);

	            String body = "BillerId=" + billerId +
	                    "&Zone=" + circleName +
	                    "&checksum=" + checksum;

	            log.info("REQUEST BODY FOR PLANS: {}", body);

	            HttpPost post = new HttpPost(planUrl);
	            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	            post.setEntity(new StringEntity(body));

	            CloseableHttpClient client = HttpClients.createDefault();
	            CloseableHttpResponse response = client.execute(post);

	            String xmlResponse = EntityUtils.toString(response.getEntity());
	            
	            log.info("Finacus API Response - fetchPlans | URL: {} | Response Size: {} bytes", planUrl, xmlResponse.length());
	            log.info("RAW XML RESPONSE: {}", xmlResponse);

	            JSONObject xmlJson = XML.toJSONObject(xmlResponse);
	            String contentJson = xmlJson.getJSONObject("string").getString("content");
	            
	            log.info("Finacus API Response - fetchPlans | URL: {} | JSON Size: {} bytes", planUrl, contentJson.length());
	            log.info("CONTENT JSON: {}", contentJson);

	            JSONObject obj = new JSONObject(contentJson);
	            
	            String responseCode = obj.optString("ResponseCode", "");
	            
	            if (!responseCode.equals("000")) {
	                log.warn("Finacus API returned non-success response - fetchPlans | URL: {} | ResponseCode: {} | Message: {}", 
	                        planUrl, responseCode, obj.optString("ResponseMessage", ""));
	                return List.of();
	            }

	            JSONArray arr = obj.optJSONArray("PlanDataList");
	            if (arr == null || arr.length() == 0) {
	                log.info("Finacus API returned empty PlanDataList - fetchPlans | URL: {} | BillerId: {} | CircleName: {}", 
	                        planUrl, billerId, circleName);
	                return List.of();
	            }

	            List<RechargePlan> list = new ArrayList<>();

	            for (int i = 0; i < arr.length(); i++) {
	                JSONObject p = arr.getJSONObject(i);

	                RechargePlan rp = new RechargePlan();
	                rp.setPlanId(p.optString("PLANID"));
	                rp.setRechargeTalkTime(extractNumber(p.optString("TALKTIME")));
	                rp.setRechargeValidity(p.optString("VALIDITY"));
	                rp.setRechargeShortDescription(p.optString("PLANDESCRIPTION"));
	                rp.setRechargeDescription(p.optString("PLANDESCRIPTION"));
	                rp.setEuronetPlanType(p.optString("CATEGORYTYPE"));
	                rp.setEuronetRechargeType(p.optString("TYPE"));
	                rp.setRechargeMaster(p.optString("TYPE"));

	                try {
	                    rp.setRechargeValue(new BigDecimal(p.optString("AMOUNT", "0")));
	                } catch (Exception e) {
	                    rp.setRechargeValue(BigDecimal.ZERO);
	                }

	                list.add(rp);
	            }
	            
	            log.info("Finacus API Response parsed successfully - fetchPlans | URL: {} | ResponseCode: {} | Plans: {}", 
	                    planUrl, responseCode, list.size());

	            return list;

	        } catch (Exception e) {
	            log.error("Finacus API call failed - fetchPlans | URL: {} | BillerId: {} | CircleName: {} | Error: {}", 
	                    planUrl, billerId, circleName, e.getMessage(), e);
	            return List.of();
	        }
	    }
	    
	    private BigDecimal extractNumber(String value) {
	        if (value == null)
	            return BigDecimal.ZERO;

	        String numeric = value.replaceAll("[^0-9.]", ""); // keep numbers only

	        if (numeric.isEmpty())
	            return BigDecimal.ZERO;

	        try {
	            return new BigDecimal(numeric);
	        } catch (Exception e) {
	            return BigDecimal.ZERO;
	        }
	    }
	    
	    public ZoneResponse getZoneByBillerId(String billerId) throws Exception {
	        
	        log.info("Calling Finacus API - getZoneByBillerId | URL: {} | BillerId: {}", getZoneByBillerIdUrl, billerId);

	        String checksumInput = billerId;
	        String checksum = checksumUtil.generateChecksum(checksumInput);

	        String body = "billerId=" + billerId +
	                "&checksum=" + checksum;

	        HttpPost post = new HttpPost(getZoneByBillerIdUrl);
	        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        post.setEntity(new StringEntity(body));

	        try (CloseableHttpClient client = HttpClients.createDefault();
	             CloseableHttpResponse response = client.execute(post)) {

	            String xmlResponse = EntityUtils.toString(response.getEntity());
	            
	            log.info("Finacus API Response - getZoneByBillerId | URL: {} | Response Size: {} bytes", getZoneByBillerIdUrl, xmlResponse.length());
	            log.debug("Finacus API Response Body: {}", xmlResponse);

	            JSONObject jsonObj = XML.toJSONObject(xmlResponse);

	            String contentJson = jsonObj.getJSONObject("string").getString("content");

	            ZoneResponse zoneResponse =
	                    objectMapper.readValue(contentJson, ZoneResponse.class);
	            
	            int zoneCount = (zoneResponse.getZoneList() != null) ? zoneResponse.getZoneList().size() : 0;
	            log.info("Finacus API Response parsed - getZoneByBillerId | URL: {} | ResponseCode: {} | Zones: {}", 
	                    getZoneByBillerIdUrl, zoneResponse.getResponseCode(), zoneCount);

	            return zoneResponse;
	        } catch (Exception e) {
	            log.error("Finacus API call failed - getZoneByBillerId | URL: {} | BillerId: {} | Error: {}", 
	                    getZoneByBillerIdUrl, billerId, e.getMessage(), e);
	            throw e;
	        }
	    }
}
