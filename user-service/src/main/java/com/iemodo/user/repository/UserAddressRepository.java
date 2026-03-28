package com.iemodo.user.repository;

import com.iemodo.user.domain.UserAddress;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link UserAddress} entity.
 * 
 * <p>All queries are automatically routed to the correct tenant schema
 * via {@link com.iemodo.common.tenant.PostgresTenantConnectionFactory}.
 */
@Repository
public interface UserAddressRepository extends ReactiveCrudRepository<UserAddress, Long> {

    /**
     * Find all addresses for a specific user.
     */
    Flux<UserAddress> findAllByCustomerId(Long customerId);

    /**
     * Find the default shipping address for a user.
     */
    Mono<UserAddress> findByCustomerIdAndIsDefaultTrue(Long customerId);

    /**
     * Find the default billing address for a user.
     */
    Mono<UserAddress> findByCustomerIdAndIsDefaultBillingTrue(Long customerId);

    /**
     * Count addresses for a user.
     */
    Mono<Long> countByCustomerId(Long customerId);

    /**
     * Clear default flag for all addresses of a user.
     */
    @Query("UPDATE user_addresses SET is_default = false WHERE customer_id = :customerId")
    Mono<Integer> clearDefaultByCustomerId(Long customerId);

    /**
     * Clear default billing flag for all addresses of a user.
     */
    @Query("UPDATE user_addresses SET is_default_billing = false WHERE customer_id = :customerId")
    Mono<Integer> clearDefaultBillingByCustomerId(Long customerId);

    /**
     * Find addresses by GeoHash prefix (for location-based queries).
     */
    @Query("SELECT * FROM user_addresses WHERE customer_id = :customerId AND geo_hash LIKE :geoHashPrefix%")
    Flux<UserAddress> findByCustomerIdAndGeoHashStartingWith(Long customerId, String geoHashPrefix);
}
