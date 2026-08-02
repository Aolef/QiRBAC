package org.zzq.qirbac.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户角色关系实体。
 *
 * 一条记录表示：某个用户拥有某个角色。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_role")
public class UserRole extends BaseEntity {

    /**
     * 用户 ID。
     */
    @Column("user_id")
    private Long userId;

    /**
     * 角色 ID。
     */
    @Column("role_id")
    private Long roleId;
}
