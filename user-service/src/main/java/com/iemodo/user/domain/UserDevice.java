package com.iemodo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Records a login session tied to a specific device.
 *
 * <p>Enables multi-device management — users can view all active sessions
 * and revoke individual devices (e.g. "log out of phone").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_devices")
public class UserDevice {

    @Id
    private Long id;

    private Long userId;

    /** Opaque device fingerprint provided by the client (e.g. FCM token, browser fingerprint). */
    private String deviceId;

    /** Human-readable label: "iPhone 14", "Chrome on macOS", etc. */
    private String deviceName;

    private String userAgent;
    private String ipAddress;

    /** Last time this device was seen (updated on each successful auth). */
    private Instant lastSeenAt;

    @CreatedDate
    private Instant createdAt;

    /** Whether this device session has been explicitly revoked by the user. */
    private boolean revoked;
}
