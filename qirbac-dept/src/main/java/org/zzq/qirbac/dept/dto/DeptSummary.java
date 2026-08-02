package org.zzq.qirbac.dept.dto;

/**
 * 部门摘要，供其它模块（如 user）引用，避免暴露完整实体。
 */
public record DeptSummary(Long id, String deptName, Long parentId) {
}
