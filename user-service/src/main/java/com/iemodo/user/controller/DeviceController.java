package com.iemodo.user.controller;

import com.iemodo.common.response.Response;
import com.iemodo.user.domain.UserDevice;
import com.iemodo.user.dto.DeviceRegistrationRequest;
import com.iemodo.user.service.DeviceService;
import com.iemodo.user.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for device management.
 * 
 * <p>Base path: /uc/api/v1/users/devices
 */
@Slf4j
@RestController
@RequestMapping("/uc/api/v1/users/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final JwtService jwtService;

    /**
     * Get all active devices for the authenticated user.
     */
    @GetMapping
    public Flux<Response<UserDevice>> getMyDevices(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return deviceService.getUserDevices(userId)
                .map(Response::success);
    }

    /**
     * Register a new device.
     */
    @PostMapping
    public Mono<Response<UserDevice>> registerDevice(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        Long userId = extractUserId(authHeader);
        return deviceService.registerDevice(
                        userId,
                        request.getDeviceId(),
                        request.getDeviceType(),
                        request.getDeviceName(),
                        request.getUserAgent(),
                        request.getIpAddress())
                .map(Response::success);
    }

    /**
     * Revoke a specific device (logout from that device).
     */
    @DeleteMapping("/{deviceId}")
    public Mono<Response<Void>> revokeDevice(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String deviceId) {
        Long userId = extractUserId(authHeader);
        return deviceService.revokeDevice(userId, deviceId)
                .then(Mono.just(Response.success()));
    }

    /**
     * Revoke all other devices except the current one.
     */
    @DeleteMapping("/others")
    public Mono<Response<Void>> revokeOtherDevices(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("currentDeviceId") String currentDeviceId) {
        Long userId = extractUserId(authHeader);
        return deviceService.revokeOtherDevices(userId, currentDeviceId)
                .then(Mono.just(Response.success()));
    }

    /**
     * Revoke all devices (logout everywhere).
     */
    @DeleteMapping
    public Mono<Response<Void>> revokeAllDevices(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return deviceService.revokeAllDevices(userId)
                .then(Mono.just(Response.success()));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replaceFirst("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
