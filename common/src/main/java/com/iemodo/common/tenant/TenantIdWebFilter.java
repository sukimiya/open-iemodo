package com.iemodo.common.tenant;

import com.iemodo.common.exception.TenantIdMissingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter that extracts the {@code X-TenantID} header and writes it
 * into the Reactor Context so downstream components (notably
 * {@link PostgresTenantConnectionFactory}) can route to the correct
 * database schema without blocking.
 *
 * <p>Filter order is set to {@code -200} so it runs before Spring Security
 * and other application filters.
 *
 * <p>Paths listed in {@link #isPublicPath(String)} are exempted from the
 * mandatory-header check (e.g. health probes).
 */
@Slf4j
@Component
@Order(-200)
public class TenantIdWebFilter implements WebFilter {

    public static final String TENANT_ID_HEADER = "X-TenantID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip tenant check for health / actuator endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String tenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);

        if (!StringUtils.hasText(tenantId)) {
            log.warn("Request to {} missing X-TenantID header", path);
            return Mono.error(new TenantIdMissingException());
        }

        log.debug("Resolved tenantId={} for path={}", tenantId, path);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(TenantContext.TENANT_ID_KEY, tenantId));
    }

    /**
     * Paths that do not require a tenant header.
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/health");
    }
}
