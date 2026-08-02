package org.zzq.qirbac.role.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class RoleRelationRepository {

    private final JdbcTemplate jdbcTemplate;

    public RoleRelationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteByRoleId(Long roleId) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
    }

    public void deleteByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(roleIds.size(), "?"));
        Object[] arguments = roleIds.toArray();
        jdbcTemplate.update(
                "DELETE FROM sys_user_role WHERE role_id IN (" + placeholders + ")",
                arguments
        );
        jdbcTemplate.update(
                "DELETE FROM sys_role_permission WHERE role_id IN (" + placeholders + ")",
                arguments
        );
    }

    /**
     * 查询某角色已分配的权限 id 列表，按 permission_id 升序。
     */
    public List<Long> findPermissionIdsByRoleId(Long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT permission_id FROM sys_role_permission WHERE role_id = ? ORDER BY permission_id",
                Long.class,
                roleId
        );
    }

    /**
     * 替换式更新角色的权限：先删后插，与 user 模块分配角色/部门语义一致。
     * permissionIds 为空时表示清空该角色的全部权限。
     */
    public void replacePermissions(Long roleId, Collection<Long> permissionIds) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                permissionIds,
                permissionIds.size(),
                (statement, permissionId) -> {
                    statement.setLong(1, roleId);
                    statement.setLong(2, permissionId);
                }
        );
    }
}

