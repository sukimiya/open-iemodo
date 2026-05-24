package com.iemodo.customer.controller;

import com.iemodo.common.response.PageResponse;
import com.iemodo.common.response.Response;
import com.iemodo.customer.dto.CustomerDTO;
import com.iemodo.customer.dto.UpdateCustomerRequest;
import com.iemodo.customer.service.CustomerService;
import com.iemodo.marketing.dto.UserCouponResponse;
import com.iemodo.marketing.service.CouponService;
import com.iemodo.order.dto.CreateOrderRequest;
import com.iemodo.order.dto.OrderDTO;
import com.iemodo.order.dto.OrderTokenResponse;
import com.iemodo.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/cc/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CouponService couponService;
    private final OrderService orderService;

    // ─── Profile ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    public Mono<Response<CustomerDTO>> getProfile(
            @RequestHeader("X-Customer-ID") Long customerId) {
        return customerService.getById(customerId)
                .map(Response::success);
    }

    @PutMapping("/me")
    public Mono<Response<CustomerDTO>> updateProfile(
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestBody @Valid UpdateCustomerRequest request) {
        return customerService.updateProfile(customerId, request)
                .map(Response::success);
    }

    // ─── Coupons ──────────────────────────────────────────────────────────

    @GetMapping("/me/coupons")
    public Mono<Response<java.util.List<UserCouponResponse>>> getMyCoupons(
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestHeader("X-TenantID") String tenantId) {
        return couponService.getMyCoupons(customerId, tenantId)
                .collectList()
                .map(Response::success);
    }

    @PostMapping("/me/coupons/{code}/claim")
    public Mono<Response<UserCouponResponse>> claimCoupon(
            @PathVariable String code,
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestHeader("X-TenantID") String tenantId) {
        return couponService.claimCoupon(code, customerId, tenantId)
                .map(Response::success);
    }

    // ─── Orders ───────────────────────────────────────────────────────────

    @PostMapping("/me/orders/token")
    public Mono<Response<OrderTokenResponse>> getOrderToken(
            @RequestHeader("X-TenantID") String tenantId) {
        return orderService.generateOrderToken(tenantId)
                .map(Response::success);
    }

    @PostMapping("/me/orders")
    public Mono<Response<OrderDTO>> createOrder(
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid CreateOrderRequest request) {
        request.setCustomerId(customerId);
        return orderService.createOrder(request, tenantId)
                .map(Response::success);
    }

    @GetMapping("/me/orders")
    public Mono<Response<PageResponse<OrderDTO>>> listOrders(
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.listOrders(customerId, page, size)
                .collectList()
                .map(orders -> Response.success(PageResponse.of(orders, orders.size(), page, size)));
    }
}
