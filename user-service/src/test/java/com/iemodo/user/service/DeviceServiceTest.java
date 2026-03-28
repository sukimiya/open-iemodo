package com.iemodo.user.service;

import com.iemodo.user.domain.UserDevice;
import com.iemodo.user.repository.UserDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("DeviceService")
class DeviceServiceTest {

    @Mock private UserDeviceRepository deviceRepository;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deviceService = new DeviceService(deviceRepository);
    }

    @Test
    @DisplayName("getUserDevices: should return active devices for user")
    void getUserDevices_shouldReturnActiveDevices() {
        UserDevice device1 = UserDevice.builder()
                .id(1L)
                .userId(100L)
                .deviceId("device-1")
                .deviceName("iPhone 14")
                .deviceType("MOBILE")
                .revoked(false)
                .lastSeenAt(Instant.now())
                .build();

        UserDevice device2 = UserDevice.builder()
                .id(2L)
                .userId(100L)
                .deviceId("device-2")
                .deviceName("Chrome on Mac")
                .deviceType("WEB")
                .revoked(false)
                .lastSeenAt(Instant.now())
                .build();

        when(deviceRepository.findActiveDevicesByUserId(100L))
                .thenReturn(Flux.just(device1, device2));

        StepVerifier.create(deviceService.getUserDevices(100L))
                .assertNext(d -> assertThat(d.getDeviceId()).isEqualTo("device-1"))
                .assertNext(d -> assertThat(d.getDeviceId()).isEqualTo("device-2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("countActiveDevices: should return count")
    void countActiveDevices_shouldReturnCount() {
        when(deviceRepository.countActiveByUserId(100L)).thenReturn(Mono.just(3L));

        StepVerifier.create(deviceService.countActiveDevices(100L))
                .assertNext(count -> assertThat(count).isEqualTo(3L))
                .verifyComplete();
    }

    @Test
    @DisplayName("revokeAllDevices: should revoke all devices for user")
    void revokeAllDevices_shouldSucceed() {
        when(deviceRepository.revokeAllByUserId(100L, null)).thenReturn(Mono.empty());

        StepVerifier.create(deviceService.revokeAllDevices(100L))
                .verifyComplete();
    }
}
