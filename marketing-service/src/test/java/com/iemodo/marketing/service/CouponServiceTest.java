package com.iemodo.marketing.service;

import com.iemodo.marketing.domain.Coupon;
import com.iemodo.marketing.dto.*;
import com.iemodo.marketing.repository.CouponRepository;
import com.iemodo.marketing.repository.UserCouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Coupon service unit tests
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    void calculateDiscount_Percentage() {
        Coupon coupon = Coupon.builder()
                .couponCode("SAVE20")
                .type(Coupon.CouponType.PERCENTAGE)
                .discountValue(new BigDecimal("0.20"))
                .maxDiscountAmount(new BigDecimal("50.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .isActive(true)
                .validFrom(Instant.now().minusSeconds(3600))
                .validTo(Instant.now().plusSeconds(3600))
                .build();

        when(couponRepository.findByCouponCodeAndTenantIdAndIsValid(anyString(), anyString()))
                .thenReturn(Mono.just(coupon));

        CalculateDiscountRequest request = new CalculateDiscountRequest();
        request.setCouponCode("SAVE20");
        request.setOrderAmount(new BigDecimal("100.00"));

        StepVerifier.create(couponService.calculateDiscount(request, "tenant_001"))
                .expectNextMatches(result -> 
                        result.getDiscountAmount().compareTo(new BigDecimal("20.00")) == 0)
                .verifyComplete();
    }

    @Test
    void calculateDiscount_FixedAmount() {
        Coupon coupon = Coupon.builder()
                .couponCode("OFF10")
                .type(Coupon.CouponType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("10.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .isActive(true)
                .validFrom(Instant.now().minusSeconds(3600))
                .validTo(Instant.now().plusSeconds(3600))
                .build();

        when(couponRepository.findByCouponCodeAndTenantIdAndIsValid(anyString(), anyString()))
                .thenReturn(Mono.just(coupon));

        CalculateDiscountRequest request = new CalculateDiscountRequest();
        request.setCouponCode("OFF10");
        request.setOrderAmount(new BigDecimal("100.00"));

        StepVerifier.create(couponService.calculateDiscount(request, "tenant_001"))
                .expectNextMatches(result -> 
                        result.getDiscountAmount().compareTo(new BigDecimal("10.00")) == 0)
                .verifyComplete();
    }

    @Test
    void validateCoupon_Success() {
        Coupon coupon = Coupon.builder()
                .couponCode("VALID")
                .type(Coupon.CouponType.PERCENTAGE)
                .discountValue(new BigDecimal("0.10"))
                .minOrderAmount(new BigDecimal("50.00"))
                .isActive(true)
                .validFrom(Instant.now().minusSeconds(3600))
                .validTo(Instant.now().plusSeconds(3600))
                .build();

        when(couponRepository.findByCouponCodeAndTenantIdAndIsValid(anyString(), anyString()))
                .thenReturn(Mono.just(coupon));

        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCouponCode("VALID");
        request.setOrderAmount(new BigDecimal("100.00"));

        StepVerifier.create(couponService.validateCoupon(request, "tenant_001"))
                .expectNextMatches(result -> result.isValid())
                .verifyComplete();
    }

    @Test
    void validateCoupon_BelowMinOrder() {
        Coupon coupon = Coupon.builder()
                .couponCode("VALID")
                .type(Coupon.CouponType.PERCENTAGE)
                .discountValue(new BigDecimal("0.10"))
                .minOrderAmount(new BigDecimal("100.00"))
                .isActive(true)
                .validFrom(Instant.now().minusSeconds(3600))
                .validTo(Instant.now().plusSeconds(3600))
                .build();

        when(couponRepository.findByCouponCodeAndTenantIdAndIsValid(anyString(), anyString()))
                .thenReturn(Mono.just(coupon));

        ValidateCouponRequest request = new ValidateCouponRequest();
        request.setCouponCode("VALID");
        request.setOrderAmount(new BigDecimal("50.00"));

        StepVerifier.create(couponService.validateCoupon(request, "tenant_001"))
                .expectNextMatches(result -> !result.isValid())
                .verifyComplete();
    }
}
