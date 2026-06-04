package com.dipcoin.api.model;

import lombok.Data;

@Data
public class BillersByCategoryRequest {
    private String categoryId;  // mandatory
    private String coverage;    // optional
}
