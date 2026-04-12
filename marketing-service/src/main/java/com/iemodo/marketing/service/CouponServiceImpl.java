package com.iemodo.marketing.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.marketing.domain.Coupon;
import com.iemodo.marketing.domain.UserCoupon;
import com.iemodo.marketing.dto.*;
import com.iemodo.marketing.repository.CouponRepository;
import com.iemodo.marketing.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;


/**
 * Coupon service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    @Transactional
    public Mono<CouponResponse> createCoupon(CreateCouponRequest request, String tenantId, Long userId) {
        // Check if coupon code already exists
        return couponRepository.findByCouponCodeAndTenantId(request.getCouponCode(), tenantId)
                .flatMap(existing -> Mono.error(new BusinessException(
                        ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Coupon code already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    Coupon coupon = Coupon.builder()
                            .couponCode(request.getCouponCode())
                            .name(request.getName())
                            .description(request.getDescription())
                            .type(request.getType())
                            .discountValue(request.getDiscountValue())
                            .maxDiscountAmount(request.getMaxDiscountAmount())
                            .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                            .maxUses(request.getMaxUses())
                            .maxUsesPerUser(request.getMaxUsesPerUser() != null ? request.getMaxUsesPerUser() : 1)
                            .usedCount(0)
                            .applicableScope(request.getApplicableScope() != null ? request.getApplicableScope() : Coupon.ApplicableScope.ALL)
                            .applicableIds(request.getApplicableIds())
                            .excludedIds(request.getExcludedIds())
                            .validFrom(request.getValidFrom())
                            .validTo(request.getValidTo())
                            .isActive(Boolean.TRUE.equals(request.getIsActive()))
                            .tenantId(tenantId)
                            .build();
                    
                    return couponRepository.save(coupon)
                            .map(this::mapToResponse);
                }))
                .cast(CouponResponse.class);
    }

    @Override
    public Mono<CouponResponse> getCoupon(Long id, String tenantId) {
        return couponRepository.findById(id)
                .filter(coupon -> tenantId.equals(coupon.getTenantId()))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .map(this::mapToResponse);
    }

    @Override
    public Mono<CouponResponse> getCouponByCode(String code, String tenantId) {
        return couponRepository.findByCouponCodeAndTenantIdAndIsValid(code, tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .map(this::mapToResponse);
    }

    @Override
    public Flux<CouponResponse> listCoupons(String tenantId) {
        return couponRepository.findByTenantIdAndIsValid(tenantId)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public Mono<CouponResponse> updateCoupon(Long id, UpdateCouponRequest request, String tenantId) {
        return couponRepository.findById(id)
                .filter(coupon -> tenantId.equals(coupon.getTenantId()))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .flatMap(coupon -> {
                    if (request.getName() != null) coupon.setName(request.getName());
                    if (request.getDescription() != null) coupon.setDescription(request.getDescription());
                    if (request.getIsActive() != null) coupon.setIsActive(request.getIsActive());
                    if (request.getValidTo() != null) coupon.setValidTo(request.getValidTo());
                    
                    return couponRepository.save(coupon);
                })
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public Mono<Void> deleteCoupon(Long id, String tenantId) {
        return couponRepository.findById(id)
                .filter(coupon -> tenantId.equals(coupon.getTenantId()))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .flatMap(coupon -> {
                    coupon.setIsValid(0);
                    return couponRepository.save(coupon);
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<UserCouponResponse> claimCoupon(String couponCode, Long customerId, String tenantId) {
        return couponRepository.findByCouponCodeAndTenantIdAndIsValid(couponCode, tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .flatMap(coupon -> {
                    // Check if coupon is valid
                    if (!coupon.isValid()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Coupon is not valid"));
                    }
                    
                    // Check if user already claimed this coupon
                    return userCouponRepository.findByCustomerIdAndCouponIdAndIsValid(customerId, coupon.getId())
                            .flatMap(existing -> Mono.error(new BusinessException(
                                    ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "You already claimed this coupon")))
                            .switchIfEmpty(Mono.defer(() -> {
                                UserCoupon userCoupon = UserCoupon.builder()
                                        .customerId(customerId)
                                        .couponId(coupon.getId())
                                        .couponCode(coupon.getCouponCode())
                                        .couponStatus(UserCoupon.UserCouponStatus.UNUSED)
                                        .validFrom(coupon.getValidFrom())
                                        .validTo(coupon.getValidTo())
                                        .tenantId(tenantId)
                                        .build();
                                
                                return userCouponRepository.save(userCoupon)
                                        .map(uc -> mapToUserCouponResponse(uc, coupon));
                            }))
                            .cast(UserCouponResponse.class);
                });
    }

    @Override
    public Flux<UserCouponResponse> getMyCoupons(Long customerId, String tenantId) {
        return userCouponRepository.findByCustomerIdAndTenantIdAndIsValid(customerId, tenantId)
                .flatMap(userCoupon -> 
                        couponRepository.findById(userCoupon.getCouponId())
                                .map(coupon -> mapToUserCouponResponse(userCoupon, coupon))
                                .defaultIfEmpty(mapToUserCouponResponse(userCoupon, null))
                );
    }

    @Override
    public Mono<CouponValidationResult> validateCoupon(ValidateCouponRequest request, String tenantId) {
        return couponRepository.findByCouponCodeAndTenantIdAndIsValid(request.getCouponCode(), tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .flatMap(coupon -> {
                    CouponValidationResult result = new CouponValidationResult();
                    result.setCouponCode(coupon.getCouponCode());
                    result.setValid(coupon.isValid());
                    
                    if (!coupon.isValid()) {
                        result.setInvalidReason("Coupon is not active or expired");
                        return Mono.just(result);
                    }
                    
                    // Check minimum order amount
                    if (coupon.getMinOrderAmount() != null && 
                            request.getOrderAmount().compareTo(coupon.getMinOrderAmount()) < 0) {
                        result.setValid(false);
                        result.setInvalidReason("Order amount does not meet minimum requirement");
                        return Mono.just(result);
                    }
                    
                    result.setValid(true);
                    return Mono.just(result);
                });
    }

    @Override
    public Mono<DiscountCalculationResult> calculateDiscount(CalculateDiscountRequest request, String tenantId) {
        return couponRepository.findByCouponCodeAndTenantIdAndIsValid(request.getCouponCode(), tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .map(coupon -> {
                    BigDecimal discount = coupon.calculateDiscount(request.getOrderAmount());
                    
                    DiscountCalculationResult result = new DiscountCalculationResult();
                    result.setCouponCode(coupon.getCouponCode());
                    result.setOrderAmount(request.getOrderAmount());
                    result.setDiscountAmount(discount);
                    result.setFinalAmount(request.getOrderAmount().subtract(discount));
                    
                    return result;
                });
    }

    @Override
    @Transactional
    public Mono<Void> useCoupon(Long userCouponId, Long orderId, String orderNo, BigDecimal discountAmount) {
        return userCouponRepository.findById(userCouponId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "User coupon not found")))
                .flatMap(userCoupon -> {
                    if (!userCoupon.isValid()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Coupon is not valid for use"));
                    }
                    
                    userCoupon.markAsUsed(orderId, orderNo, discountAmount);
                    
                    return userCouponRepository.save(userCoupon)
                            .flatMap(uc -> {
                                // Increment coupon used count
                                return couponRepository.findById(uc.getCouponId())
                                        .flatMap(coupon -> {
                                            coupon.incrementUsedCount();
                                            return couponRepository.save(coupon);
                                        });
                            });
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<CouponResponse> publishCoupon(Long id, String tenantId) {
        return couponRepository.findById(id)
                .filter(coupon -> tenantId.equals(coupon.getTenantId()))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .flatMap(coupon -> {
                    coupon.setIsActive(true);
                    return couponRepository.save(coupon);
                })
                .map(this::mapToResponse)
                .doOnSuccess(r -> log.info("Published coupon: {}", id));
    }

    @Override
    @Transactional
    public Mono<CouponResponse> unpublishCoupon(Long id, String tenantId) {
        return couponRepository.findById(id)
                .filter(coupon -> tenantId.equals(coupon.getTenantId()))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Coupon not found")))
                .flatMap(coupon -> {
                    coupon.setIsActive(false);
                    return couponRepository.save(coupon);
                })
                .map(this::mapToResponse)
                .doOnSuccess(r -> log.info("Unpublished coupon: {}", id));
    }

    // ─── Helper methods ───────────────────────────────────────────────────

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxUses(coupon.getMaxUses())
                .maxUsesPerUser(coupon.getMaxUsesPerUser())
                .usedCount(coupon.getUsedCount())
                .applicableScope(coupon.getApplicableScope())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .isActive(coupon.getIsActive())
                .build();
    }

    private UserCouponResponse mapToUserCouponResponse(UserCoupon userCoupon, Coupon coupon) {
        return UserCouponResponse.builder()
                .id(userCoupon.getId())
                .couponId(userCoupon.getCouponId())
                .couponCode(userCoupon.getCouponCode())
                .status(userCoupon.getCouponStatus())
                .validFrom(userCoupon.getValidFrom())
                .validTo(userCoupon.getValidTo())
                .couponName(coupon != null ? coupon.getName() : null)
                .couponType(coupon != null ? coupon.getType() : null)
                .discountValue(coupon != null ? coupon.getDiscountValue() : null)
                .build();
    }
}
