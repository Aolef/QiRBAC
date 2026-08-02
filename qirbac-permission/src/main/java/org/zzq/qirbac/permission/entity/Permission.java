package org.zzq.qirbac.permission.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.zzq.qirbac.permission.types.PermissionType;

/**
 * 权限实体。
 *
 * 权限支持父子级关系，用 parentId 组成权限树，用于归拢分类。
 * 约定 parent_id = 0 表示顶级权限。
 *
 * permissionType 取值见 {@link PermissionType}：
 *   FOLDER 目录（纯分组，可作父，不参与鉴权）
 *   MENU 菜单 / API 接口 / BUTTON 按钮（叶子，参与鉴权，不可作父）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_permission")
public class Permission extends BaseEntity {

    /**
     * 权限名称。
     */
    @Column("permission_name")
    private String permissionName;

    /**
     * 父级权限 ID，0 表示顶级权限。
     */
    @Column("parent_id")
    private Long parentId;

    /**
     * 路由地址。
     *
     * 菜单权限可以放前端路由，接口权限可以放后端接口路径。
     * FOLDER 类型可空。
     */
    @Column("route_path")
    private String routePath;

    /**
     * 权限类型，见 {@link PermissionType}。
     */
    @Column("permission_type")
    private PermissionType permissionType;

    /**
     * 排序值。
     *
     * 数字越小越靠前。
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 是否可用。
     */
    @Column("enabled")
    private Boolean enabled;

    /**
     * 是否逻辑删除：true 已删除，false 未删除。
     */
    @Column("deleted")
    private Boolean deleted;
}
