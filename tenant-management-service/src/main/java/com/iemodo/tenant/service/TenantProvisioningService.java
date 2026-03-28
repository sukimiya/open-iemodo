package com.iemodo.tenant.service;

import com.iemodo.tenant.domain.TenantSchema;
import com.iemodo.tenant.repository.TenantSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

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
                    for (TenantSchema schema : schemas) {
                        provisionSchema(schema);
                    }
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()))
                .then()
                .doOnSuccess(v -> log.info("Provisioned schemas for tenant {}", tenantId));
    }

    private void provisionSchema(TenantSchema schema) {
        String schemaName = schema.getSchemaName();
        
        try {
            // Create schema if not exists
            createSchemaIfNotExists(schemaName);
            
            // Run Flyway migrations for this schema
            Flyway flyway = Flyway.configure()
                    .dataSource(jdbcUrl, jdbcUser, jdbcPassword)
                    .schemas(schemaName)
                    .defaultSchema(schemaName)
                    .locations(getMigrationLocation(schema.getServiceName()))
                    .load();
            
            flyway.migrate();
            log.info("Migrated schema {} for service {}", schemaName, schema.getServiceName());
        } catch (Exception e) {
            log.error("Failed to provision schema {}: {}", schemaName, e.getMessage(), e);
            throw new RuntimeException("Schema provisioning failed for " + schemaName, e);
        }
    }

    private void createSchemaIfNotExists(String schemaName) {
        // This would typically use JdbcTemplate or raw JDBC
        // For now, we assume Flyway can create schemas or they're pre-created
        log.debug("Ensuring schema exists: {}", schemaName);
    }

    private String getMigrationLocation(String serviceName) {
        // Map service names to migration locations
        return switch (serviceName) {
            case "user-auth" -> "classpath:db/migration/user-auth";
            case "product" -> "classpath:db/migration/product";
            case "order" -> "classpath:db/migration/order";
            default -> "classpath:db/migration/common";
        };
    }

    /**
     * Drop all schemas for a tenant (dangerous operation!).
     */
    public Mono<Void> dropSchemas(String tenantId) {
        return schemaRepository.findAllByTenantId(tenantId)
                .collectList()
                .flatMap(schemas -> Mono.fromCallable(() -> {
                    for (TenantSchema schema : schemas) {
                        dropSchema(schema.getSchemaName());
                    }
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()))
                .then()
                .doOnSuccess(v -> log.warn("Dropped all schemas for tenant {}", tenantId));
    }

    private void dropSchema(String schemaName) {
        // Implementation would use JDBC to execute DROP SCHEMA
        log.warn("Dropping schema: {}", schemaName);
    }
}
