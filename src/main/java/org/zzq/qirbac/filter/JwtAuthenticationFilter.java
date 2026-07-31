package org.zzq.qirbac.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.common.Result;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.config.SecurityProperties;
import org.zzq.qirbac.security.LoginTokenService;
import org.zzq.qirbac.security.LoginUser;
import org.zzq.qirbac.security.LoginUserContext;
import org.zzq.qirbac.security.TokenResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * JWT 登录鉴权过滤器。
 *
 * 请求进入 Controller 之前，会先来到这里。
 * 白名单接口直接放行；其他接口必须携带 Authorization: Bearer 短token。
 */
public class JwtAuthenticationFilter implements Filter {

    private final LoginTokenService loginTokenService;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            LoginTokenService loginTokenService,
            SecurityProperties securityProperties,
            ObjectMapper objectMapper
    ) {
        this.loginTokenService = loginTokenService;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        /*
         * OPTIONS 通常是浏览器跨域预检请求。
         * 它不是真正的业务请求，所以这里直接放行。
         */
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod()) || isWhitelist(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String shortToken = resolveShortToken(httpRequest);
        LoginUser loginUser = loginTokenService.getLoginUser(shortToken);

        if (loginUser == null) {
            writeUnauthorized(httpResponse);
            return;
        }

        try {
            /*
             * token 校验通过后，把当前登录用户保存到上下文。
             * 后面的 Controller / Service 可以通过 LoginUserContext.get() 获取。
             */
            LoginUserContext.set(loginUser);

            /*
             * 只刷新 Redis 的有效期，不重新生成 JWT。
             * 剩余时间小于等于配置阈值时，LoginTokenService 内部才会真正续期。
             */
            loginTokenService.refreshTokenIfNecessary(shortToken);
            chain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
        }
    }

    /**
     * 判断当前请求是否在白名单里。
     *
     * 支持：
     * 1. 精确匹配：/auth/login
     * 2. 前缀匹配：/public/**
     */
    private boolean isWhitelist(HttpServletRequest request) {
        String path = getRequestPath(request);

        for (String pattern : securityProperties.getWhitelist()) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }

            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                    return true;
                }
                continue;
            }

            if (path.equals(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取请求路径。
     *
     * 如果项目以后配置了 context-path，这里会把前面的 context-path 去掉，
     * 只保留真正的接口路径。
     */
    private String getRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    /**
     * 从 Authorization 请求头中解析短 token。
     *
     * 前端传递格式：
     * Authorization: Bearer 短token
     */
    private String resolveShortToken(HttpServletRequest request) {
        return TokenResolver.resolve(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    /**
     * 返回未登录响应。
     *
     * 过滤器执行时还没有进入 Controller，
     * 所以这里需要手动把 Result 转成 JSON 写回前端。
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED)));
    }
}
