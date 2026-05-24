package com.iemodo.customer.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.customer.domain.CustomerAddress;
import com.iemodo.customer.dto.AddressDTO;
import com.iemodo.customer.dto.CreateAddressRequest;
import com.iemodo.customer.repository.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;

    public Flux<AddressDTO> getAddresses(Long customerId) {
        return addressRepository.findAllByCustomerId(customerId)
                .map(this::toDTO);
    }

    public Mono<AddressDTO> getAddress(Long customerId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(addr -> addr.getCustomerId().equals(customerId))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND, "Address not found")))
                .map(this::toDTO);
    }

    public Mono<AddressDTO> getDefaultAddress(Long customerId) {
        return addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .map(this::toDTO)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND, "No default address found")));
    }

    @Transactional
    public Mono<AddressDTO> createAddress(Long customerId, CreateAddressRequest request) {
        return addressRepository.countByCustomerId(customerId)
                .flatMap(count -> {
                    boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.getIsDefault());
                    boolean makeDefaultBilling = count == 0 || Boolean.TRUE.equals(request.getIsDefaultBilling());

                    CustomerAddress address = CustomerAddress.builder()
                            .customerId(customerId)
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

                    Mono<Integer> clearDefaults = makeDefault
                            ? addressRepository.clearDefaultByCustomerId(customerId)
                            : Mono.just(0);
                    Mono<Integer> clearBilling = makeDefaultBilling
                            ? addressRepository.clearDefaultBillingByCustomerId(customerId)
                            : Mono.just(0);

                    return Mono.zip(clearDefaults, clearBilling)
                            .then(addressRepository.save(address));
                })
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Created address id={} for customerId={}", dto.getId(), customerId));
    }

    @Transactional
    public Mono<Void> deleteAddress(Long customerId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(addr -> addr.getCustomerId().equals(customerId))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND, "Address not found")))
                .flatMap(addr -> addressRepository.deleteById(addressId))
                .doOnSuccess(v -> log.info("Deleted address id={} for customerId={}", addressId, customerId));
    }

    @Transactional
    public Mono<AddressDTO> setDefaultAddress(Long customerId, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(addr -> addr.getCustomerId().equals(customerId))
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND, "Address not found")))
                .flatMap(addr -> addressRepository.clearDefaultByCustomerId(customerId)
                        .then(Mono.defer(() -> {
                            addr.setIsDefault(true);
                            return addressRepository.save(addr);
                        })))
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Set default address id={} for customerId={}", addressId, customerId));
    }

    private AddressDTO toDTO(CustomerAddress address) {
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
