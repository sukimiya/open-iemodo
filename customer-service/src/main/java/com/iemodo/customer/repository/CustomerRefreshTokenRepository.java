package com.iemodo.customer.repository;

import com.iemodo.customer.domain.CustomerRefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRefreshTokenRepository extends ReactiveCrudRepository<CustomerRefreshToken, Long> {

    Mono<CustomerRefreshToken> findByTokenHash(String tokenHash);

    Flux<CustomerRefreshToken> findAllByCustomerId(Long customerId);

    @Modifying
    @Query("UPDATE customer_refresh_tokens SET revoked = true, update_time = NOW() WHERE customer_id = :customerId AND revoked = false")
    Mono<Integer> revokeAllByCustomerId(Long customerId);

    @Modifying
    @Query("UPDATE customer_refresh_tokens SET revoked = true, update_time = NOW() WHERE token_hash = :tokenHash")
    Mono<Integer> revokeByTokenHash(String tokenHash);
}
