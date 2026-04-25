package com.iemodo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Unified R2DBC repository scanning for all service modules.
 * In the microservices version, each service enables its own repository scanning.
 * In the monolith, we scan all service repository packages from a single config.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = {
    "com.iemodo.user.repository",
    "com.iemodo.order.repository",
    "com.iemodo.product.repository",
    "com.iemodo.inventory.repository",
    "com.iemodo.payment.repository",
    "com.iemodo.pricing.repository",
    "com.iemodo.tax.repository",
    "com.iemodo.marketing.repository",
    "com.iemodo.fulfillment.repository",
    "com.iemodo.tenant.repository",
    "com.iemodo.rma.repository",
    "com.iemodo.review.repository",
    "com.iemodo.notification.repository"
})
public class LiteR2dbcConfig {
}
