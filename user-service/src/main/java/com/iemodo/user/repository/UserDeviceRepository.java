package com.iemodo.user.repository;

import com.iemodo.user.domain.UserDevice;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link UserDevice} entity.
 */
@Repository
public interface UserDeviceRepository extends ReactiveCrudRepository<UserDevice, Long> {

    /**
     * Find all devices for a user.
     */
    Flux<UserDevice> findAllByUserId(Long userId);

    /**
     * Find device by user ID and device ID.
     */
    Mono<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    /**
     * Find active (non-revoked) devices for a user.
     */
    @Query("SELECT * FROM user_devices WHERE user_id = :userId AND revoked = false ORDER BY last_seen_at DESC")
    Flux<UserDevice> findActiveDevicesByUserId(Long userId);

    /**
     * Revoke a specific device.
     */
    @Query("UPDATE user_devices SET revoked = true WHERE user_id = :userId AND device_id = :deviceId")
    Mono<Void> revokeByUserIdAndDeviceId(Long userId, String deviceId);

    /**
     * Revoke all devices for a user (except current one if specified).
     */
    @Query("UPDATE user_devices SET revoked = true WHERE user_id = :userId AND (:exceptDeviceId IS NULL OR device_id != :exceptDeviceId)")
    Mono<Void> revokeAllByUserId(Long userId, String exceptDeviceId);

    /**
     * Count active devices for a user.
     */
    @Query("SELECT COUNT(*) FROM user_devices WHERE user_id = :userId AND revoked = false")
    Mono<Long> countActiveByUserId(Long userId);

    /**
     * Update last seen timestamp for a device.
     */
    @Query("UPDATE user_devices SET last_seen_at = NOW(), ip_address = :ipAddress WHERE user_id = :userId AND device_id = :deviceId")
    Mono<Void> updateLastSeen(Long userId, String deviceId, String ipAddress);
}
