package com.iemodo.tenant.repository;

import com.iemodo.tenant.domain.Tenant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Tenant} entity.
 */
@Repository
public interface TenantRepository extends ReactiveCrudRepository<Tenant, Long> {

    Mono<Tenant> findByTenantId(String tenantId);

    Mono<Tenant> findByTenantCode(String tenantCode);

    Mono<Boolean> existsByTenantId(String tenantId);

    Mono<Boolean> existsByTenantCode(String tenantCode);

    Flux<Tenant> findAllByTenantStatus(String tenantStatus);

    @Query("SELECT * FROM tenants WHERE tenant_status = 'ACTIVE' AND is_valid = 1")
    Flux<Tenant> findAllActive();

    @Query("SELECT * FROM tenants WHERE tenant_status != 'DELETED' ORDER BY create_time DESC LIMIT :limit OFFSET :offset")
    Flux<Tenant> findAllPaged(int limit, int offset);
}
