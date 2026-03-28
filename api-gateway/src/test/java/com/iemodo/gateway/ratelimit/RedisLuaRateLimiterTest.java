package com.iemodo.gateway.ratelimit;

import com.iemodo.gateway.domain.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("RedisLuaRateLimiter")
class RedisLuaRateLimiterTest {

    @Mock private ReactiveStringRedisTemplate redisTemplate;

    private RedisLuaRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimiter = new RedisLuaRateLimiter(redisTemplate);
    }

    @Test
    @DisplayName("isAllowed: should allow request when tokens available")
    void isAllowed_shouldAllow_whenTokensAvailable() {
        RateLimitRule rule = RateLimitRule.builder()
                .ruleName("test-rule")
                .replenishRate(10)
                .burstCapacity(20)
                .requestedTokens(1)
                .enabled(true)
                .build();

        // Mock Redis response: [allowed=1, remainingTokens=19]
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(List.of(1L, 19L)));

        StepVerifier.create(rateLimiter.isAllowed("test-key", rule))
                .assertNext(result -> {
                    assertThat(result.isAllowed()).isTrue();
                    assertThat(result.getRemainingTokens()).isEqualTo(19);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("isAllowed: should deny request when no tokens")
    void isAllowed_shouldDeny_whenNoTokens() {
        RateLimitRule rule = RateLimitRule.builder()
                .ruleName("test-rule")
                .replenishRate(1)
                .burstCapacity(1)
                .requestedTokens(1)
                .enabled(true)
                .build();

        // Mock Redis response: [allowed=0, remainingTokens=0]
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(List.of(0L, 0L)));

        StepVerifier.create(rateLimiter.isAllowed("test-key", rule))
                .assertNext(result -> {
                    assertThat(result.isAllowed()).isFalse();
                    assertThat(result.getRemainingTokens()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("isAllowed: should use default values when rule has nulls")
    void isAllowed_shouldUseDefaults_whenNulls() {
        RateLimitRule rule = RateLimitRule.builder()
                .ruleName("test-rule")
                .replenishRate(null)
                .burstCapacity(null)
                .requestedTokens(null)
                .enabled(true)
                .build();

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(List.of(1L, 50L)));

        StepVerifier.create(rateLimiter.isAllowed("test-key", rule))
                .assertNext(result -> assertThat(result.isAllowed()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("isAllowed: should allow on error (fail open)")
    void isAllowed_shouldAllowOnError() {
        RateLimitRule rule = RateLimitRule.builder()
                .ruleName("test-rule")
                .replenishRate(10)
                .burstCapacity(20)
                .enabled(true)
                .build();

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.error(new RuntimeException("Redis error")));

        StepVerifier.create(rateLimiter.isAllowed("test-key", rule))
                .assertNext(result -> {
                    assertThat(result.isAllowed()).isTrue(); // Fail open
                    assertThat(result.getRemainingTokens()).isEqualTo(0);
                })
                .verifyComplete();
    }
}
