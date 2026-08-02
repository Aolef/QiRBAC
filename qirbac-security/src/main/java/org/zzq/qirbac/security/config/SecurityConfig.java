package org.zzq.qirbac.security.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zzq.qirbac.security.filter.JwtAuthenticationFilter;
import org.zzq.qirbac.security.token.LoginTokenService;
import tools.jackson.databind.ObjectMapper;

/**
 * Web 配置。
 *
 * 这里负责把我们自己写的过滤器注册到 Spring 里，
 * 让所有请求进入 Controller 之前先经过 token 校验。
 */
@Configuration
public class SecurityConfig {

    /**
     * 注册 JWT 鉴权过滤器。
     *
     * urlPatterns 设置成 /*，表示所有接口都会先进过滤器。
     * 具体哪些接口放行，由过滤器内部的白名单配置决定。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            LoginTokenService loginTokenService,
            SecurityProperties securityProperties,
            ObjectMapper objectMapper
    ) {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                loginTokenService,
                securityProperties,
                objectMapper
        );
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(jwtAuthenticationFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
