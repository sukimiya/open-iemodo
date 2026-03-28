package com.iemodo.gateway.repository;

import com.iemodo.gateway.domain.GatewayRoute;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link GatewayRoute} entity.
 */
@Repository
public interface GatewayRouteRepository extends ReactiveCrudRepository<GatewayRoute, Long> {

    /**
     * Find all enabled routes ordered by priority.
     */
    Flux<GatewayRoute> findAllByEnabledTrueOrderByPriorityAsc();

    /**
     * Find route by route ID.
     */
    Mono<GatewayRoute> findByRouteId(String routeId);

    /**
     * Check if route exists.
     */
    Mono<Boolean> existsByRouteId(String routeId);

    /**
     * Find routes by path pattern.
     */
    @Query("SELECT * FROM gateway_routes WHERE path LIKE :pathPattern AND enabled = true")
    Flux<GatewayRoute> findByPathPattern(String pathPattern);

    /**
     * Enable/disable a route.
     */
    @Query("UPDATE gateway_routes SET enabled = :enabled, updated_at = NOW() WHERE route_id = :routeId")
    Mono<Integer> updateEnabledByRouteId(String routeId, boolean enabled);
}
