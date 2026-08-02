package org.zzq.qirbac.role.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;

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
}
