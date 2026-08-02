package org.zzq.qirbac.role.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.PageResult;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.permission.dto.PermissionTreeNode;
import org.zzq.qirbac.permission.service.PermissionQueryService;
import org.zzq.qirbac.permission.service.PermissionService;
import org.zzq.qirbac.role.dto.RoleCreateRequest;
import org.zzq.qirbac.role.dto.RolePermissionAssignRequest;
import org.zzq.qirbac.role.dto.RolePermissionTreeNode;
import org.zzq.qirbac.role.dto.RoleResponse;
import org.zzq.qirbac.role.dto.RoleUpdateRequest;
import org.zzq.qirbac.role.entity.Role;
import org.zzq.qirbac.role.repository.RoleQueryRepository;
import org.zzq.qirbac.role.repository.RoleRelationRepository;
import org.zzq.qirbac.role.repository.RoleRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private static final int MAX_ROLE_NAME_LENGTH = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final RoleRepository roleRepository;
    private final RoleQueryRepository roleQueryRepository;
    private final RoleRelationRepository roleRelationRepository;
    private final PermissionService permissionService;
    private final PermissionQueryService permissionQueryService;

    public RoleService(
            RoleRepository roleRepository,
            RoleQueryRepository roleQueryRepository,
            RoleRelationRepository roleRelationRepository,
            PermissionService permissionService,
            PermissionQueryService permissionQueryService
    ) {
        this.roleRepository = roleRepository;
        this.roleQueryRepository = roleQueryRepository;
        this.roleRelationRepository = roleRelationRepository;
        this.permissionService = permissionService;
        this.permissionQueryService = permissionQueryService;
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        String roleName = normalizeRoleName(request == null ? null : request.getRoleName());
        ensureRoleNameAvailable(roleName, null);
        Role savedRole = saveRole(new Role(roleName));
        return toResponse(savedRole);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        checkRoleId(id);
        String roleName = normalizeRoleName(request == null ? null : request.getRoleName());
        Role role = getRole(id);
        ensureRoleNameAvailable(roleName, id);
        role.setRoleName(roleName);
        return toResponse(saveRole(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        checkRoleId(id);
        getRole(id);
        roleRelationRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    @Transactional
    public void batchDeleteRoles(Collection<Long> ids) {
        List<Long> roleIds = normalizeRoleIds(ids);
        List<Role> roles = new ArrayList<>();
        roleRepository.findAllById(roleIds).forEach(roles::add);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        roleRelationRepository.deleteByRoleIds(roleIds);
        roleRepository.deleteAllById(roleIds);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleQueryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<RoleResponse> getRolePage(Integer page, Integer pageSize, String roleName) {
        checkPageArguments(page, pageSize);
        String keyword = StringUtils.hasText(roleName) ? roleName.trim() : null;
        long total = roleQueryRepository.count(keyword);
        long offset = (long) (page - 1) * pageSize;
        List<RoleResponse> records = roleQueryRepository.findPage(keyword, pageSize, offset)
                .stream()
                .map(this::toResponse)
                .toList();
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(records, total, page, pageSize, totalPages);
    }

    // ===== 角色权限分配 =====

    /**
     * 给角色分配权限（替换式）。
     *
     * permissionIds 为空表示清空该角色的全部权限。
     * 校验：角色存在 + 所有 permissionId 存在且未删除。
     */
    @Transactional
    public void assignPermissions(Long roleId, RolePermissionAssignRequest request) {
        checkRoleId(roleId);
        getRole(roleId);  // 校验角色存在
        List<Long> permissionIds = normalizePermissionIds(request == null ? null : request.getPermissionIds());
        permissionQueryService.validatePermissionIds(permissionIds);
        roleRelationRepository.replacePermissions(roleId, permissionIds);
    }

    /**
     * 回显角色权限：返回带 assigned 勾选标记的全量权限树。
     */
    @Transactional(readOnly = true)
    public List<RolePermissionTreeNode> getRolePermissionTree(Long roleId) {
        checkRoleId(roleId);
        getRole(roleId);  // 校验角色存在
        List<PermissionTreeNode> fullTree = permissionService.getFullTree();
        Set<Long> assignedIds = new HashSet<>(roleRelationRepository.findPermissionIdsByRoleId(roleId));
        return markAssigned(fullTree, assignedIds);
    }

    private List<RolePermissionTreeNode> markAssigned(List<PermissionTreeNode> nodes, Set<Long> assignedIds) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream()
                .map(node -> {
                    List<RolePermissionTreeNode> children = markAssigned(node.getChildren(), assignedIds);
                    return new RolePermissionTreeNode(
                            node.getId(),
                            node.getPermissionName(),
                            node.getParentId(),
                            node.getRoutePath(),
                            node.getPermissionType(),
                            node.getSortOrder(),
                            node.getEnabled(),
                            assignedIds.contains(node.getId()),
                            !children.isEmpty(),
                            children.isEmpty() ? null : children
                    );
                })
                .toList();
    }

    private List<Long> normalizePermissionIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private Role getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.ROLE_NOT_FOUND));
    }

    private Role saveRole(Role role) {
        try {
            return roleRepository.save(role);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.ROLE_NAME_ALREADY_EXISTS);
        }
    }

    private String normalizeRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new BusinessException(ResultCode.INVALID_ROLE_NAME);
        }
        String normalized = roleName.trim();
        if (normalized.length() > MAX_ROLE_NAME_LENGTH) {
            throw new BusinessException(ResultCode.INVALID_ROLE_NAME);
        }
        return normalized;
    }

    private void ensureRoleNameAvailable(String roleName, Long excludedId) {
        boolean exists = excludedId == null
                ? roleRepository.findByRoleName(roleName).isPresent()
                : roleRepository.findByRoleNameExcludingId(roleName, excludedId).isPresent();
        if (exists) {
            throw new BusinessException(ResultCode.ROLE_NAME_ALREADY_EXISTS);
        }
    }

    private void checkRoleId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private List<Long> normalizeRoleIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_ROLE_IDS);
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private void checkPageArguments(Integer page, Integer pageSize) {
        if (page == null || page <= 0 || pageSize == null || pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getRoleName(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
