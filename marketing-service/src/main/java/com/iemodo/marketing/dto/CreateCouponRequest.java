package com.iemodo.marketing.dto;

import com.iemodo.marketing.domain.Coupon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Create coupon request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String couponCode;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Type is required")
    private Coupon.CouponType type;

    @NotNull(message = "Discount value is required")
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer maxUses;
    private Integer maxUsesPerUser;

    private Coupon.ApplicableScope applicableScope;
    private Long[] applicableIds;
    private Long[] excludedIds;

    @NotNull(message = "Valid from is required")
    private Instant validFrom;

    @NotNull(message = "Valid to is required")
    private Instant validTo;

    /**
     * Whether to publish the coupon immediately.
     * Defaults to {@code false} — coupon is created as a draft and goes live
     * either via {@code POST /coupons/{id}/publish} or automatically when
     * {@code valid_from} is reached.
     */
    private Boolean isActive;
}
