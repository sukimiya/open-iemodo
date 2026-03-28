package com.iemodo.tax.service;

import com.iemodo.tax.domain.TaxExemption;
import com.iemodo.tax.domain.TaxRate;
import com.iemodo.tax.dto.TaxCalculationRequest;
import com.iemodo.tax.dto.TaxCalculationResponse;
import com.iemodo.tax.repository.TaxExemptionRepository;
import com.iemodo.tax.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tax calculation service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxCalculationServiceImpl implements TaxCalculationService {

    private final TaxRateRepository taxRateRepository;
    private final TaxExemptionRepository taxExemptionRepository;

    // EU country codes
    private static final Set<String> EU_COUNTRIES = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );

    // GST countries
    private static final Set<String> GST_COUNTRIES = Set.of("AU", "NZ", "SG", "CA");

    @Override
    public Mono<TaxCalculationResponse> calculateOrderTax(TaxCalculationRequest request) {
        String countryCode = request.getCountryCode().toUpperCase();
        
        // Check for B2B exemption
        Mono<Boolean> exemptionCheck = checkExemption(request.getCustomerId(), 
                request.getCustomerTaxId(), countryCode);

        return exemptionCheck.flatMap(isExempt -> {
            if (isExempt) {
                return buildExemptResponse(request);
            }
            return calculateTaxWithRates(request, countryCode);
        });
    }

    @Override
    public Mono<BigDecimal> calculateVAT(BigDecimal amount, String countryCode, String category) {
        return taxRateRepository.findByCountryAndCategory(countryCode, category != null ? category : "STANDARD")
                .filter(TaxRate::isEffective)
                .next()
                .map(rate -> amount.multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP))
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    @Override
    public Mono<BigDecimal> calculateGST(BigDecimal amount, String countryCode, String category) {
        return calculateVAT(amount, countryCode, category); // GST calculation is similar to VAT
    }

    @Override
    public Mono<BigDecimal> calculateSalesTax(BigDecimal amount, String countryCode, 
                                               String regionCode, String postalCode, String category) {
        // For US: State + County + City tax
        return taxRateRepository.findByCountryRegionAndPostalCode(countryCode, regionCode, postalCode)
                .filter(TaxRate::isEffective)
                .collectList()
                .map(rates -> {
                    BigDecimal totalRate = rates.stream()
                            .map(TaxRate::getRate)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return amount.multiply(totalRate).setScale(2, RoundingMode.HALF_UP);
                })
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    @Override
    public Mono<BigDecimal> calculateConsumptionTax(BigDecimal amount, String category) {
        // Japan consumption tax is 10% (8% for food)
        BigDecimal rate = "FOOD".equals(category) ? new BigDecimal("0.08") : new BigDecimal("0.10");
        return Mono.just(amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public Mono<TaxRate.TaxType> determineTaxSystem(String countryCode) {
        String code = countryCode.toUpperCase();
        
        if (EU_COUNTRIES.contains(code)) {
            return Mono.just(TaxRate.TaxType.VAT);
        } else if (GST_COUNTRIES.contains(code)) {
            return Mono.just(TaxRate.TaxType.GST);
        } else if ("US".equals(code)) {
            return Mono.just(TaxRate.TaxType.SALES_TAX);
        } else if ("JP".equals(code)) {
            return Mono.just(TaxRate.TaxType.CONSUMPTION);
        }
        
        return Mono.just(TaxRate.TaxType.VAT); // Default to VAT
    }

    // ─── Private helper methods ─────────────────────────────────────────────

    private Mono<Boolean> checkExemption(Long customerId, String taxId, String countryCode) {
        if (customerId == null && (taxId == null || taxId.isEmpty())) {
            return Mono.just(false);
        }
        
        return taxExemptionRepository.findValidExemption(customerId, countryCode)
                .map(TaxExemption::isValid)
                .defaultIfEmpty(false);
    }

    private Mono<TaxCalculationResponse> buildExemptResponse(TaxCalculationRequest request) {
        BigDecimal subtotal = calculateSubtotal(request.getItems());
        
        List<TaxCalculationResponse.TaxItemDetail> itemDetails = new ArrayList<>();
        for (TaxCalculationRequest.TaxItem item : request.getItems()) {
            itemDetails.add(TaxCalculationResponse.TaxItemDetail.builder()
                    .sku(item.getSku())
                    .amount(item.getAmount())
                    .taxableAmount(BigDecimal.ZERO)
                    .taxRate(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .taxCategory(item.getTaxCategory())
                    .build());
        }
        
        return Mono.just(TaxCalculationResponse.builder()
                .countryCode(request.getCountryCode().toUpperCase())
                .taxType("EXEMPT")
                .currency(request.getCurrency())
                .subtotal(subtotal)
                .totalTax(BigDecimal.ZERO)
                .totalAmount(subtotal)
                .isExempt(true)
                .exemptionReason("B2B VAT exemption verified")
                .items(itemDetails)
                .build());
    }

    private Mono<TaxCalculationResponse> calculateTaxWithRates(TaxCalculationRequest request, 
                                                                String countryCode) {
        return determineTaxSystem(countryCode)
                .flatMap(taxType -> {
                    switch (taxType) {
                        case VAT:
                            return calculateVATTax(request, countryCode);
                        case GST:
                            return calculateGSTTax(request, countryCode);
                        case SALES_TAX:
                            return calculateSalesTaxTotal(request, countryCode);
                        case CONSUMPTION:
                            return calculateConsumptionTaxTotal(request);
                        default:
                            return calculateVATTax(request, countryCode);
                    }
                });
    }

    private Mono<TaxCalculationResponse> calculateVATTax(TaxCalculationRequest request, 
                                                          String countryCode) {
        return Flux.fromIterable(request.getItems())
                .concatMap(item -> {
                    String category = item.getTaxCategory() != null ? item.getTaxCategory() : "STANDARD";
                    return calculateVAT(item.getAmount(), countryCode, category)
                            .map(taxAmount -> TaxCalculationResponse.TaxItemDetail.builder()
                                    .sku(item.getSku())
                                    .amount(item.getAmount())
                                    .taxableAmount(item.getAmount())
                                    .taxRate(taxAmount.divide(item.getAmount(), 4, RoundingMode.HALF_UP))
                                    .taxAmount(taxAmount)
                                    .taxCategory(category)
                                    .build());
                })
                .collectList()
                .map(itemDetails -> {
                    BigDecimal subtotal = itemDetails.stream()
                            .map(TaxCalculationResponse.TaxItemDetail::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal totalTax = itemDetails.stream()
                            .map(TaxCalculationResponse.TaxItemDetail::getTaxAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return TaxCalculationResponse.builder()
                            .countryCode(countryCode)
                            .taxType("VAT")
                            .currency(request.getCurrency())
                            .subtotal(subtotal)
                            .totalTax(totalTax)
                            .totalAmount(subtotal.add(totalTax))
                            .isExempt(false)
                            .items(itemDetails)
                            .build();
                });
    }

    private Mono<TaxCalculationResponse> calculateGSTTax(TaxCalculationRequest request, 
                                                          String countryCode) {
        // GST calculation is similar to VAT
        return calculateVATTax(request, countryCode)
                .map(response -> {
                    response.setTaxType("GST");
                    return response;
                });
    }

    private Mono<TaxCalculationResponse> calculateSalesTaxTotal(TaxCalculationRequest request, 
                                                                 String countryCode) {
        String regionCode = request.getRegionCode();
        String postalCode = request.getPostalCode();
        
        return Flux.fromIterable(request.getItems())
                .concatMap(item -> calculateSalesTax(item.getAmount(), countryCode, 
                        regionCode, postalCode, item.getTaxCategory())
                        .map(taxAmount -> TaxCalculationResponse.TaxItemDetail.builder()
                                .sku(item.getSku())
                                .amount(item.getAmount())
                                .taxableAmount(item.getAmount())
                                .taxAmount(taxAmount)
                                .taxCategory(item.getTaxCategory())
                                .build()))
                .collectList()
                .map(itemDetails -> {
                    BigDecimal subtotal = itemDetails.stream()
                            .map(TaxCalculationResponse.TaxItemDetail::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal totalTax = itemDetails.stream()
                            .map(TaxCalculationResponse.TaxItemDetail::getTaxAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return TaxCalculationResponse.builder()
                            .countryCode(countryCode)
                            .taxType("SALES_TAX")
                            .currency(request.getCurrency())
                            .subtotal(subtotal)
                            .totalTax(totalTax)
                            .totalAmount(subtotal.add(totalTax))
                            .isExempt(false)
                            .items(itemDetails)
                            .build();
                });
    }

    private Mono<TaxCalculationResponse> calculateConsumptionTaxTotal(TaxCalculationRequest request) {
        return Flux.fromIterable(request.getItems())
                .concatMap(item -> calculateConsumptionTax(item.getAmount(), item.getTaxCategory())
                        .map(taxAmount -> TaxCalculationResponse.TaxItemDetail.builder()
                                .sku(item.getSku())
                                .amount(item.getAmount())
                                .taxableAmount(item.getAmount())
                                .taxAmount(taxAmount)
                                .taxCategory(item.getTaxCategory())
                                .build()))
                .collectList()
                .map(itemDetails -> {
                    BigDecimal subtotal = itemDetails.stream()
                            .map(TaxCalculationResponse.TaxItemDetail::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal totalTax = itemDetails.stream()
                            .map(TaxCalculationResponse.TaxItemDetail::getTaxAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return TaxCalculationResponse.builder()
                            .countryCode("JP")
                            .taxType("CONSUMPTION")
                            .currency(request.getCurrency())
                            .subtotal(subtotal)
                            .totalTax(totalTax)
                            .totalAmount(subtotal.add(totalTax))
                            .isExempt(false)
                            .items(itemDetails)
                            .build();
                });
    }

    private BigDecimal calculateSubtotal(List<TaxCalculationRequest.TaxItem> items) {
        return items.stream()
                .map(TaxCalculationRequest.TaxItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
