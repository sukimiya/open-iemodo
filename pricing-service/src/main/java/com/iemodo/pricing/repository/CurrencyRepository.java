package com.iemodo.pricing.repository;

import com.iemodo.pricing.domain.Currency;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Currency repository
 */
@Repository
public interface CurrencyRepository extends ReactiveCrudRepository<Currency, Long> {

    Mono<Currency> findByCode(String code);

    Flux<Currency> findByIsActiveTrue();

    Mono<Currency> findByIsBaseCurrencyTrue();
}
