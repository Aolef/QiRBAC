package org.zzq.qirbac.dept.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zzq.qirbac.dept.entity.Dept;

import java.util.List;
import java.util.Optional;

/**
 * 部门表数据库操作。
 *
 * 所有查询都带 deleted = 0 过滤，避免读到逻辑删除的部门。
 */
public interface DeptRepository extends CrudRepository<Dept, Long> {

    String COLUMNS = "id, dept_name, parent_id, sort_order, deleted, created_at, updated_at";

    @Query("SELECT " + COLUMNS + " FROM sys_dept WHERE id = :id AND deleted = 0 LIMIT 1")
    Optional<Dept> findAvailableById(@Param("id") Long id);

    /**
     * 查询某父节点下未删除的子部门，按 sort_order、id 升序。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_dept WHERE parent_id = :parentId AND deleted = 0"
            + " ORDER BY sort_order ASC, id ASC")
    List<Dept> findAvailableByParentId(@Param("parentId") Long parentId);

    /**
     * 查询全部未删除部门，按 sort_order、id 升序。用于全量树。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_dept WHERE deleted = 0"
            + " ORDER BY sort_order ASC, id ASC")
    List<Dept> findAllAvailable();

    /**
     * 统计某父节点下未删除子部门数量。
     */
    @Query("SELECT COUNT(*) FROM sys_dept WHERE parent_id = :parentId AND deleted = 0")
    long countAvailableByParentId(@Param("parentId") Long parentId);

    /**
     * 校验同级下部门名称是否已存在（新增用）。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_dept WHERE parent_id = :parentId AND dept_name = :deptName AND deleted = 0 LIMIT 1")
    Optional<Dept> findAvailableByParentIdAndDeptName(
            @Param("parentId") Long parentId,
            @Param("deptName") String deptName
    );

    /**
     * 校验同级下部门名称是否已存在（修改用，排除自身）。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_dept WHERE parent_id = :parentId AND dept_name = :deptName"
            + " AND deleted = 0 AND id <> :id LIMIT 1")
    Optional<Dept> findAvailableByParentIdAndDeptNameExcludingId(
            @Param("parentId") Long parentId,
            @Param("deptName") String deptName,
            @Param("id") Long id
    );

    /**
     * 查询多个 id 的未删除部门，用于批量校验与摘要查询。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_dept WHERE deleted = 0 AND id IN (:ids)"
            + " ORDER BY sort_order ASC, id ASC")
    List<Dept> findAvailableByIds(@Param("ids") List<Long> ids);
}
