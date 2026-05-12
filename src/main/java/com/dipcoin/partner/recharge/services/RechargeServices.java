package com.dipcoin.partner.recharge.services;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

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

public interface RechargeServices {

	@Async
	public Future<BillInfoResponse> getBillInfo(final PartnerRequestContext requestContext,
			final BillInfoRequest request) throws RechargeServiceException;

	@Async
	public Future<BillRegisterComplaintResponse> registerComplaint(final PartnerRequestContext requestContext,
			final BillRegisterComplaintRequest request) throws RechargeServiceException;

	@Async
	public Future<BillRegisterComplaintResponse> getComplaintStatus(final PartnerRequestContext requestContext,
			final BillComplaintStatusRequest request) throws RechargeServiceException;

	@Async
	public Future<BillStatusResponse> getBillTransactionStatus(final PartnerRequestContext requestContext,
			final BillStatusRequest request) throws RechargeServiceException;

	@Async
	public Future<BillPaymentValidateResponse> validateBill(final PartnerRequestContext requestContext,
			final BillPaymentValidateRequest request) throws RechargeServiceException;

	@Async
	public Future<BillPaymentServiceResponse> processBillPayment(final PartnerRequestContext requestContext,
			final BillPaymentServiceRequest request) throws RechargeServiceException;

}