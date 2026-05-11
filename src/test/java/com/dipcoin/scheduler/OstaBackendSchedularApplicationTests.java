package com.dipcoin.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "com.dipcoin.scheduler.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:osta_scheduler_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class OstaBackendSchedularApplicationTests {

  @Test
  void contextLoads() {
  }
}
