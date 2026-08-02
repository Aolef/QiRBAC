package org.zzq.qirbac.permission.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.permission.dto.PermissionAncestorsResponse;
import org.zzq.qirbac.permission.dto.PermissionCreateRequest;
import org.zzq.qirbac.permission.dto.PermissionTreeNode;
import org.zzq.qirbac.permission.dto.PermissionUpdateRequest;
import org.zzq.qirbac.permission.dto.UserPermissionItem;
import org.zzq.qirbac.permission.entity.Permission;
import org.zzq.qirbac.permission.repository.PermissionRelationRepository;
import org.zzq.qirbac.permission.repository.PermissionRepository;
import org.zzq.qirbac.permission.types.PermissionType;
import org.zzq.qirbac.security.context.LoginUserContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限服务。
 *
 * 负责权限的增删改查、树构建、懒加载与编辑回显。
 *
 * 约定：parent_id = 0 表示顶级权限。
 * 删除策略：级联删除，连同所有子孙权限一起逻辑删除，并清理角色-权限关系。
 * 同级重名：同一 parent_id 下不允许重复 permission_name，由应用层校验（逻辑删除后允许同名重建）。
 * 父节点约束：仅 FOLDER 类型可作为父节点；MENU/API/BUTTON 为叶子节点。
 */
@Service
public class PermissionService {

    private static final int MAX_PERMISSION_NAME_LENGTH = 50;
    private static final long ROOT_PARENT_ID = 0L;

    private final PermissionRepository permissionRepository;
    private final PermissionRelationRepository permissionRelationRepository;

    public PermissionService(
            PermissionRepository permissionRepository,
            PermissionRelationRepository permissionRelationRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.permissionRelationRepository = permissionRelationRepository;
    }

    // ===== 写操作 =====

