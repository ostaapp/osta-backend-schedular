package com.dipcoin.partner.paymentGateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class Code {
  private String code;
  private String message;

}
