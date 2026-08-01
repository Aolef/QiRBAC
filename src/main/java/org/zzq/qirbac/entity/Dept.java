package org.zzq.qirbac.entity;

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
     * 父级部门 ID。
     *
     * 第一版约定 0 表示顶级部门。
     */
    @Column("parent_id")
    private Long parentId;
}
