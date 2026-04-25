package com.iemodo.tenant.repository;

import com.iemodo.tenant.domain.UsageRecord;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface UsageRecordRepository extends ReactiveCrudRepository<UsageRecord, Long> {

    Mono<UsageRecord> findByTenantIdAndUsageDateAndMetric(String tenantId, LocalDate usageDate, String metric);

    @Query("SELECT COALESCE(SUM(count_value), 0) FROM usage_records WHERE tenant_id = :tenantId AND metric = :metric AND usage_date >= :since")
    Mono<Long> sumMetricSince(String tenantId, String metric, LocalDate since);
}
