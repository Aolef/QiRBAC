package org.zzq.qirbac.role.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.role.entity.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RoleQueryRepository {

    private static final String SELECT_COLUMNS = "id, role_name, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public RoleQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Role> findAll() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM sys_role ORDER BY id ASC",
                this::mapRole
        );
    }

    public List<Role> findPage(String roleName, int pageSize, long offset) {
        if (StringUtils.hasText(roleName)) {
            return jdbcTemplate.query(
                    "SELECT " + SELECT_COLUMNS
                            + " FROM sys_role WHERE role_name LIKE ? ORDER BY id DESC LIMIT ? OFFSET ?",
                    this::mapRole,
                    "%" + roleName + "%",
                    pageSize,
                    offset
            );
        }

        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM sys_role ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRole,
                pageSize,
                offset
        );
    }

    public long count(String roleName) {
        Long total;
        if (StringUtils.hasText(roleName)) {
            total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_role WHERE role_name LIKE ?",
                    Long.class,
                    "%" + roleName + "%"
            );
        } else {
            total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Long.class);
        }
        return total == null ? 0L : total;
    }

    private Role mapRole(ResultSet resultSet, int rowNumber) throws SQLException {
        Role role = new Role(resultSet.getString("role_name"));
        role.setId(resultSet.getLong("id"));
        role.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        role.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return role;
    }
}
