package org.zzq.qirbac.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户部门关系实体。
 *
 * 一条记录表示：某个用户属于某个部门。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_dept")
public class UserDept extends BaseEntity {

    /**
     * 用户 ID。
     */
    @Column("user_id")
    private Long userId;

    /**
     * 部门 ID。
     */
    @Column("dept_id")
    private Long deptId;
}