    @Transactional
    public PermissionTreeNode createPermission(PermissionCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        String permissionName = normalizePermissionName(request.getPermissionName());
        PermissionType permissionType = request.getPermissionType();
        // permissionType 由 Spring 反序列化保证合法性，这里防御性判空
        if (permissionType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        Long parentId = resolveParentId(request.getParentId());
        int sortOrder = resolveSortOrder(request.getSortOrder());
        boolean enabled = request.getEnabled() == null || request.getEnabled();

        ensureParentExists(parentId);
        ensurePermissionNameAvailable(parentId, permissionName, null);

        Permission permission = new Permission(
                permissionName, parentId, request.getRoutePath(), permissionType, sortOrder, enabled, false
        );
        Permission saved = permissionRepository.save(permission);
        return toTreeNode(saved, false, null);
    }

    @Transactional
    public PermissionTreeNode updatePermission(Long id, PermissionUpdateRequest request) {
        checkPermissionId(id);
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        String permissionName = normalizePermissionName(request.getPermissionName());
        PermissionType permissionType = request.getPermissionType();
        if (permissionType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        Long parentId = resolveParentId(request.getParentId());
        int sortOrder = resolveSortOrder(request.getSortOrder());
        boolean enabled = request.getEnabled() == null || request.getEnabled();

        Permission permission = getAvailablePermission(id);
        ensureParentExists(parentId);
        ensureNotDescendant(id, parentId);
        ensurePermissionNameAvailable(parentId, permissionName, id);
        // 改为非 FOLDER 类型时，若已有子节点，禁止降级为叶子
        if (!permissionType.canHaveChildren() && hasChildren(id)) {
            throw new BusinessException(ResultCode.PERMISSION_TYPE_CANNOT_HAVE_CHILDREN);
        }

        permission.setPermissionName(permissionName);
        permission.setParentId(parentId);
        permission.setRoutePath(request.getRoutePath());
        permission.setPermissionType(permissionType);
        permission.setSortOrder(sortOrder);
        permission.setEnabled(enabled);
        Permission saved = permissionRepository.save(permission);
        boolean hasChildren = hasChildren(saved.getId());
        return toTreeNode(saved, hasChildren, null);
    }

    /**
     * 级联删除权限：连同所有子孙一起逻辑删除，并清理角色-权限关系。
     */
    @Transactional
    public void deletePermission(Long id) {
        checkPermissionId(id);
        getAvailablePermission(id);

        List<Permission> toDelete = collectDescendants(id);
        toDelete.forEach(p -> p.setDeleted(true));
        permissionRepository.saveAll(toDelete);

        Set<Long> ids = toDelete.stream().map(Permission::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        permissionRelationRepository.deleteRolePermissionsByPermissionIds(ids);
    }

    /**
     * 批量级联删除：对每个 id 收集其子孙，合并去重后统一逻辑删除并清理关系。
     */
    @Transactional
    public void batchDeletePermissions(Collection<Long> ids) {
        List<Long> permissionIds = normalizePermissionIds(ids);
        // 先校验全部存在
        for (Long id : permissionIds) {
            getAvailablePermission(id);
        }

        // 收集所有待删除节点（含子孙），按 id 去重
        Map<Long, Permission> toDeleteMap = new LinkedHashMap<>();
        for (Long id : permissionIds) {
            for (Permission permission : collectDescendants(id)) {
                toDeleteMap.put(permission.getId(), permission);
            }
        }

        List<Permission> toDelete = new ArrayList<>(toDeleteMap.values());
        toDelete.forEach(p -> p.setDeleted(true));
        permissionRepository.saveAll(toDelete);
        permissionRelationRepository.deleteRolePermissionsByPermissionIds(toDeleteMap.keySet());
    }

    // ===== 树查询 =====

    /**
     * 全量树：一次拉回所有未删除权限，在内存里构建树。适合权限数量不大的场景。
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeNode> getFullTree() {
        List<Permission> all = permissionRepository.findAllAvailable();
        return buildTree(all, ROOT_PARENT_ID);
    }

    /**
     * 懒加载：只返回某父节点的直接子节点，hasChildren 标记是否还需继续展开。
     * parentId 为 null 或 0 时返回顶级权限。
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeNode> getChildren(Long parentId) {
        Long pid = (parentId == null) ? ROOT_PARENT_ID : parentId;
        if (pid != ROOT_PARENT_ID) {
            ensureParentExists(pid);
        }
        List<Permission> children = permissionRepository.findAvailableByParentId(pid);
        return children.stream()
                .map(permission -> toTreeNode(permission, hasChildren(permission.getId()), null))
                .toList();
    }

    /**
     * 编辑回显：根据目标 permissionId 列表，返回这些节点及其全部祖先的 id 集合（含自身）。
     *
     * 前端懒加载树场景下，目标节点的祖先链尚未加载，无法直接定位。
     * 拿到本接口返回的 ancestorIds 后，前端按这些 id 逐层触发懒加载并展开即可。
     */
    @Transactional(readOnly = true)
    public PermissionAncestorsResponse getAncestors(Collection<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new PermissionAncestorsResponse(List.of());
        }
        Set<Long> requestedIds = permissionIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            return new PermissionAncestorsResponse(List.of());
        }

        // 一次查回目标节点，避免循环查库
        Map<Long, Permission> byId = new LinkedHashMap<>();
        permissionRepository.findAvailableByIds(List.copyOf(requestedIds))
                .forEach(permission -> byId.put(permission.getId(), permission));
        if (byId.size() != requestedIds.size()) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        Set<Long> ancestorIds = new LinkedHashSet<>();
        for (Long id : requestedIds) {
            Long cursor = id;
            while (cursor != null && cursor != ROOT_PARENT_ID && ancestorIds.add(cursor)) {
                Permission current = byId.get(cursor);
                if (current == null) {
                    // 祖先不在首批结果里，补查一次
                    current = permissionRepository.findAvailableById(cursor).orElse(null);
                    if (current != null) {
                        byId.put(cursor, current);
                    }
                }
                cursor = (current == null) ? null : current.getParentId();
            }
        }
        return new PermissionAncestorsResponse(new ArrayList<>(ancestorIds));
    }

    // ===== 当前用户权限 =====

    /**
     * 获取当前登录用户拥有的权限（扁平 list）。
     *
     * 超级管理员：返回全部启用权限，不走角色链路。
     * 普通用户：经 user → role → permission 链路查询，已去重，仅含 enabled = 1。
     * 未登录：抛 UNAUTHORIZED。
     */
    @Transactional(readOnly = true)
    public List<UserPermissionItem> getCurrentUserPermissions() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (Boolean.TRUE.equals(LoginUserContext.isSuperAdmin())) {
            return permissionRepository.findAllAvailable().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getEnabled()))
                    .map(this::toUserPermissionItem)
                    .toList();
        }
        return permissionRepository.findAvailableEnabledByUserId(userId).stream()
                .map(this::toUserPermissionItem)
                .toList();
    }

    // ===== 内部工具 =====

    private Permission getAvailablePermission(Long id) {
        return permissionRepository.findAvailableById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.PERMISSION_NOT_FOUND));
    }

    private void checkPermissionId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private String normalizePermissionName(String permissionName) {
        if (!StringUtils.hasText(permissionName)) {
            throw new BusinessException(ResultCode.INVALID_PERMISSION_NAME);
        }
        String normalized = permissionName.trim();
        if (normalized.length() > MAX_PERMISSION_NAME_LENGTH) {
            throw new BusinessException(ResultCode.INVALID_PERMISSION_NAME);
        }
        return normalized;
    }

    private Long resolveParentId(Long parentId) {
        return (parentId == null) ? ROOT_PARENT_ID : parentId;
    }

    private int resolveSortOrder(Integer sortOrder) {
        return (sortOrder == null) ? 0 : sortOrder;
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == ROOT_PARENT_ID) {
            return;
        }
        if (parentId == null || parentId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        Permission parent = permissionRepository.findAvailableById(parentId)
                .orElseThrow(() -> new BusinessException(ResultCode.PERMISSION_NOT_FOUND));
        // 仅 FOLDER 可作父节点
        if (!parent.getPermissionType().canHaveChildren()) {
            throw new BusinessException(ResultCode.PERMISSION_TYPE_CANNOT_HAVE_CHILDREN);
        }
    }

    /**
     * 同级重名校验：同一 parent_id 下不允许重复 permission_name。
     * excludedId 用于修改时排除自身。
     */
    private void ensurePermissionNameAvailable(Long parentId, String permissionName, Long excludedId) {
        boolean exists = (excludedId == null)
                ? permissionRepository.findAvailableByParentIdAndPermissionName(parentId, permissionName).isPresent()
                : permissionRepository.findAvailableByParentIdAndPermissionNameExcludingId(parentId, permissionName, excludedId).isPresent();
        if (exists) {
            throw new BusinessException(ResultCode.PERMISSION_NAME_ALREADY_EXISTS);
        }
    }

    /**
     * 防环校验：把 permissionId 的父改成 newParentId 时，
     * newParentId 不能是 permissionId 自己，也不能是 permissionId 的子孙。
     *
     * 判断方式：从 newParentId 向上查祖先链，若途中遇到 permissionId，说明 newParentId 是 permissionId 的子孙。
     */
    private void ensureNotDescendant(Long permissionId, Long newParentId) {
        if (Objects.equals(permissionId, newParentId)) {
            throw new BusinessException(ResultCode.PERMISSION_PARENT_INVALID);
        }
        if (newParentId == ROOT_PARENT_ID) {
            return;
        }
        Long cursor = newParentId;
        while (cursor != null && cursor != ROOT_PARENT_ID) {
            if (Objects.equals(cursor, permissionId)) {
                throw new BusinessException(ResultCode.PERMISSION_PARENT_INVALID);
            }
            Permission current = permissionRepository.findAvailableById(cursor).orElse(null);
            cursor = (current == null) ? null : current.getParentId();
        }
    }

    private boolean hasChildren(Long permissionId) {
        return permissionRepository.countAvailableByParentId(permissionId) > 0;
    }

    /**
     * 收集 permissionId 及其全部子孙（含自身）。
     *
     * 一次性拉回所有未删除权限在内存里 BFS，避免逐层查库。
     * 权限表数据量通常不大，这种方式足够。
     */
    private List<Permission> collectDescendants(Long rootId) {
        List<Permission> all = permissionRepository.findAllAvailable();
        Map<Long, List<Permission>> byParent = all.stream()
                .collect(Collectors.groupingBy(Permission::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<Permission> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        // 先把根节点自身放进来
        all.stream().filter(p -> Objects.equals(p.getId(), rootId)).findFirst().ifPresent(result::add);
        queue.add(rootId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Permission child : byParent.getOrDefault(current, List.of())) {
                result.add(child);
                queue.add(child.getId());
            }
        }
        return result;
    }

    private List<PermissionTreeNode> buildTree(List<Permission> all, Long rootParentId) {
        Map<Long, List<Permission>> byParent = all.stream()
                .collect(Collectors.groupingBy(Permission::getParentId, LinkedHashMap::new, Collectors.toList()));
        return buildChildren(byParent, rootParentId);
    }

    private List<PermissionTreeNode> buildChildren(Map<Long, List<Permission>> byParent, Long parentId) {
        List<Permission> children = byParent.getOrDefault(parentId, List.of());
        return children.stream()
                .map(permission -> {
                    List<PermissionTreeNode> sub = buildChildren(byParent, permission.getId());
                    return toTreeNode(permission, !sub.isEmpty(), sub.isEmpty() ? null : sub);
                })
                .toList();
    }

    private PermissionTreeNode toTreeNode(Permission permission, boolean hasChildren, List<PermissionTreeNode> children) {
        return new PermissionTreeNode(
                permission.getId(),
                permission.getPermissionName(),
                permission.getParentId(),
                permission.getRoutePath(),
                permission.getPermissionType(),
                permission.getSortOrder(),
                permission.getEnabled(),
                hasChildren,
                children
        );
    }

    private UserPermissionItem toUserPermissionItem(Permission permission) {
        return new UserPermissionItem(
                permission.getId(),
                permission.getPermissionName(),
                permission.getParentId(),
                permission.getRoutePath(),
                permission.getPermissionType(),
                permission.getSortOrder()
        );
    }

    private List<Long> normalizePermissionIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PERMISSION_IDS);
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
