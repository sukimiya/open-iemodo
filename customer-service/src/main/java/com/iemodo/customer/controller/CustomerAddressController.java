package com.iemodo.customer.controller;

import com.iemodo.common.response.Response;
import com.iemodo.customer.dto.AddressDTO;
import com.iemodo.customer.dto.CreateAddressRequest;
import com.iemodo.customer.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/cc/api/v1/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @GetMapping
    public Mono<Response<java.util.List<AddressDTO>>> getAddresses(
            @RequestHeader("X-Customer-ID") Long customerId) {
        return addressService.getAddresses(customerId)
                .collectList()
                .map(Response::success);
    }

    @GetMapping("/{addressId}")
    public Mono<Response<AddressDTO>> getAddress(
            @RequestHeader("X-Customer-ID") Long customerId,
            @PathVariable Long addressId) {
        return addressService.getAddress(customerId, addressId)
                .map(Response::success);
    }

    @GetMapping("/default")
    public Mono<Response<AddressDTO>> getDefaultAddress(
            @RequestHeader("X-Customer-ID") Long customerId) {
        return addressService.getDefaultAddress(customerId)
                .map(Response::success);
    }

    @PostMapping
    public Mono<Response<AddressDTO>> createAddress(
            @RequestHeader("X-Customer-ID") Long customerId,
            @RequestBody @Valid CreateAddressRequest request) {
        return addressService.createAddress(customerId, request)
                .map(Response::success);
    }

    @DeleteMapping("/{addressId}")
    public Mono<Response<Void>> deleteAddress(
            @RequestHeader("X-Customer-ID") Long customerId,
            @PathVariable Long addressId) {
        return addressService.deleteAddress(customerId, addressId)
                .thenReturn(Response.success());
    }

    @PutMapping("/{addressId}/default")
    public Mono<Response<AddressDTO>> setDefaultAddress(
            @RequestHeader("X-Customer-ID") Long customerId,
            @PathVariable Long addressId) {
        return addressService.setDefaultAddress(customerId, addressId)
                .map(Response::success);
    }
}
