package com.dipcoin.api.resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.commons.Constants;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.dipcoin.metrics.MerchantMetricRegistry;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.db.services.PartnerTransactionDBService;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerTransactionStatus;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerTransactionType;
import com.dipcoin.partner.db.services.model.PartnerTransaction;
import com.dipcoin.partner.paymentGateway.PartnerInternalServices;
import com.dipcoin.partner.paymentGateway.model.PartnerRefundRequest;
import com.dipcoin.partner.paymentGateway.model.PartnerRefundResponse;

/**
 *
 */
@Component("merchantSettlementResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class MerchantSettlementResource {

  private static final Logger LOG = LogManager.getLogger(MerchantSettlementResource.class);
  private static final String zero = "0.00";
  private static final DateFormat FORMATTER = new SimpleDateFormat("ddMMyyyy");

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private DipcoinDBService coinDBService;

  @Autowired(required = false)
  private EmailUtils emailUtils;

  @Autowired(required = false)
  private SmsClient smsClient;

  @Autowired(required = false)
  @Lazy
  private HttpServletContext httpServletContext;

  @Autowired
  private PartnerTransactionDBService partnerTransactionDBService;

  @Autowired
  private PartnerInternalServices partnerInternalServices;

  @Autowired
  @Qualifier("com.dipcoin.metrics.MerchantMetricRegistry")
  private MerchantMetricRegistry merchantMetricRegistry;

  @Autowired(required = false)
  private NotificationResource notificationResource;

  @Autowired
  private ApplicationProperties applicationProperties;

  public ResponseEntity cancelTransactions(final User user, final Merchant merchant,
	      String merchantTransactionRefId, String dipcoinTransactionRefId, String orderId,
	      String comment, BigDecimal amount, final String token) throws Exception, APIException {

	    String traceId = resolveTraceId();
	    boolean smsEnabled = resolveSmsEnabled();

	    if (user != null && merchant != null) {
	      LOG.debug(LogFormatter.instance(traceId)
	          .data("MerchantId", merchant.getId())
	          .data("merchantTransactionRefId", merchantTransactionRefId)
	          .data("dipcoinTransactionRefId", dipcoinTransactionRefId).data("orderId", orderId)
	          .data("comment", comment).format());

	      if (StringUtils.isEmpty(merchantTransactionRefId) || StringUtils.isEmpty(comment)
	          || StringUtils.isEmpty(orderId)) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(APIResponse.error(HeaderCode.BAD_REQUEST));
	      }

	      if (!(this.userDBService.isMerchantAdmin(user)
	          || this.userDBService.isMerchantSuperAdmin(user)
	          || this.userDBService.isMerchantTransactor(user)
	          || this.userDBService.isMerchantInternalUser(user))) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
	      }

	      // Fetch dipcoin transaction
	      List<String> transactionReferenceIds = new LinkedList<>();
	      transactionReferenceIds.add(merchantTransactionRefId);
	      List<DipcoinTransaction> transactions = coinDBService
	          .getMerchantTransactions(merchant, transactionReferenceIds, null, null);

	      if (CollectionUtils.isEmpty(transactions)) {
	        LOG.debug(LogFormatter.instance(traceId)
	            .message("Failed to fetch transaction from DTX").format());
	        // partner services id transaction exists in PTX

	        List<PartnerTransaction> partnerTransactions = partnerTransactionDBService
	            .findByOrderIdAndStatus(orderId, PartnerTransactionStatus.SUCCESS.value(),
	                dipcoinTransactionRefId, merchantTransactionRefId, merchant.getReferenceId(),
	                Arrays.asList(PartnerTransactionType.PAYMENT_GATEWAY_TRANSACTION.value()));
	        if (!CollectionUtils.isEmpty(partnerTransactions)) {
	          LOG.debug(LogFormatter.instance(traceId)
	              .message("transaction found in PTX ").format());
	          PartnerRefundRequest partnerRefundRequest = new PartnerRefundRequest();
	          partnerRefundRequest.setOrderId(orderId);
	          partnerRefundRequest.setAmount(amount);
	          partnerRefundRequest.setComment(comment);
	          partnerRefundRequest.setOstaTransactionReferenceId(dipcoinTransactionRefId);
	          partnerRefundRequest.setPartnerTransactionReferenceId(merchantTransactionRefId);
	          PartnerRefundResponse partnerRefundResponse = this.partnerInternalServices.initiatePartnerRefund(
	              partnerRefundRequest, token, resolveDcLoginCookie());

	          if (partnerRefundResponse == null) {
	            LOG.debug(LogFormatter.instance(traceId)
	                .message("partnerRefundResponse ").format());
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(APIResponse.error(HeaderCode.BAD_REQUEST));
	          }

	          return ResponseEntity.status(HttpStatus.OK).body(partnerRefundResponse);
	        }
	        LOG.debug(LogFormatter.instance(traceId)
	            .message("transaction not found in PTX ").format());
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(APIResponse.error(HeaderCode.TRANSACTION_DOESNT_EXIST));
	      }

	      if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(APIResponse.error(HeaderCode.BAD_REQUEST));
	      }

	      BigDecimal cancelAmount = amount;
	      BigDecimal usedTransactionAmount = null;
	      DipcoinTransaction nTx = null;
	      // @NOTE - Theoretically there will only be a single record
	      for (DipcoinTransaction dtx : transactions) {
	        if (dtx.getOrderId().equals(orderId)) {
	          if (DipcoinTransactionType.PARTIALLY_USED.equals(dtx.getType())
	              || DipcoinTransactionType.COMPLETELY_USED.equals(dtx.getType())) {
	            usedTransactionAmount = dtx.getAmount();
	            if (amount == null) {
	              cancelAmount = usedTransactionAmount;
	              amount = usedTransactionAmount;
	            }
	          } else if (DipcoinTransactionType.CANCELLED_BY_MERCHANT.equals(dtx.getType())) {
	            LOG.debug(LogFormatter.instance(traceId)
	                .message("TX already cancelled").format());
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(APIResponse.error(HeaderCode.TX_ALREADY_CANCELED));
	          } else if (DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT.equals(dtx.getType())) {
	            cancelAmount = cancelAmount.add(dtx.getAmount());
	          }
	          if (dtx.getDipcoinTransactionRefId().equals(dipcoinTransactionRefId)) {
	            nTx = (DipcoinTransaction) BeanUtils.cloneBean(dtx);
	            nTx.setId(0); // @NOTE Since entity is cloned, set Id = 0 to avoid "detached entity
	                          // passed to persist" error
	          }
	        }
	      }

	      // Partner failed before debit – no cancellation needed
	      if (amount == null) {
	        LOG.warn(LogFormatter.instance(traceId)
	            .message("Partner failure before debit. Cancellation not required.")
	            .data("merchantTransactionRefId", merchantTransactionRefId)
	            .data("orderId", orderId)
	            .format());

	        return ResponseEntity.noContent().build();
	      }

	      if (usedTransactionAmount.compareTo(cancelAmount) < 0) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(APIResponse.error(HeaderCode.BAD_REQUEST));
	      }

	      DipcoinTransactionType type = usedTransactionAmount.compareTo(amount) == 0
	          ? DipcoinTransactionType.CANCELLED_BY_MERCHANT
	          : DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT;

	      // For Promethus
	      if (usedTransactionAmount.compareTo(amount) == 0) {
	        merchantMetricRegistry.merchantCancellationFull().increment();
	      } else {
	        merchantMetricRegistry.merchantCancellationPartial().increment();
	      }

	      if (nTx == null) {
	        LOG.debug(LogFormatter.instance(traceId)
	            .message("Valid TX not found ").format());
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(APIResponse.error(HeaderCode.BAD_REQUEST));
	      }

	      // if eligible time frame crossed, cannot delete the TX
	      String ostaUsedTime = nTx.getRequestTime();
	      DateTime txRequestTime = new DateTime(Long.valueOf(ostaUsedTime), DateTimeZone.UTC);

	      if (!(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()
	          || MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment())) {
	        if (txRequestTime.plusDays(Constants.MERCHANT_TX_CANCEL_MAX_ELIGIBLE_DAYS).isBeforeNow()) {
	          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	              .body(APIResponse.error(HeaderCode.MERCHANT_TX_CANCEL_INELIGIBLE));
	        }
	      }

	      Future<Dipcoin> dcoinTask = coinDBService.asyncGetById(nTx.getDipcoinId(), false);

	      // ***** Validation
	      /**
	       * Sum value of type=2 and type=3 - type 10 amount.
	       */
	      // get the Current Time
	      DateTime currentTime = DateTime.now(DateTimeZone.UTC);
	      Long todayStartTime = Long.valueOf(currentTime.withTime(0, 0, 0, 0).getMillis());
	      Long todayEndTime = Long.valueOf(currentTime.withTime(23, 59, 59, 999).getMillis());

	      // If BusinessSegment of a Merchant isnt Toll or Recharge , then the below will
	      // be executed.
	      if (!(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()
	          || MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment())) {

	        // Status
	        List<Integer> types = Arrays.asList(DipcoinTransactionType.PARTIALLY_USED.value(),
	            DipcoinTransactionType.COMPLETELY_USED.value(), DipcoinTransactionType.CANCELLED_BY_MERCHANT.value(),
	            DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT.value());

	        List<DipcoinTransaction> dTxns = coinDBService
	            .getByTypes(types, merchant.getReferenceId(), todayStartTime, todayEndTime, null, null);

	        BigDecimal dtxnTotalUsedAmount = BigDecimal.ZERO;
	        BigDecimal dtxnTotalCancelledAmount = BigDecimal.ZERO;
	        BigDecimal todaysAmount = BigDecimal.ZERO;
	        BigDecimal calculatedAmount = BigDecimal.ZERO;
	        if (CollectionUtils.isEmpty(dTxns)) {
	          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	              .body(APIResponse.error(HeaderCode.MERCHANT_TX_CANCEL_INELIGIBLE));
	        } else {
	          for (DipcoinTransaction dipcoinTransaction : dTxns) {
	            // Total Sum value of type 2 and type 3
	            if (dipcoinTransaction.getType() == DipcoinTransactionType.PARTIALLY_USED.value()
	                || dipcoinTransaction.getType() == DipcoinTransactionType.COMPLETELY_USED.value()) {
	              dtxnTotalUsedAmount = dtxnTotalUsedAmount.add(dipcoinTransaction.getAmount());
	            } else if (dipcoinTransaction.getType() == DipcoinTransactionType.CANCELLED_BY_MERCHANT.value()
	                || dipcoinTransaction.getType() == DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT.value()) {
	              dtxnTotalCancelledAmount = dtxnTotalCancelledAmount.add(dipcoinTransaction.getAmount());
	            }
	          }
	          // Subtract the Amount.of Type 2 and 3 from type 10.
	          todaysAmount = dtxnTotalUsedAmount.subtract(dtxnTotalCancelledAmount);
	          // Requested Amount < 99% * [(Type 2 and Type 3 Amount ) - (Type 10 AMount)]
	          // for current business day (in DTX table)
	          calculatedAmount = todaysAmount.multiply(APIConstants.APPROX_MERCHANT_CANCELLATION_VALUE);
	          // Round Halfup
	          // calculatedAmount.ROUND_HALF_UP;
	          BigDecimal calculatedRoundedAmount = calculatedAmount.setScale(APIConstants.ROUND, RoundingMode.FLOOR);

	          // Check if this Amount is greater than the Amount to cancel.

	          if (calculatedRoundedAmount.compareTo(amount) < 0) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(APIResponse.error(HeaderCode.MERCHANT_TX_CANCEL_INELIGIBLE_INSUFFICIENT_FUND));
	          }
	          // ******
	        }
	      }

	      String dipcoinReferenceNumber = CoreUtils.generateDipcoinToMerchantReferenceNumber();

	      // mark tx as cancelled
	      String now = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
	      nTx.setAmount(amount);
	      nTx.setType(type.value());
	      nTx.setComments(comment);
	      nTx.setRequestTime(now);
	      nTx.setResponseTime(now);
	      nTx.setUpdateDate(now);
	      nTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
	      nTx.setIPAddress(Constants.ZERO_IP);
	      nTx = coinDBService.addTransaction(nTx);

	      if (nTx == null) {
	        LOG.debug(LogFormatter.instance(traceId)
	            .message("Failed to update transaction")
	            .data("merchantTransactionRefId", merchantTransactionRefId)
	            .data("dipcoinTransactionRefId", dipcoinTransactionRefId).data("orderId", orderId)
	            .data("dipcoinReferenceNumber", dipcoinReferenceNumber).data("comment", comment)
	            .format());
	        // throw exception to initiate rollback
	        throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
	            APIResponse.error(HeaderCode.INTERNAL_ERROR));
	      }

	      if (dcoinTask == null || dcoinTask.get() == null) {
	        LOG.debug(LogFormatter.instance(traceId)
	            .message("Failed to fetch dipcoin").data("DipcoinId", nTx.getDipcoinId()).format());
	        // throw exception to initiate rollback
	        throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
	            APIResponse.error(HeaderCode.INTERNAL_ERROR));
	      }

	      // update dipcoin status
	      Dipcoin dcoin = dcoinTask.get();
	      dcoin = coinDBService.updateCoin(dcoin, DipcoinStatus.PROCESSED_DISPUTED.value());
	      if (dcoin == null) {
	        LOG.debug(LogFormatter.instance(traceId)
	            .message("Failed to update dipcoin status").data("DipcoinId", nTx.getDipcoinId())
	            .format());
	        // throw exception to initiate rollback
	        throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
	            APIResponse.error(HeaderCode.INTERNAL_ERROR));
	      }

	      // For Recharge and Fastag Merchant email must be disabled
	      // Send email of cancellation to merchant
      if (emailUtils != null
          && (!(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()
          || MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment()))
          && (user.getIsEmailVerified() == BooleanStatus.YES.value()
              && !emailUtils.sendCancelTransactionEmail(user.getFirstName(), merchant.getEmailId(),
	                  dcoin.getCoin(), ostaUsedTime, nTx))) {
	        LOG.error(
	            "Failed to send email to user or email configuration not enabled as business segment of merchant is recharge or toll"
	                + user.getId());
	      }

	      // For Recharge and Fastag Merchant sms must be disabled
	      // Send sms

      if (smsClient != null
          && (!(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()
          || MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment()))
          && (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(merchant.getOfficeNumber(),
	              Templates.MerchantCancelTransaction.format(dcoin.getCoin(), now, dcoin.getAmount()),
	              smsEnabled))) {
	        LOG.debug(LogFormatter.instance(traceId)
	            .message(
	                "Failed to send sms to merchant or sms configuration not enabled as business segment of merchant is recharge or toll")
	            .data("phone", merchant.getOfficeNumber())
	            .data("template",
	                Templates.MerchantCancelTransaction.format(dcoin.getCoin(), now, dcoin.getAmount()))
	            .format());
	      }

      if (notificationResource != null && applicationProperties.enableSMS()
          && applicationProperties.getAwsSMSClient()) {

	        NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
	        notificationRequestContext.setTraceId(traceId);
	        if (!notificationResource.sendSms(merchant.getOfficeNumber(),
	            Templates.MerchantCancelTransaction.format(dcoin.getCoin(), now, dcoin.getAmount()),
	            smsEnabled, notificationRequestContext)) {

	          LOG.debug(LogFormatter.instance(traceId)
	              .message(
	                  "Failed to send sms to merchant or sms configuration not enabled as business segment of merchant is recharge or toll")
	              .data("phone", merchant.getOfficeNumber())
	              .data("template",
	                  Templates.MerchantCancelTransaction.format(dcoin.getCoin(), now, dcoin.getAmount()))
	              .format());

	        }
	      }

	      return ResponseEntity.noContent().build();
	    }

	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
	  }

	  private String resolveTraceId() {
	    try {
	      return StringUtils.defaultIfBlank(httpServletContext.getTraceId(), "SCHEDULER");
	    } catch (Exception e) {
	      return "SCHEDULER";
	    }
	  }

	  private String resolveDcLoginCookie() {
	    try {
	      if (httpServletContext.getRequestCookies() == null
	          || !httpServletContext.getRequestCookies().containsKey(APIConstants.DC_LOGIN_COOKIE)
	          || httpServletContext.getRequestCookies().get(APIConstants.DC_LOGIN_COOKIE) == null) {
	        return null;
	      }
	      return httpServletContext.getRequestCookies().get(APIConstants.DC_LOGIN_COOKIE).getValue();
	    } catch (Exception e) {
	      return null;
	    }
	  }

	  private boolean resolveSmsEnabled() {
	    try {
	      if (httpServletContext.getClientFeatureFlags() != null) {
	        return httpServletContext.getClientFeatureFlags().smsEnabled();
	      }
	    } catch (Exception e) {
	      // Fall back to the default feature-flag behavior when no request context is active.
	    }
	    return HttpServletContext.ClientFeatureFlags.instance().smsEnabled();
	  }
}
