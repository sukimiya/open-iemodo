package com.iemodo.fulfillment.repository;

import com.iemodo.fulfillment.domain.CustomsClearanceRule;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Customs clearance rule repository
 */
@Repository
public interface CustomsClearanceRuleRepository extends ReactiveCrudRepository<CustomsClearanceRule, Long> {

    @Query("SELECT * FROM customs_clearance_rules " +
           "WHERE origin_country = :origin AND destination_country = :destination " +
           "AND is_active = true " +
           "AND effective_from <= CURRENT_DATE AND (effective_to IS NULL OR effective_to >= CURRENT_DATE) " +
           "LIMIT 1")
    Mono<CustomsClearanceRule> findByCountries(String origin, String destination);
}
