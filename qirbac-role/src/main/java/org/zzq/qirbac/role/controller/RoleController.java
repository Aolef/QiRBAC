package org.zzq.qirbac.role.controller;

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

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public Result<RoleResponse> createRole(@RequestBody RoleCreateRequest request) {
        return Result.success("新增角色成功", roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public Result<RoleResponse> updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request
    ) {
        return Result.success("修改角色成功", roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("删除角色成功", null);
    }

    @DeleteMapping
    public Result<Void> batchDeleteRoles(@RequestBody RoleBatchDeleteRequest request) {
        roleService.batchDeleteRoles(request == null ? null : request.getIds());
        return Result.success("批量删除角色成功", null);
    }

    @GetMapping
    public Result<List<RoleResponse>> getAllRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/page")
    public Result<PageResult<RoleResponse>> getRolePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName
    ) {
        return Result.success(roleService.getRolePage(page, pageSize, roleName));
    }

    /**
     * 给角色分配权限（替换式）。
     */
    @PutMapping("/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody RolePermissionAssignRequest request
    ) {
        roleService.assignPermissions(roleId, request);
        return Result.success("分配权限成功", null);
    }

    /**
     * 回显角色已分配的权限：返回带 assigned 勾选标记的全量权限树。
     */
    @GetMapping("/{roleId}/permissions")
    public Result<List<RolePermissionTreeNode>> getRolePermissionTree(@PathVariable Long roleId) {
        return Result.success(roleService.getRolePermissionTree(roleId));
    }
}
