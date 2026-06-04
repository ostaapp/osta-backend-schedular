package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class BillerCategoriesResponse {

    @JsonProperty("Response")
    private List<BillerCategoryItem> response;

    @JsonProperty("ResponseCode")
    private String responseCode;

    @JsonProperty("ResponseMessage")
    private String responseMessage;
}
