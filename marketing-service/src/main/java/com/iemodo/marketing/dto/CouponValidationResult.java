package com.iemodo.marketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Coupon validation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResult {

    private String couponCode;
    private boolean valid;
    private String invalidReason;
}
