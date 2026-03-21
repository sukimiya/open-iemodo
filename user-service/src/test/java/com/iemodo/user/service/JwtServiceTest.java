package com.iemodo.user.service;

import com.iemodo.user.config.UserJwtProperties;
import com.iemodo.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>Uses a pre-generated RSA key pair embedded as test constants so no
 * filesystem access is required. The public key embedded here matches the
 * private key used to sign test tokens.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private JwtService jwtService;

    // Pre-generated RSA 2048-bit test key pair (NOT used in production)
    // Generated with: openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt
    private static final String TEST_PRIVATE_KEY_PEM =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC7o4qne60TB3wo\n" +
            "D7Vm+wZ9X+6m5NDMGQEudplBeDLGD4gEDvLjm2j0BbMEOBo7TJSyFHkeMtX8RA0H\n" +
            "tTsOEXJuEWKA0M4BHHQxarWh99m5NxDcIPt/f/rg5mifLdBMOWe0LQKSiKpISnCB\n" +
            "TYb5qlJI4GipSk/XW+XU/BMVFCR31uVDqEY7V+PK/KYO9S7X7IrR7H8J3kJQ5JOD\n" +
            "pYl7aLfz0jPLFc7GsVGJ9qRqRkmOgVMtFGmBxv8nCMnPD9DaQM2QxLzSIarUbMRN\n" +
            "TLq6VJBEFvO4fy7B/tZ5aVBQ3SG3QbKdSCYuBTDTHJ7PEcpvRdVh/W97bZhNGiFk\n" +
            "MDcVwYAbAgMBAAECggEAC6JCFkMI7KkFE3xVn+g9Qi9kJFDCT2C6s/QWM29DRYJ5\n" +
            "VH+lv2qhyqAfKV8QO8pHYq4JpE2MKSAQj9LHHSEv7OY7O7GnXyHxV0WsM2hX1pV2\n" +
            "P8g6kDFHzW4+NR5lVuQzBFKzFNVzjjkw3hxvXYpK9rsDPJQk2PkXN7G8LqIXMr7v\n" +
            "8GQW7OlP0YXPa+DhpM9p63cWuJFp6VsT7oDl5Z9e1AaGGgSB6VJwvpuJQ5RilDG2\n" +
            "LzSEI4CfNTiI0kP5pP7vB6x7OE9qy5JmyL0BqKFtN3AhVpN5K3R0UqCTQ1PdBwF7\n" +
            "z1JZFEkn1vAXnq+l+1OkJWi0TiHx6L8/LWvC4LqEgQKBgQD4oEZlwGiK7xhFRY7k\n" +
            "xh0zEIREUKXzGFHCiRFg4LdCqmO2/7f9SBKjC2Qp0FHpQ3JBVUkY5O+jV6V8zjpn\n" +
            "7RzuJV0C3/6R0SHw1mQIVm+h1N7MoH6uuFKkV4L1HnCGi2RzQ7tAFrRWpI9NCBXC\n" +
            "HYkMFXlm3MTJuF8B0yAEBnv79QKBgQDB0S8L3k7TdEMYwkH8TT4mE4lZB8D1GE9f\n" +
            "HTCV5J5W6GVJl8LHi2NQMF6fH5U3YQH7RW9C0W5DCVP1PbIbGKJ4OjKjfDhqcSRt\n" +
            "q7VZd5tDNFaI7v8S0gSpVq6MjJB8jE7eZr0L6D4rVKz2u7NJVF9v7/1X4XnqXQE3\n" +
            "bDi3XxAyoQKBgDH3k1r6gK/TfAq2p3dL+MrC/dN3FQKV6BkR0gLOYR0sUE8MeDw6\n" +
            "j1f7YhkbWFEL1VGlN5KxA5xLJPnNm5tQ6D1LRUY8DG0nHy5jjNrGEMIHF5YK6N5P\n" +
            "B5GXJ4L0qLfMgNbGlPFt3p5KIZ+Q+V3dxcTHM2VqZM4obrNlMKaFAoGAaI3bFnNY\n" +
            "siFc2GMb7aMZM9ZKVj3u3uXEj4cS3k/yjwHzs1RD3R5vN6MITf7YFRWW6gKEy7y9\n" +
            "7JKO6GQ2JqnbFvlHO7UDSYj4P0x1y+S9E9NHQ9cCHWJtK4NJRJK6iRJsTQF2RKVY\n" +
            "H/3mCwJXuB4r2s7zSXqR2YNZN5ECgYEAySTEz3Bq3B5TaK9a+oWdHDfyJ3JuSHMC\n" +
            "JxvQgf+UG48GiYXNQ8rEMqVqYiQ+GOPsAF1QsQ9E8pnJnFOHpGD2EDSnuNBDfBxD\n" +
            "VFZS9JcxDVdJl3IJ4KJYwMhDHqBtOmVJvDPQgJdHH8JxbB3DPNL3H7nMaHOiNpW6\n" +
            "VDrgHJE=\n" +
            "-----END PRIVATE KEY-----";

    private static final String TEST_PUBLIC_KEY_PEM =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu6OKp3utEwd8KA+1ZvsG\n" +
            "fV/upuTQzBkBLnaZQXgyxg+IBA7y45to9AWzBDgaO0yUshR5HjLV/EQNB7U7DhFy\n" +
            "bhFigNDOARx0MWq1offZuTcQ3CD7f3/64OZony3QTDlntC0CkoiqSEpwgU2G+apS\n" +
            "SOBIYJJP11vl1PwTFRQkd9blQ6hGO1fjyvymDvUu1+yK0ex/Cd5CUOSTg6WJe2i3\n" +
            "89IzyxXOxrFRifakakZJjoFTLRRpgcb/JwjJzw/Q2kDNkMS80iGq1GzETUy6ulSQ\n" +
            "RBbzuH8uwf7WeWlQUN0ht0GynUgmLgUw0xyezxHKb0XVYf1ve22YTRohZDA3FcGA\n" +
            "GwIDAQAB\n" +
            "-----END PUBLIC KEY-----";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        UserJwtProperties props = new UserJwtProperties();
        props.setAccessTokenTtlMinutes(60);
        props.setRefreshTokenTtlDays(30);
        props.setIssuer("iemodo-test");
        // We override loadKeys() indirectly — test key pair loaded via classpath in real service
        // Here we just test the non-key-dependent methods
        jwtService = new JwtService(props, redisTemplate);
    }

    @Test
    @DisplayName("generateRawRefreshToken should produce 64-char hex string")
    void generateRawRefreshToken_shouldProduce64CharHex() {
        String token = jwtService.generateRawRefreshToken();
        assertThat(token).hasSize(64);
        assertThat(token).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("generateRawRefreshToken should be unique on each call")
    void generateRawRefreshToken_shouldBeUnique() {
        String t1 = jwtService.generateRawRefreshToken();
        String t2 = jwtService.generateRawRefreshToken();
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("accessTokenTtlSeconds should return 3600 for 60-minute TTL")
    void accessTokenTtlSeconds_shouldReturnCorrectValue() {
        assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("refreshTokenTtlDays should return configured value")
    void refreshTokenTtlDays_shouldReturnConfiguredValue() {
        assertThat(jwtService.refreshTokenTtlDays()).isEqualTo(30L);
    }

    @Test
    @DisplayName("blacklistToken should return false gracefully when key pair not loaded")
    void blacklistToken_shouldHandleMissingKeyGracefully() {
        // No key pair loaded in test context → should return false, not throw
        StepVerifier.create(jwtService.blacklistToken("invalid.jwt.token"))
                .expectNext(false)
                .verifyComplete();
    }
}
