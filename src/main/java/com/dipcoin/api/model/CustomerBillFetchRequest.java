package com.dipcoin.api.model;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class CustomerBillFetchRequest extends APIRequest {

	  @ApiModelProperty(required = true)
	  private String agentId;
	  @ApiModelProperty(required = true)
	  private String billerId; 
	  private String initChannel;
	  private String mobileNumber;
	  private String customerParams;
	  private String ip;
	  private String mac;
	  

	  public String getInitChannel() {
		return initChannel;
	  }

	  public void setInitChannel(String initChannel) {
		this.initChannel = initChannel;
	  }

	  public String getMobileNumber() {
		return mobileNumber;
	  }

	  public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	  }

	  public String getCustomerParams() {
		return customerParams;
	  }

	  public void setCustomerParams(String customerParams) {
		this.customerParams = customerParams;
	  }

	  public String getIp() {
		return ip;
	  }

	  public void setIp(String ip) {
		this.ip = ip;
	  }

	  public String getMac() {
		return mac;
	  }

	  public void setMac(String mac) {
		this.mac = mac;
	  }


  @ApiModelProperty(required = true)
  private List<FieldInfoRequestMapper> subscriptionDetails;

  @ApiModelProperty(required = true)
  private ChannelDetails channelDetails;
  
  @ApiModelProperty(required = false)
  private String customerMobileNumber; //phone number of customer
  
  @ApiModelProperty(required = false)
  private String partnerReferenceId;
  
  @ApiModelProperty(required = false)
  private Integer requestSource;
  
  
  @ApiModelProperty(required = false)
  private String bbpsBranchReportId;
  
  @ApiModelProperty(required = false)
  private Boolean isBankAgentEnabled = false;

  @NoArgsConstructor
  @Accessors(chain = true)
  @Setter
  @Getter
  public static class ChannelDetails {
    private String channelCode;
    private List<FieldInfoRequestMapper> channelParams;
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    if (StringUtils.isEmpty(billerId)) {
      this.setErrorCode(HeaderCode.INVALID_MISSING_RECHARGE_BILL_INFORMATION);
      return false;
    }

    return true;
  }
  
  public String getBillerId() {
		return billerId;
	}

	public void setBillerId(String billerId) {
		this.billerId = billerId;
	}

	public List<FieldInfoRequestMapper> getSubscriptionDetails() {
		return subscriptionDetails;
	}

	public void setSubscriptionDetails(List<FieldInfoRequestMapper> subscriptionDetails) {
		this.subscriptionDetails = subscriptionDetails;
	}

	public ChannelDetails getChannelDetails() {
		return channelDetails;
	}

	public void setChannelDetails(ChannelDetails channelDetails) {
		this.channelDetails = channelDetails;
	}

	public String getCustomerMobileNumber() {
		return customerMobileNumber;
	}

	public void setCustomerMobileNumber(String customerMobileNumber) {
		this.customerMobileNumber = customerMobileNumber;
	}

	public String getPartnerReferenceId() {
		return partnerReferenceId;
	}

	public void setPartnerReferenceId(String partnerReferenceId) {
		this.partnerReferenceId = partnerReferenceId;
	}

	public Integer getRequestSource() {
		return requestSource;
	}

	public void setRequestSource(Integer requestSource) {
		this.requestSource = requestSource;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getBbpsBranchReportId() {
		return bbpsBranchReportId;
	}

	public void setBbpsBranchReportId(String bbpsBranchReportId) {
		this.bbpsBranchReportId = bbpsBranchReportId;
	}

	public Boolean getIsBankAgentEnabled() {
		return isBankAgentEnabled;
	}

	public void setIsBankAgentEnabled(Boolean isBankAgentEnabled) {
		this.isBankAgentEnabled = isBankAgentEnabled;
	}

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub

  }
}