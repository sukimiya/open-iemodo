package com.iemodo.pricing.repository;

import com.iemodo.pricing.domain.RegionalPricingConfig;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Regional pricing configuration repository
 */
@Repository
public interface RegionalPricingConfigRepository extends ReactiveCrudRepository<RegionalPricingConfig, Long> {

    /**
     * Find pricing config for specific SKU and country
     */
    @Query("SELECT * FROM regional_pricing_config " +
           "WHERE country_code = :countryCode AND sku = :sku " +
           "AND is_active = true " +
           "AND (effective_from IS NULL OR effective_from <= :now) " +
           "AND (effective_to IS NULL OR effective_to >= :now) " +
           "ORDER BY priority DESC LIMIT 1")
    Mono<RegionalPricingConfig> findForSkuAndCountry(String sku, String countryCode, Instant now);

    /**
     * Find default pricing config for country (sku is null)
     */
    @Query("SELECT * FROM regional_pricing_config " +
           "WHERE country_code = :countryCode AND sku IS NULL " +
           "AND is_active = true " +
           "AND (effective_from IS NULL OR effective_from <= :now) " +
           "AND (effective_to IS NULL OR effective_to >= :now) " +
           "LIMIT 1")
    Mono<RegionalPricingConfig> findDefaultForCountry(String countryCode, Instant now);

    /**
     * Get all configs for a country
     */
    Flux<RegionalPricingConfig> findByCountryCodeAndIsActiveTrue(String countryCode);

    /**
     * Get config by SKU (regardless of country)
     */
    Flux<RegionalPricingConfig> findBySkuAndIsActiveTrue(String sku);
}
