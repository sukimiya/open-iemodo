package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.UserAddress;
import com.iemodo.user.dto.AddressDTO;
import com.iemodo.user.dto.CreateAddressRequest;
import com.iemodo.user.repository.UserAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("AddressService")
class AddressServiceTest {

    @Mock private UserAddressRepository addressRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addressService = new AddressService(addressRepository);
    }

    @Test
    @DisplayName("createAddress: should create first address as default")
    void createAddress_shouldCreateAsDefault_whenFirstAddress() {
        CreateAddressRequest request = new CreateAddressRequest();
        request.setRecipientName("John Doe");
        request.setRecipientPhone("+1-555-1234");
        request.setAddressLine1("123 Main St");
        request.setCity("New York");
        request.setCountryCode("US");
        request.setIsDefault(false);  // User didn't request default, but it's first address

        UserAddress savedAddress = UserAddress.builder()
                .id(1L)
                .customerId(100L)
                .recipientName("John Doe")
                .addressLine1("123 Main St")
                .city("New York")
                .countryCode("US")
                .isDefault(true)  // Should be set as default because it's first
                .build();

        when(addressRepository.countByCustomerId(100L)).thenReturn(Mono.just(0L));
        when(addressRepository.clearDefaultByCustomerId(any())).thenReturn(Mono.just(1));
        when(addressRepository.clearDefaultBillingByCustomerId(any())).thenReturn(Mono.just(0));
        when(addressRepository.save(any(UserAddress.class))).thenReturn(Mono.just(savedAddress));

        StepVerifier.create(addressService.createAddress(100L, request))
                .assertNext(dto -> {
                    assertThat(dto.getId()).isEqualTo(1L);
                    assertThat(dto.getIsDefault()).isTrue();
                    assertThat(dto.getRecipientName()).isEqualTo("John Doe");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getDefaultAddress: should fail when no default exists")
    void getDefaultAddress_shouldFail_whenNoneExists() {
        when(addressRepository.findByCustomerIdAndIsDefaultTrue(100L))
                .thenReturn(Mono.empty());

        StepVerifier.create(addressService.getDefaultAddress(100L))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.USER_NOT_FOUND)
                .verify();
    }
}
