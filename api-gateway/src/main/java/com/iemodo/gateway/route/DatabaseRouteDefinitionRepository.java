package com.iemodo.gateway.route;

import com.iemodo.gateway.domain.GatewayRoute;
import com.iemodo.gateway.repository.GatewayRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Route definition repository that loads routes from the database.
 * 
 * <p>This enables dynamic route configuration without restarting the gateway.
 * Routes are refreshed periodically or on-demand via the /actuator/gateway/refresh endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseRouteDefinitionRepository implements RouteDefinitionRepository {

    private final GatewayRouteRepository routeRepository;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return routeRepository.findAllByEnabledTrueOrderByPriorityAsc()
                .map(this::convertToRouteDefinition)
                .doOnNext(route -> log.debug("Loaded route from database: {}", route.getId()))
                .doOnComplete(() -> log.info("Loaded all dynamic routes from database"));
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        // Routes are managed via database, not programmatically
        return Mono.error(new UnsupportedOperationException(
                "Routes must be saved to the database. Use GatewayRouteRepository."));
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        // Routes are managed via database, not programmatically
        return Mono.error(new UnsupportedOperationException(
                "Routes must be deleted from the database. Use GatewayRouteRepository."));
    }

    /**
     * Convert a GatewayRoute entity to Spring Cloud Gateway RouteDefinition.
     */
    private RouteDefinition convertToRouteDefinition(GatewayRoute gatewayRoute) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(gatewayRoute.getRouteId());
        definition.setUri(URI.create(gatewayRoute.getUri()));
        definition.setOrder(gatewayRoute.getEffectivePriority());

        // Add predicates
        List<PredicateDefinition> predicates = new ArrayList<>();
        predicates.add(new PredicateDefinition("Path=" + gatewayRoute.getPath()));
        if (gatewayRoute.getMethod() != null && !gatewayRoute.getMethod().isEmpty()) {
            predicates.add(new PredicateDefinition("Method=" + gatewayRoute.getMethod()));
        }
        definition.setPredicates(predicates);

        // Add default filters (rate limiting, etc.)
        List<FilterDefinition> filters = new ArrayList<>();
        filters.add(new FilterDefinition("StripPrefix=0"));
        definition.setFilters(filters);

        return definition;
    }

    /**
     * Refresh routes by reloading from database.
     * Called by the route refresh actuator endpoint.
     */
    public Mono<Void> refreshRoutes() {
        return getRouteDefinitions()
                .collectList()
                .doOnNext(routes -> log.info("Refreshed {} routes from database", routes.size()))
                .then();
    }
}
