package com.iemodo.common.tenant;

import com.iemodo.common.db.CircuitBreakerConnectionFactory;
import com.iemodo.common.db.SlowQueryCircuitBreaker;
import com.iemodo.common.db.SlowQueryProxyListener;
import com.iemodo.common.db.SlowQueryProperties;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-tenant R2DBC configuration.
 *
 * <p>Registers a {@link PostgresTenantConnectionFactory} that routes each
 * reactive database operation to the correct per-tenant PostgreSQL schema
 * based on the {@code X-TenantID} value stored in the Reactor Context.
 *
 * <p>Uses {@link PostgresDialect} explicitly so that {@link DatabaseClient}
 * and the entity template can be built without metadata introspection
 * (which would fail on the routing {@link AbstractRoutingConnectionFactory}).
 *
 * <p>Services import this configuration via {@code @Import} and declare
 * {@code @EnableR2dbcRepositories} pointing to their own repository packages.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({TenantProperties.class, SlowQueryProperties.class})
public class MultitenantR2dbcConfiguration {

    private final TenantProperties tenantProperties;
    private final SlowQueryProperties slowQueryProperties;

    public MultitenantR2dbcConfiguration(TenantProperties tenantProperties,
                                          SlowQueryProperties slowQueryProperties) {
        this.tenantProperties = tenantProperties;
        this.slowQueryProperties = slowQueryProperties;
    }

    @Bean
    public SlowQueryCircuitBreaker slowQueryCircuitBreaker() {
        return new SlowQueryCircuitBreaker(slowQueryProperties);
    }

    @Bean
    public SlowQueryProxyListener slowQueryProxyListener() {
        return new SlowQueryProxyListener(slowQueryProperties, slowQueryCircuitBreaker());
    }

    // ─── Connection factory ───────────────────────────────────────────────

    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        Map<Object, ConnectionFactory> factories = buildTenantFactories();

        PostgresTenantConnectionFactory routingFactory = new PostgresTenantConnectionFactory();
        routingFactory.setTargetConnectionFactories(factories);
        routingFactory.afterPropertiesSet();

        log.info("Multi-tenant ConnectionFactory initialised with {} tenant(s): {}",
                factories.size(), factories.keySet());
        return routingFactory;
    }

    // ─── R2DBC infrastructure ─────────────────────────────────────────────

    /**
     * Build a {@link DatabaseClient} using the routing factory.
     * We use {@link DatabaseClient#builder()} which does NOT call
     * {@code getMetadata()} eagerly — metadata is resolved lazily on first use.
     */
    @Bean
    @Primary
    public DatabaseClient databaseClient() {
        // Use the routing connection factory.
        // BindMarkersFactory for PostgreSQL ($1, $2, ...) is set explicitly
        // so that DatabaseClient does not need to call connectionFactory.getMetadata().
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory())
                .bindMarkers(PostgresDialect.INSTANCE.getBindMarkersFactory())
                .build();
    }

    @Bean
    @Primary
    public R2dbcMappingContext r2dbcMappingContext() throws Exception {
        R2dbcMappingContext context = new R2dbcMappingContext(NamingStrategy.INSTANCE);
        context.setManagedTypes(RelationalManagedTypes.empty());
        return context;
    }

    @Bean
    @Primary
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, List.of());
    }

    @Bean
    @Primary
    public MappingR2dbcConverter r2dbcConverter() throws Exception {
        return new MappingR2dbcConverter(r2dbcMappingContext(), r2dbcCustomConversions());
    }

    @Bean
    @Primary
    public DefaultReactiveDataAccessStrategy reactiveDataAccessStrategy() throws Exception {
        return new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE, r2dbcConverter());
    }

    @Bean("r2dbcEntityTemplate")
    @Primary
    public R2dbcEntityOperations r2dbcEntityTemplate() throws Exception {
        return new R2dbcEntityTemplate(databaseClient(), reactiveDataAccessStrategy());
    }

    // ─── Tenant factory building ──────────────────────────────────────────

    private Map<Object, ConnectionFactory> buildTenantFactories() {
        Map<Object, ConnectionFactory> map = new HashMap<>();

        for (TenantProperties.TenantDataSourceConfig cfg : tenantProperties.getTenants()) {
            map.put(cfg.getId(), createFactory(cfg));
            log.debug("Registered ConnectionFactory for tenant '{}' schema '{}'",
                    cfg.getId(), cfg.getSchema());
        }

        if (map.isEmpty()) {
            log.warn("No tenants configured — check iemodo.tenants in application.yml / Nacos");
        }
        return map;
    }

    private ConnectionFactory createFactory(TenantProperties.TenantDataSourceConfig cfg) {
        PostgresqlConnectionConfiguration.Builder builder =
                PostgresqlConnectionConfiguration.builder()
                        .host(cfg.getHost())
                        .port(cfg.getPort())
                        .database(cfg.getDatabase())
                        .username(cfg.getUsername())
                        .password(cfg.getPassword());

        if (cfg.getType() == TenantProperties.IsolationType.SCHEMA
                && cfg.getSchema() != null) {
            builder.schema(cfg.getSchema());
        }

        // Layer 1: real PostgreSQL connection factory
        ConnectionFactory pgFactory = new PostgresqlConnectionFactory(builder.build());

        // Layer 2: r2dbc-proxy for slow-query timing and logging
        ConnectionFactory proxyFactory = ProxyConnectionFactory.builder(pgFactory)
                .listener(slowQueryProxyListener())
                .build();

        // Layer 3: circuit breaker gate — rejects connections when circuit is OPEN
        return new CircuitBreakerConnectionFactory(proxyFactory, slowQueryCircuitBreaker());
    }
}
