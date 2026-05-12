package com.dipcoin.partner.recharge.services;

import java.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.dipcoin.partner.recharge.client.RechargeClientFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.client.RechargeClient;
import com.dipcoin.partner.recharge.comm.BillComplaintStatusRequest;
import com.dipcoin.partner.recharge.comm.BillInfoRequest;
import com.dipcoin.partner.recharge.comm.BillInfoResponse;
import com.dipcoin.partner.recharge.comm.BillPaymentServiceRequest;
import com.dipcoin.partner.recharge.comm.BillPaymentServiceResponse;
import com.dipcoin.partner.recharge.comm.BillPaymentValidateRequest;
import com.dipcoin.partner.recharge.comm.BillPaymentValidateResponse;
import com.dipcoin.partner.recharge.comm.BillRegisterComplaintRequest;
import com.dipcoin.partner.recharge.comm.BillRegisterComplaintResponse;
import com.dipcoin.partner.recharge.comm.BillStatusRequest;
import com.dipcoin.partner.recharge.comm.BillStatusResponse;
import com.dipcoin.partner.recharge.utils.RechargeServiceException;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.dipcoin.scheduler.constants.RechargeConstants.RechargeResponseStatus;

@Component("rechargeServices")
public class RechargeServicesImpl implements RechargeServices {

	@Autowired
	private RechargeClientFactory rechargeClientFactory;

	private static final Logger LOG = LogManager.getLogger(RechargeServicesImpl.class);

	@Override
	public Future<BillInfoResponse> getBillInfo(PartnerRequestContext requestContext, BillInfoRequest billInfoRequest)
			throws RechargeServiceException {
		LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Validating the request").format());

		billInfoRequest.setRequestType(OperationType.VALIDATE_RECHARGE.value());

		BillInfoResponse errorResp = new BillInfoResponse();
		try {
			// validate incoming request
			if (billInfoRequest == null || !billInfoRequest.validate()) {
				String desc = "Request parameters are invalid";
				LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
				errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
				errorResp.setResponseMessage(desc);
				return new AsyncResult<>(errorResp);
			}

			LOG.info(LogFormatter.instance(requestContext.getTraceId())
					.message("Bill request validated and sent to recharge client.").format());

			String rechargeClientName = clientLookupKey(billInfoRequest.getMerchantReferenceId(),
					billInfoRequest.getOperationType());

			RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

			if (client != null) {
				BillInfoResponse response = client.processRequest(requestContext, billInfoRequest);
				if (response != null) {

					if (response.getResponseCode() == null
							|| !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
						LOG.info(LogFormatter.instance(requestContext.getTraceId())
								.message("Bill info response indicates failure.").format());

						errorResp = (BillInfoResponse) response.clone();
						return new AsyncResult<>(errorResp);
					}
				}
				LOG.info(LogFormatter.instance(requestContext.getTraceId())
						.message("Bill info response processed successfully.").format());

				return new AsyncResult<>(response);
			}
		} catch (RechargeServiceException e) {
			LOG.error(LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(), e);
		}

		errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
		errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

		// return errorResp;
		return new AsyncResult<>(errorResp);
	}

	private String clientLookupKey(String merchantId, OperationType operationType) {
		return operationType.value();
	}

