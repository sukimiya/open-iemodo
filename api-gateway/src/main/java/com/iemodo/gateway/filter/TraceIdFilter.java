package com.iemodo.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GlobalFilter that ensures every request carries a unique trace ID.
 *
 * <ul>
 *   <li>If the incoming request already has {@code X-Request-ID}, that value
 *       is reused (supports external tracing systems).</li>
 *   <li>Otherwise a random UUID is generated.</li>
 *   <li>The trace ID is added to the request forwarded downstream AND to the
 *       response so callers can correlate logs.</li>
 * </ul>
 *
 * <p>Order = {@code -300} — runs before JWT filter ({@code -200}) and tenant
 * filter ({@code -100}).
 */
@Slf4j
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Request-ID";

    @Override
    public int getOrder() {
        return -300;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        final String finalTraceId = traceId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType ->
                        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId)
                );
    }
}
