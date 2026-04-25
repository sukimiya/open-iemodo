package com.iemodo.user.controller;

import com.iemodo.common.response.Response;
import com.iemodo.user.dto.UpdateUserRequest;
import com.iemodo.user.dto.UserDTO;
import com.iemodo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * User profile management endpoints.
 * All requests require a valid JWT (enforced at the API Gateway).
 * The gateway injects {@code X-User-ID} from the verified JWT.
 */
@Slf4j
@RestController
@RequestMapping("/uc/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /uc/api/v1/users/me
     * Returns the current authenticated user's profile.
     */
    @GetMapping("/me")
    public Mono<Response<UserDTO>> getCurrentUser(
            @RequestHeader("X-User-ID") Long userId) {
        return userService.getUser(userId).map(Response::success);
    }

    /**
     * PUT /uc/api/v1/users/me
     * Updates the current user's display name and/or avatar URL.
     */
    @PutMapping("/me")
    public Mono<Response<UserDTO>> updateCurrentUser(
            @RequestHeader("X-User-ID") Long userId,
            @RequestBody @Valid UpdateUserRequest request) {
        return userService.updateUser(userId, request).map(Response::success);
    }

    /**
     * GET /uc/api/v1/users
     * Admin endpoint — list all users. Optional ?tenantId= filter.
     */
    @GetMapping
    public Mono<Response<java.util.List<UserDTO>>> getAllUsers(
            @RequestParam(value = "tenantId", required = false) String tenantId) {
        Flux<UserDTO> users = tenantId != null
                ? userService.getUsersByTenant(tenantId)
                : userService.getAllUsers();
        return users.collectList().map(Response::success);
    }

    /**
     * GET /uc/api/v1/users/{userId}
     * Admin endpoint — returns any user by ID.
     */
    @GetMapping("/{userId}")
    public Mono<Response<UserDTO>> getUserById(@PathVariable("userId") Long userId) {
        return userService.getUser(userId).map(Response::success);
    }
}
