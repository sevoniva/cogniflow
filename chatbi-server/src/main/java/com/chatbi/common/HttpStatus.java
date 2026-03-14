package com.chatbi.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HTTP 状态码定义
 */
@Getter
@AllArgsConstructor
public class HttpStatus {

    /**
     * 成功
     */
    public static final Integer SUCCESS = 200;

    /**
     * 请求参数错误
     */
    public static final Integer BAD_REQUEST = 400;

    /**
     * 未授权
     */
    public static final Integer UNAUTHORIZED = 401;

    /**
     * 禁止访问
     */
    public static final Integer FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final Integer NOT_FOUND = 404;

    /**
     * 方法不允许
     */
    public static final Integer METHOD_NOT_ALLOWED = 405;

    /**
     * 服务器内部错误
     */
    public static final Integer ERROR = 500;

    // ========== 业务错误码 ==========

    /**
     * 用户相关错误 (1001-1999)
     */
    public static final Integer USER_NOT_FOUND = 1001;
    public static final Integer USER_PASSWORD_ERROR = 1002;
    public static final Integer USER_LOCKED = 1003;
    public static final Integer USER_DISABLED = 1004;

    /**
     * Token 相关错误 (2001-2999)
     */
    public static final Integer TOKEN_INVALID = 2001;
    public static final Integer TOKEN_EXPIRED = 2002;
    public static final Integer TOKEN_MISSING = 2003;

    /**
     * 权限相关错误 (3001-3999)
     */
    public static final Integer PERMISSION_DENIED = 3001;
    public static final Integer ROLE_NOT_FOUND = 3002;

    /**
     * 数据相关错误 (4001-4999)
     */
    public static final Integer DATA_NOT_FOUND = 4001;
    public static final Integer DATA_DUPLICATE = 4002;
    public static final Integer DATA_INVALID = 4003;

    /**
     * 查询相关错误 (5001-5999)
     */
    public static final Integer QUERY_EXECUTE_ERROR = 5001;
    public static final Integer QUERY_TIMEOUT = 5002;
    public static final Integer QUERY_NO_RESULT = 5003;

    /**
     * 系统相关错误 (9001-9999)
     */
    public static final Integer SYSTEM_ERROR = 9001;
    public static final Integer DATABASE_ERROR = 9002;
    public static final Integer CACHE_ERROR = 9003;
}
