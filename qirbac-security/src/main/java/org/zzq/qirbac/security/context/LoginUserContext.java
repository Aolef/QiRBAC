package org.zzq.qirbac.security.context;

/**
 * 当前登录用户上下文。
 *
 * 它可以理解成“当前这一次请求里的登录用户缓存”。
 * 过滤器校验 token 成功后，会把 LoginUser 放进来；
 * 后面的 Controller 或 Service 就可以直接获取当前用户。
 */
public class LoginUserContext {

    private static final ThreadLocal<LoginUser> LOGIN_USER_HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    /**
     * 保存当前请求的登录用户。
     */
    public static void set(LoginUser loginUser) {
        LOGIN_USER_HOLDER.set(loginUser);
    }

    /**
     * 获取当前请求的登录用户。
     */
    public static LoginUser get() {
        return LOGIN_USER_HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID。
     */
    public static Long getUserId() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    /**
     * 判断当前用户是否超级管理员。
     */
    public static Boolean isSuperAdmin() {
        LoginUser loginUser = get();
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    /**
     * 清理当前请求的登录用户。
     *
     * 后端线程会被重复使用，请求结束后必须清理，
     * 否则下一个请求可能拿到上一个请求的用户信息。
     */
    public static void clear() {
        LOGIN_USER_HOLDER.remove();
    }
}
