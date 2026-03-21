package com.iemodo.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TraceIdFilter")
class TraceIdFilterTest {

    private TraceIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("should generate X-Request-ID when not present")
    void shouldGenerateTraceId_whenNotPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/oc/api/v1/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // The response header should contain a generated trace ID
        String traceId = exchange.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotBlank();
        assertThat(traceId).hasSize(32); // UUID without hyphens
    }

    @Test
    @DisplayName("should reuse X-Request-ID when already present")
    void shouldReuseTraceId_whenAlreadyPresent() {
        String existingTraceId = "abc123existingtraceid";
        MockServerHttpRequest request = MockServerHttpRequest.get("/oc/api/v1/orders")
                .header(TraceIdFilter.TRACE_ID_HEADER, existingTraceId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String traceId = exchange.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isEqualTo(existingTraceId);
    }

    @Test
    @DisplayName("should have order -300")
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(-300);
    }
}
