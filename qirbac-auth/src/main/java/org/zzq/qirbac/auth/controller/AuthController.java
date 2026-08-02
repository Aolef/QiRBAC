package org.zzq.qirbac.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zzq.qirbac.common.Result;
import org.zzq.qirbac.auth.dto.LoginRequest;
import org.zzq.qirbac.auth.dto.LoginResponse;
import org.zzq.qirbac.auth.service.AuthService;

/**
 * 认证接口。
 *
 * 这里提供登录和退出登录接口。
 */
@Tag(name = "认证", description = "登录与退出登录")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "用户名密码登录，返回短 token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    @Operation(summary = "退出登录", description = "清除 Redis 登录态，请求头需携带 Authorization: Bearer 短token")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        authService.logout(authorization);
        return Result.success("退出登录成功", null);
    }
}
