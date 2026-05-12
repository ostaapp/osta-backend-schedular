package com.dipcoin.metrics;

import com.dipcoin.api.config.ApplicationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component("com.dipcoin.metrics.MerchantMetricRegistry")
@Accessors(fluent = true)
public class MerchantMetricRegistry {

  private static final Logger LOG = LogManager.getLogger(MerchantMetricRegistry.class);

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private MeterRegistry meterRegistry;

  private static Tag tag(String tag, String value) {
    return Tag.of(tag, value);
  }

  public Counter merchantCancellationFull() {
    return Counter.builder("number_of_transaction_cancelled_by_merchant_full")
        .description("Number of transactions cancelled by Merchant full")
        .register(meterRegistry);
  }

  public Counter merchantCancellationPartial() {
    return Counter.builder("number_of_transaction_cancelled_by_merchant_partial")
        .description("Number of transactions cancelled by Merchant partial")
        .register(meterRegistry);
  }
  
  @PostConstruct
  public void construct() throws Exception {
    LOG.info("Constructing " + this.getClass().getSimpleName() + " ...");
  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");
  }

}
