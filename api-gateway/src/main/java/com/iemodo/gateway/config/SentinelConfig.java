package com.iemodo.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.EventObserverRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sentinel configuration for the API Gateway.
 *
 * <p>Covers two concerns:
 * <ol>
 *   <li><b>Flow control (rate limiting)</b> — per-route QPS caps via {@link GatewayFlowRule}.
 *       Rules here are static defaults; production overrides come from the Sentinel Dashboard
 *       or a Nacos datasource.</li>
 *   <li><b>Circuit breaking (degradation)</b> — per-route {@link DegradeRule} that opens
 *       the circuit when the error ratio or slow-call ratio exceeds a threshold, then
 *       automatically half-opens after the configured recovery window.</li>
 * </ol>
 *
 * <p>All circuit-breaker state transitions are logged as {@code [CIRCUIT_BREAKER]} entries
 * so that an alerting rule can fire on {@code CLOSED → OPEN} events.
 */
@Slf4j
@Configuration
public class SentinelConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                          ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /** Replaces the default handler with one that returns structured JSON. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CustomBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new CustomBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @PostConstruct
    public void initGatewayRules() {
        initFlowRules();
        initDegradeRules();
        registerCircuitBreakerObserver();
        log.info("Sentinel gateway rules initialised");
    }

    // ─── Flow rules (rate limiting) ───────────────────────────────────────

    private void initFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // User service: 500 QPS — general browsing + account operations
        rules.add(new GatewayFlowRule("user-service-route")
                .setCount(500).setIntervalSec(1));

        // Order service: 1000 QPS — high-traffic checkout path
        rules.add(new GatewayFlowRule("order-service-route")
                .setCount(1000).setIntervalSec(1));

        // Payment service: 300 QPS — financial operations, intentionally conservative
        rules.add(new GatewayFlowRule("payment-service-route")
                .setCount(300).setIntervalSec(1));

        // Inventory service: 800 QPS — read-heavy stock queries
        rules.add(new GatewayFlowRule("inventory-service-route")
                .setCount(800).setIntervalSec(1));

        // Marketing / Pricing: 500 QPS each
        rules.add(new GatewayFlowRule("marketing-service-route")
                .setCount(500).setIntervalSec(1));

        rules.add(new GatewayFlowRule("pricing-service-route")
                .setCount(500).setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
        log.info("Flow rules loaded: {} rules", rules.size());
    }

    // ─── Degrade rules (circuit breaking) ────────────────────────────────

    /**
     * Circuit breaker rules for the three money-critical routes.
     *
     * <p>Strategy: error-ratio first (catches upstream failures), plus a
     * slow-call guard on payment to catch latency spikes before they cascade.
     * Recovery windows are intentionally short (15–30 s) so services can
     * self-heal quickly after transient blips.
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // Order service — open when error ratio > 50% over ≥10 requests; recover in 20 s
        rules.add(new DegradeRule("order-service-route")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.5)
                .setMinRequestAmount(10)
                .setStatIntervalMs(60_000)
                .setTimeWindow(20));

        // Payment service — open when error ratio > 30% over ≥5 requests; recover in 30 s
        // Lower threshold because payment failures directly affect revenue.
        rules.add(new DegradeRule("payment-service-route")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.3)
                .setMinRequestAmount(5)
                .setStatIntervalMs(60_000)
                .setTimeWindow(30));

        // Payment service — also open when 80%+ of calls are slow (> 2000 ms); recover in 30 s
        rules.add(new DegradeRule("payment-service-route")
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(2000)
                .setSlowRatioThreshold(0.8)
                .setMinRequestAmount(5)
                .setStatIntervalMs(60_000)
                .setTimeWindow(30));

        // Inventory service — open when error ratio > 50% over ≥10 requests; recover in 15 s
        rules.add(new DegradeRule("inventory-service-route")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.5)
                .setMinRequestAmount(10)
                .setStatIntervalMs(60_000)
                .setTimeWindow(15));

        DegradeRuleManager.loadRules(rules);
        log.info("Degrade (circuit breaker) rules loaded: {} rules", rules.size());
    }

    // ─── Circuit breaker state observer (auto-recovery logging) ──────────

    /**
     * Logs every circuit-breaker state transition so that CLOSED→OPEN events
     * can trigger alerts, and OPEN→HALF_OPEN / HALF_OPEN→CLOSED confirm recovery.
     */
    private void registerCircuitBreakerObserver() {
        EventObserverRegistry.getInstance().addStateChangeObserver(
                "iemodo-gateway-cb-logger",
                (prevState, newState, rule, snapshotValue) ->
                        log.warn("[CIRCUIT_BREAKER] resource={} {}→{} (snapshotValue={})",
                                rule.getResource(), prevState.name(), newState.name(), snapshotValue)
        );
    }
}
