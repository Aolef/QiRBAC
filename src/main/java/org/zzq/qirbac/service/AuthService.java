package org.zzq.qirbac.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.config.LoginTokenProperties;
import org.zzq.qirbac.dto.LoginRequest;
import org.zzq.qirbac.dto.LoginResponse;
import org.zzq.qirbac.entity.User;
import org.zzq.qirbac.repository.UserRepository;
import org.zzq.qirbac.security.LoginTokenService;
import org.zzq.qirbac.security.LoginUser;
import org.zzq.qirbac.security.TokenResolver;

/**
 * 登录相关业务。
 *
 * Controller 只负责接收请求和返回结果，
 * 真正的登录判断、查用户、生成 token 都放在这里。
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final LoginTokenService loginTokenService;
    private final LoginTokenProperties loginTokenProperties;

    public AuthService(
            UserRepository userRepository,
            LoginTokenService loginTokenService,
            LoginTokenProperties loginTokenProperties
    ) {
        this.userRepository = userRepository;
        this.loginTokenService = loginTokenService;
        this.loginTokenProperties = loginTokenProperties;
    }

    /**
     * 用户登录。
     *
     * 当前版本使用明文密码校验：
     * 前端传来的密码需要和数据库 password 字段完全一致。
     */
    public LoginResponse login(LoginRequest request) {
        checkLoginRequest(request);

        User user = userRepository.findAvailableByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (!request.getPassword().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getSuperAdmin()
        );
        String token = loginTokenService.createLoginToken(loginUser);
        Long expiresIn = loginTokenProperties.getExpiration() / 1000;

        return new LoginResponse(token, TOKEN_TYPE, expiresIn);
    }

    /**
     * 退出登录。
     *
     * 退出登录的目标是废弃前端传来的短 token。
     * 只要 Authorization 是正确的 Bearer 格式，就删除 Redis key 并返回成功。
     * Redis 里原本有没有这个 key 不重要，最终效果都是这个 token 不再有效。
     */
    public void logout(String authorization) {
        String shortToken = TokenResolver.resolve(authorization);

        if (!StringUtils.hasText(shortToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        loginTokenService.removeLoginToken(shortToken);
    }

    /**
     * 检查登录参数。
     *
     * username 或 password 为空时，直接返回请求参数错误。
     */
    private void checkLoginRequest(LoginRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }
}
