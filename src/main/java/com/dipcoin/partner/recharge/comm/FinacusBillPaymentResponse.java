package com.dipcoin.partner.recharge.comm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FinacusBillPaymentResponse extends BillResponse {

    private static final long serialVersionUID = 1L;

    public interface Fields extends BillResponse.Fields {
        String FINACUS_RESPONSE_CODE = "finacusResponseCode";
        String FINACUS_STATUS = "finacusStatus";
        String FINACUS_MESSAGE = "finacusMessage";
        String FINACUS_TRANSACTION_ID = "finacusTransactionId";
        String FINACUS_BILLER_ID = "finacusBillerId";
    }

    public void setFinacusResponseCode(String code) {
        this.put(Fields.FINACUS_RESPONSE_CODE, code);
    }

    public String getFinacusResponseCode() {
        return (String) this.get(Fields.FINACUS_RESPONSE_CODE);
    }

    public void setFinacusStatus(String status) {
        this.put(Fields.FINACUS_STATUS, status);
    }

    public String getFinacusStatus() {
        return (String) this.get(Fields.FINACUS_STATUS);
    }

    public void setFinacusMessage(String message) {
        this.put(Fields.FINACUS_MESSAGE, message);
    }

    public String getFinacusMessage() {
        return (String) this.get(Fields.FINACUS_MESSAGE);
    }

    public void setFinacusTransactionId(String txnId) {
        this.put(Fields.FINACUS_TRANSACTION_ID, txnId);
    }

    public String getFinacusTransactionId() {
        return (String) this.get(Fields.FINACUS_TRANSACTION_ID);
    }

    public void setFinacusBillerId(String billerId) {
        this.put(Fields.FINACUS_BILLER_ID, billerId);
    }

    public String getFinacusBillerId() {
        return (String) this.get(Fields.FINACUS_BILLER_ID);
    }
}
