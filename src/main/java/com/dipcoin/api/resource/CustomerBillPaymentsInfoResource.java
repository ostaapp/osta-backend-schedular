package com.dipcoin.api.resource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.service.BbpsRefundService;
import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.RechargePlanDetailsDBService;
import com.dipcoin.db.services.model.RechargePlanDetails;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;


@Slf4j
@Component("customerBillPaymentsInfoResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class CustomerBillPaymentsInfoResource {

	@Autowired
	MerchantDBService merchantDBService;

	@Autowired
	@Lazy
	private BbpsRefundService bbpsRefundService;

	@Autowired
	private FinacusHttpClient finacusHttpClient;

	@Autowired
	private RechargePlanDetailsDBService rechargePlanDetailsDBService;
	
	public List<RechargePlanDetails> getRechargePlans(String billerId, String zone) throws Exception {
		String json = finacusHttpClient.fetchRechargePlans(billerId, zone);

		JsonNode rootNode = mapper.readTree(json);

		if (!"000".equals(rootNode.path("ResponseCode").asText())) {
			throw new Exception("Failed to fetch plans: " + rootNode.path("ResponseMessage").asText());
		}

		JsonNode planListNode = rootNode.path("PlanDataList");
		if (planListNode.isMissingNode() || !planListNode.isArray()) {
			throw new Exception("No PlanDataList found in response");
		}

		List<RechargePlanDetails> plans = new ArrayList<>();

		for (JsonNode plan : planListNode) {
			RechargePlanDetails details = new RechargePlanDetails();
			details.setBillerId(billerId);
			details.setZone(zone);
			details.setPlanId(plan.path("PLANID").asText());
			details.setCategoryType(plan.path("CATEGORYTYPE").asText());
			details.setAmount(new BigDecimal(plan.path("AMOUNT").asText("0")));
			details.setPlanDescription(plan.path("PLANDESCRIPTION").asText());
			details.setType(plan.path("TYPE").asText());
			details.setTalktime(plan.path("TALKTIME").asText());
			details.setValidity(plan.path("VALIDITY").asText());
			details.setData(plan.path("DATA").asText());
			details.setStatus(1);

			// Save to DB
			if (rechargePlanDetailsDBService != null) {
				rechargePlanDetailsDBService.saveOrUpdate(details);
			}

			plans.add(details);
		}

		return plans;
	}
	
	private final ObjectMapper mapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

}
