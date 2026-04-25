package com.iemodo.order.config;

import com.iemodo.common.tenant.MultitenantR2dbcConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables multi-tenant R2DBC repositories and scheduled tasks for order-service.
 */
@Configuration
@Import(MultitenantR2dbcConfiguration.class)
@EnableScheduling
public class OrderR2dbcConfig {
}
