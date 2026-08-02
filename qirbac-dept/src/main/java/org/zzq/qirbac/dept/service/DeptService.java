package org.zzq.qirbac.dept.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.dept.dto.DeptAncestorsResponse;
import org.zzq.qirbac.dept.dto.DeptCreateRequest;
import org.zzq.qirbac.dept.dto.DeptTreeNode;
import org.zzq.qirbac.dept.dto.DeptUpdateRequest;
import org.zzq.qirbac.dept.entity.Dept;
import org.zzq.qirbac.dept.repository.DeptRelationRepository;
import org.zzq.qirbac.dept.repository.DeptRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门服务。
 *
 * 负责部门的增删改查、树构建、懒加载与编辑回显。
 *
 * 约定：parent_id = 0 表示顶级部门。
 * 删除策略：级联删除，连同所有子孙部门一起逻辑删除，并清理用户-部门关系。
 * 同级重名：同一 parent_id 下不允许重复 dept_name，由应用层校验（逻辑删除后允许同名重建）。
 */
@Service
public class DeptService {

    private static final int MAX_DEPT_NAME_LENGTH = 50;
    private static final long ROOT_PARENT_ID = 0L;

    private final DeptRepository deptRepository;
    private final DeptRelationRepository deptRelationRepository;

    public DeptService(DeptRepository deptRepository, DeptRelationRepository deptRelationRepository) {
        this.deptRepository = deptRepository;
        this.deptRelationRepository = deptRelationRepository;
    }

    // ===== 写操作 =====

    @Transactional
    public DeptTreeNode createDept(DeptCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        String deptName = normalizeDeptName(request.getDeptName());
        Long parentId = resolveParentId(request.getParentId());
        int sortOrder = resolveSortOrder(request.getSortOrder());

        ensureParentExists(parentId);
        ensureDeptNameAvailable(parentId, deptName, null);

        Dept dept = new Dept(deptName, parentId, sortOrder, false);
        Dept saved = deptRepository.save(dept);
        return toTreeNode(saved, false, null);
    }

    @Transactional
    public DeptTreeNode updateDept(Long id, DeptUpdateRequest request) {
        checkDeptId(id);
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        String deptName = normalizeDeptName(request.getDeptName());
        Long parentId = resolveParentId(request.getParentId());
        int sortOrder = resolveSortOrder(request.getSortOrder());

        Dept dept = getAvailableDept(id);
        ensureParentExists(parentId);
        ensureNotDescendant(id, parentId);
        ensureDeptNameAvailable(parentId, deptName, id);

        dept.setDeptName(deptName);
        dept.setParentId(parentId);
        dept.setSortOrder(sortOrder);
        Dept saved = deptRepository.save(dept);
        boolean hasChildren = hasChildren(saved.getId());
        return toTreeNode(saved, hasChildren, null);
    }

    /**
     * 级联删除部门：连同所有子孙一起逻辑删除，并清理用户-部门关系。
     */
    @Transactional
    public void deleteDept(Long id) {
        checkDeptId(id);
        getAvailableDept(id);

        List<Dept> toDelete = collectDescendants(id);
        toDelete.forEach(d -> d.setDeleted(true));
        deptRepository.saveAll(toDelete);

        Set<Long> ids = toDelete.stream().map(Dept::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        deptRelationRepository.deleteUserDeptsByDeptIds(ids);
    }

    /**
     * 批量级联删除：对每个 id 收集其子孙，合并去重后统一逻辑删除并清理关系。
     */
    @Transactional
    public void batchDeleteDepts(Collection<Long> ids) {
        List<Long> deptIds = normalizeDeptIds(ids);
        // 先校验全部存在
        for (Long id : deptIds) {
            getAvailableDept(id);
        }

        // 收集所有待删除节点（含子孙），按 id 去重
        Map<Long, Dept> toDeleteMap = new LinkedHashMap<>();
        for (Long id : deptIds) {
            for (Dept dept : collectDescendants(id)) {
                toDeleteMap.put(dept.getId(), dept);
            }
        }

        List<Dept> toDelete = new ArrayList<>(toDeleteMap.values());
        toDelete.forEach(d -> d.setDeleted(true));
        deptRepository.saveAll(toDelete);
        deptRelationRepository.deleteUserDeptsByDeptIds(toDeleteMap.keySet());
    }

    // ===== 树查询 =====

    /**
     * 全量树：一次拉回所有未删除部门，在内存里构建树。适合部门数量不大的场景。
     */
    @Transactional(readOnly = true)
    public List<DeptTreeNode> getFullTree() {
        List<Dept> all = deptRepository.findAllAvailable();
        return buildTree(all, ROOT_PARENT_ID);
    }

    /**
     * 懒加载：只返回某父节点的直接子节点，hasChildren 标记是否还需继续展开。
     * parentId 为 null 或 0 时返回顶级部门。
     */
    @Transactional(readOnly = true)
    public List<DeptTreeNode> getChildren(Long parentId) {
        Long pid = (parentId == null) ? ROOT_PARENT_ID : parentId;
        if (pid != ROOT_PARENT_ID) {
            ensureParentExists(pid);
        }
        List<Dept> children = deptRepository.findAvailableByParentId(pid);
        return children.stream()
                .map(dept -> toTreeNode(dept, hasChildren(dept.getId()), null))
                .toList();
    }

    /**
     * 编辑回显：根据目标 deptId 列表，返回这些节点及其全部祖先的 id 集合（含自身）。
     *
     * 前端懒加载树场景下，目标节点的祖先链尚未加载，无法直接定位。
     * 拿到本接口返回的 ancestorIds 后，前端按这些 id 逐层触发懒加载并展开即可。
     */
    @Transactional(readOnly = true)
    public DeptAncestorsResponse getAncestors(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return new DeptAncestorsResponse(List.of());
        }
        Set<Long> requestedIds = deptIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            return new DeptAncestorsResponse(List.of());
        }

