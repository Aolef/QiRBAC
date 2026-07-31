package org.zzq.qirbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后返回给前端的数据。
 *
 * token 是短 token，不是完整 JWT。
 * 前端后续请求接口时，把它放到 Authorization 请求头里。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 短 token。
     */
    private String token;

    /**
     * token 类型。
     *
     * 前端拼请求头时使用：Authorization: Bearer {token}
     */
    private String tokenType;

    /**
     * Redis 登录态有效期，单位秒。
     *
     * 这里返回给前端用于展示或本地判断，不代表前端可以自己决定登录是否有效。
     * 真正是否有效以后端 Redis 为准。
     */
    private Long expiresIn;
}
