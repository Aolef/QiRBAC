package org.zzq.qirbac.role.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zzq.qirbac.permission.types.PermissionType;

import java.util.List;

/**
 * 角色权限树节点。
 *
 * 在全量权限树基础上增加 assigned 字段，标记该权限是否已分配给目标角色。
 * 用于角色权限分配页面的回显：前端拿到全树 + 勾选标记，一次渲染勾选框。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionTreeNode {

    private Long id;
    private String permissionName;
    private Long parentId;
    private String routePath;
    private PermissionType permissionType;
    private Integer sortOrder;
    private Boolean enabled;

    /**
     * 该权限是否已分配给当前角色。
     */
    private Boolean assigned;

    private Boolean hasChildren;
    private List<RolePermissionTreeNode> children;
}
