package com.iemodo.tax.controller;

import com.iemodo.common.response.Response;
import com.iemodo.tax.domain.TaxRate;
import com.iemodo.tax.dto.TaxCalculationRequest;
import com.iemodo.tax.dto.TaxCalculationResponse;
import com.iemodo.tax.repository.TaxRateRepository;
import com.iemodo.tax.service.TaxCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Tax controller
 */
@Slf4j
@RestController
@RequestMapping("/tax/api/v1")
@RequiredArgsConstructor
public class TaxController {

    private final TaxCalculationService taxCalculationService;
    private final TaxRateRepository taxRateRepository;

    /**
     * Calculate tax for an order
     */
    @PostMapping("/calculate")
    public Mono<Response<TaxCalculationResponse>> calculateTax(
            @Valid @RequestBody TaxCalculationRequest request) {
        
        log.info("Calculating tax for country: {}", request.getCountryCode());
        
        return taxCalculationService.calculateOrderTax(request)
                .map(Response::success);
    }

    /**
     * Get tax rates for a country
     */
    @GetMapping("/rates/{countryCode}")
    public Mono<Response<Flux<TaxRate>>> getTaxRates(@PathVariable String countryCode) {
        
        return Mono.just(Response.success(
                taxRateRepository.findByCountryCode(countryCode.toUpperCase())));
    }

    /**
     * Calculate simple tax
     */
    @GetMapping("/calculate/simple")
    public Mono<Response<BigDecimal>> calculateSimpleTax(
            @RequestParam BigDecimal amount,
            @RequestParam String countryCode,
            @RequestParam(required = false) String category) {
        
        return taxCalculationService.calculateVAT(amount, countryCode.toUpperCase(), category)
                .map(Response::success);
    }

    /**
     * Get supported tax systems
     */
    @GetMapping("/systems/{countryCode}")
    public Mono<Response<String>> getTaxSystem(@PathVariable String countryCode) {
        
        return taxCalculationService.determineTaxSystem(countryCode.toUpperCase())
                .map(TaxRate.TaxType::name)
                .map(Response::success);
    }
}
