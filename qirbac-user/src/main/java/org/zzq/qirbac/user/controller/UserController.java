package org.zzq.qirbac.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zzq.qirbac.common.Result;
import org.zzq.qirbac.user.dto.CurrentUserResponse;
import org.zzq.qirbac.user.dto.UserBatchDeleteRequest;
import org.zzq.qirbac.user.dto.UserCreateRequest;
import org.zzq.qirbac.user.dto.UserDetailResponse;
import org.zzq.qirbac.user.dto.UserResponse;
import org.zzq.qirbac.user.dto.UserUpdateRequest;
import org.zzq.qirbac.user.service.UserService;

@Tag(name = "用户管理", description = "用户的增删改查与当前用户信息")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "新增用户", description = "用户名唯一校验；可同时分配角色与部门")
    @PostMapping
    public Result<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        return Result.success("新增用户成功", userService.createUser(request));
    }

    @Operation(summary = "修改用户", description = "支持修改用户名、启用状态、密码，以及替换式分配角色与部门")
    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request
    ) {
        return Result.success("修改用户成功", userService.updateUser(id, request));
    }

    @Operation(summary = "删除用户", description = "逻辑删除用户，并清理角色、部门关系与登录态；禁止删除当前登录用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除用户成功", null);
    }

    @Operation(summary = "批量删除用户", description = "批量逻辑删除，并清理关联关系与登录态；禁止删除当前登录用户")
    @DeleteMapping
    public Result<Void> batchDeleteUsers(@RequestBody UserBatchDeleteRequest request) {
        userService.batchDeleteUsers(request == null ? null : request.getIds());
        return Result.success("批量删除用户成功", null);
    }

    @Operation(summary = "用户详情", description = "返回用户基础信息 + 角色 + 部门")
    @GetMapping("/{id}")
    public Result<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @Operation(summary = "当前登录用户信息", description = "根据当前 token 返回当前用户的基础信息 + 角色 + 部门")
    @GetMapping("/me")
    public Result<CurrentUserResponse> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }
}
