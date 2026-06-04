package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillerItem {

    @JsonProperty("Id")
    private Double id;

    @JsonProperty("BillerCategory")
    private Double billerCategory;

    @JsonProperty("BillerCategoryDesc")
    private String billerCategoryDesc;

    @JsonProperty("BillerMode")
    private String billerMode;

    @JsonProperty("BillerId")
    private String billerId;

    @JsonProperty("BillerName")
    private String billerName;

    @JsonProperty("AcceptAdHocPayment")
    private Boolean acceptAdHocPayment;

    @JsonProperty("PaymentAmtExactness")
    private String paymentAmtExactness;

    @JsonProperty("BillerDescription")
    private String billerDescription;

    @JsonProperty("BillerInputParams")
    private List<BillerInputParams> billerInputParams;
}
