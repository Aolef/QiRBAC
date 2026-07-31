package org.zzq.qirbac.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 *
 * 它的作用是把后端异常统一包装成 Result，
 * 避免前端收到 Spring 默认的错误结构或 Java 报错信息。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理我们主动抛出的业务异常。
     *
     * 例如登录失败时抛出 BusinessException，
     * 这里会统一返回 { code, message, data, success }。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理其他未预料到的异常。
     *
     * 这里不把具体异常内容直接返回给前端，
     * 避免暴露后端内部细节。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(ResultCode.FAIL);
    }
}
