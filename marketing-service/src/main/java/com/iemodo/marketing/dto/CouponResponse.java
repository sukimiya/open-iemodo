package com.iemodo.marketing.dto;

import com.iemodo.marketing.domain.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Coupon response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private Long id;
    private String couponCode;
    private String name;
    private String description;
    private Coupon.CouponType type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer maxUses;
    private Integer maxUsesPerUser;
    private Integer usedCount;
    private Coupon.ApplicableScope applicableScope;
    private Instant validFrom;
    private Instant validTo;
    private Boolean isActive;
}
