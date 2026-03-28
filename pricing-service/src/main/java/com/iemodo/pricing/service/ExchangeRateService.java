package com.iemodo.pricing.service;

import com.iemodo.pricing.domain.ExchangeRate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Exchange rate service interface
 */
public interface ExchangeRateService {

    /**
     * Get current exchange rate between two currencies
     * Uses L1 (Caffeine) -> L2 (Redis) -> L3 (DB/API) cache
     */
    Mono<BigDecimal> getRate(String fromCurrency, String toCurrency);

    /**
     * Get multiple rates from base currency
     */
    Mono<java.util.Map<String, BigDecimal>> getRates(String fromCurrency, List<String> toCurrencies);

    /**
     * Convert amount from one currency to another
     */
    Mono<BigDecimal> convert(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * Convert with rounding to target currency decimal places
     */
    Mono<BigDecimal> convertWithRounding(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * Get supported currencies
     */
    Flux<String> getSupportedCurrencies();

    /**
     * Get base currency (USD)
     */
    Mono<String> getBaseCurrency();

    /**
     * Get exchange rate history
     */
    Flux<ExchangeRate> getRateHistory(String fromCurrency, String toCurrency, 
                                       Instant fromDate, Instant toDate);

    /**
     * Refresh all exchange rates from external API
     */
    Mono<Void> refreshRates();

    /**
     * Check if currency is supported
     */
    Mono<Boolean> isCurrencySupported(String currencyCode);
}
