package org.zzq.qirbac.permission.dto;

import lombok.Data;
import org.zzq.qirbac.permission.types.PermissionType;

/**
 * 新增权限请求。
 *
 * parentId 为空时表示顶级权限。
 * permissionType 仅支持 FOLDER / MENU / API / BUTTON，由 Spring 自动反序列化校验。
 */
@Data
public class PermissionCreateRequest {

    private String permissionName;
    private Long parentId;
    private String routePath;
    private PermissionType permissionType;
    private Integer sortOrder;
    private Boolean enabled;
}
