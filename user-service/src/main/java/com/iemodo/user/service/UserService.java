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

    public Mono<UserDTO> updateUser(Long userId, UpdateUserRequest request) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    if (request.getDisplayName() != null) {
                        user.setDisplayName(request.getDisplayName());
                    }
                    if (request.getAvatarUrl() != null) {
                        user.setAvatarUrl(request.getAvatarUrl());
                    }
                    return userRepository.save(user);
                })
                .map(this::toDTO)
                .doOnSuccess(u -> log.info("Updated profile userId={}", userId));
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .oauthProvider(user.getOauthProvider())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
