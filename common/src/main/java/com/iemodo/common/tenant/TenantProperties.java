package com.iemodo.common.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Tenant data-source properties loaded from Nacos / application config.
 *
 * <p>Example YAML (Nacos Data ID: {@code iemodo-tenants.yaml}):
 * <pre>
 * tenants:
 *   - id: tenant_001
 *     type: SCHEMA
 *     schema: schema_tenant_001
 *     host: localhost
 *     port: 5432
 *     database: iemodo
 *     username: iemodo
 *     password: iemodo123
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "iemodo")
public class TenantProperties {

    private List<TenantDataSourceConfig> tenants = new ArrayList<>();

    @Data
    public static class TenantDataSourceConfig {

        /** Tenant identifier (matches X-TenantID header). */
        private String id;

        /** Isolation type: SCHEMA or DB. */
        private IsolationType type = IsolationType.SCHEMA;

        /** PostgreSQL schema name (used when type=SCHEMA). */
        private String schema;

        private String host = "localhost";
        private int port = 5432;
        private String database = "iemodo";
        private String username;
        private String password;
    }

    public enum IsolationType {
        SCHEMA, DB
    }
}
