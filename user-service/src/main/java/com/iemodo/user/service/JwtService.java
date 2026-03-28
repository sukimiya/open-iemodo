package com.iemodo.user.service;

import com.iemodo.user.config.UserJwtProperties;
import com.iemodo.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT service — issues Access Tokens (RS256 signed) and manages the
 * Redis blacklist for logout/revocation.
 *
 * <p>Keys are loaded from PEM files at startup. In production these files
 * are mounted from Kubernetes Secrets or AWS Secrets Manager.
 */
@Slf4j
@Service
public class JwtService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserJwtProperties props;
    private final ReactiveStringRedisTemplate redisTemplate;

    private PrivateKey privateKey;
    private PublicKey  publicKey;

    public JwtService(UserJwtProperties props, ReactiveStringRedisTemplate redisTemplate) {
        this.props = props;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void loadKeys() {
        try {
            privateKey = loadPrivateKey(props.getPrivateKeyPath());
            publicKey  = loadPublicKey(props.getPublicKeyPath());
            log.info("JWT RSA key pair loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load JWT keys — token signing will fail at runtime: {}", e.getMessage());
        }
    }

    // ─── Token generation ──────────────────────────────────────────────────

    /**
     * Generate a signed RS256 Access Token.
     *
     * @param user     the authenticated user
     * @param tenantId the tenant context
     * @return signed JWT string
     */
    public String generateAccessToken(User user, String tenantId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenTtlMinutes() * 60L);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .issuer(props.getIssuer())
                .claim("email", user.getEmail())
                .claim("tid", tenantId)
                .claim("name", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(privateKey)
                .compact();
    }

    /**
     * Generate a raw (opaque) Refresh Token string.
     * The caller is responsible for hashing and persisting it.
     */
    public String generateRawRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    // ─── Blacklist (logout) ────────────────────────────────────────────────

    /**
     * Add an Access Token's JTI to the Redis blacklist.
     * TTL is set to the token's remaining validity so the key auto-expires.
     *
     * @param token     the raw JWT string
     * @return Mono completing when the key is written
     */
    public Mono<Boolean> blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti == null) {
                log.warn("Token has no JTI — skipping blacklist");
                return Mono.just(false);
            }

            long remainingSeconds = claims.getExpiration().getTime() / 1000 - Instant.now().getEpochSecond();
            if (remainingSeconds <= 0) {
                return Mono.just(true); // already expired, nothing to blacklist
            }

            String key = BLACKLIST_PREFIX + jti;
            return redisTemplate.opsForValue()
                    .set(key, "1", Duration.ofSeconds(remainingSeconds))
                    .doOnSuccess(ok -> log.debug("Blacklisted token JTI={} TTL={}s", jti, remainingSeconds));
        } catch (Exception e) {
            log.warn("Could not blacklist token: {}", e.getMessage());
            return Mono.just(false);
        }
    }

    public long accessTokenTtlSeconds() {
        return props.getAccessTokenTtlMinutes() * 60L;
    }

    public long refreshTokenTtlDays() {
        return props.getRefreshTokenTtlDays();
    }

    /**
     * Extract user ID from JWT token.
     */
    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.warn("Could not extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    // ─── Key loading helpers ───────────────────────────────────────────────

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = readPem(path)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        String pem = readPem(path)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String readPem(String resourcePath) throws Exception {
        Resource resource = new DefaultResourceLoader().getResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
