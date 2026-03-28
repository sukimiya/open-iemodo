package com.iemodo.user.controller;

import com.iemodo.common.response.Response;
import com.iemodo.user.dto.AddressDTO;
import com.iemodo.user.dto.CreateAddressRequest;
import com.iemodo.user.service.AddressService;
import com.iemodo.user.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for user address management.
 * 
 * <p>Base path: /uc/api/v1/users/addresses
 */
@Slf4j
@RestController
@RequestMapping("/uc/api/v1/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final JwtService jwtService;

    /**
     * Get all addresses for the authenticated user.
     */
    @GetMapping
    public Flux<Response<AddressDTO>> getMyAddresses(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-TenantID") String tenantId) {
        Long userId = extractUserId(authHeader);
        return addressService.getUserAddresses(userId)
                .map(Response::success);
    }

    /**
     * Get the default shipping address.
     */
    @GetMapping("/default")
    public Mono<Response<AddressDTO>> getDefaultAddress(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-TenantID") String tenantId) {
        Long userId = extractUserId(authHeader);
        return addressService.getDefaultAddress(userId)
                .map(Response::success);
    }

    /**
     * Get a specific address by ID.
     */
    @GetMapping("/{addressId}")
    public Mono<Response<AddressDTO>> getAddress(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable Long addressId) {
        Long userId = extractUserId(authHeader);
        return addressService.getAddress(userId, addressId)
                .map(Response::success);
    }

    /**
     * Create a new address.
     */
    @PostMapping
    public Mono<Response<AddressDTO>> createAddress(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-TenantID") String tenantId,
            @Valid @RequestBody CreateAddressRequest request) {
        Long userId = extractUserId(authHeader);
        return addressService.createAddress(userId, request)
                .map(Response::success);
    }

    /**
     * Delete an address.
     */
    @DeleteMapping("/{addressId}")
    public Mono<Response<Void>> deleteAddress(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable Long addressId) {
        Long userId = extractUserId(authHeader);
        return addressService.deleteAddress(userId, addressId)
                .then(Mono.just(Response.success()));
    }

    /**
     * Set an address as the default shipping address.
     */
    @PutMapping("/{addressId}/default")
    public Mono<Response<AddressDTO>> setDefaultAddress(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable Long addressId) {
        Long userId = extractUserId(authHeader);
        return addressService.setDefaultAddress(userId, addressId)
                .map(Response::success);
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replaceFirst("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
