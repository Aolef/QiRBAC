package org.zzq.qirbac.security.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.security.config.LoginTokenProperties;
import org.zzq.qirbac.security.context.LoginUser;

import java.util.ArrayList;
import java.util.List;
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
     * 获取 Redis 中所有仍然有效的登录用户。
     *
     * 使用 SCAN 分批遍历登录 key，避免 KEYS 在 key 较多时阻塞 Redis。
     * 一个用户可能有多个 token，所以这里保留重复项，由业务层按 userId 去重。
     */
    public List<LoginUser> findOnlineLoginUsers() {
        List<LoginUser> loginUsers = new ArrayList<>();

        for (LoginTokenEntry entry : scanValidLoginTokens()) {
            loginUsers.add(entry.loginUser());
        }

        return loginUsers;
    }

    /**
     * 删除指定用户的全部登录 token，用于删除、禁用或后台强制下线。
     */
    public Long removeLoginTokensByUserId(Long userId) {
        if (userId == null) {
            return 0L;
        }

        List<String> redisKeys = scanValidLoginTokens().stream()
                .filter(entry -> userId.equals(entry.loginUser().getUserId()))
                .map(LoginTokenEntry::redisKey)
                .toList();

        if (redisKeys.isEmpty()) {
            return 0L;
        }

        Long deleted = stringRedisTemplate.delete(redisKeys);
        return deleted == null ? 0L : deleted;
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

    private List<LoginTokenEntry> scanValidLoginTokens() {
        List<LoginTokenEntry> entries = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(loginTokenProperties.getPrefix() + "*")
                .count(100)
                .build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String redisKey = cursor.next();
                String jwtToken = stringRedisTemplate.opsForValue().get(redisKey);

                if (!StringUtils.hasText(jwtToken) || !jwtTokenProvider.validateToken(jwtToken)) {
                    continue;
                }

                try {
                    entries.add(new LoginTokenEntry(redisKey, jwtTokenProvider.getLoginUser(jwtToken)));
                } catch (RuntimeException ignored) {
                    // token 在校验与解析之间失效或内容不完整时跳过该登录态。
                }
            }
        }

        return entries;
    }

    private record LoginTokenEntry(String redisKey, LoginUser loginUser) {
    }
}
