package org.zzq.qirbac.user.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zzq.qirbac.common.Result;
import org.zzq.qirbac.user.dto.OnlineUserResponse;
import org.zzq.qirbac.user.dto.UserBatchDeleteRequest;
import org.zzq.qirbac.user.dto.UserCreateRequest;
import org.zzq.qirbac.user.dto.UserDetailResponse;
import org.zzq.qirbac.user.dto.UserResponse;
import org.zzq.qirbac.user.dto.UserUpdateRequest;
import org.zzq.qirbac.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Result<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        return Result.success("新增用户成功", userService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request
    ) {
        return Result.success("修改用户成功", userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除用户成功", null);
    }

    @DeleteMapping
    public Result<Void> batchDeleteUsers(@RequestBody UserBatchDeleteRequest request) {
        userService.batchDeleteUsers(request == null ? null : request.getIds());
        return Result.success("批量删除用户成功", null);
    }

    @GetMapping("/{id}")
    public Result<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @GetMapping("/online")
    public Result<List<OnlineUserResponse>> getOnlineUsers() {
        return Result.success(userService.getOnlineUsers());
    }
}
