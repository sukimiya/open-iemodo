package com.iemodo.config;

import com.iemodo.common.billing.BillingServiceClient;
import com.iemodo.common.billing.ServiceUrlsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Single BillingServiceClient bean for the monolith. All services share one instance.
 */
@Configuration
@EnableConfigurationProperties(ServiceUrlsProperties.class)
public class BillingConfig {

    @Bean
    public BillingServiceClient billingServiceClient(WebClient.Builder builder,
                                                     ServiceUrlsProperties urls) {
        return new BillingServiceClient(builder, urls.getTenantManagement());
    }
}
