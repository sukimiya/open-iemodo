package com.iemodo.user.controller;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import com.iemodo.user.domain.User;
import com.iemodo.user.dto.UserDTO;
import com.iemodo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * User management endpoints (requires valid JWT, enforced at gateway).
 */
@Slf4j
@RestController
@RequestMapping("/uc/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * GET /uc/api/v1/users/me
     * Returns the current authenticated user's profile.
     * X-User-ID is set by the gateway after JWT validation.
     */
    @GetMapping("/me")
    public Mono<Response<UserDTO>> getCurrentUser(
            @RequestHeader("X-User-ID") Long userId) {

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .map(user -> Response.success(toDTO(user)));
    }

    /**
     * GET /uc/api/v1/users/{userId}
     * Admin endpoint — returns any user by ID.
     */
    @GetMapping("/{userId}")
    public Mono<Response<UserDTO>> getUserById(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .map(user -> Response.success(toDTO(user)));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

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