        // 一次查回目标节点，避免循环查库
        Map<Long, Dept> byId = new LinkedHashMap<>();
        deptRepository.findAvailableByIds(List.copyOf(requestedIds))
                .forEach(dept -> byId.put(dept.getId(), dept));
        if (byId.size() != requestedIds.size()) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }

        Set<Long> ancestorIds = new LinkedHashSet<>();
        for (Long id : requestedIds) {
            Long cursor = id;
            while (cursor != null && cursor != ROOT_PARENT_ID && ancestorIds.add(cursor)) {
                Dept current = byId.get(cursor);
                if (current == null) {
                    // 祖先不在首批结果里，补查一次
                    current = deptRepository.findAvailableById(cursor).orElse(null);
                    if (current != null) {
                        byId.put(cursor, current);
                    }
                }
                cursor = (current == null) ? null : current.getParentId();
            }
        }
        return new DeptAncestorsResponse(new ArrayList<>(ancestorIds));
    }

    // ===== 内部工具 =====

    private Dept getAvailableDept(Long id) {
        return deptRepository.findAvailableById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DEPT_NOT_FOUND));
    }

    private void checkDeptId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private String normalizeDeptName(String deptName) {
        if (!StringUtils.hasText(deptName)) {
            throw new BusinessException(ResultCode.INVALID_DEPT_NAME);
        }
        String normalized = deptName.trim();
        if (normalized.length() > MAX_DEPT_NAME_LENGTH) {
            throw new BusinessException(ResultCode.INVALID_DEPT_NAME);
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
        if (deptRepository.findAvailableById(parentId).isEmpty()) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }
    }

    /**
     * 同级重名校验：同一 parent_id 下不允许重复 dept_name。
     * excludedId 用于修改时排除自身。
     */
    private void ensureDeptNameAvailable(Long parentId, String deptName, Long excludedId) {
        boolean exists = (excludedId == null)
                ? deptRepository.findAvailableByParentIdAndDeptName(parentId, deptName).isPresent()
                : deptRepository.findAvailableByParentIdAndDeptNameExcludingId(parentId, deptName, excludedId).isPresent();
        if (exists) {
            throw new BusinessException(ResultCode.DEPT_NAME_ALREADY_EXISTS);
        }
    }

    /**
     * 防环校验：把 deptId 的父改成 newParentId 时，
     * newParentId 不能是 deptId 自己，也不能是 deptId 的子孙。
     *
     * 判断方式：从 newParentId 向上查祖先链，若途中遇到 deptId，说明 newParentId 是 deptId 的子孙。
     */
    private void ensureNotDescendant(Long deptId, Long newParentId) {
        if (Objects.equals(deptId, newParentId)) {
            throw new BusinessException(ResultCode.DEPT_PARENT_INVALID);
        }
        if (newParentId == ROOT_PARENT_ID) {
            return;
        }
        Long cursor = newParentId;
        while (cursor != null && cursor != ROOT_PARENT_ID) {
            if (Objects.equals(cursor, deptId)) {
                throw new BusinessException(ResultCode.DEPT_PARENT_INVALID);
            }
            Dept current = deptRepository.findAvailableById(cursor).orElse(null);
            cursor = (current == null) ? null : current.getParentId();
        }
    }

    private boolean hasChildren(Long deptId) {
        return deptRepository.countAvailableByParentId(deptId) > 0;
    }

    /**
     * 收集 deptId 及其全部子孙（含自身）。
     *
     * 一次性拉回所有未删除部门在内存里 BFS，避免逐层查库。
     * 部门表数据量通常不大，这种方式足够。
     */
    private List<Dept> collectDescendants(Long rootId) {
        List<Dept> all = deptRepository.findAllAvailable();
        Map<Long, List<Dept>> byParent = all.stream()
                .collect(Collectors.groupingBy(Dept::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<Dept> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        // 先把根节点自身放进来
        all.stream().filter(d -> Objects.equals(d.getId(), rootId)).findFirst().ifPresent(result::add);
        queue.add(rootId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Dept child : byParent.getOrDefault(current, List.of())) {
                result.add(child);
                queue.add(child.getId());
            }
        }
        return result;
    }

    private List<DeptTreeNode> buildTree(List<Dept> all, Long rootParentId) {
        Map<Long, List<Dept>> byParent = all.stream()
                .collect(Collectors.groupingBy(Dept::getParentId, LinkedHashMap::new, Collectors.toList()));
        return buildChildren(byParent, rootParentId);
    }

    private List<DeptTreeNode> buildChildren(Map<Long, List<Dept>> byParent, Long parentId) {
        List<Dept> children = byParent.getOrDefault(parentId, List.of());
        return children.stream()
                .map(dept -> {
                    List<DeptTreeNode> sub = buildChildren(byParent, dept.getId());
                    return toTreeNode(dept, !sub.isEmpty(), sub.isEmpty() ? null : sub);
                })
                .toList();
    }

    private DeptTreeNode toTreeNode(Dept dept, boolean hasChildren, List<DeptTreeNode> children) {
        return new DeptTreeNode(
                dept.getId(),
                dept.getDeptName(),
                dept.getParentId(),
                dept.getSortOrder(),
                hasChildren,
                children
        );
    }

    private List<Long> normalizeDeptIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_DEPT_IDS);
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
