package org.zzq.qirbac.dept.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 部门实体。
 *
 * 部门支持父子级关系，可以用 parentId 组成部门树。
 * 约定 parent_id = 0 表示顶级部门。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_dept")
public class Dept extends BaseEntity {

    /**
     * 部门名称。
     */
    @Column("dept_name")
    private String deptName;

    /**
     * 父级部门 ID，0 表示顶级部门。
     */
    @Column("parent_id")
    private Long parentId;

    /**
     * 同级排序，越小越靠前。
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 是否逻辑删除：true 已删除，false 未删除。
     */
    @Column("deleted")
    private Boolean deleted;
}
