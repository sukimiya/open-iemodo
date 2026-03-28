package com.iemodo.user.repository;

import com.iemodo.user.domain.OAuthConnection;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link OAuthConnection} entity.
 */
@Repository
public interface OAuthConnectionRepository extends ReactiveCrudRepository<OAuthConnection, Long> {

    /**
     * Find all OAuth connections for a user.
     */
    Flux<OAuthConnection> findAllByUserId(Long userId);

    /**
     * Find connection by provider and provider subject (unique constraint).
     */
    Mono<OAuthConnection> findByProviderAndProviderSubject(String provider, String providerSubject);

    /**
     * Find connection for a specific user and provider.
     */
    Mono<OAuthConnection> findByUserIdAndProvider(Long userId, String provider);

    /**
     * Delete all connections for a user.
     */
    Mono<Void> deleteAllByUserId(Long userId);

    /**
     * Delete a specific connection for a user.
     */
    Mono<Void> deleteByUserIdAndProvider(Long userId, String provider);

    /**
     * Check if a connection exists for provider and subject.
     */
    Mono<Boolean> existsByProviderAndProviderSubject(String provider, String providerSubject);
}
