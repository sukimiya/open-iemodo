package com.iemodo.tenant.repository;

import com.iemodo.tenant.domain.TenantConfig;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link TenantConfig} entity.
 */
@Repository
public interface TenantConfigRepository extends ReactiveCrudRepository<TenantConfig, Long> {

    Flux<TenantConfig> findAllByTenantId(String tenantId);

    Mono<TenantConfig> findByTenantIdAndConfigKey(String tenantId, String configKey);

    Mono<Boolean> existsByTenantIdAndConfigKey(String tenantId, String configKey);

    @Query("DELETE FROM tenant_configs WHERE tenant_id = :tenantId")
    Mono<Void> deleteAllByTenantId(String tenantId);

    @Query("SELECT * FROM tenant_configs WHERE tenant_id = :tenantId AND config_key LIKE :prefix%")
    Flux<TenantConfig> findAllByTenantIdAndConfigKeyStartingWith(String tenantId, String prefix);
}
