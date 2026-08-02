package org.zzq.qirbac.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 角色权限关系实体。
 *
 * 一条记录表示：某个角色拥有某个权限。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_permission")
public class RolePermission extends BaseEntity {

    /**
     * 角色 ID。
     */
    @Column("role_id")
    private Long roleId;

    /**
     * 权限 ID。
     */
    @Column("permission_id")
    private Long permissionId;
}
