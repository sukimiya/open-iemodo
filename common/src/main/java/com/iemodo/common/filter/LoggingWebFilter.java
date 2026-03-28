package com.iemodo.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * WebFilter that logs incoming HTTP requests and their responses.
 * 
 * <p>Logs include:
 * <ul>
 *   <li>Method and URI
 *   <li>Query parameters
 *   <li>Client IP
 *   <li>Response status
 *   <li>Request duration
 * </ul>
 */
@Slf4j
@Component
@Order(-100)  // Run after TenantIdWebFilter but before other filters
public class LoggingWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant start = Instant.now();

        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String clientIp = getClientIp(request);

        log.info("[Request] {} {}{} from {}", 
                method, 
                uri, 
                query != null ? "?" + query : "",
                clientIp);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    Duration duration = Duration.between(start, Instant.now());
                    int status = exchange.getResponse().getStatusCode() != null 
                            ? exchange.getResponse().getStatusCode().value() 
                            : 200;
                    
                    log.info("[Response] {} {} - {} in {}ms",
                            method,
                            uri,
                            status,
                            duration.toMillis());
                })
                .doOnError(error -> {
                    Duration duration = Duration.between(start, Instant.now());
                    log.error("[Error] {} {} - {} in {}ms: {}",
                            method,
                            uri,
                            500,
                            duration.toMillis(),
                            error.getMessage());
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }
}
