package com.iemodo.user.repository;

import com.iemodo.user.domain.AdminPermission;
import com.iemodo.user.domain.AdminRolePermission;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AdminRolePermissionRepository extends R2dbcRepository<AdminRolePermission, Long> {

    @Query("""
        SELECT p.* FROM admin_permissions p
        JOIN admin_role_permissions rp ON rp.permission_id = p.id
        WHERE rp.role = :role AND rp.is_valid = TRUE AND p.is_valid = TRUE
        """)
    Flux<AdminPermission> findPermissionsByRole(String role);

    Mono<Void> deleteByRole(String role);

    @Query("INSERT INTO admin_role_permissions (id, role, permission_id) VALUES (:id, :role, :permissionId)")
    Mono<Void> addRolePermission(long id, String role, long permissionId);
}
