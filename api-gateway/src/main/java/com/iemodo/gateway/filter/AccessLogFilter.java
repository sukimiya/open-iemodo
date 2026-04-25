package com.iemodo.gateway.filter;

import com.iemodo.common.billing.BillingServiceClient;
import com.iemodo.gateway.domain.AccessLog;
import com.iemodo.gateway.repository.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Global filter that logs all requests passing through the gateway.
 * 
 * <p>Logs are persisted to the database for monitoring and analytics.
 * Includes: request details, response status, timing, client info, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogFilter implements GlobalFilter, Ordered {

    private final AccessLogRepository accessLogRepository;
    private final BillingServiceClient billingServiceClient;

    // Order after TraceIdFilter but before other filters
    public static final int ORDER = -50;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant start = Instant.now();
        
        // Generate request ID if not present
        String reqId = request.getHeaders().getFirst("X-Request-ID");
        if (reqId == null) {
            reqId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String requestId = reqId;
        
        // Get trace ID if present
        final String traceId = request.getHeaders().getFirst("X-Trace-ID");
        
        // Get tenant ID if present
        final String tenantId = request.getHeaders().getFirst("X-TenantID");
        
        // Extract user ID from JWT (simplified - in production, parse from security context)
        final Long userId = extractUserId(request);
        
        // Store request ID in exchange attributes for other filters
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", start);
        
        final Instant startTime = start;
        
        // Capture request details
        final String method = request.getMethod().name();
        final String path = request.getURI().getPath();
        final String queryParams = request.getURI().getQuery();
        final String clientIp = getClientIp(request);
        final String userAgent = request.getHeaders().getFirst("User-Agent");
        
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    ServerHttpResponse response = exchange.getResponse();
                    
                    // Build access log entry
                    AccessLog accessLog = AccessLog.builder()
                            .requestId(requestId)
                            .traceId(traceId)
                            .tenantId(tenantId)
                            .userId(userId)
                            .method(method)
                            .path(path)
                            .queryParams(queryParams)
                            .clientIp(clientIp)
                            .userAgent(userAgent)
                            .statusCode(response.getStatusCode() != null ? response.getStatusCode().value() : null)
                            .responseTime((int) duration.toMillis())
                            .routeId(exchange.getAttribute("routeId"))
                            .targetUri(exchange.getAttribute("targetUri"))
                            .build();
                    
                    // Save log asynchronously (fire and forget)
                    saveAccessLog(accessLog);

                    // Record billing usage when tenant is identified (fire and forget)
                    if (tenantId != null) {
                        billingServiceClient.recordUsage(tenantId, "api_calls", 1)
                                .onErrorResume(e -> {
                                    log.debug("Failed to record api_calls for tenant {}: {}", tenantId, e.getMessage());
                                    return Mono.empty();
                                })
                                .subscribe();
                    }
                });
    }

    private void saveAccessLog(AccessLog accessLog) {
        accessLogRepository.save(accessLog)
                .doOnSuccess(saved -> log.debug("Access log saved: {} {}", saved.getMethod(), saved.getPath()))
                .doOnError(e -> log.error("Failed to save access log: {}", e.getMessage()))
                .subscribe();
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

    private Long extractUserId(ServerHttpRequest request) {
        // In production, extract from JWT token or security context
        String userIdHeader = request.getHeaders().getFirst("X-User-ID");
        if (userIdHeader != null) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
