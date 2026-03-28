package com.iemodo.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed price breakdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDetail {

    private String currency;
    private BigDecimal basePrice;
    private List<PriceComponent> components;
    private BigDecimal subtotal;      // Before discounts
    private BigDecimal discounts;     // Total discounts
    private BigDecimal taxes;         // Calculated separately
    private BigDecimal finalPrice;    // After discounts, before tax
}
