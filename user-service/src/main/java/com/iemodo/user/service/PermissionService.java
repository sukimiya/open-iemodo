package com.iemodo.user.service;

import com.iemodo.common.enums.AdminRole;
import com.iemodo.user.domain.AdminPermission;
import com.iemodo.user.repository.AdminPermissionRepository;
import com.iemodo.user.repository.AdminRolePermissionRepository;
import com.iemodo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;
    private final AdminPermissionRepository permissionRepository;
    private final AdminRolePermissionRepository rolePermissionRepository;

    /**
     * Check if a user has a specific permission.
     */
    public Mono<Boolean> hasPermission(Long userId, String permissionCode) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    if (user.getRole() == null) return Mono.just(false);
                    if ("SUPER_ADMIN".equals(user.getRole())) return Mono.just(true);
                    return rolePermissionRepository.findPermissionsByRole(user.getRole())
                            .any(p -> p.getCode().equals(permissionCode));
                })
                .defaultIfEmpty(false);
    }

    /**
     * Get all permissions grouped by module for a user's role.
     */
    public Mono<Map<String, List<AdminPermission>>> getPermissionsByUser(Long userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    if (user.getRole() == null)
                        return Mono.<Map<String, List<AdminPermission>>>just(Map.of());
                    if ("SUPER_ADMIN".equals(user.getRole())) {
                        return permissionRepository.findAllByOrderByModuleAsc()
                                .collect(Collectors.groupingBy(AdminPermission::getModule,
                                        Collectors.toList()));
                    }
                    return rolePermissionRepository.findPermissionsByRole(user.getRole())
                            .collect(Collectors.groupingBy(AdminPermission::getModule,
                                    Collectors.toList()));
                })
                .defaultIfEmpty(Map.of());
    }

    /**
     * Get all permissions for a role (admin use).
     */
    public Flux<AdminPermission> getPermissionsByRole(String role) {
        return rolePermissionRepository.findPermissionsByRole(role);
    }

    /**
     * Get all available permissions (admin use).
     */
    public Flux<AdminPermission> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAsc();
    }

    /**
     * Update role-permission assignments.
     */
    public Mono<Void> updateRolePermissions(String role, List<Long> permissionIds) {
        return rolePermissionRepository.deleteByRole(role)
                .thenMany(Flux.fromIterable(permissionIds))
                .flatMap(permId -> {
                    long id = 350000L + (long) (Math.random() * 50000);
                    return rolePermissionRepository.addRolePermission(id, role, permId);
                })
                .then();
    }
}
