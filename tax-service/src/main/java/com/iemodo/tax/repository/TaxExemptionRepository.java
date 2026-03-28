package com.iemodo.tax.repository;

import com.iemodo.tax.domain.TaxExemption;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tax exemption repository
 */
@Repository
public interface TaxExemptionRepository extends ReactiveCrudRepository<TaxExemption, Long> {

    @Query("SELECT * FROM tax_exemptions WHERE customer_id = :customerId " +
           "AND country_code = :countryCode " +
           "AND status = 'ACTIVE' AND is_verified = true " +
           "AND (valid_to IS NULL OR valid_to >= CURRENT_DATE)")
    Mono<TaxExemption> findValidExemption(Long customerId, String countryCode);

    Flux<TaxExemption> findByCustomerId(Long customerId);

    Flux<TaxExemption> findByStatus(TaxExemption.ExemptionStatus status);
}
