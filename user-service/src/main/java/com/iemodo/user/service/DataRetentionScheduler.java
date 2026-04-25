package com.iemodo.user.service;

import com.iemodo.user.repository.RefreshTokenRepository;
import com.iemodo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Periodic data retention cleanup (GDPR Art. 5(1)(e)).
 *
 * <p>Runs daily to purge:
 * <ul>
 *   <li>Expired refresh tokens</li>
 *   <li>Soft-deleted users past the retention period</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${iemodo.gdpr.retention.user-data-days:365}")
    private int userDataRetentionDays;

    @Value("${iemodo.gdpr.retention.token-days:90}")
    private int tokenRetentionDays;

    /**
     * Clean up expired tokens daily at 03:00.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeExpiredTokens() {
        Instant cutoff = Instant.now().minusSeconds(tokenRetentionDays * 86400L);
        refreshTokenRepository.deleteExpiredBefore(cutoff)
                .doOnSuccess(count -> {
                    if (count > 0) {
                        log.info("Retention: purged {} expired tokens older than {} days", count, tokenRetentionDays);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Retention: failed to purge expired tokens", e);
                    return Mono.empty();
                })
                .subscribe();
    }

    /**
     * Purge soft-deleted users past the retention period daily at 03:30.
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeDeletedUsers() {
        Instant cutoff = Instant.now().minusSeconds(userDataRetentionDays * 86400L);
        userRepository.physicalDeleteDeletedBefore(cutoff)
                .doOnSuccess(count -> {
                    if (count > 0) {
                        log.info("Retention: physically purged {} users deleted before retention period", count);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Retention: failed to purge deleted users", e);
                    return Mono.empty();
                })
                .subscribe();
    }
}
