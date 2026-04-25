package com.iemodo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
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
 * WebFilter that validates JWT Bearer tokens on every non-whitelisted request.
 * Replaces the Gateway's GlobalFilter for the iemodo-lite monolith.
 *
 * <p>On success, propagates X-User-ID and X-TenantID headers downstream.
 * On failure, returns 401 in the unified response format.
 */
@Slf4j
@Component
public class JwtWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final LiteGatewayProperties gatewayProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private PublicKey publicKey;

    public JwtWebFilter(LiteGatewayProperties gatewayProperties,
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
                log.info("JWT RSA public key loaded from '{}'", gatewayProperties.getJwt().getPublicKeyPath());
            }
        } catch (Exception e) {
            log.error("Failed to load JWT public key: {}", e.getMessage(), e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();

        // 1. Whitelist check
        if (isWhitelisted(method, path)) {
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

                    // 5. Propagate user info via headers
                    ServerHttpRequest mutated = request.mutate()
                            .header("X-User-ID", userId != null ? userId : "")
                            .header("X-TenantID", tenantId != null ? tenantId : "")
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    private boolean isWhitelisted(String method, String path) {
        List<String> whitelist = gatewayProperties.getWhitelist();
        for (String entry : whitelist) {
            int colonIdx = entry.indexOf(':');
            if (colonIdx == -1) {
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
            byte[] body = MAPPER.writeValueAsBytes(Response.error(ErrorCode.UNAUTHORIZED, reason));
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] fallback = "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
