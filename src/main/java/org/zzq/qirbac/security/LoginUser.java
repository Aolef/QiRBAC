package org.zzq.qirbac.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户信息。
 *
 * 登录成功后，会用这个对象生成 JWT。
 * 后面做 RBAC 时，可以继续在这里增加 roles、permissions 等字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /**
     * 用户 ID。
     *
     * JWT 里会把它放到 subject，也就是 token 代表的主体。
     */
    private Long userId;

    /**
     * 用户名称。
     */
    private String username;

    /**
     * 是否超级管理员。
     *
     * 超级管理员后面可以跳过一部分普通权限判断。
     */
    private Boolean superAdmin;
}
