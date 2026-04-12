package com.iemodo.order.service;

import com.iemodo.common.exception.InsufficientStockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    /**
     * Lua script for idempotency state check.
     *
     * <p>Return codes:
     * <ul>
     *   <li>{@code  1} — PENDING (token exists, proceed with deduction)</li>
     *   <li>{@code  2} — SUCCESS (already processed, duplicate request)</li>
     *   <li>{@code -1} — NOT_FOUND (token expired or never registered)</li>
     * </ul>
     */
    private static final String IDEMPOTENCY_CHECK_LUA = """
            local state = redis.call('get', KEYS[1])
            if not state then
                return -1
            end
            if state == 'SUCCESS' then
                return 2
            end
            return 1
            """;

    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT;
    private static final DefaultRedisScript<Long> IDEMPOTENCY_SCRIPT;

    static {
        DEDUCT_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_SCRIPT.setScriptText(DEDUCT_LUA);
        DEDUCT_SCRIPT.setResultType(Long.class);

        IDEMPOTENCY_SCRIPT = new DefaultRedisScript<>();
        IDEMPOTENCY_SCRIPT.setScriptText(IDEMPOTENCY_CHECK_LUA);
        IDEMPOTENCY_SCRIPT.setResultType(Long.class);
    }

    private final ReactiveStringRedisTemplate redisTemplate;

    public InventoryRedisService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ─── Key builders ─────────────────────────────────────────────────────

    public static String stockKey(String tenantId, String sku) {
        return "stock:" + tenantId + ":" + sku;
    }

    public static String deductKey(String tenantId, String orderNo) {
        return "deduct:" + tenantId + ":" + orderNo;
    }

    // ─── Idempotency token ────────────────────────────────────────────────

    /**
     * Register a fresh idempotency token with PENDING state (10 min TTL).
     * Call this when issuing a token to the client before order creation.
     */
    public Mono<Void> registerToken(String tenantId, String orderNo) {
        return redisTemplate.opsForValue()
                .set(deductKey(tenantId, orderNo), "PENDING", Duration.ofMinutes(10))
                .then();
    }

    /**
     * Check the idempotency state for an order.
     *
     * @return {@code 1} = PENDING (proceed), {@code 2} = SUCCESS (duplicate),
     *         {@code -1} = NOT_FOUND (token expired or invalid)
     */
    public Mono<Long> checkIdempotency(String tenantId, String orderNo) {
        return redisTemplate.execute(IDEMPOTENCY_SCRIPT, List.of(deductKey(tenantId, orderNo)), List.of())
                .next()
                .defaultIfEmpty(-1L);
    }

    /**
     * Mark the idempotency token as successfully processed (24 h TTL).
     * Call this after all inventory deductions and order persistence succeed.
     */
    public Mono<Void> markSuccess(String tenantId, String orderNo) {
        return redisTemplate.opsForValue()
                .set(deductKey(tenantId, orderNo), "SUCCESS", Duration.ofHours(24))
                .then();
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
