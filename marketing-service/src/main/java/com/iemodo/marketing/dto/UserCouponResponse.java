package com.iemodo.marketing.dto;

import com.iemodo.marketing.domain.Coupon;
import com.iemodo.marketing.domain.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * User coupon response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {

    private Long id;
    private Long couponId;
    private String couponCode;
    private UserCoupon.UserCouponStatus status;
    private Instant validFrom;
    private Instant validTo;
    
    // Coupon details
    private String couponName;
    private Coupon.CouponType couponType;
    private BigDecimal discountValue;
}
