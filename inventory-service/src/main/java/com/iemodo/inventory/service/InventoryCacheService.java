package com.iemodo.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Redis-based inventory cache service for anti-overselling.
 * 
 * <p>Uses Lua scripts for atomic operations to prevent race conditions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCacheService {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String STOCK_KEY_PREFIX = "inv:stock:";
    private static final String RESERVED_KEY_PREFIX = "inv:reserved:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * Lua script for pre-deducting stock.
     * KEYS[1]: stock key
     * ARGV[1]: quantity to deduct
     * Returns: 1 if successful, 0 if insufficient stock, -1 if key not exists
     */
    private static final String PRE_DEDUCT_LUA = """
            local stock = tonumber(redis.call('get', KEYS[1]));
            if stock == nil then return -1; end;
            if stock >= tonumber(ARGV[1]) then
                redis.call('decrby', KEYS[1], ARGV[1]);
                redis.call('expire', KEYS[1], 86400);
                return 1;
            else
                return 0;
            end
            """;

    /**
     * Lua script for releasing reserved stock.
     */
    private static final String RELEASE_LUA = """
            local stock = tonumber(redis.call('get', KEYS[1]));
            if stock == nil then return -1; end;
            redis.call('incrby', KEYS[1], ARGV[1]);
            redis.call('expire', KEYS[1], 86400);
            return 1;
            """;

    private final RedisScript<Long> preDeductScript = RedisScript.of(PRE_DEDUCT_LUA, Long.class);
    private final RedisScript<Long> releaseScript = RedisScript.of(RELEASE_LUA, Long.class);

    /**
     * Pre-deduct stock in Redis (prevent overselling).
     *
     * @param tenantId tenant identifier
     * @param skuId    SKU ID
     * @param quantity quantity to deduct
     * @return true if successful
     */
    public Mono<Boolean> preDeduct(String tenantId, Long skuId, int quantity) {
        String key = buildStockKey(tenantId, skuId);
        return redisTemplate.execute(preDeductScript, List.of(key), List.of(String.valueOf(quantity)))
                .next()
                .map(result -> {
                    if (result == 1) {
                        log.debug("Pre-deducted {} stock for SKU {} in tenant {}", quantity, skuId, tenantId);
                        return true;
                    } else if (result == 0) {
                        log.warn("Insufficient stock for SKU {} in tenant {}", skuId, tenantId);
                        return false;
                    } else {
                        log.error("Stock key not found for SKU {} in tenant {}", skuId, tenantId);
                        return false;
                    }
                })
                .doOnError(e -> log.error("Error pre-deducting stock: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Confirm stock deduction (after order confirmed).
     *
     * @param tenantId tenant identifier
     * @param skuId    SKU ID
     * @param quantity quantity to confirm
     * @param orderNo  order number for tracking
     * @return true if successful
     */
    public Mono<Boolean> confirmDeduct(String tenantId, Long skuId, int quantity, String orderNo) {
        // In real implementation, this would update database and remove reservation
        log.info("Confirmed stock deduction: SKU={}, Qty={}, Order={}", skuId, quantity, orderNo);
        return Mono.just(true);
    }

    /**
     * Release reserved stock (order cancelled).
     *
     * @param tenantId tenant identifier
     * @param skuId    SKU ID
     * @param quantity quantity to release
     * @return true if successful
     */
    public Mono<Boolean> releaseStock(String tenantId, Long skuId, int quantity) {
        String key = buildStockKey(tenantId, skuId);
        return redisTemplate.execute(releaseScript, List.of(key), List.of(String.valueOf(quantity)))
                .next()
                .map(result -> result == 1)
                .doOnSuccess(success -> {
                    if (success) {
                        log.debug("Released {} stock for SKU {} in tenant {}", quantity, skuId, tenantId);
                    }
                })
                .doOnError(e -> log.error("Error releasing stock: {}", e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Get current stock from cache.
     */
    public Mono<Integer> getStock(String tenantId, Long skuId) {
        String key = buildStockKey(tenantId, skuId);
        return redisTemplate.opsForValue().get(key)
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .defaultIfEmpty(0);
    }

    /**
     * Set stock in cache (called when inventory updates).
     */
    public Mono<Boolean> setStock(String tenantId, Long skuId, int quantity) {
        String key = buildStockKey(tenantId, skuId);
        return redisTemplate.opsForValue().set(key, String.valueOf(quantity), CACHE_TTL);
    }

    /**
     * Increment stock in cache (for inbound).
     */
    public Mono<Long> incrementStock(String tenantId, Long skuId, int delta) {
        String key = buildStockKey(tenantId, skuId);
        return redisTemplate.opsForValue().increment(key, delta)
                .flatMap(newVal -> redisTemplate.expire(key, CACHE_TTL).thenReturn(newVal));
    }

    private String buildStockKey(String tenantId, Long skuId) {
        return STOCK_KEY_PREFIX + tenantId + ":" + skuId;
    }
}
