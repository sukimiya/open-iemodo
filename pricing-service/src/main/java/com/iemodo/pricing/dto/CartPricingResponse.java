package com.iemodo.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart pricing response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPricingResponse {

    private String countryCode;
    private String currency;
    private String customerSegment;
    
    private List<CartItemResult> items;
    
    private BigDecimal subtotal;
    private BigDecimal discounts;
    private BigDecimal taxes;
    private BigDecimal shipping;
    private BigDecimal finalTotal;

    /**
     * Cart item result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResult {
        private String sku;
        private Integer quantity;
        private PriceDetail priceDetail;
    }
}
