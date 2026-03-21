package com.iemodo.common.tenant;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.util.HashMap;
import java.util.Map;

/**
 * R2DBC configuration that wires up a {@link PostgresTenantConnectionFactory}
 * backed by per-tenant {@link PostgresqlConnectionFactory} instances.
 *
 * <p>Tenant list is loaded from Nacos / application YAML via
 * {@link TenantProperties}.  A Nacos config change (add/remove tenant) will
 * trigger a Spring Cloud config refresh which rebuilds the factory map.
 *
 * <p>Note: this class is NOT annotated with {@code @Configuration} itself —
 * individual services should extend or import it and add
 * {@link EnableR2dbcRepositories} pointing to their own repository packages.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class MultitenantR2dbcConfiguration extends AbstractR2dbcConfiguration {

    private final TenantProperties tenantProperties;

    public MultitenantR2dbcConfiguration(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        Map<Object, ConnectionFactory> factories = buildTenantFactories();

        PostgresTenantConnectionFactory routingFactory = new PostgresTenantConnectionFactory();
        routingFactory.setTargetConnectionFactories(factories);
        routingFactory.afterPropertiesSet();

        log.info("Multi-tenant ConnectionFactory initialised with {} tenants: {}",
                factories.size(), factories.keySet());
        return routingFactory;
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private Map<Object, ConnectionFactory> buildTenantFactories() {
        Map<Object, ConnectionFactory> map = new HashMap<>();

        for (TenantProperties.TenantDataSourceConfig cfg : tenantProperties.getTenants()) {
            ConnectionFactory factory = createFactory(cfg);
            map.put(cfg.getId(), factory);
            log.debug("Registered ConnectionFactory for tenant '{}' → schema '{}'",
                    cfg.getId(), cfg.getSchema());
        }

        if (map.isEmpty()) {
            log.warn("No tenant datasources configured! Check iemodo.tenants in Nacos / application.yml");
        }

        return map;
    }

    private ConnectionFactory createFactory(TenantProperties.TenantDataSourceConfig cfg) {
        PostgresqlConnectionConfiguration.Builder builder = PostgresqlConnectionConfiguration.builder()
                .host(cfg.getHost())
                .port(cfg.getPort())
                .database(cfg.getDatabase())
                .username(cfg.getUsername())
                .password(cfg.getPassword());

        // For SCHEMA isolation, set the default schema via options
        if (cfg.getType() == TenantProperties.IsolationType.SCHEMA
                && cfg.getSchema() != null) {
            builder.schema(cfg.getSchema());
        }

        return new PostgresqlConnectionFactory(builder.build());
    }
}
