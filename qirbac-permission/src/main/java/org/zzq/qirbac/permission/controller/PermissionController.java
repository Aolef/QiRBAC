package org.zzq.qirbac.permission.controller;

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
import org.zzq.qirbac.permission.dto.PermissionAncestorsResponse;
import org.zzq.qirbac.permission.dto.PermissionBatchDeleteRequest;
import org.zzq.qirbac.permission.dto.PermissionCreateRequest;
import org.zzq.qirbac.permission.dto.PermissionTreeNode;
import org.zzq.qirbac.permission.dto.PermissionUpdateRequest;
import org.zzq.qirbac.permission.dto.UserPermissionItem;
import org.zzq.qirbac.permission.service.PermissionService;

import java.util.List;

@Tag(name = "权限管理", description = "权限的增删改查、树形结构与当前用户权限")
@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "新增权限", description = "仅 FOLDER 类型可作为父节点")
    @PostMapping
    public Result<PermissionTreeNode> createPermission(@RequestBody PermissionCreateRequest request) {
        return Result.success("新增权限成功", permissionService.createPermission(request));
    }

    @Operation(summary = "修改权限", description = "防环校验；已有子节点时禁止将类型改为非 FOLDER")
    @PutMapping("/{id}")
    public Result<PermissionTreeNode> updatePermission(
            @PathVariable Long id,
            @RequestBody PermissionUpdateRequest request
    ) {
        return Result.success("修改权限成功", permissionService.updatePermission(id, request));
    }

    @Operation(summary = "删除权限", description = "级联逻辑删除全部子孙，并清理 sys_role_permission 关联")
    @DeleteMapping("/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success("删除权限成功", null);
    }

    @Operation(summary = "批量删除权限", description = "级联逻辑删除多棵子树，并清理 sys_role_permission 关联")
    @DeleteMapping
    public Result<Void> batchDeletePermissions(@RequestBody PermissionBatchDeleteRequest request) {
        permissionService.batchDeletePermissions(request == null ? null : request.getIds());
        return Result.success("批量删除权限成功", null);
    }

    @Operation(summary = "全量权限树", description = "数据量不大时一次拉完，前端本地渲染")
    @GetMapping("/tree")
    public Result<List<PermissionTreeNode>> getFullTree() {
        return Result.success(permissionService.getFullTree());
    }

    @Operation(summary = "懒加载子节点", description = "按 parentId 取直接子节点，根节点传 0 或不传")
    @GetMapping("/children")
    public Result<List<PermissionTreeNode>> getChildren(
            @RequestParam(required = false) Long parentId
    ) {
        return Result.success(permissionService.getChildren(parentId));
    }

    @Operation(summary = "祖先链回显", description = "根据 permissionId 列表返回所有祖先 id（含自身），用于编辑回显")
    @GetMapping("/ancestors")
    public Result<PermissionAncestorsResponse> getAncestors(@RequestParam List<Long> ids) {
        return Result.success(permissionService.getAncestors(ids));
    }

    @Operation(summary = "当前登录用户拥有的权限", description = "返回扁平 list；超管返回全部启用权限")
    @GetMapping("/me")
    public Result<List<UserPermissionItem>> getCurrentUserPermissions() {
        return Result.success(permissionService.getCurrentUserPermissions());
    }
}
