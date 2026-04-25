package com.iemodo.tenant.service;

import com.iemodo.tenant.domain.TenantSchema;
import com.iemodo.tenant.repository.TenantSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;

/**
 * Service for provisioning tenant database schemas using Flyway.
 *
 * <p>Creates the actual PostgreSQL schemas and runs migrations for each
 * service that the tenant needs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantSchemaRepository schemaRepository;
    private final DataSource dataSource;

    @Value("${spring.flyway.url}")
    private String jdbcUrl;

    @Value("${spring.flyway.user}")
    private String jdbcUser;

    @Value("${spring.flyway.password}")
    private String jdbcPassword;

    /**
     * Provision database schemas for a new tenant.
     *
     * <p>This is a blocking operation that uses JDBC (not R2DBC) to run Flyway migrations.
     */
    public Mono<Void> provisionSchemas(String tenantId) {
        return schemaRepository.findAllByTenantId(tenantId)
                .collectList()
                .flatMap(schemas -> Mono.fromCallable(() -> {
                    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                    for (TenantSchema schema : schemas) {
                        provisionSchema(jdbc, schema);
                    }
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()))
                .then()
                .doOnSuccess(v -> log.info("Provisioned schemas for tenant {}", tenantId));
    }

    private void provisionSchema(JdbcTemplate jdbc, TenantSchema schema) {
        String schemaName = schema.getSchemaName();

        try {
            // Create schema if not exists
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

            // Run Flyway migrations for this schema
            Flyway flyway = Flyway.configure()
                    .dataSource(jdbcUrl, jdbcUser, jdbcPassword)
                    .schemas(schemaName)
                    .defaultSchema(schemaName)
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();
            log.info("Migrated schema {} for service {}", schemaName, schema.getServiceName());
        } catch (Exception e) {
            log.error("Failed to provision schema {}: {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Schema provisioning failed for " + schemaName, e);
        }
    }

    /**
     * Drop all schemas for a tenant (dangerous operation!).
     */
    public Mono<Void> dropSchemas(String tenantId) {
        return schemaRepository.findAllByTenantId(tenantId)
                .collectList()
                .flatMap(schemas -> Mono.fromCallable(() -> {
                    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                    for (TenantSchema schema : schemas) {
                        jdbc.execute("DROP SCHEMA IF EXISTS " + schema.getSchemaName() + " CASCADE");
                        log.warn("Dropped schema: {}", schema.getSchemaName());
                    }
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()))
                .then()
                .doOnSuccess(v -> log.warn("Dropped all schemas for tenant {}", tenantId));
    }
}
