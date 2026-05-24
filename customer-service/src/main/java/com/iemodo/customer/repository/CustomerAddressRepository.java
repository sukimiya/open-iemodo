package com.iemodo.customer.repository;

import com.iemodo.customer.domain.CustomerAddress;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerAddressRepository extends ReactiveCrudRepository<CustomerAddress, Long> {

    Flux<CustomerAddress> findAllByCustomerId(Long customerId);

    Mono<CustomerAddress> findByCustomerIdAndIsDefaultTrue(Long customerId);

    Mono<Long> countByCustomerId(Long customerId);

    @Query("UPDATE customer_addresses SET is_default = false WHERE customer_id = :customerId")
    Mono<Integer> clearDefaultByCustomerId(Long customerId);

    @Query("UPDATE customer_addresses SET is_default_billing = false WHERE customer_id = :customerId")
    Mono<Integer> clearDefaultBillingByCustomerId(Long customerId);
}
