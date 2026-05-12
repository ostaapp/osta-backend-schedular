package com.dipcoin.partner.recharge.comm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BillPaymentServiceResponse extends BillResponse {

  /**
   * 
   */
	
	@JsonProperty("PaymentRefId")
	private String paymentRefId;

	public String getPaymentRefId() {
	    return paymentRefId;
	}

	public void setPaymentRefId(String paymentRefId) {
	    this.paymentRefId = paymentRefId;
	}

  private static final long serialVersionUID = 1L;

  public static interface Fields extends BillResponse.Fields {
    public static final String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
  }

  public final String getEuronetRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEuronetRefNo(String euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }

}
