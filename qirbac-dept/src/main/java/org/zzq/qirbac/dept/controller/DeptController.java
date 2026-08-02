package org.zzq.qirbac.dept.controller;

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

@RestController
@RequestMapping("/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @PostMapping
    public Result<DeptTreeNode> createDept(@RequestBody DeptCreateRequest request) {
        return Result.success("新增部门成功", deptService.createDept(request));
    }

    @PutMapping("/{id}")
    public Result<DeptTreeNode> updateDept(
            @PathVariable Long id,
            @RequestBody DeptUpdateRequest request
    ) {
        return Result.success("修改部门成功", deptService.updateDept(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDept(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success("删除部门成功", null);
    }

    @DeleteMapping
    public Result<Void> batchDeleteDepts(@RequestBody DeptBatchDeleteRequest request) {
        deptService.batchDeleteDepts(request == null ? null : request.getIds());
        return Result.success("批量删除部门成功", null);
    }

    /**
     * 全量树：数据量不大时一次拉完，前端本地渲染。
     */
    @GetMapping("/tree")
    public Result<List<DeptTreeNode>> getFullTree() {
        return Result.success(deptService.getFullTree());
    }

    /**
     * 懒加载：按 parentId 取直接子节点，根节点传 0 或不传。
     */
    @GetMapping("/children")
    public Result<List<DeptTreeNode>> getChildren(
            @RequestParam(required = false) Long parentId
    ) {
        return Result.success(deptService.getChildren(parentId));
    }

    /**
     * 编辑回显：根据 deptId 列表返回所有祖先 id（含自身）。
     */
    @GetMapping("/ancestors")
    public Result<DeptAncestorsResponse> getAncestors(@RequestParam List<Long> ids) {
        return Result.success(deptService.getAncestors(ids));
    }
}
