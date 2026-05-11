package com.dipcoin.scheduler.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "BillPaymentsInfo")
@NamedQuery(name = "BillPaymentsInfo.findAll", query = "SELECT b FROM BillPaymentsInfo b")
public class BillPaymentsInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String billerId;

  private String billerName;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getBillerId() {
    return billerId;
  }

  public void setBillerId(String billerId) {
    this.billerId = billerId;
  }

  public String getBillerName() {
    return billerName;
  }

  public void setBillerName(String billerName) {
    this.billerName = billerName;
  }
}
