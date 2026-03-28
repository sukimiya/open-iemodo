package com.iemodo.pricing.service;

import com.iemodo.pricing.domain.Currency;
import com.iemodo.pricing.domain.ExchangeRate;
import com.iemodo.pricing.repository.CurrencyRepository;
import com.iemodo.pricing.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Exchange rate service unit tests
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Test
    void getRate_SameCurrency_ReturnsOne() {
        StepVerifier.create(exchangeRateService.getRate("USD", "USD"))
                .expectNext(BigDecimal.ONE)
                .verifyComplete();
    }

    @Test
    void getRate_FromCache() {
        // Mock Redis returning cached rate
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just("0.92"));

        StepVerifier.create(exchangeRateService.getRate("USD", "EUR"))
                .expectNext(new BigDecimal("0.92"))
                .verifyComplete();
    }

    @Test
    void getRates_MultipleCurrencies() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rate:USD:EUR")).thenReturn(Mono.just("0.92"));
        when(valueOperations.get("rate:USD:GBP")).thenReturn(Mono.just("0.79"));

        StepVerifier.create(exchangeRateService.getRates("USD", List.of("EUR", "GBP")))
                .expectNextMatches(rates -> 
                        rates.size() == 2 &&
                        rates.get("EUR").compareTo(new BigDecimal("0.92")) == 0 &&
                        rates.get("GBP").compareTo(new BigDecimal("0.79")) == 0)
                .verifyComplete();
    }

    @Test
    void convert_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just("0.92"));

        BigDecimal amount = new BigDecimal("100.00");
        
        StepVerifier.create(exchangeRateService.convert(amount, "USD", "EUR"))
                .expectNextMatches(result -> result.compareTo(new BigDecimal("92.00")) == 0)
                .verifyComplete();
    }

    @Test
    void getBaseCurrency_ReturnsUSD() {
        StepVerifier.create(exchangeRateService.getBaseCurrency())
                .expectNext("USD")
                .verifyComplete();
    }

    @Test
    void isCurrencySupported_True() {
        Currency currency = Currency.builder()
                        .code("EUR")
                        .isActive(true)
                        .build();
        
        when(currencyRepository.findByCode("EUR")).thenReturn(Mono.just(currency));

        StepVerifier.create(exchangeRateService.isCurrencySupported("EUR"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isCurrencySupported_False() {
        when(currencyRepository.findByCode("XYZ")).thenReturn(Mono.empty());

        StepVerifier.create(exchangeRateService.isCurrencySupported("XYZ"))
                .expectNext(false)
                .verifyComplete();
    }
}
