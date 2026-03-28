package com.iemodo.pricing.service;

import com.iemodo.pricing.domain.Currency;
import com.iemodo.pricing.domain.RegionalPricingConfig;
import com.iemodo.pricing.dto.*;
import com.iemodo.pricing.repository.CurrencyRepository;
import com.iemodo.pricing.repository.RegionalPricingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * International pricing service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final ExchangeRateService exchangeRateService;
    private final RegionalPricingConfigRepository pricingConfigRepository;
    private final CurrencyRepository currencyRepository;
    private final R2dbcEntityTemplate r2dbcTemplate;

    // Base currency for all calculations
    private static final String BASE_CURRENCY = "USD";

    @Override
    public Mono<PriceDetail> calculateFinalPrice(String sku, String countryCode, 
                                                  Integer quantity, String customerSegment) {
        log.debug("Calculating price for SKU: {}, Country: {}, Qty: {}", sku, countryCode, quantity);
        
        final Instant now = Instant.now();
        final int qty = quantity != null && quantity > 0 ? quantity : 1;
        
        // 1. Get base price (mock - would call product service)
        Mono<BigDecimal> basePriceMono = getBasePrice(sku);
        
        // 2. Get regional pricing config
        Mono<RegionalPricingConfig> configMono = getRegionalConfig(sku, countryCode, now);
        
        // 3. Get target currency
        Mono<String> targetCurrencyMono = getTargetCurrency(countryCode);
        
        return Mono.zip(basePriceMono, configMono, targetCurrencyMono)
                .flatMap(tuple -> {
                    BigDecimal basePrice = tuple.getT1();
                    RegionalPricingConfig config = tuple.getT2();
                    String targetCurrency = tuple.getT3();
                    
                    return calculatePriceWithConfig(basePrice, config, targetCurrency, 
                                                     sku, countryCode, qty, customerSegment);
                });
    }

    @Override
    public Mono<CartPricingResponse> calculateCartPricing(CartPricingRequest request) {
        String countryCode = request.getCountryCode();
        String customerSegment = request.getCustomerSegment();
        
        return Flux.fromIterable(request.getItems())
                .concatMap(item -> calculateFinalPrice(
                        item.getSku(),
                        countryCode,
                        item.getQuantity(),
                        customerSegment
                ).map(priceDetail -> {
                    CartPricingResponse.CartItemResult result = new CartPricingResponse.CartItemResult();
                    result.setSku(item.getSku());
                    result.setQuantity(item.getQuantity());
                    result.setPriceDetail(priceDetail);
                    return result;
                }))
                .collectList()
                .map(itemResults -> {
                    CartPricingResponse response = new CartPricingResponse();
                    response.setCountryCode(countryCode);
                    response.setCustomerSegment(customerSegment);
                    response.setItems(itemResults);
                    
                    // Calculate totals
                    BigDecimal subtotal = itemResults.stream()
                            .map(r -> r.getPriceDetail().getFinalPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    response.setSubtotal(subtotal);
                    response.setFinalTotal(subtotal); // Tax calculated separately
                    
                    return response;
                });
    }

    @Override
    public Mono<BigDecimal> convertPrice(BigDecimal price, String fromCurrency, String toCurrency) {
        return exchangeRateService.convertWithRounding(price, fromCurrency, toCurrency);
    }

    @Override
    public Mono<BigDecimal> applyQuantityDiscount(BigDecimal price, String sku, 
                                                   String countryCode, Integer quantity) {
        if (quantity == null || quantity <= 1) {
            return Mono.just(price);
        }
        
        final int qty = quantity;
        
        return r2dbcTemplate.select(Query.query(
                        Criteria.where("sku").is(sku)
                                .and("is_active").is(true)
                                .and("min_quantity").lessThanOrEquals(qty)
                                .and(Criteria.where("max_quantity").isNull()
                                        .or("max_quantity").greaterThanOrEquals(qty))
                                .and(Criteria.where("effective_from").isNull()
                                        .or("effective_from").lessThanOrEquals(Instant.now()))
                                .and(Criteria.where("effective_to").isNull()
                                        .or("effective_to").greaterThanOrEquals(Instant.now()))
                ), com.iemodo.pricing.domain.QuantityDiscountTier.class)
                .collectList()
                .map(tiers -> {
                    if (tiers.isEmpty()) return price;
                    
                    // Get best discount (highest percentage)
                    BigDecimal maxDiscount = tiers.stream()
                            .map(com.iemodo.pricing.domain.QuantityDiscountTier::getDiscountPercent)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    
                    BigDecimal multiplier = BigDecimal.ONE.subtract(
                            maxDiscount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                    
                    return price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                });
    }

    @Override
    public Mono<BigDecimal> applySegmentDiscount(BigDecimal price, String sku, String customerSegment) {
        if (customerSegment == null || customerSegment.isEmpty()) {
            return Mono.just(price);
        }
        
        return r2dbcTemplate.select(Query.query(
                        Criteria.where("segment_code").is(customerSegment)
                                .and("is_active").is(true)
                                .and(Criteria.where("sku").isNull().or("sku").is(sku))
                                .and(Criteria.where("effective_from").isNull()
                                        .or("effective_from").lessThanOrEquals(Instant.now()))
                                .and(Criteria.where("effective_to").isNull()
                                        .or("effective_to").greaterThanOrEquals(Instant.now()))
                ), com.iemodo.pricing.domain.SegmentPricing.class)
                .collectList()
                .map(pricings -> {
                    if (pricings.isEmpty()) return price;
                    
                    // Get specific SKU discount if available, otherwise use default
                    BigDecimal discount = pricings.stream()
                            .filter(p -> sku.equals(p.getSku()))
                            .findFirst()
                            .orElse(pricings.get(0))
                            .getDiscountPercent();
                    
                    BigDecimal multiplier = BigDecimal.ONE.subtract(
                            discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                    
                    return price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
                });
    }

    // ─── Private helper methods ─────────────────────────────────────────────

    private Mono<BigDecimal> getBasePrice(String sku) {
        // TODO: In production, call product-service to get base price
        // For now, return mock price based on SKU hash
        int mockPrice = Math.abs(sku.hashCode() % 1000) + 10;
        return Mono.just(new BigDecimal(mockPrice));
    }

    private Mono<RegionalPricingConfig> getRegionalConfig(String sku, String countryCode, Instant now) {
        // Try specific SKU config first
        return pricingConfigRepository.findForSkuAndCountry(sku, countryCode, now)
                .switchIfEmpty(pricingConfigRepository.findDefaultForCountry(countryCode, now))
                .defaultIfEmpty(createDefaultConfig());
    }

    private Mono<String> getTargetCurrency(String countryCode) {
        return pricingConfigRepository.findDefaultForCountry(countryCode, Instant.now())
                .map(RegionalPricingConfig::getCurrencyCode)
                .switchIfEmpty(currencyRepository.findByCode(countryCode)
                        .map(Currency::getCode))
                .defaultIfEmpty("USD");
    }

    private Mono<PriceDetail> calculatePriceWithConfig(
            BigDecimal basePrice,
            RegionalPricingConfig config,
            String targetCurrency,
            String sku,
            String countryCode,
            int quantity,
            String customerSegment) {

        List<PriceComponent> components = new ArrayList<>();
        
        // Step 1: Base price in USD
        components.add(new PriceComponent("BASE_PRICE", basePrice, "Base price in USD"));
        BigDecimal currentPrice = basePrice;

        // Step 2: Apply regional markup
        if (config != null && config.getMarkupMultiplier() != null) {
            BigDecimal markupPrice = config.applyMarkup(currentPrice);
            BigDecimal markupAmount = markupPrice.subtract(currentPrice);
            components.add(new PriceComponent("REGIONAL_MARKUP", markupAmount, 
                    "Regional markup for " + countryCode));
            currentPrice = markupPrice;
        }

        // Step 3: Convert to target currency
        if (!BASE_CURRENCY.equals(targetCurrency)) {
            BigDecimal beforeConvert = currentPrice;
            return exchangeRateService.convert(currentPrice, BASE_CURRENCY, targetCurrency)
                    .flatMap(converted -> {
                        components.add(new PriceComponent("CURRENCY_CONVERSION", 
                                converted.subtract(beforeConvert),
                                "Exchange rate conversion to " + targetCurrency));
                        return applyDiscountsAndBuildDetail(components, converted, sku, 
                                countryCode, quantity, customerSegment, targetCurrency);
                    });
        }

        return applyDiscountsAndBuildDetail(components, currentPrice, sku, countryCode, 
                quantity, customerSegment, targetCurrency);
    }

    private Mono<PriceDetail> applyDiscountsAndBuildDetail(
            List<PriceComponent> components,
            BigDecimal currentPrice,
            String sku,
            String countryCode,
            int quantity,
            String customerSegment,
            String currency) {
        
        BigDecimal discountedPrice = currentPrice;
        
        // Step 4: Apply quantity discount
        if (quantity > 1) {
            BigDecimal finalDiscountedPrice = discountedPrice;
            return applyQuantityDiscount(discountedPrice, sku, countryCode, quantity)
                    .flatMap(qtyPrice -> {
                        BigDecimal qtyDiscount = finalDiscountedPrice.subtract(qtyPrice);
                        if (qtyDiscount.compareTo(BigDecimal.ZERO) > 0) {
                            components.add(new PriceComponent("QUANTITY_DISCOUNT", 
                                    qtyDiscount.negate(), "Quantity discount for " + quantity + " items"));
                        }
                        return applySegmentAndBuild(qtyPrice, sku, customerSegment, 
                                components, currency);
                    });
        }
        
        return applySegmentAndBuild(discountedPrice, sku, customerSegment, 
                components, currency);
    }

    private Mono<PriceDetail> applySegmentAndBuild(
            BigDecimal price,
            String sku,
            String customerSegment,
            List<PriceComponent> components,
            String currency) {
        
        return applySegmentDiscount(price, sku, customerSegment)
                .map(segmentPrice -> {
                    BigDecimal segmentDiscount = price.subtract(segmentPrice);
                    if (segmentDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        components.add(new PriceComponent("SEGMENT_DISCOUNT", 
                                segmentDiscount.negate(), 
                                "Customer segment discount: " + customerSegment));
                    }
                    
                    // Build final detail
                    return PriceDetail.builder()
                            .currency(currency)
                            .basePrice(components.get(0).getAmount())
                            .components(components)
                            .subtotal(segmentPrice)
                            .discounts(components.stream()
                                    .filter(c -> c.getAmount().compareTo(BigDecimal.ZERO) < 0)
                                    .map(PriceComponent::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .negate())
                            .finalPrice(segmentPrice)
                            .build();
                });
    }

    private RegionalPricingConfig createDefaultConfig() {
        return RegionalPricingConfig.builder()
                .currencyCode("USD")
                .markupMultiplier(BigDecimal.ONE)
                .discountMultiplier(BigDecimal.ONE)
                .isActive(true)
                .build();
    }
}
