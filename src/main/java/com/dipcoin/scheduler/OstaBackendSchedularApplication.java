package com.dipcoin.scheduler;

import com.dipcoin.scheduler.config.SchedulerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.dipcoin")
@EnableConfigurationProperties(SchedulerProperties.class)
public class OstaBackendSchedularApplication {

  public static void main(String[] args) {
    SpringApplication.run(OstaBackendSchedularApplication.class, args);
  }
}
