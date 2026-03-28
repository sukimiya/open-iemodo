package com.iemodo.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Individual price component (markup, discount, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponent {

    private String type;        // BASE_PRICE, REGIONAL_MARKUP, QUANTITY_DISCOUNT, etc.
    private BigDecimal amount;  // Positive = charge, Negative = discount
    private String description;
}
