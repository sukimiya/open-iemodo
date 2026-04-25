package com.iemodo.tenant.service;

import com.iemodo.tenant.domain.Plan;
import com.iemodo.tenant.repository.TenantRepository;
import com.iemodo.tenant.repository.TenantSubscriptionRepository;
import com.iemodo.tenant.repository.UsageAlertLogRepository;
import com.iemodo.tenant.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Periodically checks tenant usage vs plan limits and sends alerts at 80%, 90%, 100% thresholds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageAlertScheduler {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final UsageAlertLogRepository alertLogRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${iemodo.services.notification:http://notification-service:8089}")
    private String notificationServiceUrl;

    @Value("${iemodo.billing.alert.cooldown-hours:24}")
    private int alertCooldownHours;

    /** Metrics to monitor with their plan limit keys. */
    private static final Map<String, String> MONITORED_METRICS = new LinkedHashMap<>();
    static {
        MONITORED_METRICS.put("api_calls", "api_calls");
        MONITORED_METRICS.put("orders_placed", "orders_placed");
        MONITORED_METRICS.put("storage_mb", "storage_mb");
    }

    private static final int[] THRESHOLDS = {80, 90, 100};

    /**
     * Check usage for all active tenants every hour.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkUsageAlerts() {
        subscriptionRepository.findAll()
                .filter(sub -> "ACTIVE".equals(sub.getSubscriptionStatus()))
                .flatMap(sub -> {
                    Plan plan = Plan.fromString(sub.getPlanId());
                    return checkTenantAlerts(sub.getTenantId(), plan);
                })
                .doOnError(e -> log.error("Usage alert check failed", e))
                .subscribe();
    }

    private Mono<Void> checkTenantAlerts(String tenantId, Plan plan) {
        return Mono.fromRunnable(() -> {
            for (var entry : MONITORED_METRICS.entrySet()) {
                String metric = entry.getKey();
                long limit = getPlanLimit(plan, metric);
                if (limit == Long.MAX_VALUE || limit <= 0) continue;

                LocalDate since = getMetricSinceDate(metric);
                Long currentUsage = usageRecordRepository.sumMetricSince(tenantId, metric, since).block();
                if (currentUsage == null) currentUsage = 0L;

                for (int threshold : THRESHOLDS) {
                    long triggerAt = limit * threshold / 100;
                    if (currentUsage >= triggerAt) {
                        Instant cooldownSince = Instant.now().minusSeconds(alertCooldownHours * 3600L);
                        Boolean alreadySent = alertLogRepository
                                .hasRecentAlert(tenantId, metric, threshold, cooldownSince).block();
                        if (Boolean.TRUE.equals(alreadySent)) continue;

                        sendAlert(tenantId, metric, threshold, currentUsage, limit);
                        break; // send only the highest reached threshold per metric
                    }
                }
            }
        }).then();
    }

    private void sendAlert(String tenantId, String metric, int threshold, long currentUsage, long limit) {
        // Get tenant contact email
        String email = tenantRepository.findByTenantId(tenantId)
                .map(t -> t.getContactEmail())
                .block();

        if (email == null || email.isBlank()) {
            log.warn("No contact email for tenant {}, skipping usage alert", tenantId);
            return;
        }

        // Call notification service
        try {
            WebClient client = webClientBuilder.baseUrl(notificationServiceUrl).build();
            Map<String, String> variables = new LinkedHashMap<>();
            variables.put("metric", metric);
            variables.put("threshold", threshold + "%");
            variables.put("currentUsage", String.valueOf(currentUsage));
            variables.put("limit", String.valueOf(limit));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", 0L);
            body.put("tenantId", tenantId);
            body.put("channel", "EMAIL");
            body.put("type", "USAGE_ALERT");
            body.put("recipient", email);
            body.put("language", "en");
            body.put("variables", variables);

            client.post()
                    .uri("/notify/send")
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Usage alert sent: tenant={} metric={} threshold={}% usage={}/{}",
                    tenantId, metric, threshold, currentUsage, limit);
        } catch (Exception e) {
            log.error("Failed to send usage alert to tenant {}: {}", tenantId, e.getMessage());
        }
    }

    private long getPlanLimit(Plan plan, String metric) {
        return switch (metric) {
            case "api_calls" -> plan.getMaxApiCallsPerDay();
            case "orders_placed" -> plan.getMaxOrdersPerMonth();
            case "storage_mb" -> plan.getMaxStorageMb();
            default -> Long.MAX_VALUE;
        };
    }

    private static LocalDate getMetricSinceDate(String metric) {
        return switch (metric) {
            case "api_calls" -> LocalDate.now();
            case "orders_placed" -> LocalDate.now().withDayOfMonth(1);
            case "storage_mb" -> LocalDate.MIN;
            default -> LocalDate.now();
        };
    }
}
