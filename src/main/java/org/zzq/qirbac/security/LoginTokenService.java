package org.zzq.qirbac.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.config.LoginTokenProperties;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 登录短 token 服务。
 *
 * 前端拿到的是短 token，真正的 JWT 存在 Redis。
 * 这样后端可以通过删除 Redis key，让 token 立刻失效。
 */
@Service
public class LoginTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginTokenProperties loginTokenProperties;

    public LoginTokenService(
            StringRedisTemplate stringRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            LoginTokenProperties loginTokenProperties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginTokenProperties = loginTokenProperties;
    }

    /**
     * 创建登录短 token。
     *
     * 流程：
     * 1. 根据登录用户生成完整 JWT。
     * 2. 生成一个短 token。
     * 3. 把 JWT 存到 Redis，key 使用短 token。
     * 4. 把短 token 返回给前端。
     */
    public String createLoginToken(LoginUser loginUser) {
        String jwtToken = jwtTokenProvider.generateToken(loginUser);
        String shortToken = generateShortToken();
        String redisKey = buildRedisKey(shortToken);

        stringRedisTemplate.opsForValue().set(
                redisKey,
                jwtToken,
                Duration.ofMillis(loginTokenProperties.getExpiration())
        );

        return shortToken;
    }

    /**
     * 根据短 token 获取当前登录用户。
     *
     * 如果 Redis 中不存在，说明用户未登录、登录已过期，或者已经退出登录。
     * 如果 Redis 中存在，会继续校验 JWT 是否真的合法。
     */
    public LoginUser getLoginUser(String shortToken) {
        String jwtToken = getJwtToken(shortToken);

        if (!StringUtils.hasText(jwtToken) || !jwtTokenProvider.validateToken(jwtToken)) {
            return null;
        }

        return jwtTokenProvider.getLoginUser(jwtToken);
    }

    /**
     * 根据短 token 获取完整 JWT。
     *
     * 后面如果需要调试或做更细的鉴权，可以单独拿到 Redis 里的 JWT。
     */
    public String getJwtToken(String shortToken) {
        if (!StringUtils.hasText(shortToken)) {
            return null;
        }

        return stringRedisTemplate.opsForValue().get(buildRedisKey(shortToken));
    }

    /**
     * 在快过期时刷新短 token 的 Redis 有效期。
     *
     * 这个方法不会每次请求都续期。
     * 只有 Redis 剩余有效期小于等于配置的 refreshThreshold 时，
     * 才会把 Redis key 刷新回完整有效期。
     */
    public Boolean refreshTokenIfNecessary(String shortToken) {
        if (!StringUtils.hasText(shortToken)) {
            return false;
        }

        String redisKey = buildRedisKey(shortToken);
        Long expire = stringRedisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);

        /*
         * expire 的常见值：
         * -2：key 不存在。
         * -1：key 存在，但没有设置过期时间。
         * 大于 0：key 剩余有效期，单位毫秒。
         *
         * 这里只处理正常倒计时的 key。
         */
        if (expire == null || expire <= 0 || expire > loginTokenProperties.getRefreshThreshold()) {
            return false;
        }

        return stringRedisTemplate.expire(
                redisKey,
                Duration.ofMillis(loginTokenProperties.getExpiration())
        );
    }

    /**
     * 删除登录短 token。
     *
     * 退出登录、后台踢人、修改密码后强制下线，
     * 都可以通过删除 Redis key 来让这次登录立刻失效。
     */
    public Boolean removeLoginToken(String shortToken) {
        if (!StringUtils.hasText(shortToken)) {
            return false;
        }

        return stringRedisTemplate.delete(buildRedisKey(shortToken));
    }

    /**
     * 拼 Redis key。
     *
     * 前端只看到短 token，例如 abc；
     * Redis 里真正保存的 key 是 login:token:abc。
     */
    public String buildRedisKey(String shortToken) {
        return loginTokenProperties.getPrefix() + shortToken;
    }

    /**
     * 生成短 token。
     *
     * UUID 去掉横线后长度较短，适合给前端保存和传递。
     */
    private String generateShortToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
