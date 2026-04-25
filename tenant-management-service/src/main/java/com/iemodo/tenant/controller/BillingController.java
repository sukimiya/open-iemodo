package com.iemodo.tenant.controller;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import com.iemodo.tenant.domain.TenantSubscription;
import com.iemodo.tenant.service.BillingService;
import com.iemodo.tenant.service.FeatureGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final FeatureGateService featureGateService;

    /**
     * Create a Stripe Checkout Session for subscription.
     */
    @PostMapping("/checkout")
    public Mono<Response<Map<String, String>>> createCheckoutSession(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam(defaultValue = "STANDARD") String planId) {
        return billingService.createCheckoutSession(tenantId, planId)
                .map(url -> Response.success(Map.of("checkoutUrl", url)))
                .onErrorMap(e -> new BusinessException(ErrorCode.CHECKOUT_SESSION_FAILED, HttpStatus.BAD_GATEWAY, e.getMessage()));
    }

    /**
     * Get current subscription for the tenant.
     */
    @GetMapping("/subscription")
    public Mono<Response<TenantSubscription>> getSubscription(
            @RequestHeader("X-TenantID") String tenantId) {
        return billingService.getSubscription(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .map(Response::success);
    }

    /**
     * Cancel subscription at period end.
     */
    @PostMapping("/cancel")
    public Mono<Response<Void>> cancelSubscription(
            @RequestHeader("X-TenantID") String tenantId) {
        return billingService.cancelAtPeriodEnd(tenantId)
                .then(Mono.just(Response.success()));
    }

    /**
     * Get plan details with limits.
     */
    @GetMapping("/plans/{planId}")
    public Mono<Response<Map<String, Object>>> getPlanDetails(@PathVariable String planId) {
        return billingService.getPlanDetails(planId)
                .map(Response::success);
    }

    /**
     * Get all available plans.
     */
    @GetMapping("/plans")
    public Mono<Response<Map<String, Map<String, Object>>>> getAllPlans() {
        return billingService.getAllPlans()
                .map(Response::success);
    }

    /**
     * Check if a feature is available for the tenant (for internal/resource checks).
     */
    @GetMapping("/limits/{metric}")
    public Mono<Response<Map<String, Object>>> checkLimit(
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable String metric) {
        return featureGateService.getUsageStatus(tenantId, metric)
                .map(Response::success);
    }

    /**
     * Check if a specific operation is allowed, considering plan limits.
     * Used by other services via the billing client before performing an action.
     */
    @PostMapping("/usage/check")
    public Mono<Response<Void>> checkUsage(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam String metric,
            @RequestParam(defaultValue = "1") long amount) {
        return featureGateService.checkLimit(tenantId, metric, amount)
                .then(Mono.just(Response.success()));
    }

    /**
     * Record usage for a tenant metric. Used by other services after performing an action.
     */
    @PostMapping("/usage/record")
    public Mono<Response<Void>> recordUsage(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam String metric,
            @RequestParam(defaultValue = "1") long amount) {
        return featureGateService.recordUsage(tenantId, metric, amount)
                .then(Mono.just(Response.success()));
    }
}
