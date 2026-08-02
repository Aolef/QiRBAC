package org.zzq.qirbac.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserRelationRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRelationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findRoleIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_user_role WHERE user_id = ? ORDER BY role_id",
                Long.class,
                userId
        );
    }

    public List<Long> findDeptIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT dept_id FROM sys_user_dept WHERE user_id = ? ORDER BY dept_id",
                Long.class,
                userId
        );
    }

    public Map<Long, List<Long>> findRoleIdsByUserIds(Collection<Long> userIds) {
        return queryRelations("sys_user_role", "role_id", userIds);
    }

    public Map<Long, List<Long>> findDeptIdsByUserIds(Collection<Long> userIds) {
        return queryRelations("sys_user_dept", "dept_id", userIds);
    }

    public void replaceRoles(Long userId, Collection<Long> roleIds) {
        deleteRolesByUserId(userId);
        batchInsert("sys_user_role", "role_id", userId, roleIds);
    }

    public void replaceDepts(Long userId, Collection<Long> deptIds) {
        deleteDeptsByUserId(userId);
        batchInsert("sys_user_dept", "dept_id", userId, deptIds);
    }

    public void deleteRolesByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
    }

    public void deleteDeptsByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM sys_user_dept WHERE user_id = ?", userId);
    }

    public void deleteRolesByUserIds(Collection<Long> userIds) {
        deleteByUserIds("sys_user_role", userIds);
    }

    public void deleteDeptsByUserIds(Collection<Long> userIds) {
        deleteByUserIds("sys_user_dept", userIds);
    }

    private Map<Long, List<Long>> queryRelations(
            String table,
            String relationColumn,
            Collection<Long> userIds
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<Long>> result = new LinkedHashMap<>();
        String sql = "SELECT user_id, " + relationColumn + " FROM " + table
                + " WHERE user_id IN (" + placeholders(userIds.size()) + ")"
                + " ORDER BY user_id, " + relationColumn;
        jdbcTemplate.query(sql, rs -> {
            Long userId = rs.getLong("user_id");
            Long relationId = rs.getLong(relationColumn);
            result.computeIfAbsent(userId, ignored -> new java.util.ArrayList<>()).add(relationId);
        }, userIds.toArray());
        return result;
    }

    private void batchInsert(
            String table,
            String relationColumn,
            Long userId,
            Collection<Long> relationIds
    ) {
        if (relationIds == null || relationIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO " + table + " (user_id, " + relationColumn + ") VALUES (?, ?)";
        jdbcTemplate.batchUpdate(
                sql,
                relationIds,
                relationIds.size(),
                (statement, relationId) -> {
                    statement.setLong(1, userId);
                    statement.setLong(2, relationId);
                }
        );
    }

    private void deleteByUserIds(String table, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM " + table + " WHERE user_id IN ("
                + placeholders(userIds.size()) + ")";
        jdbcTemplate.update(sql, userIds.toArray());
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }
}
