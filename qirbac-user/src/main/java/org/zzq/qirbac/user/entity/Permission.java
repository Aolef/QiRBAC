package org.zzq.qirbac.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 权限实体。
 *
 * 权限可以表示菜单、接口或按钮。
 * 第一版 permissionType 使用字符串，建议取值：MENU、API、BUTTON。
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
     * 路由地址。
     *
     * 菜单权限可以放前端路由，接口权限可以放后端接口路径。
     */
    @Column("route_path")
    private String routePath;

    /**
     * 权限类型。
     *
     * 建议值：
     * MENU：菜单
     * API：接口
     * BUTTON：按钮
     */
    @Column("permission_type")
    private String permissionType;

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
}
