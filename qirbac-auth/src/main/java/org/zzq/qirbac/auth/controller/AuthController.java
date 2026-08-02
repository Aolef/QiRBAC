package org.zzq.qirbac.auth.controller;

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
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录接口。
     *
     * 请求地址：POST /auth/login
     * 请求体：{ "username": "admin", "password": "123456" }
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    /**
     * 退出登录接口。
     *
     * 请求地址：POST /auth/logout
     * 请求头：Authorization: Bearer 短token
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        authService.logout(authorization);
        return Result.success("退出登录成功", null);
    }
}
