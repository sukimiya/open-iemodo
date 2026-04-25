package com.iemodo.common.billing;

import com.iemodo.common.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reactive client for calling the tenant-management-service billing API from other services.
 *
 * <p>Each service should create a {@code @Bean} pointing to the tenant-management-service base URL.
 *
 * <pre>{@code
 * @Bean
 * public BillingServiceClient billingServiceClient(WebClient.Builder builder) {
 *     return new BillingServiceClient(builder, "http://tenant-management-service:8091");
 * }
 * }</pre>
 */
@RequiredArgsConstructor
public class BillingServiceClient {

    private final WebClient webClient;

    public BillingServiceClient(WebClient.Builder builder, String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Check whether a usage operation is allowed under the tenant's current plan.
     *
     * @return empty Mono if allowed, or errors with {@code PLAN_LIMIT_EXCEEDED} if exceeded.
     */
    public Mono<Void> checkUsage(String tenantId, String metric, long amount) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/billing/usage/check")
                        .queryParam("metric", metric)
                        .queryParam("amount", amount)
                        .build())
                .header("X-TenantID", tenantId)
                .retrieve()
                .bodyToMono(Response.class)
                .then();
    }

    /**
     * Record usage for a tenant metric (daily aggregation).
     */
    public Mono<Void> recordUsage(String tenantId, String metric, long amount) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/billing/usage/record")
                        .queryParam("metric", metric)
                        .queryParam("amount", amount)
                        .build())
                .header("X-TenantID", tenantId)
                .retrieve()
                .bodyToMono(Response.class)
                .then();
    }

    /**
     * Get usage status for a specific metric.
     */
    public Mono<Map<String, Object>> getUsageStatus(String tenantId, String metric) {
        return webClient.get()
                .uri("/api/v1/billing/limits/{metric}", metric)
                .header("X-TenantID", tenantId)
                .retrieve()
                .bodyToMono(Response.class)
                .map(r -> (Map<String, Object>) r.getData());
    }

    /**
     * Get plan details.
     */
    public Mono<Map<String, Object>> getPlanDetails(String planId) {
        return webClient.get()
                .uri("/api/v1/billing/plans/{planId}", planId)
                .retrieve()
                .bodyToMono(Response.class)
                .map(r -> (Map<String, Object>) r.getData());
    }
}
