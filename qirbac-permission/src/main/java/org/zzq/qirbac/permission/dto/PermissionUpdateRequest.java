package org.zzq.qirbac.permission.dto;

import lombok.Data;
import org.zzq.qirbac.permission.types.PermissionType;

/**
 * 修改权限请求。
 *
 * parentId 为空时表示顶级权限。
 */
@Data
public class PermissionUpdateRequest {

    private String permissionName;
    private Long parentId;
    private String routePath;
    private PermissionType permissionType;
    private Integer sortOrder;
    private Boolean enabled;
}
