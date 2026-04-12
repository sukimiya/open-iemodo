package com.iemodo.marketing.service;

import com.iemodo.marketing.domain.Coupon;
import com.iemodo.marketing.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Coupon service interface
 */
public interface CouponService {

    /**
     * Create a new coupon
     */
    Mono<CouponResponse> createCoupon(CreateCouponRequest request, String tenantId, Long userId);

    /**
     * Get coupon by ID
     */
    Mono<CouponResponse> getCoupon(Long id, String tenantId);

    /**
     * Get coupon by code
     */
    Mono<CouponResponse> getCouponByCode(String code, String tenantId);

    /**
     * List all coupons for tenant
     */
    Flux<CouponResponse> listCoupons(String tenantId);

    /**
     * Update coupon
     */
    Mono<CouponResponse> updateCoupon(Long id, UpdateCouponRequest request, String tenantId);

    /**
     * Delete (soft delete) coupon
     */
    Mono<Void> deleteCoupon(Long id, String tenantId);

    /**
     * Claim a coupon for customer
     */
    Mono<UserCouponResponse> claimCoupon(String couponCode, Long customerId, String tenantId);

    /**
     * Get my coupons
     */
    Flux<UserCouponResponse> getMyCoupons(Long customerId, String tenantId);

    /**
     * Validate coupon for order
     */
    Mono<CouponValidationResult> validateCoupon(ValidateCouponRequest request, String tenantId);

    /**
     * Calculate discount for order
     */
    Mono<DiscountCalculationResult> calculateDiscount(CalculateDiscountRequest request, String tenantId);

    /**
     * Use coupon (mark as used)
     */
    Mono<Void> useCoupon(Long userCouponId, Long orderId, String orderNo, java.math.BigDecimal discountAmount);

    /**
     * Publish (activate) a coupon immediately
     */
    Mono<CouponResponse> publishCoupon(Long id, String tenantId);

    /**
     * Unpublish (deactivate) a coupon
     */
    Mono<CouponResponse> unpublishCoupon(Long id, String tenantId);
}
