package com.iemodo.customer.service;

import com.iemodo.customer.config.CustomerJwtProperties;
import com.iemodo.customer.domain.Customer;
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

@Slf4j
@Service
public class CustomerJwtService {

    private static final String BLACKLIST_PREFIX = "customer:jwt:blacklist:";

    private final CustomerJwtProperties props;
    private final ReactiveStringRedisTemplate redisTemplate;

    private PrivateKey privateKey;
    private PublicKey  publicKey;

    public CustomerJwtService(CustomerJwtProperties props, ReactiveStringRedisTemplate redisTemplate) {
        this.props = props;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void loadKeys() {
        try {
            privateKey = loadPrivateKey(props.getPrivateKeyPath());
            publicKey  = loadPublicKey(props.getPublicKeyPath());
            log.info("Customer JWT RSA key pair loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load customer JWT keys: {}", e.getMessage());
        }
    }

    public String generateAccessToken(Customer customer, String tenantId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenTtlMinutes() * 60L);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(customer.getId()))
                .issuer(props.getIssuer())
                .claim("phone", customer.getPhone())
                .claim("email", customer.getEmail())
                .claim("tid", tenantId)
                .claim("name", customer.getDisplayName())
                .claim("role", "CUSTOMER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(privateKey)
                .compact();
    }

    public String generateRawRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    public Mono<Boolean> blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti == null) {
                return Mono.just(false);
            }

            long remainingSeconds = claims.getExpiration().getTime() / 1000 - Instant.now().getEpochSecond();
            if (remainingSeconds <= 0) {
                return Mono.just(true);
            }

            String key = BLACKLIST_PREFIX + jti;
            return redisTemplate.opsForValue()
                    .set(key, "1", Duration.ofSeconds(remainingSeconds))
                    .doOnSuccess(ok -> log.debug("Blacklisted customer token JTI={} TTL={}s", jti, remainingSeconds));
        } catch (Exception e) {
            log.warn("Could not blacklist customer token: {}", e.getMessage());
            return Mono.just(false);
        }
    }

    public long accessTokenTtlSeconds() {
        return props.getAccessTokenTtlMinutes() * 60L;
    }

    public long refreshTokenTtlDays() {
        return props.getRefreshTokenTtlDays();
    }

    public Long extractCustomerId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.warn("Could not extract customerId from token: {}", e.getMessage());
            return null;
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
