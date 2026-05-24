package com.iemodo.customer.controller;

import com.iemodo.common.response.Response;
import com.iemodo.customer.dto.AddToCartRequest;
import com.iemodo.customer.dto.CartResponse;
import com.iemodo.customer.dto.UpdateCartItemRequest;
import com.iemodo.customer.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/cc/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Mono<Response<CartResponse>> getCart(
            @RequestHeader(value = "X-Customer-ID", required = false) Long customerId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader("X-TenantID") String tenantId) {
        return cartService.getCart(tenantId, customerId, sessionId)
                .map(Response::success);
    }

    @PostMapping("/items")
    public Mono<Response<CartResponse>> addItem(
            @RequestHeader(value = "X-Customer-ID", required = false) Long customerId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid AddToCartRequest request) {
        return cartService.addItem(tenantId, customerId, sessionId,
                        request.getSkuCode(), request.getQuantity())
                .map(Response::success);
    }

    @PutMapping("/items/{skuCode}")
    public Mono<Response<CartResponse>> updateItem(
            @RequestHeader(value = "X-Customer-ID", required = false) Long customerId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable String skuCode,
            @RequestBody @Valid UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(tenantId, customerId, sessionId,
                        skuCode, request.getQuantity())
                .map(Response::success);
    }

    @DeleteMapping("/items/{skuCode}")
    public Mono<Response<CartResponse>> removeItem(
            @RequestHeader(value = "X-Customer-ID", required = false) Long customerId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable String skuCode) {
        return cartService.removeItem(tenantId, customerId, sessionId, skuCode)
                .map(Response::success);
    }

    @DeleteMapping
    public Mono<Response<Void>> clearCart(
            @RequestHeader(value = "X-Customer-ID", required = false) Long customerId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader("X-TenantID") String tenantId) {
        return cartService.clearCart(tenantId, customerId, sessionId)
                .thenReturn(Response.success());
    }

    @PostMapping("/merge")
    public Mono<Response<CartResponse>> mergeGuestCart(
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestHeader("X-TenantID") String tenantId) {
        return cartService.mergeGuestCart(tenantId, customerId, sessionId)
                .map(Response::success);
    }
}
