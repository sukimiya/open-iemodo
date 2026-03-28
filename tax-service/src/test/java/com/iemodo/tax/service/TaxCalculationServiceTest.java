package com.iemodo.tax.service;

import com.iemodo.tax.domain.TaxRate;
import com.iemodo.tax.dto.TaxCalculationRequest;
import com.iemodo.tax.dto.TaxCalculationResponse;
import com.iemodo.tax.repository.TaxExemptionRepository;
import com.iemodo.tax.repository.TaxRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tax calculation service unit tests
 */
@ExtendWith(MockitoExtension.class)
class TaxCalculationServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxExemptionRepository taxExemptionRepository;

    @InjectMocks
    private TaxCalculationServiceImpl taxCalculationService;

    @Test
    void calculateVAT_Success() {
        TaxRate rate = TaxRate.builder()
                .countryCode("DE")
                .taxType(TaxRate.TaxType.VAT)
                .taxCategory("STANDARD")
                .rate(new BigDecimal("0.19"))
                .isActive(true)
                .build();

        when(taxRateRepository.findByCountryAndCategory("DE", "STANDARD"))
                .thenReturn(Flux.just(rate));

        StepVerifier.create(taxCalculationService.calculateVAT(
                        new BigDecimal("100.00"), "DE", "STANDARD"))
                .expectNext(new BigDecimal("19.00"))
                .verifyComplete();
    }

    @Test
    void calculateOrderTax_VAT() {
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .countryCode("DE")
                .items(List.of(
                        new TaxCalculationRequest.TaxItem("SKU-001", new BigDecimal("100.00"), "STANDARD", 1)
                ))
                .build();

        TaxRate rate = TaxRate.builder()
                .countryCode("DE")
                .taxType(TaxRate.TaxType.VAT)
                .rate(new BigDecimal("0.19"))
                .isActive(true)
                .build();

        // Use lenient stubbing
        org.mockito.Mockito.lenient().when(taxExemptionRepository.findValidExemption(any(), any()))
                .thenReturn(Mono.empty());
        when(taxRateRepository.findByCountryAndCategory("DE", "STANDARD"))
                .thenReturn(Flux.just(rate));

        StepVerifier.create(taxCalculationService.calculateOrderTax(request))
                .expectNextMatches(response -> 
                        response.getCountryCode().equals("DE") &&
                        response.getTaxType().equals("VAT") &&
                        response.getTotalTax().compareTo(new BigDecimal("19.00")) == 0)
                .verifyComplete();
    }

    @Test
    void determineTaxSystem_EU() {
        StepVerifier.create(taxCalculationService.determineTaxSystem("DE"))
                .expectNext(TaxRate.TaxType.VAT)
                .verifyComplete();
    }

    @Test
    void determineTaxSystem_US() {
        StepVerifier.create(taxCalculationService.determineTaxSystem("US"))
                .expectNext(TaxRate.TaxType.SALES_TAX)
                .verifyComplete();
    }

    @Test
    void determineTaxSystem_JP() {
        StepVerifier.create(taxCalculationService.determineTaxSystem("JP"))
                .expectNext(TaxRate.TaxType.CONSUMPTION)
                .verifyComplete();
    }

    @Test
    void calculateConsumptionTax_Standard() {
        StepVerifier.create(taxCalculationService.calculateConsumptionTax(
                        new BigDecimal("100.00"), "STANDARD"))
                .expectNext(new BigDecimal("10.00"))
                .verifyComplete();
    }

    @Test
    void calculateConsumptionTax_Food() {
        StepVerifier.create(taxCalculationService.calculateConsumptionTax(
                        new BigDecimal("100.00"), "FOOD"))
                .expectNext(new BigDecimal("8.00"))
                .verifyComplete();
    }
}
