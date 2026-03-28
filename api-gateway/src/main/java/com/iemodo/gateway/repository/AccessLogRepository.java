package com.iemodo.gateway.repository;

import com.iemodo.gateway.domain.AccessLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository for {@link AccessLog} entity.
 */
@Repository
public interface AccessLogRepository extends ReactiveCrudRepository<AccessLog, Long> {

    /**
     * Find logs by tenant ID within time range.
     */
    @Query("SELECT * FROM gateway_access_logs WHERE tenant_id = :tenantId AND created_at BETWEEN :start AND :end ORDER BY created_at DESC")
    Flux<AccessLog> findByTenantIdAndTimeRange(String tenantId, Instant start, Instant end);

    /**
     * Find logs by trace ID.
     */
    Flux<AccessLog> findByTraceId(String traceId);

    /**
     * Find logs by request ID.
     */
    Mono<AccessLog> findByRequestId(String requestId);

    /**
     * Count requests by status code within time range.
     */
    @Query("SELECT COUNT(*) FROM gateway_access_logs WHERE status_code = :statusCode AND created_at BETWEEN :start AND :end")
    Mono<Long> countByStatusCodeAndTimeRange(Integer statusCode, Instant start, Instant end);

    /**
     * Get average response time within time range.
     */
    @Query("SELECT AVG(response_time) FROM gateway_access_logs WHERE created_at BETWEEN :start AND :end")
    Mono<Double> averageResponseTime(Instant start, Instant end);

    /**
     * Find recent logs with limit.
     */
    @Query("SELECT * FROM gateway_access_logs ORDER BY created_at DESC LIMIT :limit")
    Flux<AccessLog> findRecent(int limit);

    /**
     * Count total requests within time range.
     */
    @Query("SELECT COUNT(*) FROM gateway_access_logs WHERE created_at BETWEEN :start AND :end")
    Mono<Long> countByTimeRange(Instant start, Instant end);
}
