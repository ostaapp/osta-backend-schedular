package com.dipcoin.db.services.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "RechargeServiceProvider")
@Data
public class RechargeServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_provider", nullable = false, length = 100)
    private String serviceProvider;

    @Column(name = "service_provider_code", nullable = false, length = 20)
    private String serviceProviderCode;

    @Column(name = "circle_code", nullable = false, length = 20)
    private String circleCode;

    @Column(name = "circle_name", nullable = false, length = 100)
    private String circleName;
}
