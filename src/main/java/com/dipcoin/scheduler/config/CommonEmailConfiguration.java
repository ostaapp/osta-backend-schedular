package com.dipcoin.scheduler.config;

import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.commons.EmailClient;
import com.dipcoin.commons.PostfixEmailClient;
import com.dipcoin.commons.SesEmailClient;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.commons.config.CommonsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CommonEmailConfiguration {

  @Bean
  public CommonsProperties commonsProperties() {
    return new CommonsProperties();
  }

  @Bean
  public ApplicationProperties applicationProperties() {
    return new ApplicationProperties();
  }

  @Bean("smsClient")
  public SmsClient smsClient() {
    return new SmsClient();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean("userUtil")
  public UserUtil userUtil() {
    return new UserUtil();
  }

  @Bean("emailClient")
  @Profile({"dev", "stage", "test", "unit"})
  public EmailClient postfixEmailClient() {
    return new PostfixEmailClient();
  }

  @Bean("emailClient")
  @Profile({"uat", "prod"})
  public EmailClient sesEmailClient() {
    return new SesEmailClient();
  }
}
