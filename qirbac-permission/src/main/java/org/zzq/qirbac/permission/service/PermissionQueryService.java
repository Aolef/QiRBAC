package org.zzq.qirbac.permission.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.permission.entity.Permission;
import org.zzq.qirbac.permission.repository.PermissionRepository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限查询服务。
 *
 * 提供给其他模块（如 role）跨模块校验权限 id 合法性的入口，
 * 仿 qirbac-role 的 RoleQueryService 设计。
 */
@Service
public class PermissionQueryService {

    private final PermissionRepository permissionRepository;

    public PermissionQueryService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * 校验给定 permissionId 是否全部存在且未逻辑删除。
     * role 模块分配权限时调用。
     */
    @Transactional(readOnly = true)
    public void validatePermissionIds(Collection<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        Set<Long> requestedIds = permissionIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            return;
        }
        List<Permission> found = permissionRepository.findAvailableByIds(List.copyOf(requestedIds));
        if (found.size() != requestedIds.size()) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }
    }
}
