package com.iemodo.order.service;

import com.iemodo.common.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("InventoryRedisService")
class InventoryRedisServiceTest {

    @Mock private ReactiveStringRedisTemplate redisTemplate;
    @Mock private ReactiveValueOperations<String, String> valueOps;

    private InventoryRedisService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new InventoryRedisService(redisTemplate);
    }

    // ─── stockKey ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("stockKey should follow stock:{tenantId}:{sku} format")
    void stockKey_shouldHaveCorrectFormat() {
        String key = InventoryRedisService.stockKey("tenant_001", "SKU-ABC");
        assertThat(key).isEqualTo("stock:tenant_001:SKU-ABC");
    }

    // ─── preDeduct ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("preDeduct: should complete when Lua script returns 1 (success)")
    void preDeduct_shouldComplete_whenLuaReturns1() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(1L));

        StepVerifier.create(service.preDeduct("tenant_001", "SKU-001", 2))
                .verifyComplete();
    }

    @Test
    @DisplayName("preDeduct: should throw InsufficientStockException when Lua returns 0")
    void preDeduct_shouldThrow_whenLuaReturns0() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(0L));

        StepVerifier.create(service.preDeduct("tenant_001", "SKU-001", 5))
                .expectError(InsufficientStockException.class)
                .verify();
    }

    @Test
    @DisplayName("preDeduct: should complete (no error) when Lua returns -1 (key missing)")
    void preDeduct_shouldComplete_whenLuaReturnsMinus1() {
        // -1 means stock not initialised in Redis — fallback: allow the order
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(-1L));

        StepVerifier.create(service.preDeduct("tenant_001", "SKU-NEW", 1))
                .verifyComplete();
    }

    // ─── rollback ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rollback: should increment Redis counter by quantity")
    void rollback_shouldIncrementStock() {
        when(valueOps.increment(anyString(), anyLong())).thenReturn(Mono.just(12L));

        StepVerifier.create(service.rollback("tenant_001", "SKU-001", 3))
                .verifyComplete();
    }

    // ─── setStock ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setStock: should set Redis key with string value")
    void setStock_shouldSetRedisKey() {
        when(valueOps.set(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(service.setStock("tenant_001", "SKU-001", 100))
                .verifyComplete();
    }
}
