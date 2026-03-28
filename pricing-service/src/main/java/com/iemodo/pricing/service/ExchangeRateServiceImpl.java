package com.iemodo.pricing.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.iemodo.pricing.domain.ExchangeRate;
import com.iemodo.pricing.repository.CurrencyRepository;
import com.iemodo.pricing.repository.ExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Exchange rate service implementation with L1/L2/L3 caching
 * L1: Caffeine (10 minutes)
 * L2: Redis (1 hour)
 * L3: Database / External API
 */
@Slf4j
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final String BASE_CURRENCY = "USD";
    private static final int RATE_SCALE = 10;
    
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient.Builder webClientBuilder;

    // L1 Cache: Caffeine (10 minutes TTL)
    private final Cache<String, BigDecimal> localCache;

    public ExchangeRateServiceImpl(
            ExchangeRateRepository exchangeRateRepository,
            CurrencyRepository currencyRepository,
            ReactiveStringRedisTemplate redisTemplate,
            WebClient.Builder webClientBuilder) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyRepository = currencyRepository;
        this.redisTemplate = redisTemplate;
        this.webClientBuilder = webClientBuilder;
        
        this.localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
        log.info("ExchangeRateService initialized with L1 (Caffeine) + L2 (Redis) + L3 (DB/API) cache");
    }

    @Override
    public Mono<BigDecimal> getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Mono.just(BigDecimal.ONE);
        }

        String cacheKey = buildCacheKey(fromCurrency, toCurrency);
        
        // L1: Check Caffeine local cache
        BigDecimal cachedRate = localCache.getIfPresent(cacheKey);
        if (cachedRate != null) {
            log.debug("L1 cache hit: {} -> {} = {}", fromCurrency, toCurrency, cachedRate);
            return Mono.just(cachedRate);
        }

        // L2: Check Redis
        return getRateFromRedis(cacheKey)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("L2 cache miss, fetching from L3: {} -> {}", fromCurrency, toCurrency);
                    // L3: Database or external API
                    return fetchAndCacheRate(fromCurrency, toCurrency, cacheKey);
                }))
                .doOnNext(rate -> {
                    // Populate L1 cache
                    localCache.put(cacheKey, rate);
                    log.debug("L1 cache populated: {} -> {} = {}", fromCurrency, toCurrency, rate);
                });
    }

    @Override
    public Mono<Map<String, BigDecimal>> getRates(String fromCurrency, List<String> toCurrencies) {
        return Flux.fromIterable(toCurrencies)
                .flatMap(to -> getRate(fromCurrency, to)
                        .map(rate -> Map.entry(to, rate)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Mono<BigDecimal> convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Mono.just(amount);
        }
        
        return getRate(fromCurrency, toCurrency)
                .map(rate -> amount.multiply(rate));
    }

    @Override
    public Mono<BigDecimal> convertWithRounding(BigDecimal amount, String fromCurrency, String toCurrency) {
        return convert(amount, fromCurrency, toCurrency)
                .flatMap(converted -> 
                        currencyRepository.findByCode(toCurrency)
                                .map(currency -> {
                                    int decimalPlaces = currency.getDecimalPlaces() != null ? 
                                            currency.getDecimalPlaces() : 2;
                                    return converted.setScale(decimalPlaces, RoundingMode.HALF_UP);
                                })
                                .defaultIfEmpty(converted.setScale(2, RoundingMode.HALF_UP))
                );
    }

    @Override
    public Flux<String> getSupportedCurrencies() {
        return currencyRepository.findByIsActiveTrue()
                .map(com.iemodo.pricing.domain.Currency::getCode);
    }

    @Override
    public Mono<String> getBaseCurrency() {
        return Mono.just(BASE_CURRENCY);
    }

    @Override
    public Flux<ExchangeRate> getRateHistory(String fromCurrency, String toCurrency, 
                                              Instant fromDate, Instant toDate) {
        return exchangeRateRepository.findRateHistory(fromCurrency, toCurrency, fromDate, toDate);
    }

    @Override
    public Mono<Void> refreshRates() {
        log.info("Refreshing all exchange rates from external API...");
        
        // Get all supported currencies
        return currencyRepository.findByIsActiveTrue()
                .map(com.iemodo.pricing.domain.Currency::getCode)
                .collectList()
                .flatMap(currencies -> fetchRatesFromApi(BASE_CURRENCY, currencies)
                        .flatMap(rates -> saveRatesToDatabase(BASE_CURRENCY, rates))
                        .then(clearAllCaches())
                        .doOnSuccess(v -> log.info("Exchange rates refreshed successfully")));
    }

    @Override
    public Mono<Boolean> isCurrencySupported(String currencyCode) {
        return currencyRepository.findByCode(currencyCode.toUpperCase())
                .map(c -> Boolean.TRUE.equals(c.getIsActive()))
                .defaultIfEmpty(false);
    }

    // ─── Private helper methods ─────────────────────────────────────────────

    private String buildCacheKey(String from, String to) {
        return String.format("rate:%s:%s", from.toUpperCase(), to.toUpperCase());
    }

    private Mono<BigDecimal> getRateFromRedis(String cacheKey) {
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .map(BigDecimal::new)
                .doOnNext(rate -> log.debug("L2 cache hit (Redis): {} = {}", cacheKey, rate))
                .onErrorResume(e -> {
                    log.warn("Redis error, falling back to DB: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<BigDecimal> fetchAndCacheRate(String from, String to, String cacheKey) {
        // Try to get from database first
        return exchangeRateRepository.findLatestRate(from, to)
                .filter(ExchangeRate::isRecent)
                .map(ExchangeRate::getRate)
                .switchIfEmpty(Mono.defer(() -> {
                    // If not in DB or stale, try cross-rate via USD
                    if (!from.equalsIgnoreCase(BASE_CURRENCY) && !to.equalsIgnoreCase(BASE_CURRENCY)) {
                        return calculateCrossRate(from, to);
                    }
                    // Otherwise fetch from external API
                    return fetchSingleRateFromApi(from, to);
                }))
                .flatMap(rate -> saveRateToRedis(cacheKey, rate)
                        .thenReturn(rate));
    }

    private Mono<BigDecimal> calculateCrossRate(String from, String to) {
        // Convert via USD: EUR -> CNY = (EUR -> USD) * (USD -> CNY)
        return getRate(from, BASE_CURRENCY)
                .zipWith(getRate(BASE_CURRENCY, to))
                .map(tuple -> tuple.getT1().multiply(tuple.getT2()));
    }

    private Mono<BigDecimal> fetchSingleRateFromApi(String from, String to) {
        // For demo, return a mock rate if API key not configured
        log.warn("External API not configured, using fallback rates");
        
        // Mock rates for common currencies
        Map<String, BigDecimal> mockRates = Map.of(
                "USD-EUR", new BigDecimal("0.92"),
                "USD-GBP", new BigDecimal("0.79"),
                "USD-CNY", new BigDecimal("7.24"),
                "USD-JPY", new BigDecimal("151.50"),
                "USD-AUD", new BigDecimal("1.52"),
                "USD-CAD", new BigDecimal("1.36"),
                "EUR-USD", new BigDecimal("1.09"),
                "GBP-USD", new BigDecimal("1.27"),
                "CNY-USD", new BigDecimal("0.138")
        );

        String key = from.toUpperCase() + "-" + to.toUpperCase();
        BigDecimal rate = mockRates.getOrDefault(key, new BigDecimal("1.0"));
        
        // Save to database
        ExchangeRate exchangeRate = ExchangeRate.create(from, to, rate, "FALLBACK");
        return exchangeRateRepository.save(exchangeRate)
                .thenReturn(rate);
    }

    private Mono<Map<String, BigDecimal>> fetchRatesFromApi(String base, List<String> currencies) {
        // Mock implementation - would call Fixer.io or similar
        log.info("Would fetch rates from external API for base: {}", base);
        
        Map<String, BigDecimal> rates = Map.of(
                "EUR", new BigDecimal("0.92"),
                "GBP", new BigDecimal("0.79"),
                "CNY", new BigDecimal("7.24"),
                "JPY", new BigDecimal("151.50")
        );
        
        return Mono.just(rates);
    }

    private Mono<Void> saveRatesToDatabase(String base, Map<String, BigDecimal> rates) {
        return Flux.fromIterable(rates.entrySet())
                .map(entry -> ExchangeRate.create(base, entry.getKey(), entry.getValue(), "API"))
                .flatMap(exchangeRateRepository::save)
                .then();
    }

    private Mono<Boolean> saveRateToRedis(String cacheKey, BigDecimal rate) {
        return redisTemplate.opsForValue()
                .set(cacheKey, rate.toString(), Duration.ofHours(1));
    }

    private Mono<Void> clearAllCaches() {
        localCache.invalidateAll();
        return redisTemplate.delete(redisTemplate.keys("rate:*"))
                .then();
    }
}
