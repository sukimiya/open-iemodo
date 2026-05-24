package com.iemodo.customer.service;

import com.iemodo.customer.dto.AddToCartRequest;
import com.iemodo.customer.dto.CartItemDTO;
import com.iemodo.customer.dto.CartResponse;
import com.iemodo.customer.dto.UpdateCartItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Duration CART_TTL = Duration.ofDays(7);
    private static final String CUSTOMER_CART_PREFIX = "cart:";
    private static final String GUEST_PREFIX = "guest:";

    private final ReactiveStringRedisTemplate redisTemplate;

    // ─── Key helpers ───────────────────────────────────────────────────────────

    private String customerCartKey(String tenantId, long customerId) {
        return CUSTOMER_CART_PREFIX + tenantId + ":" + customerId;
    }

    private String guestCartKey(String tenantId, String sessionId) {
        return CUSTOMER_CART_PREFIX + tenantId + ":" + GUEST_PREFIX + sessionId;
    }

    private String resolveKey(String tenantId, Long customerId, String sessionId) {
        if (customerId != null) {
            return customerCartKey(tenantId, customerId);
        }
        return guestCartKey(tenantId, sessionId);
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    public Mono<CartResponse> getCart(String tenantId, Long customerId, String sessionId) {
        String key = resolveKey(tenantId, customerId, sessionId);
        return redisTemplate.<String, String>opsForHash().entries(key)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(entries -> buildResponse(key, entries));
    }

    public Mono<CartResponse> addItem(String tenantId, Long customerId, String sessionId,
                                       String skuCode, int quantity) {
        String key = resolveKey(tenantId, customerId, sessionId);
        return redisTemplate.<String, String>opsForHash()
                .put(key, skuCode, String.valueOf(quantity))
                .then(redisTemplate.expire(key, CART_TTL))
                .then(getCart(tenantId, customerId, sessionId));
    }

    public Mono<CartResponse> updateItemQuantity(String tenantId, Long customerId, String sessionId,
                                                  String skuCode, int quantity) {
        String key = resolveKey(tenantId, customerId, sessionId);
        if (quantity <= 0) {
            return redisTemplate.<String, String>opsForHash()
                    .remove(key, skuCode)
                    .then(redisTemplate.expire(key, CART_TTL))
                    .then(getCart(tenantId, customerId, sessionId));
        }
        return redisTemplate.<String, String>opsForHash()
                .put(key, skuCode, String.valueOf(quantity))
                .then(redisTemplate.expire(key, CART_TTL))
                .then(getCart(tenantId, customerId, sessionId));
    }

    public Mono<CartResponse> removeItem(String tenantId, Long customerId, String sessionId,
                                          String skuCode) {
        String key = resolveKey(tenantId, customerId, sessionId);
        return redisTemplate.<String, String>opsForHash()
                .remove(key, skuCode)
                .then(redisTemplate.expire(key, CART_TTL))
                .then(getCart(tenantId, customerId, sessionId));
    }

    public Mono<Void> clearCart(String tenantId, Long customerId, String sessionId) {
        String key = resolveKey(tenantId, customerId, sessionId);
        return redisTemplate.delete(key).then();
    }

    public Mono<CartResponse> mergeGuestCart(String tenantId, Long customerId, String sessionId) {
        String guestKey = guestCartKey(tenantId, sessionId);
        String customerKey = customerCartKey(tenantId, customerId);

        return redisTemplate.<String, String>opsForHash().entries(guestKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(guestItems -> {
                    if (guestItems.isEmpty()) {
                        return getCart(tenantId, customerId, null);
                    }

                    return redisTemplate.<String, String>opsForHash().entries(customerKey)
                            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                            .flatMap(customerItems -> {
                                // Merge: keep the larger quantity for overlapping SKUs
                                for (var entry : guestItems.entrySet()) {
                                    String sku = entry.getKey();
                                    int guestQty = Integer.parseInt(entry.getValue());
                                    String existing = customerItems.get(sku);
                                    int customerQty = existing != null ? Integer.parseInt(existing) : 0;
                                    int mergedQty = Math.max(guestQty, customerQty);
                                    customerItems.put(sku, String.valueOf(mergedQty));
                                }
                                // Write merged items to customer cart
                                return redisTemplate.<String, String>opsForHash()
                                        .putAll(customerKey, customerItems)
                                        .then(redisTemplate.expire(customerKey, CART_TTL))
                                        .then(redisTemplate.delete(guestKey))
                                        .then(buildResponse(customerKey, customerItems));
                            });
                });
    }

    public Mono<Long> getCartItemCount(String tenantId, Long customerId, String sessionId) {
        String key = resolveKey(tenantId, customerId, sessionId);
        return redisTemplate.<String, String>opsForHash().size(key);
    }

    // ─── Internal ──────────────────────────────────────────────────────────────

    private Mono<CartResponse> buildResponse(String key, Map<String, String> entries) {
        String cartId = key.substring(CUSTOMER_CART_PREFIX.length());

        if (entries.isEmpty()) {
            return Mono.just(CartResponse.builder()
                    .cartId(cartId)
                    .items(java.util.List.of())
                    .totalItems(0)
                    .uniqueItems(0)
                    .build());
        }

        var items = entries.entrySet().stream()
                .map(e -> CartItemDTO.builder()
                        .skuCode(e.getKey())
                        .quantity(Integer.parseInt(e.getValue()))
                        .build())
                .collect(Collectors.toList());

        int totalItems = items.stream().mapToInt(CartItemDTO::getQuantity).sum();

        return Mono.just(CartResponse.builder()
                .cartId(cartId)
                .items(items)
                .totalItems(totalItems)
                .uniqueItems(items.size())
                .build());
    }
}
