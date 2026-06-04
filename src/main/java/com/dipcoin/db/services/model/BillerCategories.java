package com.dipcoin.db.services.model;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "BillerCategories")
public class BillerCategories implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "category_id", length = 20)
    private String categoryId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "seq_id")
    private Integer seqId;

    @Column(name = "is_pre_fetch")
    private Boolean isPreFetch;

    @Column(name = "payment_option_modes", length = 255)
    private String paymentOptionModes;

    public BillerCategories() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getSeqId() {
        return seqId;
    }

    public void setSeqId(Integer seqId) {
        this.seqId = seqId;
    }

    public Boolean getIsPreFetch() {
        return isPreFetch;
    }

    public void setIsPreFetch(Boolean isPreFetch) {
        this.isPreFetch = isPreFetch;
    }

    public String getPaymentOptionModes() {
        return paymentOptionModes;
    }

    public void setPaymentOptionModes(String paymentOptionModes) {
        this.paymentOptionModes = paymentOptionModes;
    }
}
