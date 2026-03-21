package com.iemodo.order.service;

import com.iemodo.common.exception.InsufficientStockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Manages inventory pre-deduction via Redis Lua scripts.
 *
 * <p>The Lua script is atomic — it prevents race conditions (overselling)
 * without requiring distributed locks. The Redis key format is:
 * {@code stock:{tenantId}:{sku}}
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Order creation calls {@link #preDeduct} to atomically decrement Redis stock.</li>
 *   <li>If Redis stock is insufficient → throws {@link InsufficientStockException}.</li>
 *   <li>If key doesn't exist (stock not initialised) → returns {@code -1}
 *       and the caller decides whether to proceed or reject.</li>
 *   <li>On order cancellation, {@link #rollback} increments the Redis counter.</li>
 * </ol>
 */
@Slf4j
@Service
public class InventoryRedisService {

    /**
     * Lua script for atomic inventory deduction.
     *
     * <p>Return codes:
     * <ul>
     *   <li>{@code 1}  — success, stock decremented</li>
     *   <li>{@code 0}  — insufficient stock</li>
     *   <li>{@code -1} — key not found (stock not initialised)</li>
     * </ul>
     */
    private static final String DEDUCT_LUA = """
            local stock = tonumber(redis.call('get', KEYS[1]))
            if stock == nil then
                return -1
            end
            if stock >= tonumber(ARGV[1]) then
                redis.call('decrby', KEYS[1], ARGV[1])
                return 1
            else
                return 0
            end
            """;

    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT;

    static {
        DEDUCT_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_SCRIPT.setScriptText(DEDUCT_LUA);
        DEDUCT_SCRIPT.setResultType(Long.class);
    }

    private final ReactiveStringRedisTemplate redisTemplate;

    public InventoryRedisService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ─── Stock key builder ────────────────────────────────────────────────

    public static String stockKey(String tenantId, String sku) {
        return "stock:" + tenantId + ":" + sku;
    }

    // ─── Pre-deduct ───────────────────────────────────────────────────────

    /**
     * Atomically pre-deduct {@code quantity} units from the Redis stock counter.
     *
     * @param tenantId the tenant context
     * @param sku      the product SKU
     * @param quantity the quantity to reserve
     * @return {@link Mono#empty()} on success
     * @throws InsufficientStockException if stock is insufficient (code 0)
     */
    public Mono<Void> preDeduct(String tenantId, String sku, int quantity) {
        String key = stockKey(tenantId, sku);

        return redisTemplate.execute(DEDUCT_SCRIPT, List.of(key), List.of(String.valueOf(quantity)))
                .next()
                .flatMap(result -> {
                    if (result == 1L) {
                        log.debug("Pre-deducted {} units of sku={} tenant={}", quantity, sku, tenantId);
                        return Mono.<Void>empty();
                    } else if (result == 0L) {
                        log.warn("Insufficient stock sku={} tenant={} requested={}", sku, tenantId, quantity);
                        return Mono.error(new InsufficientStockException(sku));
                    } else {
                        // -1: key not found — treat as stock initialisation not done,
                        // allow order to proceed (DB will be the truth)
                        log.warn("Stock key not found in Redis for sku={} tenant={} — skipping Redis check",
                                sku, tenantId);
                        return Mono.<Void>empty();
                    }
                });
    }

    // ─── Rollback ─────────────────────────────────────────────────────────

    /**
     * Roll back a pre-deduction (e.g. on order cancellation).
     *
     * @param tenantId the tenant context
     * @param sku      the product SKU
     * @param quantity the quantity to return
     * @return {@link Mono#empty()} always (best-effort, non-blocking)
     */
    public Mono<Void> rollback(String tenantId, String sku, int quantity) {
        String key = stockKey(tenantId, sku);
        return redisTemplate.opsForValue()
                .increment(key, quantity)
                .doOnSuccess(newStock ->
                        log.debug("Rolled back {} units of sku={} tenant={} newStock={}",
                                quantity, sku, tenantId, newStock))
                .then();
    }

    /**
     * Initialise (or overwrite) the Redis stock counter.
     * Called by product-service when stock is set/updated.
     */
    public Mono<Void> setStock(String tenantId, String sku, long stock) {
        return redisTemplate.opsForValue()
                .set(stockKey(tenantId, sku), String.valueOf(stock))
                .then();
    }
}
