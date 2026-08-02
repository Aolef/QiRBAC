package org.zzq.qirbac.permission.controller;

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
import org.zzq.qirbac.permission.dto.PermissionAncestorsResponse;
import org.zzq.qirbac.permission.dto.PermissionBatchDeleteRequest;
import org.zzq.qirbac.permission.dto.PermissionCreateRequest;
import org.zzq.qirbac.permission.dto.PermissionTreeNode;
import org.zzq.qirbac.permission.dto.PermissionUpdateRequest;
import org.zzq.qirbac.permission.dto.UserPermissionItem;
import org.zzq.qirbac.permission.service.PermissionService;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    public Result<PermissionTreeNode> createPermission(@RequestBody PermissionCreateRequest request) {
        return Result.success("新增权限成功", permissionService.createPermission(request));
    }

    @PutMapping("/{id}")
    public Result<PermissionTreeNode> updatePermission(
            @PathVariable Long id,
            @RequestBody PermissionUpdateRequest request
    ) {
        return Result.success("修改权限成功", permissionService.updatePermission(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success("删除权限成功", null);
    }

    @DeleteMapping
    public Result<Void> batchDeletePermissions(@RequestBody PermissionBatchDeleteRequest request) {
        permissionService.batchDeletePermissions(request == null ? null : request.getIds());
        return Result.success("批量删除权限成功", null);
    }

    /**
     * 全量树：数据量不大时一次拉完，前端本地渲染。
     */
    @GetMapping("/tree")
    public Result<List<PermissionTreeNode>> getFullTree() {
        return Result.success(permissionService.getFullTree());
    }

    /**
     * 懒加载：按 parentId 取直接子节点，根节点传 0 或不传。
     */
    @GetMapping("/children")
    public Result<List<PermissionTreeNode>> getChildren(
            @RequestParam(required = false) Long parentId
    ) {
        return Result.success(permissionService.getChildren(parentId));
    }

    /**
     * 编辑回显：根据 permissionId 列表返回所有祖先 id（含自身）。
     */
    @GetMapping("/ancestors")
    public Result<PermissionAncestorsResponse> getAncestors(@RequestParam List<Long> ids) {
        return Result.success(permissionService.getAncestors(ids));
    }

    /**
     * 当前登录用户拥有的权限（扁平 list）。
     */
    @GetMapping("/me")
    public Result<List<UserPermissionItem>> getCurrentUserPermissions() {
        return Result.success(permissionService.getCurrentUserPermissions());
    }
}
