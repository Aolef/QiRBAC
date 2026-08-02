package org.zzq.qirbac.dept.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zzq.qirbac.common.Result;
import org.zzq.qirbac.dept.dto.DeptAncestorsResponse;
import org.zzq.qirbac.dept.dto.DeptBatchDeleteRequest;
import org.zzq.qirbac.dept.dto.DeptCreateRequest;
import org.zzq.qirbac.dept.dto.DeptTreeNode;
import org.zzq.qirbac.dept.dto.DeptUpdateRequest;
import org.zzq.qirbac.dept.service.DeptService;

import java.util.List;

@Tag(name = "部门管理", description = "部门的增删改查、树形结构与编辑回显")
@RestController
@RequestMapping("/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @Operation(summary = "新增部门", description = "同级重名校验；parentId 为空或 0 表示顶级部门")
    @PostMapping
    public Result<DeptTreeNode> createDept(@RequestBody DeptCreateRequest request) {
        return Result.success("新增部门成功", deptService.createDept(request));
    }

    @Operation(summary = "修改部门", description = "防环校验；同级重名校验排除自身")
    @PutMapping("/{id}")
    public Result<DeptTreeNode> updateDept(
            @PathVariable Long id,
            @RequestBody DeptUpdateRequest request
    ) {
        return Result.success("修改部门成功", deptService.updateDept(id, request));
    }

    @Operation(summary = "删除部门", description = "级联逻辑删除全部子孙，并清理用户-部门关系")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDept(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success("删除部门成功", null);
    }

    @Operation(summary = "批量删除部门", description = "级联逻辑删除多棵子树，并清理用户-部门关系")
    @DeleteMapping
    public Result<Void> batchDeleteDepts(@RequestBody DeptBatchDeleteRequest request) {
        deptService.batchDeleteDepts(request == null ? null : request.getIds());
        return Result.success("批量删除部门成功", null);
    }

    @Operation(summary = "全量部门树", description = "数据量不大时一次拉完，前端本地渲染")
    @GetMapping("/tree")
    public Result<List<DeptTreeNode>> getFullTree() {
        return Result.success(deptService.getFullTree());
    }

    @Operation(summary = "懒加载子节点", description = "按 parentId 取直接子节点，根节点传 0 或不传")
    @GetMapping("/children")
    public Result<List<DeptTreeNode>> getChildren(
            @RequestParam(required = false) Long parentId
    ) {
        return Result.success(deptService.getChildren(parentId));
    }

    @Operation(summary = "祖先链回显", description = "根据 deptId 列表返回所有祖先 id（含自身），用于编辑回显")
    @GetMapping("/ancestors")
    public Result<DeptAncestorsResponse> getAncestors(@RequestParam List<Long> ids) {
        return Result.success(deptService.getAncestors(ids));
    }
}
