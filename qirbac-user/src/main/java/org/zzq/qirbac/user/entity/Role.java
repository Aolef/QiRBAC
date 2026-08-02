package org.zzq.qirbac.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 角色实体。
 *
 * 角色用于给用户分组授权，例如管理员、普通用户、审核员。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class Role extends BaseEntity {

    /**
     * 角色名称。
     */
    @Column("role_name")
    private String roleName;
}
