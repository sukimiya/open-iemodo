package com.iemodo.pricing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cart pricing request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPricingRequest {

    @NotBlank(message = "Country code is required")
    private String countryCode;

    private String customerSegment;  // VIP, WHOLESALE, etc.

    private String couponCode;

    @NotEmpty(message = "Cart items are required")
    @Valid
    private List<CartItem> items;

    /**
     * Cart item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        @NotBlank(message = "SKU is required")
        private String sku;
        
        private Integer quantity = 1;
    }
}
