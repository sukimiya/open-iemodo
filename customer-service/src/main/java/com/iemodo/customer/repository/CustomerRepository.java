package com.iemodo.customer.repository;

import com.iemodo.customer.domain.Customer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {

    Mono<Customer> findByPhoneAndTenantIdAndIsValid(String phone, String tenantId, Boolean isValid);

    Mono<Customer> findByEmailAndTenantIdAndIsValid(String email, String tenantId, Boolean isValid);

    Mono<Customer> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);

    Mono<Boolean> existsByPhoneAndTenantIdAndIsValid(String phone, String tenantId, Boolean isValid);

    Mono<Boolean> existsByEmailAndTenantIdAndIsValid(String email, String tenantId, Boolean isValid);

    @Query("SELECT EXISTS (SELECT 1 FROM customers WHERE oauth_provider = :provider AND oauth_subject = :subject AND is_valid = true)")
    Mono<Boolean> existsByOauthProviderAndOauthSubject(String provider, String subject);

    // ─── Admin queries ────────────────────────────────────────────────────────

    Flux<Customer> findAllByIsValid(Boolean isValid);

    Flux<Customer> findAllByTenantIdAndIsValid(String tenantId, Boolean isValid);
}
