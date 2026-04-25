package com.iemodo.tenant.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.tenant.domain.Plan;
import com.iemodo.tenant.domain.TenantSubscription;
import com.iemodo.tenant.repository.TenantRepository;
import com.iemodo.tenant.repository.TenantSubscriptionRepository;
import com.iemodo.tenant.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Enforces plan-based feature limits using usage records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureGateService {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final UsageRecordRepository usageRecordRepository;

    /**
     * Check whether the tenant can perform an action that consumes {@code metric}.
     *
     * @return Mono that emits successfully if allowed, or errors with PLAN_LIMIT_EXCEEDED if not.
     */
    public Mono<Void> checkLimit(String tenantId, String metric, long requestedAmount) {
        return getEffectivePlan(tenantId)
                .flatMap(plan -> {
                    long limit = getMetricLimit(plan, metric);
                    if (limit == Long.MAX_VALUE) return Mono.empty(); // unlimited

                    LocalDate since = getMetricSinceDate(metric);
                    return usageRecordRepository.sumMetricSince(tenantId, metric, since)
                            .flatMap(currentUsage -> {
                                if (currentUsage + requestedAmount > limit) {
                                    log.warn("Plan limit exceeded for tenant={} metric={} usage={} limit={}",
                                            tenantId, metric, currentUsage, limit);
                                    return Mono.error(new BusinessException(
                                            ErrorCode.PLAN_LIMIT_EXCEEDED, HttpStatus.FORBIDDEN,
                                            "Plan limit exceeded for " + metric + ": "
                                                    + currentUsage + " used, " + limit + " allowed"));
                                }
                                return Mono.empty();
                            });
                });
    }

    /**
     * Record usage for a tenant metric (daily aggregation, upsert).
     */
    public Mono<Void> recordUsage(String tenantId, String metric, long amount) {
        return usageRecordRepository.findByTenantIdAndUsageDateAndMetric(tenantId, LocalDate.now(), metric)
                .defaultIfEmpty(com.iemodo.tenant.domain.UsageRecord.builder()
                        .tenantId(tenantId)
                        .usageDate(LocalDate.now())
                        .metric(metric)
                        .countValue(0L)
                        .build())
                .flatMap(record -> {
                    record.increment(amount);
                    return usageRecordRepository.save(record);
                })
                .then();
    }

    /**
     * Get usage status for a tenant: current usage and limit for a given metric.
     */
    public Mono<Map<String, Object>> getUsageStatus(String tenantId, String metric) {
        return getEffectivePlan(tenantId)
                .flatMap(plan -> {
                    long limit = getMetricLimit(plan, metric);
                    LocalDate since = getMetricSinceDate(metric);
                    return usageRecordRepository.sumMetricSince(tenantId, metric, since)
                            .map(currentUsage -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("metric", metric);
                                result.put("currentUsage", currentUsage);
                                result.put("limit", limit == Long.MAX_VALUE ? "unlimited" : limit);
                                result.put("remaining", limit == Long.MAX_VALUE ? "unlimited" : Math.max(0, limit - currentUsage));
                                result.put("planId", plan.getId());
                                return result;
                            });
                });
    }

    private Mono<Plan> getEffectivePlan(String tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .flatMap(sub -> {
                    if (!"ACTIVE".equals(sub.getSubscriptionStatus())) {
                        return Mono.error(new BusinessException(ErrorCode.SUBSCRIPTION_INACTIVE, HttpStatus.FORBIDDEN));
                    }
                    return Mono.just(Plan.fromString(sub.getPlanId()));
                })
                .switchIfEmpty(Mono.defer(() ->
                        tenantRepository.findByTenantId(tenantId)
                                .map(tenant -> Plan.fromString(tenant.getPlanType()))));
    }

    private long getMetricLimit(Plan plan, String metric) {
        return switch (metric) {
            case "api_calls" -> plan.getMaxApiCallsPerDay();
            case "products_created" -> plan.getMaxProducts();
            case "skus" -> plan.getMaxSkus();
            case "orders_placed" -> plan.getMaxOrdersPerMonth();
            case "storage_mb" -> plan.getMaxStorageMb();
            case "admin_users" -> plan.getMaxAdminUsers();
            case "staff_accounts" -> plan.getMaxStaffAccounts();
            default -> Long.MAX_VALUE;
        };
    }

    private LocalDate getMetricSinceDate(String metric) {
        return switch (metric) {
            case "api_calls" -> LocalDate.now(); // daily
            case "orders_placed" -> LocalDate.now().withDayOfMonth(1); // monthly
            case "products_created" -> LocalDate.MIN; // total
            case "skus" -> LocalDate.MIN;
            case "storage_mb" -> LocalDate.MIN;
            case "admin_users" -> LocalDate.MIN;
            case "staff_accounts" -> LocalDate.MIN;
            default -> LocalDate.now();
        };
    }
}
