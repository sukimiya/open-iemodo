package com.iemodo.pricing.controller;

import com.iemodo.common.response.Response;
import com.iemodo.pricing.domain.ExchangeRate;
import com.iemodo.pricing.dto.ExchangeRateRequest;
import com.iemodo.pricing.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Exchange rate controller
 */
@Slf4j
@RestController
@RequestMapping("/pricing/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * Get current exchange rate between two currencies
     */
    @GetMapping
    public Mono<Response<BigDecimal>> getRate(
            @RequestParam String from,
            @RequestParam String to) {
        
        return exchangeRateService.getRate(from.toUpperCase(), to.toUpperCase())
                .map(Response::success);
    }

    /**
     * Get multiple exchange rates from base currency
     */
    @GetMapping("/batch")
    public Mono<Response<Map<String, BigDecimal>>> getRates(
            @RequestParam String from,
            @RequestParam List<String> to) {
        
        return exchangeRateService.getRates(from.toUpperCase(), to)
                .map(Response::success);
    }

    /**
     * Convert amount between currencies
     */
    @PostMapping("/convert")
    public Mono<Response<BigDecimal>> convert(@Valid @RequestBody ExchangeRateRequest request) {
        
        return exchangeRateService.convertWithRounding(
                        request.getAmount(),
                        request.getFromCurrency().toUpperCase(),
                        request.getToCurrency().toUpperCase())
                .map(Response::success);
    }

    /**
     * Get supported currencies
     */
    @GetMapping("/currencies")
    public Mono<Response<List<String>>> getSupportedCurrencies() {
        
        return exchangeRateService.getSupportedCurrencies()
                .collectList()
                .map(Response::success);
    }

    /**
     * Get exchange rate history
     */
    @GetMapping("/history")
    public Flux<ExchangeRate> getRateHistory(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        
        return exchangeRateService.getRateHistory(
                from.toUpperCase(),
                to.toUpperCase(),
                fromDate,
                toDate);
    }

    /**
     * Refresh all exchange rates from external API
     */
    @PostMapping("/refresh")
    public Mono<Response<String>> refreshRates() {
        
        return exchangeRateService.refreshRates()
                .thenReturn(Response.success("Exchange rates refreshed successfully"));
    }
}
