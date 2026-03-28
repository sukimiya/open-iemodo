package com.iemodo.marketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Discount calculation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCalculationResult {

    private String couponCode;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
