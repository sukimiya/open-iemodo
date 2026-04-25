package com.iemodo.user.repository;

import com.iemodo.user.domain.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    @Query("SELECT * FROM users WHERE oauth_provider = :provider AND oauth_subject = :subject LIMIT 1")
    Mono<User> findByOauthProviderAndOauthSubject(String provider, String subject);

    Flux<User> findAllByTenantId(String tenantId);

    @Modifying
    @Query("DELETE FROM users WHERE is_valid = FALSE AND deleted_at IS NOT NULL AND deleted_at < :cutoff")
    Mono<Integer> physicalDeleteDeletedBefore(Instant cutoff);
}
