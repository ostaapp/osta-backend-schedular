package com.dipcoin.api.model;

import java.util.List;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.CustomerBillFetchRequest.ChannelDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class FieldInfoRequestMapper extends APIRequest {
  private String name;
  private String value;

  public String getName() {
	return name;
}

public void setName(String name) {
	this.name = name;
}

public String getValue() {
	return value;
}

public void setValue(String value) {
	this.value = value;
}

@Override
  public boolean validate(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub

  }
}