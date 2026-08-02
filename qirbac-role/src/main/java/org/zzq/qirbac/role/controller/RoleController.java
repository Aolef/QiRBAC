package org.zzq.qirbac.role.controller;

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
import org.zzq.qirbac.common.PageResult;
import org.zzq.qirbac.common.Result;
import org.zzq.qirbac.role.dto.RoleBatchDeleteRequest;
import org.zzq.qirbac.role.dto.RoleCreateRequest;
import org.zzq.qirbac.role.dto.RolePermissionAssignRequest;
import org.zzq.qirbac.role.dto.RolePermissionTreeNode;
import org.zzq.qirbac.role.dto.RoleResponse;
import org.zzq.qirbac.role.dto.RoleUpdateRequest;
import org.zzq.qirbac.role.service.RoleService;

import java.util.List;

@Tag(name = "角色管理", description = "角色的增删改查与权限分配")
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "新增角色", description = "角色名唯一校验")
    @PostMapping
    public Result<RoleResponse> createRole(@RequestBody RoleCreateRequest request) {
        return Result.success("新增角色成功", roleService.createRole(request));
    }

    @Operation(summary = "修改角色", description = "角色名唯一校验，排除自身")
    @PutMapping("/{id}")
    public Result<RoleResponse> updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request
    ) {
        return Result.success("修改角色成功", roleService.updateRole(id, request));
    }

    @Operation(summary = "删除角色", description = "物理删除角色，并清理 sys_user_role、sys_role_permission 关联")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("删除角色成功", null);
    }

    @Operation(summary = "批量删除角色", description = "批量物理删除，并清理关联关系")
    @DeleteMapping
    public Result<Void> batchDeleteRoles(@RequestBody RoleBatchDeleteRequest request) {
        roleService.batchDeleteRoles(request == null ? null : request.getIds());
        return Result.success("批量删除角色成功", null);
    }

    @Operation(summary = "全部角色列表", description = "不分页，通常用于下拉选择")
    @GetMapping
    public Result<List<RoleResponse>> getAllRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @Operation(summary = "角色分页查询", description = "支持按角色名模糊查询")
    @GetMapping("/page")
    public Result<PageResult<RoleResponse>> getRolePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName
    ) {
        return Result.success(roleService.getRolePage(page, pageSize, roleName));
    }

    @Operation(summary = "给角色分配权限", description = "替换式更新：permissionIds 为空表示清空该角色的全部权限")
    @PutMapping("/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody RolePermissionAssignRequest request
    ) {
        roleService.assignPermissions(roleId, request);
        return Result.success("分配权限成功", null);
    }

    @Operation(summary = "回显角色权限", description = "返回带 assigned 勾选标记的全量权限树，供前端渲染勾选框")
    @GetMapping("/{roleId}/permissions")
    public Result<List<RolePermissionTreeNode>> getRolePermissionTree(@PathVariable Long roleId) {
        return Result.success(roleService.getRolePermissionTree(roleId));
    }
}
