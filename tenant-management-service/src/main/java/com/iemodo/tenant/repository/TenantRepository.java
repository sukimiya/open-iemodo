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

    Flux<Tenant> findAllByStatus(String status);

    @Query("SELECT * FROM tenants WHERE status = 'ACTIVE' AND deleted_at IS NULL")
    Flux<Tenant> findAllActive();

    @Query("SELECT * FROM tenants WHERE status != 'DELETED' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Tenant> findAllPaged(int limit, int offset);
}
