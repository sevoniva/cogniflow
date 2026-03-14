package com.chatbi.aspect;

import com.chatbi.common.constant.SysConstant;
import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import com.chatbi.security.user.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 切入点：标注了 @AuditLog 注解的方法
     */
    @Pointcut("@annotation(com.chatbi.annotation.Audit)")
    public void auditPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("auditPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ?
                (HttpServletRequest) attributes.getRequest() : null;

        // 构建审计日志
        AuditLog auditLog = AuditLog.builder()
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .createdAt(LocalDateTime.now())
                .build();

        // 获取用户信息
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) principal;
            auditLog.setUserId(loginUser.getUserId());
            auditLog.setUsername(loginUser.getUsername());
        }

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        com.chatbi.annotation.Audit auditAnnotation =
                signature.getMethod().getAnnotation(com.chatbi.annotation.Audit.class);

        if (auditAnnotation != null) {
            auditLog.setAction(auditAnnotation.action());
            auditLog.setResourceType(auditAnnotation.resourceType());
        }

        // 设置请求信息
        if (request != null) {
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setRequestUri(request.getRequestURI());
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));

            // 记录请求体
            try {
                auditLog.setRequestBody(objectMapper.writeValueAsString(joinPoint.getArgs()));
            } catch (Exception e) {
                log.debug("记录请求体失败：{}", e.getMessage());
            }
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            auditLog.setResult(SysConstant.RESULT_SUCCESS);

        } catch (Throwable e) {
            auditLog.setResult(SysConstant.RESULT_FAILED);
            auditLog.setErrorMessage(e.getMessage());
            throw e;

        } finally {
            // 设置响应状态和执行时间
            auditLog.setExecuteTimeMs((int) (System.currentTimeMillis() - startTime));

            // 异步保存审计日志
            saveAuditLogAsync(auditLog, result);
        }

        return result;
    }

    /**
     * 异步保存审计日志
     */
    private void saveAuditLogAsync(AuditLog auditLog, Object result) {
        try {
            // 异步执行，不阻塞主流程
            new Thread(() -> {
                try {
                    if (result != null) {
                        try {
                            auditLog.setResponseBody(objectMapper.writeValueAsString(result));
                        } catch (Exception e) {
                            log.debug("记录响应体失败：{}", e.getMessage());
                        }
                    }
                    auditLogMapper.insert(auditLog);
                } catch (Exception e) {
                    log.error("保存审计日志失败：{}", e.getMessage(), e);
                }
            }).start();

        } catch (Exception e) {
            log.error("异步保存审计日志失败：{}", e.getMessage());
        }
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
