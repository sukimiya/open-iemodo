package com.iemodo.tenant.repository;

import com.iemodo.tenant.domain.TenantSchema;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link TenantSchema} entity.
 */
@Repository
public interface TenantSchemaRepository extends ReactiveCrudRepository<TenantSchema, Long> {

    Flux<TenantSchema> findAllByTenantId(String tenantId);

    Mono<TenantSchema> findByTenantIdAndServiceName(String tenantId, String serviceName);

    Mono<Boolean> existsByTenantIdAndServiceName(String tenantId, String serviceName);

    Mono<Void> deleteAllByTenantId(String tenantId);
}
