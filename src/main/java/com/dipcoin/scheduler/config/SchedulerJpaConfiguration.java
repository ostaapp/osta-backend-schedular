package com.dipcoin.scheduler.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerJpaConfiguration {

  private static final String BOOT_ENTITY_MANAGER_FACTORY = "entityManagerFactory";
  private static final String SCHEDULER_PERSISTENCE_UNIT = "schedulerJpaPersistenceUnit";
  private static final String COMMON_PERSISTENCE_UNIT = "springJpaPersistenceUnit";
  private static final String LEGACY_DB_ENTITY_MANAGER_FACTORY =
      "com.dipcoin.db.services.beans:EntityManagerFactory";
  private static final String LEGACY_BBPS_PERSISTENCE_UNIT = "bbpsJpaPersistenceUnit";

  @Bean
  public static BeanFactoryPostProcessor legacyEntityManagerFactoryAliases() {
    return new BeanFactoryPostProcessor() {
      @Override
      public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
          throws BeansException {
        if (beanFactory.containsBeanDefinition(BOOT_ENTITY_MANAGER_FACTORY)) {
          registerAliasIfMissing(beanFactory, SCHEDULER_PERSISTENCE_UNIT);
          registerAliasIfMissing(beanFactory, COMMON_PERSISTENCE_UNIT);
          registerAliasIfMissing(beanFactory, LEGACY_DB_ENTITY_MANAGER_FACTORY);
          registerAliasIfMissing(beanFactory, LEGACY_BBPS_PERSISTENCE_UNIT);
        }
      }
    };
  }

  private static void registerAliasIfMissing(ConfigurableListableBeanFactory beanFactory,
      String alias) {
    if (!beanFactory.containsBean(alias)) {
      beanFactory.registerAlias(BOOT_ENTITY_MANAGER_FACTORY, alias);
    }
  }
}
