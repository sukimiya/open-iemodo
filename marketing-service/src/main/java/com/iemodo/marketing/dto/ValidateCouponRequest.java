package com.iemodo.marketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Validate coupon request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String couponCode;

    @NotNull(message = "Order amount is required")
    private BigDecimal orderAmount;
}
