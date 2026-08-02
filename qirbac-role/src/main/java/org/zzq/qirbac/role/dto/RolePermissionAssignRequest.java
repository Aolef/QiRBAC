package org.zzq.qirbac.role.dto;

import lombok.Data;

import java.util.List;

/**
 * 给角色分配权限的请求。
 *
 * 替换式语义：permissionIds 为空表示清空该角色的全部权限。
 */
@Data
public class RolePermissionAssignRequest {

    private List<Long> permissionIds;
}
