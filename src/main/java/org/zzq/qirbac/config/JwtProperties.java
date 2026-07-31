package org.zzq.qirbac.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 *
 * 这里负责读取 application.yml 里的 jwt 配置，
 * 后面的 JWT 工具类会用这些配置生成和校验 token。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥。
     *
     * 它相当于后端给 token 盖章用的私钥。
     * 生产环境不要写死在配置文件里，建议从环境变量或配置中心读取。
     */
    private String secret;

    /**
     * JWT 签发者。
     *
     * 校验 token 时会检查 issuer，
     * 防止其他系统签发的 token 被当前系统误认为合法。
     */
    private String issuer;

    /**
     * JWT 有效期，单位毫秒。
     */
    private Long expiration;
}
