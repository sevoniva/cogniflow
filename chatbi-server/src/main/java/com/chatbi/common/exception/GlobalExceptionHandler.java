package com.chatbi.common.exception;

import com.chatbi.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败：{}", message);
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        log.warn("参数绑定失败：{}", message);
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 单个参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getMessage();
        log.warn("参数校验失败：{}", message);
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持 [{}]: {}", request.getRequestURI(), e.getMethod());
        return Result.error(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "不支持的请求方法：" + e.getMethod());
    }

    /**
     * 资源不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("资源不存在 [{}]", request.getRequestURI());
        return Result.error(HttpStatus.NOT_FOUND.value(), "资源不存在");
    }

    /**
     * 认证异常 - 凭证错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<?> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败：用户名或密码错误");
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
    }

    /**
     * 认证异常 - 账户已禁用
     */
    @ExceptionHandler(DisabledException.class)
    public Result<?> handleDisabledException(DisabledException e) {
        log.warn("认证失败：账户已禁用");
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "账户已禁用");
    }

    /**
     * 认证异常 - 账户已锁定
     */
    @ExceptionHandler(LockedException.class)
    public Result<?> handleLockedException(LockedException e) {
        log.warn("认证失败：账户已锁定");
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "账户已锁定，请稍后再试");
    }

    /**
     * 访问 denied 异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝：{}", e.getMessage());
        return Result.error(HttpStatus.FORBIDDEN.value(), "权限不足");
    }

    /**
     * SQL 异常
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public Result<?> handleDataAccessException(org.springframework.dao.DataAccessException e) {
        log.error("数据库访问异常", e);

        String message = "数据库访问失败，请稍后再试";

        // 根据具体异常类型提供更友好的提示
        String errorMsg = e.getMessage();
        if (errorMsg != null) {
            if (errorMsg.contains("Duplicate entry")) {
                message = "数据已存在，请勿重复添加";
            } else if (errorMsg.contains("foreign key constraint")) {
                message = "该数据正在被使用，无法删除";
            } else if (errorMsg.contains("syntax error")) {
                message = "SQL语法错误，请检查查询语句";
            } else if (errorMsg.contains("doesn't exist")) {
                message = "表或字段不存在，请检查数据源配置";
            } else if (errorMsg.contains("Access denied")) {
                message = "数据库访问权限不足";
            } else if (errorMsg.contains("Communications link failure")) {
                message = "数据库连接失败，请检查网络或数据源配置";
            }
        }

        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    /**
     * 运行时异常 - 增强错误提示
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);

        String message = e.getMessage();
        if (message != null) {
            // 未配置相关
            if (message.contains("未配置") || message.contains("not configured")) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), message);
            }
            // 未找到相关
            else if (message.contains("未找到") || message.contains("not found")) {
                return Result.error(HttpStatus.NOT_FOUND.value(), message);
            }
            // LLM相关错误
            else if (message.contains("LLM") || message.contains("OpenAI")) {
                return Result.error(HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "AI服务���时不可用，已使用规则引擎生成查询");
            }
            // SQL生成失败
            else if (message.contains("SQL") && message.contains("生成")) {
                return Result.error(HttpStatus.BAD_REQUEST.value(),
                    "无法理解您的查询，请尝试更明确的表达");
            }
        }

        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "操作失败：" + (message != null ? message : "未知错误"));
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 [{}]", request.getRequestURI(), e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "系统内部错误，请联系管理员");
    }

    /**
     * 其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "系统异常，请稍后再试");
    }
}
