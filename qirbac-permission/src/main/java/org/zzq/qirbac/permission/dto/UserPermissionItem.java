package org.zzq.qirbac.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zzq.qirbac.permission.types.PermissionType;

/**
 * 当前登录用户拥有的单条权限。
 *
 * 用于 /permissions/me 接口，返回扁平 list（不建树），
 * 前端据此做菜单渲染或按钮控制。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionItem {

    private Long id;
    private String permissionName;
    private Long parentId;
    private String routePath;
    private PermissionType permissionType;
    private Integer sortOrder;
}
