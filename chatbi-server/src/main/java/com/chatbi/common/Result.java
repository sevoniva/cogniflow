package com.chatbi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一 API 响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 链路 ID
     */
    private String traceId;

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> ok() {
        return new Result<>(HttpStatus.SUCCESS, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(HttpStatus.SUCCESS, "操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(HttpStatus.SUCCESS, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(HttpStatus.ERROR, message, null);
    }

    /**
     * 失败响应（带错误码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（带错误码和数据）
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(HttpStatus.UNAUTHORIZED, message, null);
    }

    /**
     * 禁止访问响应
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(HttpStatus.FORBIDDEN, message, null);
    }

    /**
     * 未找到资源响应
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(HttpStatus.NOT_FOUND, message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return HttpStatus.SUCCESS.equals(this.code);
    }
}
