package org.zzq.qirbac.permission.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zzq.qirbac.permission.entity.Permission;

import java.util.List;
import java.util.Optional;

/**
 * 权限表数据库操作。
 *
 * 所有查询都带 deleted = 0 过滤，避免读到逻辑删除的权限。
 */
public interface PermissionRepository extends CrudRepository<Permission, Long> {

    String COLUMNS = "id, permission_name, parent_id, route_path, permission_type, sort_order, enabled, deleted, created_at, updated_at";

    @Query("SELECT " + COLUMNS + " FROM sys_permission WHERE id = :id AND deleted = 0 LIMIT 1")
    Optional<Permission> findAvailableById(@Param("id") Long id);

    /**
     * 查询某父节点下未删除的子权限，按 sort_order、id 升序。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_permission WHERE parent_id = :parentId AND deleted = 0"
            + " ORDER BY sort_order ASC, id ASC")
    List<Permission> findAvailableByParentId(@Param("parentId") Long parentId);

    /**
     * 查询全部未删除权限，按 sort_order、id 升序。用于全量树。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_permission WHERE deleted = 0"
            + " ORDER BY sort_order ASC, id ASC")
    List<Permission> findAllAvailable();

    /**
     * 统计某父节点下未删除子权限数量。
     */
    @Query("SELECT COUNT(*) FROM sys_permission WHERE parent_id = :parentId AND deleted = 0")
    long countAvailableByParentId(@Param("parentId") Long parentId);

    /**
     * 校验同级下权限名称是否已存在（新增用）。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_permission WHERE parent_id = :parentId AND permission_name = :permissionName"
            + " AND deleted = 0 LIMIT 1")
    Optional<Permission> findAvailableByParentIdAndPermissionName(
            @Param("parentId") Long parentId,
            @Param("permissionName") String permissionName
    );

    /**
     * 校验同级下权限名称是否已存在（修改用，排除自身）。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_permission WHERE parent_id = :parentId AND permission_name = :permissionName"
            + " AND deleted = 0 AND id <> :id LIMIT 1")
    Optional<Permission> findAvailableByParentIdAndPermissionNameExcludingId(
            @Param("parentId") Long parentId,
            @Param("permissionName") String permissionName,
            @Param("id") Long id
    );

    /**
     * 查询多个 id 的未删除权限，用于批量校验与摘要查询。
     */
    @Query("SELECT " + COLUMNS
            + " FROM sys_permission WHERE deleted = 0 AND id IN (:ids)"
            + " ORDER BY sort_order ASC, id ASC")
    List<Permission> findAvailableByIds(@Param("ids") List<Long> ids);

    /**
     * 查询某用户拥有的全部启用权限。
     *
     * 链路：sys_user_role → sys_role_permission → sys_permission。
     * 仅返回 deleted = 0 且 enabled = 1 的权限，已去重。
     */
    @Query("SELECT DISTINCT " + COLUMNS
            + " FROM sys_permission p"
            + " WHERE p.deleted = 0 AND p.enabled = 1"
            + "   AND p.id IN ("
            + "       SELECT rp.permission_id FROM sys_role_permission rp"
            + "       INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id"
            + "       WHERE ur.user_id = :userId"
            + "   )"
            + " ORDER BY p.sort_order ASC, p.id ASC")
    List<Permission> findAvailableEnabledByUserId(@Param("userId") Long userId);
}
