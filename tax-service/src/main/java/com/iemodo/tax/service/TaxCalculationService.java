package com.iemodo.tax.service;

import com.iemodo.tax.domain.TaxRate;
import com.iemodo.tax.dto.TaxCalculationRequest;
import com.iemodo.tax.dto.TaxCalculationResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Tax calculation service interface
 */
public interface TaxCalculationService {

    /**
     * Calculate tax for an order
     */
    Mono<TaxCalculationResponse> calculateOrderTax(TaxCalculationRequest request);

    /**
     * Calculate VAT (EU)
     */
    Mono<BigDecimal> calculateVAT(BigDecimal amount, String countryCode, String category);

    /**
     * Calculate GST (AU, NZ, SG, CA)
     */
    Mono<BigDecimal> calculateGST(BigDecimal amount, String countryCode, String category);

    /**
     * Calculate US Sales Tax
     */
    Mono<BigDecimal> calculateSalesTax(BigDecimal amount, String countryCode, 
                                        String regionCode, String postalCode, String category);

    /**
     * Calculate Japan Consumption Tax
     */
    Mono<BigDecimal> calculateConsumptionTax(BigDecimal amount, String category);

    /**
     * Determine tax system for a country
     */
    Mono<TaxRate.TaxType> determineTaxSystem(String countryCode);
}
