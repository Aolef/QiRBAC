package org.zzq.qirbac.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口鉴权配置。
 *
 * 这里读取 application.yml 里的 security.whitelist。
 * 白名单里的接口不需要登录，其他接口都需要带 token。
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 不需要登录就能访问的接口列表。
     *
     * 支持两种写法：
     * 1. 精确匹配，例如 /auth/login
     * 2. 前缀匹配，例如 /public/**
     */
    private List<String> whitelist = new ArrayList<>();
}
