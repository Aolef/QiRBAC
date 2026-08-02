package org.zzq.qirbac.dept.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;

/**
 * 部门相关的关系表操作。
 *
 * 目前只处理 sys_user_dept：部门被删除时，需要清理挂在该部门下的用户关系。
 * 部门本身做逻辑删除，关系表是物理删除（关系本身没有保留价值）。
 */
@Repository
public class DeptRelationRepository {

    private final JdbcTemplate jdbcTemplate;

    public DeptRelationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 删除挂在单个部门下的全部用户关系。
     */
    public void deleteUserDeptsByDeptId(Long deptId) {
        jdbcTemplate.update("DELETE FROM sys_user_dept WHERE dept_id = ?", deptId);
    }

    /**
     * 批量删除挂在多个部门下的全部用户关系。
     */
    public void deleteUserDeptsByDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(deptIds.size(), "?"));
        jdbcTemplate.update(
                "DELETE FROM sys_user_dept WHERE dept_id IN (" + placeholders + ")",
                deptIds.toArray()
        );
    }
}
