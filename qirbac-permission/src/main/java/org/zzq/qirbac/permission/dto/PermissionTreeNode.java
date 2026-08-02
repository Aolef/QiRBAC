package org.zzq.qirbac.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zzq.qirbac.permission.types.PermissionType;

import java.util.List;

/**
 * 权限树节点。
 *
 * 全量树查询时 children 有值；懒加载查询时 children 为 null，
 * 前端通过 hasChildren 判断是否需要继续展开。
 *
 * 不返回 description，前端按 permissionType 自行映射文案。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionTreeNode {

    private Long id;
    private String permissionName;
    private Long parentId;
    private String routePath;
    private PermissionType permissionType;
    private Integer sortOrder;
    private Boolean enabled;

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
    private List<PermissionTreeNode> children;
}
