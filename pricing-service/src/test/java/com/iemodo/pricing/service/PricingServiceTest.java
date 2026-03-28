package com.iemodo.pricing.service;

import com.iemodo.pricing.dto.CartPricingRequest;
import com.iemodo.pricing.dto.PriceDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Pricing service unit tests
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private PricingServiceImpl pricingService;

    @Test
    void convertPrice_Success() {
        when(exchangeRateService.convertWithRounding(any(), anyString(), anyString()))
                .thenReturn(Mono.just(new BigDecimal("92.00")));

        StepVerifier.create(pricingService.convertPrice(
                        new BigDecimal("100.00"), "USD", "EUR"))
                .expectNextMatches(result -> result.compareTo(new BigDecimal("92.00")) == 0)
                .verifyComplete();
    }

    @Test
    void applySegmentDiscount_NoSegment() {
        StepVerifier.create(pricingService.applySegmentDiscount(
                        new BigDecimal("100.00"), "SKU-001", null))
                .expectNext(new BigDecimal("100.00"))
                .verifyComplete();
    }
}
