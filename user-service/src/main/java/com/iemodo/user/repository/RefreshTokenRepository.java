package com.iemodo.user.repository;

import com.iemodo.user.domain.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, Long> {

    Mono<RefreshToken> findByTokenHash(String tokenHash);

    Flux<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = :userId AND revoked = FALSE")
    Mono<Integer> revokeAllByUserId(Long userId);

    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = :tokenHash")
    Mono<Integer> revokeByTokenHash(String tokenHash);
}
