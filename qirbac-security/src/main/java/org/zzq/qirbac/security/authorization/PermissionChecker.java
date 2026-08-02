package org.zzq.qirbac.security.authorization;

/**
 * 权限模块接入安全过滤链的扩展点。
 *
 * qirbac-security 只定义鉴权契约，不依赖角色、权限表的具体实现。
 * 未来由独立的 RBAC 模块实现该接口。
 */
public interface PermissionChecker {

    boolean hasPermission(Long userId, String requestMethod, String requestPath);
}
