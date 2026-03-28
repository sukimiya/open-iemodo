package com.iemodo.pricing.repository;

import com.iemodo.pricing.domain.ExchangeRate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Exchange rate repository
 */
@Repository
public interface ExchangeRateRepository extends ReactiveCrudRepository<ExchangeRate, Long> {

    /**
     * Get latest rate between two currencies
     */
    @Query("SELECT * FROM exchange_rates " +
           "WHERE from_currency = :from AND to_currency = :to " +
           "ORDER BY recorded_at DESC LIMIT 1")
    Mono<ExchangeRate> findLatestRate(String from, String to);

    /**
     * Get all latest rates from a base currency
     */
    @Query("SELECT DISTINCT ON (to_currency) * FROM exchange_rates " +
           "WHERE from_currency = :from " +
           "ORDER BY to_currency, recorded_at DESC")
    Flux<ExchangeRate> findLatestRatesFrom(String from);

    /**
     * Get rate history between two currencies
     */
    @Query("SELECT * FROM exchange_rates " +
           "WHERE from_currency = :from AND to_currency = :to " +
           "AND recorded_at BETWEEN :fromDate AND :toDate " +
           "ORDER BY recorded_at DESC")
    Flux<ExchangeRate> findRateHistory(String from, String to, Instant fromDate, Instant toDate);

    /**
     * Check if recent rate exists (within 1 hour)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM exchange_rates " +
           "WHERE from_currency = :from AND to_currency = :to " +
           "AND recorded_at > :since)")
    Mono<Boolean> existsRecentRate(String from, String to, Instant since);
}
