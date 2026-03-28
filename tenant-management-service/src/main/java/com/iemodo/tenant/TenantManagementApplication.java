package com.iemodo.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tenant Management Service - manages tenant lifecycle, schema provisioning,
 * and tenant-specific configurations.
 * 
 * <p>Port: 8091
 * <p>Schema: tenant_meta (platform-level, shared across all tenants)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.iemodo.tenant", "com.iemodo.common"})
public class TenantManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantManagementApplication.class, args);
    }
}
