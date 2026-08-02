package org.zzq.qirbac.common;

/**
 * 统一接口状态码。
 *
 * 前端可以根据 code 判断接口结果，比如：
 * 200 表示成功，401 表示未登录，403 表示没有权限。
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    FAIL(500, "操作失败"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USER_DISABLED(1002, "用户已被禁用"),
    TOKEN_INVALID(1003, "Token 无效"),
    TOKEN_EXPIRED(1004, "Token 已过期"),

    USER_NOT_FOUND(2001, "用户不存在"),
    USERNAME_ALREADY_EXISTS(2002, "用户名已存在"),
    ROLE_NOT_FOUND(2003, "角色不存在"),
    DEPT_NOT_FOUND(2004, "部门不存在"),
    INVALID_USER_IDS(2005, "用户 ID 列表不能为空"),
    CANNOT_DELETE_SELF(2006, "不能删除当前登录用户"),

    ROLE_NAME_ALREADY_EXISTS(3001, "角色名称已存在"),
    INVALID_ROLE_IDS(3002, "角色 ID 列表不能为空"),
    INVALID_ROLE_NAME(3003, "角色名称不能为空且长度不能超过 50 个字符"),

    INVALID_DEPT_NAME(4001, "部门名称不能为空且长度不能超过 50 个字符"),
    DEPT_NAME_ALREADY_EXISTS(4002, "同级下部门名称已存在"),
    DEPT_PARENT_INVALID(4003, "父部门不能是自己或自己的子部门"),
    INVALID_DEPT_IDS(4004, "部门 ID 列表不能为空");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
