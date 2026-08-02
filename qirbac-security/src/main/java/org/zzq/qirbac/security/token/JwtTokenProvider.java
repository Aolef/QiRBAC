package org.zzq.qirbac.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.zzq.qirbac.security.config.JwtProperties;
import org.zzq.qirbac.security.context.LoginUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 *
 * 它只负责 JWT 本身的事情：
 * 生成 JWT、校验 JWT、从 JWT 中解析用户信息。
 *
 * Redis 短 token 的保存和删除不放在这里，
 * 这样 JWT 逻辑和登录状态管理逻辑会更清楚。
 */
@Component
public class JwtTokenProvider {

    private static final String USERNAME_CLAIM = "username";
    private static final String SUPER_ADMIN_CLAIM = "superAdmin";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据登录用户生成 JWT。
     *
     * 这个 JWT 不直接返回给前端，而是存到 Redis。
     * 前端最终拿到的是 Redis key 里的短 token。
     */
    public String generateToken(LoginUser loginUser) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim(USERNAME_CLAIM, loginUser.getUsername())
                .claim(SUPER_ADMIN_CLAIM, loginUser.getSuperAdmin())
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验 JWT 是否合法。
     *
     * 会检查签名、过期时间、签发者。
     * 只要其中一个不通过，就返回 false。
     */
    public boolean validateToken(String jwtToken) {
        try {
            parseClaims(jwtToken);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 解析 JWT 中的数据。
     *
     * 如果 token 不合法，这里会抛出异常。
     * 对外判断是否合法时，优先用 validateToken。
     */
    public Claims parseClaims(String jwtToken) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    /**
     * 从 JWT 中取用户 ID。
     */
    public Long getUserId(String jwtToken) {
        return Long.valueOf(parseClaims(jwtToken).getSubject());
    }

    /**
     * 从 JWT 中取用户名称。
     */
    public String getUsername(String jwtToken) {
        return parseClaims(jwtToken).get(USERNAME_CLAIM, String.class);
    }

    /**
     * 从 JWT 中判断是否超级管理员。
     */
    public Boolean getSuperAdmin(String jwtToken) {
        return parseClaims(jwtToken).get(SUPER_ADMIN_CLAIM, Boolean.class);
    }

    /**
     * 把 JWT 解析成登录用户对象。
     *
     * 后面接口鉴权时，可以通过这个方法拿到当前登录人。
     */
    public LoginUser getLoginUser(String jwtToken) {
        Claims claims = parseClaims(jwtToken);
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get(USERNAME_CLAIM, String.class),
                claims.get(SUPER_ADMIN_CLAIM, Boolean.class)
        );
    }
}
