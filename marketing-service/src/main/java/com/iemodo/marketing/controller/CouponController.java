package com.iemodo.marketing.controller;

import com.iemodo.common.response.Response;
import com.iemodo.marketing.dto.*;
import com.iemodo.marketing.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Coupon controller
 */
@Slf4j
@RestController
@RequestMapping("/marketing/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ─── Admin Endpoints ────────────────────────────────────────────────────

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Response<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId,
            @RequestHeader(value = "X-UserID", defaultValue = "1") Long userId) {
        
        return couponService.createCoupon(request, tenantId, userId)
                .map(Response::success);
    }

    @GetMapping("/coupons/{id}")
    public Mono<Response<CouponResponse>> getCoupon(
            @PathVariable Long id,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.getCoupon(id, tenantId)
                .map(Response::success);
    }

    @GetMapping("/coupons")
    public Mono<Response<java.util.List<CouponResponse>>> listCoupons(
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.listCoupons(tenantId)
                .collectList()
                .map(Response::success);
    }

    @PutMapping("/coupons/{id}")
    public Mono<Response<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @RequestBody UpdateCouponRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.updateCoupon(id, request, tenantId)
                .map(Response::success);
    }

    @DeleteMapping("/coupons/{id}")
    public Mono<Response<Void>> deleteCoupon(
            @PathVariable Long id,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.deleteCoupon(id, tenantId)
                .thenReturn(Response.success());
    }

    // ─── Customer Endpoints ─────────────────────────────────────────────────

    @PostMapping("/coupons/{code}/claim")
    public Mono<Response<UserCouponResponse>> claimCoupon(
            @PathVariable String code,
            @RequestHeader(value = "X-CustomerID", defaultValue = "1") Long customerId,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.claimCoupon(code, customerId, tenantId)
                .map(Response::success);
    }

    @GetMapping("/my-coupons")
    public Mono<Response<java.util.List<UserCouponResponse>>> getMyCoupons(
            @RequestHeader(value = "X-CustomerID", defaultValue = "1") Long customerId,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.getMyCoupons(customerId, tenantId)
                .collectList()
                .map(Response::success);
    }

    @PostMapping("/coupons/validate")
    public Mono<Response<CouponValidationResult>> validateCoupon(
            @RequestBody ValidateCouponRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.validateCoupon(request, tenantId)
                .map(Response::success);
    }

    @PostMapping("/coupons/calculate")
    public Mono<Response<DiscountCalculationResult>> calculateDiscount(
            @RequestBody CalculateDiscountRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return couponService.calculateDiscount(request, tenantId)
                .map(Response::success);
    }
}
