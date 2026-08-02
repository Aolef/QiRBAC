package org.zzq.qirbac.role.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.role.dto.RoleSummary;
import org.zzq.qirbac.role.entity.Role;
import org.zzq.qirbac.role.repository.RoleRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class RoleQueryService {

    private final RoleRepository roleRepository;

    public RoleQueryService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public void validateRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        Set<Long> requestedIds = normalizeIds(roleIds);
        Map<Long, RoleSummary> roles = findRoleSummaries(requestedIds);
        if (roles.size() != requestedIds.size()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, RoleSummary> findRoleSummaries(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> requestedIds = normalizeIds(roleIds);
        Map<Long, RoleSummary> result = new LinkedHashMap<>();
        roleRepository.findAllById(requestedIds).forEach(role -> result.put(
                role.getId(),
                new RoleSummary(role.getId(), role.getRoleName())
        ));
        return result;
    }

    private Set<Long> normalizeIds(Collection<Long> roleIds) {
        if (roleIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new LinkedHashSet<>(roleIds);
    }
}
