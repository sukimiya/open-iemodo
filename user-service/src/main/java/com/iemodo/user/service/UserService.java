package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.User;
import com.iemodo.user.dto.UpdateUserRequest;
import com.iemodo.user.dto.UserDTO;
import com.iemodo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * User profile CRUD service.
 *
 * <p>Handles profile reads and updates. Authentication (register/login/JWT)
 * is handled separately by {@link AuthService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Mono<UserDTO> getUser(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .map(this::toDTO);
    }

    public Flux<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .map(this::toDTO);
    }

    public Flux<UserDTO> getUsersByTenant(String tenantId) {
        return userRepository.findAllByTenantId(tenantId)
                .map(this::toDTO);
    }

    public Mono<UserDTO> updateUser(Long userId, UpdateUserRequest request) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    if (request.getDisplayName() != null) {
                        user.setDisplayName(request.getDisplayName());
                    }
                    if (request.getFirstName() != null) {
                        user.setFirstName(request.getFirstName());
                    }
                    if (request.getLastName() != null) {
                        user.setLastName(request.getLastName());
                    }
                    if (request.getPhone() != null) {
                        user.setPhone(request.getPhone());
                    }
                    if (request.getAvatarUrl() != null) {
                        user.setAvatarUrl(request.getAvatarUrl());
                    }
                    if (request.getPreferredCurrency() != null) {
                        user.setPreferredCurrency(request.getPreferredCurrency().toUpperCase());
                    }
                    if (request.getPreferredLanguage() != null) {
                        user.setPreferredLanguage(request.getPreferredLanguage().toLowerCase());
                    }
                    if (request.getPreferredCountry() != null) {
                        user.setPreferredCountry(request.getPreferredCountry().toUpperCase());
                    }
                    return userRepository.save(user);
                })
                .map(this::toDTO)
                .doOnSuccess(u -> log.info("Updated profile userId={}", userId));
    }

    public Mono<UserDTO> updateUserRole(Long userId, String role) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    user.setRole(role);
                    return userRepository.save(user);
                })
                .map(this::toDTO)
                .doOnSuccess(u -> log.info("Updated role userId={} role={}", userId, role));
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .oauthProvider(user.getOauthProvider())
                .status(user.getStatus() != null && user.getStatus() == 1 ? "ACTIVE" : "DISABLED")
                .role(user.getRole() != null ? user.getRole() : "TENANT_ADMIN")
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .preferredCurrency(user.getPreferredCurrency())
                .preferredLanguage(user.getPreferredLanguage())
                .preferredCountry(user.getPreferredCountry())
                .totalOrders(user.getTotalOrders())
                .totalSpent(user.getTotalSpent())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
