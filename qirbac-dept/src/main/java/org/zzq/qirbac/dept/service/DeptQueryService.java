package org.zzq.qirbac.dept.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.dept.dto.DeptSummary;
import org.zzq.qirbac.dept.entity.Dept;
import org.zzq.qirbac.dept.repository.DeptRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门查询服务。
 *
 * 作为其它模块访问部门数据的门面，提供校验与摘要查询，
 * 避免外部模块直接依赖 DeptRepository。
 */
@Service
public class DeptQueryService {

    private final DeptRepository deptRepository;

    public DeptQueryService(DeptRepository deptRepository) {
        this.deptRepository = deptRepository;
    }

    /**
     * 校验给定部门 id 是否全部存在且未删除。
     *
     * ids 为空时直接通过，调用方常用于"用户未选部门"的场景。
     */
    @Transactional(readOnly = true)
    public void validateDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        Set<Long> requestedIds = normalizeIds(deptIds);
        List<Dept> found = deptRepository.findAvailableByIds(List.copyOf(requestedIds));
        if (found.size() != requestedIds.size()) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }
    }

    /**
     * 按 id 批量查询部门摘要，返回 id -> DeptSummary 的映射。
     */
    @Transactional(readOnly = true)
    public Map<Long, DeptSummary> findDeptSummaries(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> requestedIds = normalizeIds(deptIds);
        List<Dept> depts = deptRepository.findAvailableByIds(List.copyOf(requestedIds));
        Map<Long, DeptSummary> result = new LinkedHashMap<>();
        for (Dept dept : depts) {
            result.put(dept.getId(), new DeptSummary(dept.getId(), dept.getDeptName(), dept.getParentId()));
        }
        return result;
    }

    private Set<Long> normalizeIds(Collection<Long> deptIds) {
        if (deptIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new LinkedHashSet<>(deptIds);
    }
}
