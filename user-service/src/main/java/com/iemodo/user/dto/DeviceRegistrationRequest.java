package com.iemodo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request to register a device.
 */
@Data
public class DeviceRegistrationRequest {

    @NotBlank(message = "Device ID is required")
    @Size(max = 100, message = "Device ID must not exceed 100 characters")
    private String deviceId;

    @Size(max = 50, message = "Device type must not exceed 50 characters")
    private String deviceType;  // MOBILE, WEB, TABLET

    @Size(max = 200, message = "Device name must not exceed 200 characters")
    private String deviceName;

    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;
}
