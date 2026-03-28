package com.iemodo.pricing.service;

import com.iemodo.pricing.dto.CartPricingRequest;
import com.iemodo.pricing.dto.CartPricingResponse;
import com.iemodo.pricing.dto.PriceDetail;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * International pricing service interface
 */
public interface PricingService {

    /**
     * Calculate final price for a single SKU
     * 
     * @param sku Product SKU
     * @param countryCode ISO country code
     * @param quantity Purchase quantity
     * @param customerSegment Customer segment (VIP, WHOLESALE, etc.)
     * @return Price detail with breakdown
     */
    Mono<PriceDetail> calculateFinalPrice(String sku, String countryCode, 
                                           Integer quantity, String customerSegment);

    /**
     * Calculate cart pricing with multiple items
     * 
     * @param request Cart pricing request
     * @return Cart pricing response
     */
    Mono<CartPricingResponse> calculateCartPricing(CartPricingRequest request);

    /**
     * Convert price between currencies
     */
    Mono<BigDecimal> convertPrice(BigDecimal price, String fromCurrency, String toCurrency);

    /**
     * Apply quantity discount
     */
    Mono<BigDecimal> applyQuantityDiscount(BigDecimal price, String sku, 
                                            String countryCode, Integer quantity);

    /**
     * Apply segment discount
     */
    Mono<BigDecimal> applySegmentDiscount(BigDecimal price, String sku, String customerSegment);
}
