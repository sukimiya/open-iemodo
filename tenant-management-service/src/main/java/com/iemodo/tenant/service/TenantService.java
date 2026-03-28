package com.iemodo.tenant.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.tenant.domain.Tenant;
import com.iemodo.tenant.domain.TenantConfig;
import com.iemodo.tenant.domain.TenantSchema;
import com.iemodo.tenant.dto.CreateTenantRequest;
import com.iemodo.tenant.dto.TenantDTO;
import com.iemodo.tenant.repository.TenantConfigRepository;
import com.iemodo.tenant.repository.TenantRepository;
import com.iemodo.tenant.repository.TenantSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing tenant lifecycle, schemas, and configurations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantSchemaRepository schemaRepository;
    private final TenantConfigRepository configRepository;
    private final TenantProvisioningService provisioningService;

    private static final List<String> SERVICE_NAMES = List.of(
            "user-auth", "product", "order", "inventory", 
            "payment", "pricing", "tax", "marketing", "fulfillment"
    );

    /**
     * Get all tenants.
     */
    public Flux<TenantDTO> getAllTenants() {
        return tenantRepository.findAll()
                .map(this::toDTO);
    }

    /**
     * Get tenant by ID.
     */
    public Mono<TenantDTO> getTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TENANT_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(this::enrichWithConfigs);
    }

    /**
     * Create a new tenant with schemas and default configs.
     */
    @Transactional
    public Mono<TenantDTO> createTenant(CreateTenantRequest request) {
        return tenantRepository.existsByTenantId(request.getTenantId())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, 
                                "Tenant ID already exists: " + request.getTenantId()));
                    }
                    return tenantRepository.existsByTenantCode(request.getTenantCode());
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, 
                                "Tenant code already exists: " + request.getTenantCode()));
                    }

                    Tenant tenant = Tenant.builder()
                            .tenantId(request.getTenantId())
                            .tenantName(request.getTenantName())
                            .tenantCode(request.getTenantCode())
                            .tenantStatus("ACTIVE")
                            .planType(request.getPlanType())
                            .contactEmail(request.getContactEmail())
                            .contactPhone(request.getContactPhone())
                            .build();

                    return tenantRepository.save(tenant);
                })
                .flatMap(savedTenant -> 
                        // Create schema mappings
                        createSchemaMappings(savedTenant.getTenantId())
                                .then(createDefaultConfigs(savedTenant.getTenantId(), request.getInitialConfigs()))
                                // Provision actual database schemas via Flyway
                                .then(provisioningService.provisionSchemas(savedTenant.getTenantId()))
                                .thenReturn(savedTenant)
                )
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Created tenant id={} name={}", dto.getTenantId(), dto.getTenantName()));
    }

    /**
     * Suspend a tenant.
     */
    @Transactional
    public Mono<TenantDTO> suspendTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TENANT_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(tenant -> {
                    tenant.suspend();
                    return tenantRepository.save(tenant);
                })
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Suspended tenant id={}", tenantId));
    }

    /**
     * Activate a suspended tenant.
     */
    @Transactional
    public Mono<TenantDTO> activateTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TENANT_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(tenant -> {
                    tenant.activate();
                    return tenantRepository.save(tenant);
                })
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Activated tenant id={}", tenantId));
    }

    /**
     * Soft delete a tenant.
     */
    @Transactional
    public Mono<Void> deleteTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TENANT_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(tenant -> {
                    tenant.softDelete();
                    return tenantRepository.save(tenant);
                })
                .doOnSuccess(v -> log.info("Deleted tenant id={}", tenantId))
                .then();
    }

    // ─── Configuration Management ──────────────────────────────────────────

    /**
     * Get all configurations for a tenant.
     */
    public Mono<Map<String, String>> getTenantConfigs(String tenantId) {
        return configRepository.findAllByTenantId(tenantId)
                .collectMap(TenantConfig::getConfigKey, TenantConfig::getConfigValue);
    }

    /**
     * Update a tenant configuration.
     */
    @Transactional
    public Mono<Void> updateTenantConfig(String tenantId, String key, String value) {
        return configRepository.findByTenantIdAndConfigKey(tenantId, key)
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new config if not exists
                    TenantConfig config = TenantConfig.builder()
                            .tenantId(tenantId)
                            .configKey(key)
                            .configType("STRING")
                            .editable(true)
                            .build();
                    return Mono.just(config);
                }))
                .flatMap(config -> {
                    config.setConfigValue(value);
                    return configRepository.save(config);
                })
                .doOnSuccess(c -> log.info("Updated config for tenant {}: {}={}", tenantId, key, value))
                .then();
    }

    // ─── Helper methods ────────────────────────────────────────────────────

    private Mono<Void> createSchemaMappings(String tenantId) {
        return Flux.fromIterable(SERVICE_NAMES)
                .flatMap(serviceName -> {
                    String schemaName = tenantId.replace("-", "_") + "_" + serviceName.replace("-", "_");
                    TenantSchema schema = TenantSchema.of(tenantId, serviceName, schemaName);
                    return schemaRepository.save(schema);
                })
                .then();
    }

    private Mono<Void> createDefaultConfigs(String tenantId, Map<String, String> initialConfigs) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TenantConfig.DEFAULT_CURRENCY, "USD");
        configs.put(TenantConfig.DEFAULT_LANGUAGE, "en");
        configs.put(TenantConfig.TIMEZONE, "UTC");
        configs.put(TenantConfig.DATE_FORMAT, "yyyy-MM-dd");

        if (initialConfigs != null) {
            configs.putAll(initialConfigs);
        }

        return Flux.fromIterable(configs.entrySet())
                .flatMap(entry -> {
                    TenantConfig config = TenantConfig.builder()
                            .tenantId(tenantId)
                            .configKey(entry.getKey())
                            .configValue(entry.getValue())
                            .configType("STRING")
                            .editable(true)
                            .build();
                    return configRepository.save(config);
                })
                .then();
    }

    private Mono<TenantDTO> enrichWithConfigs(Tenant tenant) {
        return configRepository.findAllByTenantId(tenant.getTenantId())
                .collectMap(TenantConfig::getConfigKey, TenantConfig::getConfigValue)
                .map(configs -> {
                    TenantDTO dto = toDTO(tenant);
                    dto.setConfigs(configs);
                    return dto;
                });
    }

    private TenantDTO toDTO(Tenant tenant) {
        return TenantDTO.builder()
                .id(tenant.getId())
                .tenantId(tenant.getTenantId())
                .tenantName(tenant.getTenantName())
                .tenantCode(tenant.getTenantCode())
                .status(tenant.getTenantStatus())
                .planType(tenant.getPlanType())
                .contactEmail(tenant.getContactEmail())
                .contactPhone(tenant.getContactPhone())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
