package com.iemodo.common.tenant;

import com.iemodo.common.exception.TenantIdMissingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TenantIdWebFilter")
class TenantIdWebFilterTest {

    private TenantIdWebFilter filter;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new TenantIdWebFilter();
        chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("should write tenant ID to Reactor Context when X-TenantID header is present")
    void shouldWriteTenantIdToContext_whenHeaderPresent() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/oc/api/v1/orders")
                .header(TenantIdWebFilter.TENANT_ID_HEADER, "tenant_001")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Capture the context after the filter runs
        Mono<Void> result = filter.filter(exchange, ex ->
                Mono.deferContextual(ctx -> {
                    String tenantId = ctx.get(TenantContext.TENANT_ID_KEY);
                    assert "tenant_001".equals(tenantId) : "Expected tenant_001 but got: " + tenantId;
                    return Mono.empty();
                })
        );

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("should return error when X-TenantID header is missing")
    void shouldReturnError_whenHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/oc/api/v1/orders")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .expectError(TenantIdMissingException.class)
                .verify();
    }

    @Test
    @DisplayName("should skip tenant check for actuator paths")
    void shouldSkipTenantCheck_forActuatorPaths() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("should skip tenant check for health paths")
    void shouldSkipTenantCheck_forHealthPaths() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();
    }
}
