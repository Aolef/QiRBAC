package org.zzq.qirbac.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录短 token 的配置。
 *
 * 前端拿到的是短 token，真正的 JWT 会存在 Redis 里。
 * 这个配置类负责读取 Redis key 前缀、过期时间、续期阈值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "login.token")
public class  LoginTokenProperties {

    /**
     * Redis key 前缀。
     *
     * 例如短 token 是 abc，
     * 最终 Redis 里的 key 就是 login:token:abc。
     */
    private String prefix;

    /**
     * Redis 中登录状态的有效期，单位毫秒。
     *
     * 删除这个 key 就能让用户立刻下线，
     * 所以后端可以比纯 JWT 更灵活地控制登录状态。
     */
    private Long expiration;

    /**
     * Redis 登录状态的续期阈值，单位毫秒。
     *
     * 例如配置 30 分钟：
     * 当 Redis key 剩余时间大于 30 分钟时，不续期；
     * 当 Redis key 剩余时间小于等于 30 分钟时，才刷新回完整有效期。
     */
    private Long refreshThreshold;
}
