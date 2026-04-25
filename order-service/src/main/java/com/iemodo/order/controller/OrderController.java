package com.iemodo.order.controller;

import com.iemodo.common.response.PageResponse;
import com.iemodo.common.response.Response;
import com.iemodo.order.domain.OrderStatus;
import com.iemodo.order.dto.CreateOrderRequest;
import com.iemodo.order.dto.OrderDTO;
import com.iemodo.order.dto.OrderTokenResponse;
import com.iemodo.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Order management endpoints.
 *
 * <p>All requests require a valid JWT (enforced at the API Gateway).
 * The gateway injects {@code X-User-ID} and {@code X-TenantID} from the JWT.
 */
@Slf4j
@RestController
@RequestMapping("/oc/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Issue an idempotency token (pre-generated orderNo) for a new order.
     *
     * <pre>
     * POST /oc/api/v1/orders/token
     * X-TenantID: tenant_001
     * </pre>
     *
     * The returned {@code idempotencyKey} must be included in the subsequent
     * {@code POST /orders} request. It expires in 10 minutes.
     */
    @PostMapping("/token")
    public Mono<Response<OrderTokenResponse>> getOrderToken(
            @RequestHeader("X-TenantID") String tenantId) {

        return orderService.generateOrderToken(tenantId)
                .map(Response::success);
    }

    /**
     * Create a new order.
     *
     * <pre>
     * POST /oc/api/v1/orders
     * X-TenantID: tenant_001
     * Authorization: Bearer <token>
     * </pre>
     */
    @PostMapping
    public Mono<Response<OrderDTO>> createOrder(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid CreateOrderRequest request) {

        return orderService.createOrder(request, tenantId)
                .map(Response::success);
    }

    /**
     * Get order by ID.
     */
    @GetMapping("/{orderId}")
    public Mono<Response<OrderDTO>> getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId)
                .map(Response::success);
    }

    /**
     * List orders for a customer (paginated).
     */
    @GetMapping
    public Mono<Response<PageResponse<OrderDTO>>> listOrders(
            @RequestHeader("X-User-ID") Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return orderService.listOrders(userId, page, size)
                .collectList()
                .map(orders -> Response.success(PageResponse.of(orders, orders.size(), page, size)));
    }

    /**
     * GET /oc/api/v1/orders/today-count
     * Admin dashboard — today's order count for a tenant.
     */
    @GetMapping("/today-count")
    public Mono<Response<Long>> todayOrderCount(
            @RequestHeader("X-TenantID") String tenantId) {
        return orderService.countTodayOrders(tenantId)
                .map(Response::success);
    }

    /**
     * Cancel an order (PENDING_PAYMENT or PAID only).
     */
    @PostMapping("/{orderId}/cancel")
    public Mono<Response<OrderDTO>> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-TenantID") String tenantId) {

        return orderService.cancelOrder(orderId, tenantId)
                .map(Response::success);
    }

    /**
     * Confirm payment — internal endpoint called by payment-service callback.
     */
    @PostMapping("/{orderId}/confirm-payment")
    public Mono<Response<OrderDTO>> confirmPayment(@PathVariable Long orderId) {
        return orderService.transitionStatus(orderId, OrderStatus.PAID)
                .map(Response::success);
    }
}
