package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BillerCategoryItem {

    // Works for both Category and Coverage
    @JsonProperty("Id")
    private String id;          // maps to categoryId or coverageId dynamically

    @JsonProperty("Name")
    private String name;

    // Optional field for coverages
    @JsonProperty("IsActive")
    private Boolean isActive;

    // Optional dynamic type field: "CATEGORY" or "COVERAGE"
    private String type;
}
