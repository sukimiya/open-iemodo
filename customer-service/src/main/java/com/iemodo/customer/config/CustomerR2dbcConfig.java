package com.iemodo.customer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * R2DBC repository scanning for the customer module.
 * Used in microservices deployment; in the monolith ({@code app-boot}),
 * scanning is handled by {@code LiteR2dbcConfig}.
 */
@Configuration
@ConditionalOnProperty(prefix = "iemodo.deployment", name = "mode", havingValue = "microservices")
@EnableR2dbcRepositories(basePackages = "com.iemodo.customer.repository")
public class CustomerR2dbcConfig {
}
