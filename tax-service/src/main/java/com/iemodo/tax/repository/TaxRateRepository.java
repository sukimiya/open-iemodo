package com.iemodo.tax.repository;

import com.iemodo.tax.domain.TaxRate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Tax rate repository
 */
@Repository
public interface TaxRateRepository extends ReactiveCrudRepository<TaxRate, Long> {

    @Query("SELECT * FROM tax_rates WHERE country_code = :countryCode " +
           "AND (tax_category = :category OR (tax_category = 'STANDARD' AND :category IS NULL)) " +
           "AND is_active = true AND effective_from <= CURRENT_DATE " +
           "AND (effective_to IS NULL OR effective_to >= CURRENT_DATE) " +
           "ORDER BY rate DESC LIMIT 1")
    Flux<TaxRate> findByCountryAndCategory(String countryCode, String category);

    @Query("SELECT * FROM tax_rates WHERE country_code = :countryCode " +
           "AND is_active = true AND effective_from <= CURRENT_DATE " +
           "AND (effective_to IS NULL OR effective_to >= CURRENT_DATE)")
    Flux<TaxRate> findByCountryCode(String countryCode);

    @Query("SELECT * FROM tax_rates WHERE country_code = :countryCode " +
           "AND region_code = :regionCode " +
           "AND (postal_code_start IS NULL OR postal_code_start <= :postalCode) " +
           "AND (postal_code_end IS NULL OR postal_code_end >= :postalCode) " +
           "AND is_active = true AND effective_from <= CURRENT_DATE " +
           "AND (effective_to IS NULL OR effective_to >= CURRENT_DATE)")
    Flux<TaxRate> findByCountryRegionAndPostalCode(String countryCode, String regionCode, String postalCode);

    @Query("SELECT DISTINCT country_code FROM tax_rates WHERE is_active = true")
    Flux<String> findAllCountryCodes();
}
