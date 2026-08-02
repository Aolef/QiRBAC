package org.zzq.qirbac.auth.dto;

import lombok.Data;

/**
 * 登录请求参数。
 *
 * 前端调用 /auth/login 时，请求体需要传 username 和 password。
 */
@Data
public class LoginRequest {

    /**
     * 用户名称。
     */
    private String username;

    /**
     * 用户密码。
     *
     * 当前版本按你的要求使用明文密码校验。
     */
    private String password;
}
