package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.UserDevice;
import com.iemodo.user.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Service for managing user devices and sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final UserDeviceRepository deviceRepository;

    /**
     * Get all active devices for a user.
     */
    public Flux<UserDevice> getUserDevices(Long userId) {
        return deviceRepository.findActiveDevicesByUserId(userId);
    }

    /**
     * Register or update a device for a user.
     */
    public Mono<UserDevice> registerDevice(Long userId, String deviceId, String deviceType, 
                                            String deviceName, String userAgent, String ipAddress) {
        return deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .flatMap(existing -> {
                    // Update existing device
                    existing.setDeviceType(deviceType);
                    existing.setDeviceName(deviceName);
                    existing.setUserAgent(userAgent);
                    existing.setIpAddress(ipAddress);
                    existing.setLastSeenAt(Instant.now());
                    existing.setRevoked(false);
                    return deviceRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new device
                    UserDevice device = UserDevice.builder()
                            .userId(userId)
                            .deviceId(deviceId)
                            .deviceType(deviceType)
                            .deviceName(deviceName)
                            .userAgent(userAgent)
                            .ipAddress(ipAddress)
                            .lastSeenAt(Instant.now())
                            .revoked(false)
                            .build();
                    return deviceRepository.save(device);
                }))
                .doOnSuccess(d -> log.info("Registered device {} for user {}", deviceId, userId));
    }

    /**
     * Revoke a specific device (logout from that device).
     */
    public Mono<Void> revokeDevice(Long userId, String deviceId) {
        return deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Device not found")))
                .flatMap(device -> deviceRepository.revokeByUserIdAndDeviceId(userId, deviceId))
                .doOnSuccess(v -> log.info("Revoked device {} for user {}", deviceId, userId));
    }

    /**
     * Revoke all devices except the current one.
     */
    public Mono<Void> revokeOtherDevices(Long userId, String currentDeviceId) {
        return deviceRepository.revokeAllByUserId(userId, currentDeviceId)
                .doOnSuccess(v -> log.info("Revoked all other devices for user {}", userId));
    }

    /**
     * Revoke all devices for a user (logout everywhere).
     */
    public Mono<Void> revokeAllDevices(Long userId) {
        return deviceRepository.revokeAllByUserId(userId, null)
                .doOnSuccess(v -> log.info("Revoked all devices for user {}", userId));
    }

    /**
     * Count active devices for a user.
     */
    public Mono<Long> countActiveDevices(Long userId) {
        return deviceRepository.countActiveByUserId(userId);
    }

    /**
     * Update device last seen timestamp.
     */
    public Mono<Void> updateLastSeen(Long userId, String deviceId, String ipAddress) {
        return deviceRepository.updateLastSeen(userId, deviceId, ipAddress)
                .doOnSuccess(v -> log.debug("Updated last seen for device {} of user {}", deviceId, userId));
    }
}
