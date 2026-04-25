package com.iemodo.user.repository;

import com.iemodo.user.domain.AdminPermission;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AdminPermissionRepository extends R2dbcRepository<AdminPermission, Long> {
    Flux<AdminPermission> findAllByOrderByModuleAsc();
}
