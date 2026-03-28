package com.iemodo.gateway.route;

import com.iemodo.gateway.domain.GatewayRoute;
import com.iemodo.gateway.repository.GatewayRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("DatabaseRouteDefinitionRepository")
class DatabaseRouteDefinitionRepositoryTest {

    @Mock private GatewayRouteRepository routeRepository;

    private DatabaseRouteDefinitionRepository routeDefinitionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        routeDefinitionRepository = new DatabaseRouteDefinitionRepository(routeRepository);
    }

    @Test
    @DisplayName("getRouteDefinitions: should convert gateway routes to route definitions")
    void getRouteDefinitions_shouldConvertRoutes() {
        GatewayRoute route1 = GatewayRoute.builder()
                .id(1L)
                .routeId("user-service-route")
                .uri("lb://user-service")
                .path("/uc/**")
                .priority(100)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        GatewayRoute route2 = GatewayRoute.builder()
                .id(2L)
                .routeId("order-service-route")
                .uri("lb://order-service")
                .path("/oc/**")
                .method("GET,POST")
                .priority(100)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        when(routeRepository.findAllByEnabledTrueOrderByPriorityAsc())
                .thenReturn(Flux.just(route1, route2));

        StepVerifier.create(routeDefinitionRepository.getRouteDefinitions())
                .assertNext(def -> {
                    assertThat(def.getId()).isEqualTo("user-service-route");
                    assertThat(def.getUri().toString()).isEqualTo("lb://user-service");
                    assertThat(def.getOrder()).isEqualTo(100);
                })
                .assertNext(def -> {
                    assertThat(def.getId()).isEqualTo("order-service-route");
                    assertThat(def.getPredicates()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getRouteDefinitions: should return empty when no routes")
    void getRouteDefinitions_shouldReturnEmpty_whenNoRoutes() {
        when(routeRepository.findAllByEnabledTrueOrderByPriorityAsc())
                .thenReturn(Flux.empty());

        StepVerifier.create(routeDefinitionRepository.getRouteDefinitions())
                .verifyComplete();
    }
}
