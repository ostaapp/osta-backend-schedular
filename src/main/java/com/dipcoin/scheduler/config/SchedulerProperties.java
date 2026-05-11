package com.dipcoin.scheduler.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "com.dipcoin.scheduler")
public class SchedulerProperties {

  private boolean enabled = true;

  private String zoneId = "Asia/Kolkata";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getZoneId() {
    return zoneId;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }

  public ZoneId resolveZoneId() {
    return ZoneId.of(zoneId);
  }
}
