package com.iemodo.tenant.repository;

import com.iemodo.tenant.domain.UsageAlertLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface UsageAlertLogRepository extends ReactiveCrudRepository<UsageAlertLog, Long> {

    /**
     * Check if an alert was recently sent for a tenant/metric/threshold combination.
     */
    @Query("SELECT COUNT(*) > 0 FROM usage_alert_log "
         + "WHERE tenant_id = :tenantId AND metric = :metric AND threshold_pct = :thresholdPct "
         + "AND alert_sent_at > :since AND is_valid = TRUE")
    Mono<Boolean> hasRecentAlert(String tenantId, String metric, int thresholdPct, Instant since);
}
