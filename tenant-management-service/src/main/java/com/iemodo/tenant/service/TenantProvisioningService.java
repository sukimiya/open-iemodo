package com.iemodo.tenant.service;

import com.iemodo.tenant.domain.TenantSchema;
import com.iemodo.tenant.repository.TenantSchemaRepository;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

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

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    /**
     * Provision database schemas for a new tenant.
     *
     * <p>This is a blocking operation that uses Flyway to run migrations.
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
            // Create schema if not exists using R2DBC
            createSchemaIfNotExists(schemaName);

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

    private void createSchemaIfNotExists(String schemaName) {
        // Parse the R2DBC URL to extract connection parameters
        String r2dbc = r2dbcUrl;
        String host = "localhost";
        int port = 5432;
        String database = "iemodo";

        // Parse r2dbc:postgresql://host:port/database
        if (r2dbc != null && r2dbc.startsWith("r2dbc:")) {
            String withoutPrefix = r2dbc.substring(6);
            if (withoutPrefix.startsWith("postgresql://")) {
                withoutPrefix = withoutPrefix.substring(14);
                int slashIdx = withoutPrefix.indexOf('/');
                if (slashIdx > 0) {
                    String hostPort = withoutPrefix.substring(0, slashIdx);
                    database = withoutPrefix.substring(slashIdx + 1);
                    int colonIdx = hostPort.indexOf(':');
                    if (colonIdx > 0) {
                        host = hostPort.substring(0, colonIdx);
                        port = Integer.parseInt(hostPort.substring(colonIdx + 1));
                    } else {
                        host = hostPort;
                    }
                }
            }
        }

        ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, host)
                .option(PORT, port)
                .option(DATABASE, database)
                .option(USER, jdbcUser)
                .option(PASSWORD, jdbcPassword)
                .build());

        // Use connection to create schema
        Mono.from(connectionFactory.create())
                .flatMapMany(connection -> connection
                        .createStatement("CREATE SCHEMA IF NOT EXISTS " + schemaName)
                        .execute())
                .blockLast();
    }

    /**
     * Drop all schemas for a tenant (dangerous operation!).
     */
    public Mono<Void> dropSchemas(String tenantId) {
        return schemaRepository.findAllByTenantId(tenantId)
                .collectList()
                .flatMap(schemas -> Mono.fromCallable(() -> {
                    String r2dbc = r2dbcUrl;
                    String host = "localhost";
                    int port = 5432;
                    String database = "iemodo";

                    if (r2dbc != null && r2dbc.startsWith("r2dbc:")) {
                        String withoutPrefix = r2dbc.substring(6);
                        if (withoutPrefix.startsWith("postgresql://")) {
                            withoutPrefix = withoutPrefix.substring(14);
                            int slashIdx = withoutPrefix.indexOf('/');
                            if (slashIdx > 0) {
                                String hostPort = withoutPrefix.substring(0, slashIdx);
                                database = withoutPrefix.substring(slashIdx + 1);
                                int colonIdx = hostPort.indexOf(':');
                                if (colonIdx > 0) {
                                    host = hostPort.substring(0, colonIdx);
                                    port = Integer.parseInt(hostPort.substring(colonIdx + 1));
                                } else {
                                    host = hostPort;
                                }
                            }
                        }
                    }

                    ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                            .option(DRIVER, "postgresql")
                            .option(HOST, host)
                            .option(PORT, port)
                            .option(DATABASE, database)
                            .option(USER, jdbcUser)
                            .option(PASSWORD, jdbcPassword)
                            .build());

                    for (TenantSchema schema : schemas) {
                        Mono.from(connectionFactory.create())
                                .flatMapMany(connection -> connection
                                        .createStatement("DROP SCHEMA IF EXISTS " + schema.getSchemaName() + " CASCADE")
                                        .execute())
                                .blockLast();
                        log.warn("Dropped schema: {}", schema.getSchemaName());
                    }
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()))
                .then()
                .doOnSuccess(v -> log.warn("Dropped all schemas for tenant {}", tenantId));
    }
}
