package com.iemodo.user.controller;

import com.iemodo.common.enums.AdminRole;
import com.iemodo.common.response.Response;
import com.iemodo.user.domain.AdminPermission;
import com.iemodo.user.dto.UserDTO;
import com.iemodo.user.service.PermissionService;
import com.iemodo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/uc/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final PermissionService permissionService;

    /**
     * GET /uc/api/v1/admin/permissions — all available permissions.
     */
    @GetMapping("/permissions")
    public Mono<Response<List<AdminPermission>>> getAllPermissions() {
        return permissionService.getAllPermissions()
                .collectList()
                .map(Response::success);
    }

    /**
     * GET /uc/api/v1/admin/permissions/my — current user's permissions (grouped by module).
     */
    @GetMapping("/permissions/my")
    public Mono<Response<Map<String, List<AdminPermission>>>> getMyPermissions(
            @RequestHeader("X-User-ID") Long userId) {
        return permissionService.getPermissionsByUser(userId)
                .map(Response::success);
    }

    /**
     * GET /uc/api/v1/admin/roles — list all roles.
     */
    @GetMapping("/roles")
    public Mono<Response<List<Map<String, Object>>>> getRoles() {
        List<Map<String, Object>> roles = List.of(
                Map.of("role", "SUPER_ADMIN", "name", "超级管理员", "description", "所有权限，包括角色管理"),
                Map.of("role", "TENANT_ADMIN", "name", "租户管理员", "description", "管理租户、用户、订单、账单"),
                Map.of("role", "SUPPORT", "name", "客服", "description", "查看用户和订单，管理用户"),
                Map.of("role", "BILLING", "name", "财务", "description", "查看和管理账单"),
                Map.of("role", "ANALYST", "name", "数据分析师", "description", "查看仪表盘和数据")
        );
        return Mono.just(Response.success(roles));
    }

    /**
     * GET /uc/api/v1/admin/roles/{role}/permissions — permissions for a role.
     */
    @GetMapping("/roles/{role}/permissions")
    public Mono<Response<List<String>>> getRolePermissions(@PathVariable("role") String role) {
        return permissionService.getPermissionsByRole(role)
                .map(AdminPermission::getCode)
                .collectList()
                .map(Response::success);
    }

    /**
     * PUT /uc/api/v1/admin/roles/{role}/permissions — update role permissions.
     */
    @PutMapping("/roles/{role}/permissions")
    public Mono<Response<Void>> updateRolePermissions(
            @PathVariable("role") String role,
            @RequestBody List<Long> permissionIds) {
        return permissionService.updateRolePermissions(role, permissionIds)
                .then(Mono.just(Response.success()));
    }

    /**
     * PUT /uc/api/v1/admin/users/{userId}/role — update user role.
     */
    @PutMapping("/users/{userId}/role")
    public Mono<Response<UserDTO>> updateUserRole(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null) {
            return Mono.just(Response.error(400, "role is required"));
        }
        return userService.updateUserRole(userId, role)
                .map(Response::success);
    }
}
