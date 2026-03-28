package com.iemodo.gateway.ratelimit;

import com.iemodo.gateway.domain.RateLimitRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Redis-based rate limiter using Lua scripts for atomic operations.
 * 
 * <p>Implements the token bucket algorithm with configurable:
 * <ul>
 *   <li>replenishRate - tokens added per second
 *   <li>burstCapacity - maximum bucket size
 *   <li>requestedTokens - tokens consumed per request
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLuaRateLimiter {

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Lua script for token bucket rate limiting.
     * 
     * KEYS[1]: rate limiter key
     * ARGV[1]: replenish rate (tokens per second)
     * ARGV[2]: burst capacity (max tokens)
     * ARGV[3]: requested tokens
     * ARGV[4]: current timestamp in milliseconds
     * 
     * Returns: [allowed (1/0), remaining tokens]
     */
    private static final String TOKEN_BUCKET_LUA = """
            local key = KEYS[1]
            local replenishRate = tonumber(ARGV[1])
            local burstCapacity = tonumber(ARGV[2])
            local requestedTokens = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])
            
            -- Get current bucket state
            local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill')
            local tokens = tonumber(bucket[1]) or burstCapacity
            local lastRefill = tonumber(bucket[2]) or now
            
            -- Calculate tokens to add based on time elapsed
            local elapsed = math.max(0, now - lastRefill)
            local tokensToAdd = elapsed * replenishRate / 1000
            tokens = math.min(burstCapacity, tokens + tokensToAdd)
            
            -- Check if request can be allowed
            local allowed = 0
            if tokens >= requestedTokens then
                tokens = tokens - requestedTokens
                allowed = 1
            end
            
            -- Update bucket state
            redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now)
            redis.call('PEXPIRE', key, 60000) -- Expire after 60 seconds of inactivity
            
            return {allowed, math.floor(tokens)}
            """;

    private final RedisScript<List<Long>> rateLimitScript = RedisScript.of(
            TOKEN_BUCKET_LUA, 
            (Class<List<Long>>) (Class<?>) List.class
    );

    /**
     * Check if a request is allowed under the rate limit.
     *
     * @param key             the rate limiter key (e.g., "rate_limit:user:123")
     * @param rateLimitRule   the rate limit rule
     * @return Mono containing the result (allowed and remaining tokens)
     */
    public Mono<RateLimitResult> isAllowed(String key, RateLimitRule rateLimitRule) {
        List<String> keys = List.of(key);
        List<String> args = Arrays.asList(
                String.valueOf(rateLimitRule.getEffectiveReplenishRate()),
                String.valueOf(rateLimitRule.getEffectiveBurstCapacity()),
                String.valueOf(rateLimitRule.getEffectiveRequestedTokens()),
                String.valueOf(Instant.now().toEpochMilli())
        );

        return redisTemplate.execute(rateLimitScript, keys, args)
                .next()
                .map(result -> {
                    boolean allowed = result.get(0) == 1;
                    long remainingTokens = result.get(1);
                    return new RateLimitResult(allowed, remainingTokens);
                })
                .doOnError(e -> log.error("Rate limit check failed for key {}: {}", key, e.getMessage()))
                .onErrorReturn(new RateLimitResult(true, 0)); // Allow on error (fail open)
    }

    /**
     * Check if a request is allowed with custom parameters.
     */
    public Mono<RateLimitResult> isAllowed(String key, int replenishRate, int burstCapacity, int requestedTokens) {
        List<String> keys = List.of(key);
        List<String> args = Arrays.asList(
                String.valueOf(replenishRate),
                String.valueOf(burstCapacity),
                String.valueOf(requestedTokens),
                String.valueOf(Instant.now().toEpochMilli())
        );

        return redisTemplate.execute(rateLimitScript, keys, args)
                .next()
                .map(result -> {
                    boolean allowed = result.get(0) == 1;
                    long remainingTokens = result.get(1);
                    return new RateLimitResult(allowed, remainingTokens);
                })
                .doOnError(e -> log.error("Rate limit check failed for key {}: {}", key, e.getMessage()))
                .onErrorReturn(new RateLimitResult(true, 0));
    }

    /**
     * Rate limit check result.
     */
    public record RateLimitResult(boolean allowed, long remainingTokens) {
        
        public boolean isAllowed() {
            return allowed;
        }

        public long getRemainingTokens() {
            return remainingTokens;
        }
    }
}
