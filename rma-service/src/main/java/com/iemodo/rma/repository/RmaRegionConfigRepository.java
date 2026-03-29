package com.iemodo.rma.repository;

import com.iemodo.rma.domain.RmaRegionConfig;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RmaRegionConfigRepository extends ReactiveCrudRepository<RmaRegionConfig, Long> {

    /** Tenant-specific override takes precedence over platform default (tenantId = null). */
    Mono<RmaRegionConfig> findByRegionCodeAndTenantId(String regionCode, String tenantId);

    /** Platform-wide default for a region. */
    Mono<RmaRegionConfig> findByRegionCodeAndTenantIdIsNull(String regionCode);
}
