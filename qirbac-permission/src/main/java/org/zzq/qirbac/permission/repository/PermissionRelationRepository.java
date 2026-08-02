package org.zzq.qirbac.permission.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;

/**
 * 权限相关的关系表操作。
 *
 * 目前只处理 sys_role_permission：权限被删除时，需要清理挂在该权限下的角色关系。
 * 权限本身做逻辑删除，关系表是物理删除（关系本身没有保留价值）。
 */
@Repository
public class PermissionRelationRepository {

    private final JdbcTemplate jdbcTemplate;

    public PermissionRelationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 删除挂在单个权限下的全部角色关系。
     */
    public void deleteRolePermissionsByPermissionId(Long permissionId) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE permission_id = ?", permissionId);
    }

    /**
     * 批量删除挂在多个权限下的全部角色关系。
     */
    public void deleteRolePermissionsByPermissionIds(Collection<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(permissionIds.size(), "?"));
        jdbcTemplate.update(
                "DELETE FROM sys_role_permission WHERE permission_id IN (" + placeholders + ")",
                permissionIds.toArray()
        );
    }
}
