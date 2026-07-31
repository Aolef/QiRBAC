package org.zzq.qirbac.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结构。
 *
 * Controller 不直接返回零散数据，而是统一返回 Result：
 * {
 *     "code": 200,
 *     "message": "操作成功",
 *     "data": {},
 *     "success": true
 * }
 *
 * 这样前端处理接口时会更稳定，不用每个接口单独适配一种格式。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 业务状态码。
     *
     * 这里的 code 是业务约定，不一定完全等于 HTTP 状态码。
     */
    private Integer code;

    /**
     * 提示信息。
     *
     * 成功时通常是“操作成功”，失败时是具体错误原因。
     */
    private String message;

    /**
     * 真正返回给前端的数据。
     *
     * 没有数据时可以是 null。
     */
    private T data;

    /**
     * 是否成功。
     *
     * 前端可以直接用这个字段判断请求是否成功。
     */
    private Boolean success;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, true);
    }

    public static <T> Result<T> fail() {
        return fail(ResultCode.FAIL);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null, false);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, false);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }
}
