package com.iemodo.customer.repository;

import com.iemodo.customer.domain.CustomerOAuthConnection;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerOAuthConnectionRepository extends ReactiveCrudRepository<CustomerOAuthConnection, Long> {

    Flux<CustomerOAuthConnection> findAllByCustomerId(Long customerId);

    Mono<CustomerOAuthConnection> findByProviderAndProviderSubject(String provider, String providerSubject);

    Mono<CustomerOAuthConnection> findByCustomerIdAndProvider(Long customerId, String provider);

    Mono<Void> deleteAllByCustomerId(Long customerId);

    Mono<Boolean> existsByProviderAndProviderSubject(String provider, String providerSubject);
}
