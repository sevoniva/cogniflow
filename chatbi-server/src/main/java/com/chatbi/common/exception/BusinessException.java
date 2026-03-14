package com.chatbi.common.exception;

import com.chatbi.common.HttpStatus;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = HttpStatus.ERROR;
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = HttpStatus.ERROR;
        this.message = message;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    // ========== 快捷构造方法 ==========

    public static BusinessException userNotFound() {
        return new BusinessException(HttpStatus.USER_NOT_FOUND, "用户不存在");
    }

    public static BusinessException userPasswordError() {
        return new BusinessException(HttpStatus.USER_PASSWORD_ERROR, "用户名或密码错误");
    }

    public static BusinessException userLocked() {
        return new BusinessException(HttpStatus.USER_LOCKED, "用户已被锁定，请稍后再试");
    }

    public static BusinessException userDisabled() {
        return new BusinessException(HttpStatus.USER_DISABLED, "用户已被禁用");
    }

    public static BusinessException tokenInvalid() {
        return new BusinessException(HttpStatus.TOKEN_INVALID, "Token 无效");
    }

    public static BusinessException tokenExpired() {
        return new BusinessException(HttpStatus.TOKEN_EXPIRED, "Token 已过期");
    }

    public static BusinessException tokenMissing() {
        return new BusinessException(HttpStatus.TOKEN_MISSING, "Token 不能为空");
    }

    public static BusinessException permissionDenied() {
        return new BusinessException(HttpStatus.PERMISSION_DENIED, "权限不足");
    }

    public static BusinessException dataNotFound() {
        return new BusinessException(HttpStatus.DATA_NOT_FOUND, "数据不存在");
    }

    public static BusinessException dataDuplicate(String field) {
        return new BusinessException(HttpStatus.DATA_DUPLICATE, field + "已存在");
    }

    public static BusinessException queryExecuteError(String message) {
        return new BusinessException(HttpStatus.QUERY_EXECUTE_ERROR, "查询执行失败：" + message);
    }

    public static BusinessException businessError(String message) {
        return new BusinessException(HttpStatus.ERROR, message);
    }
}
