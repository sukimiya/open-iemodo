package com.iemodo.common.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs for internal microservices. Each service sets its own via application.yml.
 *
 * <pre>{@code
 * iemodo:
 *   services:
 *     tenant-management: http://localhost:8091
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "iemodo.services")
public class ServiceUrlsProperties {

    /** Base URL for tenant-management-service (billing API). */
    private String tenantManagement = "http://localhost:8091";
}
