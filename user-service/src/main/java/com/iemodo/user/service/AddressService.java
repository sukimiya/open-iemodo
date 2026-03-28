package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.UserAddress;
import com.iemodo.user.dto.AddressDTO;
import com.iemodo.user.dto.CreateAddressRequest;
import com.iemodo.user.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for managing user addresses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressRepository addressRepository;

    /**
     * Get all addresses for a user.
     */
    public Flux<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findAllByCustomerId(userId)
                .map(this::toDTO);
    }

    /**
     * Get a specific address by ID.
     */
    public Mono<AddressDTO> getAddress(Long userId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(addr -> addr.getCustomerId().equals(userId))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "Address not found")))
                .map(this::toDTO);
    }

    /**
     * Get the default shipping address for a user.
     */
    public Mono<AddressDTO> getDefaultAddress(Long userId) {
        return addressRepository.findByCustomerIdAndIsDefaultTrue(userId)
                .map(this::toDTO)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "No default address found")));
    }

    /**
     * Create a new address for a user.
     */
    @Transactional
    public Mono<AddressDTO> createAddress(Long userId, CreateAddressRequest request) {
        return addressRepository.countByCustomerId(userId)
                .flatMap(count -> {
                    // If this is the first address, make it default
                    boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.getIsDefault());
                    boolean makeDefaultBilling = count == 0 || Boolean.TRUE.equals(request.getIsDefaultBilling());

                    UserAddress address = UserAddress.builder()
                            .customerId(userId)
                            .addressName(request.getAddressName())
                            .recipientName(request.getRecipientName())
                            .recipientPhone(request.getRecipientPhone())
                            .recipientEmail(request.getRecipientEmail())
                            .countryCode(request.getCountryCode().toUpperCase())
                            .regionCode(request.getRegionCode())
                            .regionName(request.getRegionName())
                            .city(request.getCity())
                            .district(request.getDistrict())
                            .addressLine1(request.getAddressLine1())
                            .addressLine2(request.getAddressLine2())
                            .postalCode(request.getPostalCode())
                            .isDefault(makeDefault)
                            .isDefaultBilling(makeDefaultBilling)
                            .isVerified(false)
                            .build();

                    Mono<UserAddress> saveAddress = addressRepository.save(address);
                    
                    Mono<Integer> clearDefaults = makeDefault 
                            ? addressRepository.clearDefaultByCustomerId(userId) 
                            : Mono.just(0);
                    Mono<Integer> clearBilling = makeDefaultBilling 
                            ? addressRepository.clearDefaultBillingByCustomerId(userId) 
                            : Mono.just(0);

                    return Mono.zip(clearDefaults, clearBilling)
                            .then(saveAddress);
                })
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Created address id={} for userId={}", dto.getId(), userId));
    }

    /**
     * Delete an address.
     */
    @Transactional
    public Mono<Void> deleteAddress(Long userId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(addr -> addr.getCustomerId().equals(userId))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "Address not found")))
                .flatMap(addr -> addressRepository.deleteById(addressId))
                .doOnSuccess(v -> log.info("Deleted address id={} for userId={}", addressId, userId));
    }

    /**
     * Set an address as the default shipping address.
     */
    @Transactional
    public Mono<AddressDTO> setDefaultAddress(Long userId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(addr -> addr.getCustomerId().equals(userId))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "Address not found")))
                .flatMap(addr -> addressRepository.clearDefaultByCustomerId(userId)
                        .then(Mono.defer(() -> {
                            addr.setIsDefault(true);
                            return addressRepository.save(addr);
                        })))
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Set default address id={} for userId={}", addressId, userId));
    }

    private AddressDTO toDTO(UserAddress address) {
        return AddressDTO.builder()
                .id(address.getId())
                .addressName(address.getAddressName())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .recipientEmail(address.getRecipientEmail())
                .countryCode(address.getCountryCode())
                .regionCode(address.getRegionCode())
                .regionName(address.getRegionName())
                .city(address.getCity())
                .district(address.getDistrict())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .postalCode(address.getPostalCode())
                .geoHash(address.getGeoHash())
                .isVerified(address.getIsVerified())
                .isDefault(address.getIsDefault())
                .isDefaultBilling(address.getIsDefaultBilling())
                .formattedAddress(address.getFormattedAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
