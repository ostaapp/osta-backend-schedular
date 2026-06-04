package com.dipcoin.db.services.model;


import lombok.Data;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "RechargePlanDetails")
@Data
public class RechargePlanDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "BillerId")
    private String billerId;

    @Column(name = "Zone")
    private String zone;

    @Column(name = "PlanId")
    private String planId;

    @Column(name = "CategoryType")
    private String categoryType;

    @Column(name = "Amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "PlanDescription", length = 1000)
    private String planDescription;

    @Column(name = "Type")
    private String type;

    @Column(name = "Talktime")
    private String talktime;

    @Column(name = "Validity")
    private String validity;

    @Column(name = "Data")
    private String data;

    @Column(name = "Status")
    private Integer status;

    @Column(name = "AddedDeleted")
    private String addedDeleted;

    @Column(name = "PartnerReferenceId")
    private String partnerReferenceId;
}