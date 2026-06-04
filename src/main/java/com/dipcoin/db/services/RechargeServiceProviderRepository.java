package com.dipcoin.db.services;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dipcoin.db.services.model.RechargeServiceProvider;

public interface RechargeServiceProviderRepository extends JpaRepository<RechargeServiceProvider, Long> {

    // Match using billerName → service_provider
    List<RechargeServiceProvider> findByServiceProvider(String serviceProvider);

    // Match using unique provider code
    List<RechargeServiceProvider> findByServiceProviderCode(String serviceProviderCode);

    // Optional: circle filtering
    List<RechargeServiceProvider> findByCircleCode(String circleCode);

    // Optional: exact provider + circle
    List<RechargeServiceProvider> findByServiceProviderAndCircleCode(String serviceProvider,
                                                                      String circleCode);
}
