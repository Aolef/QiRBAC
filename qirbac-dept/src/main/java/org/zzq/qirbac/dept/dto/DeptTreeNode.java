package org.zzq.qirbac.dept.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 部门树节点。
 *
 * 全量树查询时 children 有值；懒加载查询时 children 为 null，
 * 前端通过 hasChildren 判断是否需要继续展开。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeptTreeNode {

    private Long id;
    private String deptName;
    private Long parentId;
    private Integer sortOrder;

    /**
     * 是否存在子节点。
     *
     * 懒加载场景下前端据此决定是否显示展开箭头。
     */
    private Boolean hasChildren;

    /**
     * 子节点列表。
     *
     * 全量树时有值；懒加载单层查询时为 null。
     */
    private List<DeptTreeNode> children;
}
