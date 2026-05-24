package com.iemodo.customer.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.customer.domain.Customer;
import com.iemodo.customer.dto.CustomerDTO;
import com.iemodo.customer.dto.UpdateCustomerRequest;
import com.iemodo.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Mono<CustomerDTO> getById(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .map(this::toDTO);
    }

    public Mono<CustomerDTO> updateProfile(Long id, UpdateCustomerRequest request) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(customer -> {
                    if (request.getDisplayName() != null) {
                        customer.setDisplayName(request.getDisplayName());
                    }
                    if (request.getFirstName() != null) {
                        customer.setFirstName(request.getFirstName());
                    }
                    if (request.getLastName() != null) {
                        customer.setLastName(request.getLastName());
                    }
                    if (request.getAvatarUrl() != null) {
                        customer.setAvatarUrl(request.getAvatarUrl());
                    }
                    if (request.getPreferredCurrency() != null) {
                        customer.setPreferredCurrency(request.getPreferredCurrency());
                    }
                    if (request.getPreferredLanguage() != null) {
                        customer.setPreferredLanguage(request.getPreferredLanguage());
                    }
                    if (request.getPreferredCountry() != null) {
                        customer.setPreferredCountry(request.getPreferredCountry());
                    }
                    return customerRepository.save(customer);
                })
                .map(this::toDTO);
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    public Flux<CustomerDTO> getAllCustomers(String tenantId) {
        Flux<Customer> customers = tenantId != null && !tenantId.isBlank()
                ? customerRepository.findAllByTenantIdAndIsValid(tenantId, true)
                : customerRepository.findAllByIsValid(true);
        return customers.map(this::toDTO);
    }

    public Mono<Void> suspendCustomer(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(customer -> {
                    customer.setStatus(0);
                    return customerRepository.save(customer);
                })
                .then();
    }

    public Mono<Void> activateCustomer(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(customer -> {
                    customer.setStatus(1);
                    return customerRepository.save(customer);
                })
                .then();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .displayName(customer.getDisplayName())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .avatarUrl(customer.getAvatarUrl())
                .oauthProvider(customer.getOauthProvider())
                .phoneVerified(customer.getPhoneVerified())
                .emailVerified(customer.getEmailVerified())
                .preferredCurrency(customer.getPreferredCurrency())
                .preferredLanguage(customer.getPreferredLanguage())
                .preferredCountry(customer.getPreferredCountry())
                .status(customer.getStatus())
                .lastLoginIp(customer.getLastLoginIp())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
