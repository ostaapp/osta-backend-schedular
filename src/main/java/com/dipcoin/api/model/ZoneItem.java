package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ZoneItem {
	
	@JsonProperty("Zone")
    private String zone;

}
