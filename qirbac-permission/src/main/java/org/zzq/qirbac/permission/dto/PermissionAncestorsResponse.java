package org.zzq.qirbac.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 权限祖先链响应。
 *
 * 用于懒加载树场景下的编辑回显：前端传入目标 permissionId 列表，
 * 后端返回这些节点及其全部祖先的 id 集合（含自身，从根到自身顺序）。
 * 前端拿到后按这些 id 逐层触发懒加载并展开，即可定位到目标节点。
 */
@Data
@AllArgsConstructor
public class PermissionAncestorsResponse {

    /**
     * 祖先及自身 id 列表，按从根到目标的顺序排列。
     *
     * 多个目标节点的祖先链会合并去重。
     */
    private List<Long> ancestorIds;
}