	@Override
	public Future<BillRegisterComplaintResponse> registerComplaint(PartnerRequestContext requestContext,
			BillRegisterComplaintRequest request) throws RechargeServiceException {
		request.setRequestType(OperationType.REGISTER_COMPLAINT.value());

		BillRegisterComplaintResponse errorResp = new BillRegisterComplaintResponse();

		try {
			// validate incoming request
			if (request == null || !request.validate()) {
				String desc = "Request parameters are invalid";
				LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
				errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
				errorResp.setResponseMessage(desc);
				return new AsyncResult<>(errorResp);
			}

			LOG.info(LogFormatter.instance(requestContext.getTraceId())
					.message("Complaint registration request validated and sent to recharge client.").format());

			String rechargeClientName = clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

			RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

			if (client != null) {
				BillRegisterComplaintResponse response = client.processRequest(requestContext, request);
				if (response != null) {

					if (response.getResponseCode() == null
							|| !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {

						LOG.info(LogFormatter.instance(requestContext.getTraceId())
								.message("Complaint registration response indicates failure.").format());

						errorResp = (BillRegisterComplaintResponse) response.clone();
						return new AsyncResult<>(errorResp);
					}
				}

				LOG.info(LogFormatter.instance(requestContext.getTraceId())
						.message("Complaint registration response processed successfully.").format());

				return new AsyncResult<>(response);
			}

		} catch (RechargeServiceException e) {
			LOG.error(LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(), e);
		}

		errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
		errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

		return new AsyncResult<>(errorResp);
	}

	@Override
	public Future<BillRegisterComplaintResponse> getComplaintStatus(PartnerRequestContext requestContext,
			BillComplaintStatusRequest request) throws RechargeServiceException {

		request.setRequestType(OperationType.COMPLAINT_STATUS.value());

		BillRegisterComplaintResponse errorResp = new BillRegisterComplaintResponse();

		try {
			// validate incoming request
			if (request == null || !request.validate()) {
				String desc = "Request parameters are invalid";
				LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
				errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
				errorResp.setResponseMessage(desc);
				return new AsyncResult<>(errorResp);
			}

			LOG.info(LogFormatter.instance(requestContext.getTraceId())
					.message("Complaint status request validated and sent to recharge client.").format());

			String rechargeClientName = clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

			RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

			if (client != null) {
				BillRegisterComplaintResponse response = client.processRequest(requestContext, request);
				if (response != null) {

					if (response.getResponseCode() == null
							|| !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {

						LOG.info(LogFormatter.instance(requestContext.getTraceId())
								.message("Complaint status response indicates failure.").format());

						errorResp = (BillRegisterComplaintResponse) response.clone();
						return new AsyncResult<>(errorResp);
					}
				}

				LOG.info(LogFormatter.instance(requestContext.getTraceId())
						.message("Complaint status response processed successfully.").format());

				return new AsyncResult<>(response);
			}

		} catch (RechargeServiceException e) {
			LOG.error(LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(), e);
		}

		errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
		errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

		return new AsyncResult<>(errorResp);
	}

	@Override
	public Future<BillStatusResponse> getBillTransactionStatus(PartnerRequestContext requestContext,
			BillStatusRequest request) throws RechargeServiceException {
		BillStatusResponse errorResp = new BillStatusResponse();
		request.setRequestType(OperationType.FETCH_STATUS.value());

		try {
			// validate incoming request
			if (request == null || !request.validate()) {
				String desc = "Request parameters are invalid";
				LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
				errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
				errorResp.setResponseMessage(desc);
				return new AsyncResult<>(errorResp);
			}

			String rechargeClientName = clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

			RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

			if (client != null) {
				BillStatusResponse response = client.processRequest(requestContext, request);
				if (response != null) {

					if (response.getResponseCode() == null
							|| !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {

						LOG.info(LogFormatter.instance(requestContext.getTraceId())
								.message("Bill status response indicates failure.").format());

						errorResp = (BillStatusResponse) response.clone();
						return new AsyncResult<>(errorResp);
					}
				}

				LOG.info(LogFormatter.instance(requestContext.getTraceId())
						.message("Bill status response processed successfully.").format());
				return new AsyncResult<>(response);
			}

		} catch (RechargeServiceException e) {
			LOG.error(LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(), e);
		}

		errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
		errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

		return new AsyncResult<>(errorResp);
	}

	@Override
	public Future<BillPaymentValidateResponse> validateBill(PartnerRequestContext requestContext,
			BillPaymentValidateRequest request) throws RechargeServiceException {

		BillPaymentValidateResponse errorResp = new BillPaymentValidateResponse();
		request.setRequestType(OperationType.VALIDATE_BILL.value());

		try {
			// validate incoming request
			if (request == null || !request.validate()) {
				String desc = "Request parameters are invalid";
				LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
				errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
				errorResp.setResponseMessage(desc);
				return new AsyncResult<>(errorResp);
			}

			String rechargeClientName = clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

			RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

			if (client != null) {
				BillPaymentValidateResponse response = client.processRequest(requestContext, request);
				if (response != null) {
					if (response.getResponseCode() == null
							|| !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {

						LOG.info(LogFormatter.instance(requestContext.getTraceId())
								.message("Bill payment validation response indicates failure.").format());

						errorResp = (BillPaymentValidateResponse) response.clone();
						return new AsyncResult<>(errorResp);
					}
				}

				LOG.info(LogFormatter.instance(requestContext.getTraceId())
						.message("Bill payment validation response processed successfully.").format());

				return new AsyncResult<>(response);
			}

		} catch (RechargeServiceException e) {
			LOG.error(LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(), e);
		}

		errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
		errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

		return new AsyncResult<>(errorResp);
	}

	@Override
	public Future<BillPaymentServiceResponse> processBillPayment(PartnerRequestContext requestContext,
			BillPaymentServiceRequest request) throws RechargeServiceException {
		LOG.debug(
				LogFormatter.instance(requestContext.getTraceId()).message("Requested for recharge service").format());

		request.setRequestType(OperationType.SERVICE_RECHARGE.value());

		BillPaymentServiceResponse errorResp = new BillPaymentServiceResponse();
		try {
			// validate incoming request
			if (request == null || !request.validate()) {
				String desc = "Request parameters are invalid";
				LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
				errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
				errorResp.setResponseMessage(desc);
				return new AsyncResult<>(errorResp);
			}

			String rechargeClientName = clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

			RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

			if (client != null) {

				BillPaymentServiceResponse response = client.processRequest(requestContext, request);

				if (response != null) {

					if (response.getResponseCode() == null
							|| !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {

						LOG.info(LogFormatter.instance(requestContext.getTraceId())
								.message("Bill payment service response indicates failure.").format());

						errorResp = (BillPaymentServiceResponse) response.clone();
						errorResp.setMerchantRefNo(
								request.getBillMerchantServiceDetails().getMerchantRefNo().toString());
						errorResp.setEuronetRefNo(response.getEuronetRefNo());
						return new AsyncResult<>(errorResp);
					}
				}

				LOG.info(LogFormatter.instance(requestContext.getTraceId())
						.message("Bill payment service response processed successfully.").format());

				return new AsyncResult<>(response);
			}

		} catch (RechargeServiceException e) {
			LOG.error(LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(), e);
		}

		errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
		errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

		return new AsyncResult<>(errorResp);
	}

}
