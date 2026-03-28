package com.iemodo.pricing.controller;

import com.iemodo.common.response.Response;
import com.iemodo.pricing.dto.CartPricingRequest;
import com.iemodo.pricing.dto.CartPricingResponse;
import com.iemodo.pricing.dto.PriceDetail;
import com.iemodo.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Pricing controller
 */
@Slf4j
@RestController
@RequestMapping("/pricing/api/v1")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    /**
     * Calculate price for a single SKU
     */
    @PostMapping("/price/calculate")
    public Mono<Response<PriceDetail>> calculatePrice(
            @RequestParam String sku,
            @RequestParam String countryCode,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String customerSegment) {
        
        log.info("Calculating price for SKU: {}, Country: {}, Qty: {}", sku, countryCode, quantity);
        
        return pricingService.calculateFinalPrice(sku, countryCode.toUpperCase(), quantity, customerSegment)
                .map(Response::success);
    }

    /**
     * Calculate cart pricing with multiple items
     */
    @PostMapping("/cart/calculate")
    public Mono<Response<CartPricingResponse>> calculateCartPricing(
            @Valid @RequestBody CartPricingRequest request) {
        
        log.info("Calculating cart pricing for country: {}", request.getCountryCode());
        
        return pricingService.calculateCartPricing(request)
                .map(Response::success);
    }

    /**
     * Convert price between currencies
     */
    @GetMapping("/price/convert")
    public Mono<Response<BigDecimal>> convertPrice(
            @RequestParam BigDecimal price,
            @RequestParam String from,
            @RequestParam String to) {
        
        return pricingService.convertPrice(price, from.toUpperCase(), to.toUpperCase())
                .map(Response::success);
    }
}
