package org.zzq.qirbac.common;

/**
 * 业务异常。
 *
 * 当业务逻辑发现不能继续时，可以抛出这个异常。
 * 例如：用户名密码错误、用户被禁用、Token 过期。
 *
 * 抛出后不用每个接口都手动 try/catch，
 * GlobalExceptionHandler 会统一把它转换成 Result 返回给前端。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
