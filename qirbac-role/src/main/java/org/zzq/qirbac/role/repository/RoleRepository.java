package org.zzq.qirbac.role.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zzq.qirbac.role.entity.Role;

import java.util.Optional;

public interface RoleRepository extends CrudRepository<Role, Long> {

    @Query("SELECT id, role_name, created_at, updated_at FROM sys_role WHERE role_name = :roleName LIMIT 1")
    Optional<Role> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT id, role_name, created_at, updated_at FROM sys_role WHERE role_name = :roleName AND id <> :id LIMIT 1")
    Optional<Role> findByRoleNameExcludingId(
            @Param("roleName") String roleName,
            @Param("id") Long id
    );
}
