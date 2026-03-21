package com.iemodo.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import com.iemodo.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * GlobalFilter that validates the JWT {@code Authorization: Bearer <token>}
 * header on every non-whitelisted request.
 *
 * <p>On success, it propagates the resolved {@code X-User-ID} and
 * (if missing) {@code X-TenantID} headers to downstream services so they
 * don't need to re-parse the token.
 *
 * <p>On failure (missing / expired / blacklisted token) it short-circuits
 * the filter chain with a {@code 401} response in the unified
 * {@link Response} format.
 *
 * <p>Order = {@code -200} — after TraceId ({@code -300}), before Tenant
 * filter ({@code -100}).
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final GatewayProperties gatewayProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Parsed RSA public key — initialised in @PostConstruct
    private PublicKey publicKey;

    public JwtAuthFilter(GatewayProperties gatewayProperties,
                         ReactiveStringRedisTemplate redisTemplate) {
        this.gatewayProperties = gatewayProperties;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void loadPublicKey() {
        try {
            Resource resource = new DefaultResourceLoader()
                    .getResource(gatewayProperties.getJwt().getPublicKeyPath());

            if (!resource.exists()) {
                log.warn("JWT public key not found at '{}'. JWT validation will fail at runtime.",
                        gatewayProperties.getJwt().getPublicKeyPath());
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");

                byte[] decoded = Base64.getDecoder().decode(pem);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
                publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
                log.info("JWT RSA public key loaded successfully from '{}'",
                        gatewayProperties.getJwt().getPublicKeyPath());
            }
        } catch (Exception e) {
            log.error("Failed to load JWT public key: {}", e.getMessage(), e);
        }
    }

    @Override
    public int getOrder() {
        return -200;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();

        // 1. Whitelist check
        if (isWhitelisted(method, path)) {
            log.debug("Whitelisted path: {} {}", method, path);
            return chain.filter(exchange);
        }

        // 2. Extract Bearer token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // 3. Parse & validate JWT
        Claims claims;
        try {
            if (publicKey == null) {
                return unauthorized(exchange.getResponse(), "JWT validation unavailable — public key not loaded");
            }
            claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return unauthorized(exchange.getResponse(), "Token has expired");
        } catch (JwtException e) {
            return unauthorized(exchange.getResponse(), "Invalid token");
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        String tenantId = claims.get("tid", String.class);

        // 4. Check Redis blacklist (logout)
        String blacklistKey = BLACKLIST_PREFIX + (jti != null ? jti : token);
        return redisTemplate.hasKey(blacklistKey)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return unauthorized(exchange.getResponse(), "Token has been revoked");
                    }

                    // 5. Propagate user info to downstream via headers
                    ServerHttpRequest mutated = request.mutate()
                            .header("X-User-ID", userId != null ? userId : "")
                            .header("X-TenantID", tenantId != null ? tenantId : "")
                            .build();

                    log.debug("JWT valid — userId={} tenantId={} path={}", userId, tenantId, path);
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private boolean isWhitelisted(String method, String path) {
        List<String> whitelist = gatewayProperties.getWhitelist();
        for (String entry : whitelist) {
            // Format: METHOD:/path/pattern  or  GET:/path/**
            int colonIdx = entry.indexOf(':');
            if (colonIdx == -1) {
                // No method prefix — match path only
                if (pathMatcher.match(entry, path)) return true;
            } else {
                String wMethod = entry.substring(0, colonIdx);
                String wPath = entry.substring(colonIdx + 1);
                if (wMethod.equalsIgnoreCase(method) && pathMatcher.match(wPath, path)) return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String reason) {
        log.warn("JWT auth failed: {}", reason);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = MAPPER.writeValueAsBytes(
                    Response.error(ErrorCode.UNAUTHORIZED, reason));
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] fallback = "{\"code\":401,\"message\":\"Unauthorized\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
