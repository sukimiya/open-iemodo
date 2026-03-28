package com.iemodo.gateway.repository;

import com.iemodo.gateway.domain.RateLimitRule;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link RateLimitRule} entity.
 */
@Repository
public interface RateLimitRuleRepository extends ReactiveCrudRepository<RateLimitRule, Long> {

    /**
     * Find all enabled rate limit rules.
     */
    Flux<RateLimitRule> findAllByEnabledTrue();

    /**
     * Find rule by name.
     */
    Mono<RateLimitRule> findByRuleName(String ruleName);

    /**
     * Find rules by route ID.
     */
    Flux<RateLimitRule> findByRouteIdAndEnabledTrue(String routeId);

    /**
     * Find default rule (no specific route).
     */
    @Query("SELECT * FROM rate_limit_rules WHERE route_id IS NULL AND enabled = true LIMIT 1")
    Mono<RateLimitRule> findDefaultRule();

    /**
     * Check if rule exists.
     */
    Mono<Boolean> existsByRuleName(String ruleName);
}
