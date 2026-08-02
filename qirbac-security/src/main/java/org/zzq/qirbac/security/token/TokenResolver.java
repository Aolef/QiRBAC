package org.zzq.qirbac.security.token;

import org.springframework.util.StringUtils;

/**
 * token 解析工具。
 *
 * 前端统一通过 Authorization 请求头传短 token：
 * Authorization: Bearer 短token
 *
 * 过滤器和退出登录接口都用这个工具解析，
 * 这样两边对 token 格式的要求保持一致。
 */
public class TokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private TokenResolver() {
    }

    /**
     * 从 Authorization 请求头中取出短 token。
     *
     * 如果请求头不存在、不是 Bearer 格式、或者 Bearer 后面没有内容，
     * 就返回 null。
     */
    public static String resolve(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            return null;
        }

        return token;
    }
}
