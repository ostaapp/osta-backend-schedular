package com.dipcoin.scheduler;

import com.dipcoin.api.resource.CustomerBillPaymentsInfoResource;
import com.dipcoin.api.resource.MerchantSettlementResource;
import com.dipcoin.partner.utils.ChecksumUtil;
import com.dipcoin.scheduler.config.SchedulerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.dipcoin.scheduler", "com.dipcoin.api.service", "com.dipcoin.db.services",
    "com.dipcoin.db.service.client", "com.dipcoin.metrics", "com.dipcoin.partner.db.services",
    "com.dipcoin.partner.paymentGateway"},
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
        pattern = "com\\.dipcoin\\.db\\.services\\.commons\\.DBConfig"))
@EntityScan({"com.dipcoin.db.services.model", "com.dipcoin.partner.db.services.model"})
@EnableJpaRepositories(basePackages = {"com.dipcoin.scheduler.repository",
    "com.dipcoin.db.services.dao", "com.dipcoin.db.services"})
@Import({MerchantSettlementResource.class, CustomerBillPaymentsInfoResource.class, ChecksumUtil.class})
@EnableConfigurationProperties(SchedulerProperties.class)
public class OstaBackendSchedularApplication {

  public static void main(String[] args) {
    SpringApplication.run(OstaBackendSchedularApplication.class, args);
  }
}
