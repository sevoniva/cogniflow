package com.chatbi.aspect;

import com.chatbi.common.constant.SysConstant;
import com.chatbi.entity.AuditLog;
import com.chatbi.security.user.LoginUser;
import com.chatbi.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 审计日志切面
 *
 * 改造说明：
 * - 将 new Thread() 替换为 @Async + 线程池（可监控、可配置）
 * - 敏感字段（password/token/secret 等）自动掩码
 * - traceId 复用链路追踪 MDC 中的值，实现日志关联
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwd", "pwd", "token", "secret", "apiKey", "apiSecret",
            "accessKey", "accessSecret", "auth", "authorization", "credential",
            "privateKey", "creditCard", "cardNo", "cvv", "ssn"
    );

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

        // 构建审计日志 - traceId 复用链路追踪上下文
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = java.util.UUID.randomUUID().toString().replace("-", "");
        }

        AuditLog auditLog = AuditLog.builder()
                .traceId(traceId)
                .createdAt(LocalDateTime.now())
                .build();

        // 获取用户信息
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof LoginUser) {
                LoginUser loginUser = (LoginUser) principal;
                auditLog.setUserId(loginUser.getUserId());
                auditLog.setUsername(loginUser.getUsername());
            }
        } catch (Exception ignored) {
            // 未登录或匿名访问时忽略
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

            // 记录请求体（敏感字段掩码）
            try {
                String requestBody = objectMapper.writeValueAsString(joinPoint.getArgs());
                auditLog.setRequestBody(maskSensitiveFields(requestBody));
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
            auditLog.setExecuteTimeMs((int) (System.currentTimeMillis() - startTime));
            saveAuditLogAsync(auditLog, result);
        }

        return result;
    }

    /**
     * 异步保存审计日志（使用 @Async + 线程池）
     */
    private void saveAuditLogAsync(AuditLog auditLog, Object result) {
        try {
            if (result != null) {
                try {
                    String responseBody = objectMapper.writeValueAsString(result);
                    auditLog.setResponseBody(maskSensitiveFields(responseBody));
                } catch (Exception e) {
                    log.debug("记录响应体失败：{}", e.getMessage());
                }
            }
            auditLogService.saveAsync(auditLog);
        } catch (Exception e) {
            log.error("异步保存审计日志失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 敏感字段掩码
     *
     * 对 JSON 字符串中的敏感字段值进行掩码处理。
     * 策略：保留前 3 位和后 3 位，中间替换为 ***；短于 6 位则全部替换。
     */
    String maskSensitiveFields(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            if (json.trim().startsWith("{")) {
                ObjectNode node = (ObjectNode) objectMapper.readTree(json);
                maskObjectNode(node);
                return objectMapper.writeValueAsString(node);
            }
            if (json.trim().startsWith("[")) {
                ArrayNode array = (ArrayNode) objectMapper.readTree(json);
                for (int i = 0; i < array.size(); i++) {
                    if (array.get(i).isObject()) {
                        maskObjectNode((ObjectNode) array.get(i));
                    }
                }
                return objectMapper.writeValueAsString(array);
            }
        } catch (Exception e) {
            log.debug("敏感字段掩码失败（可能为非 JSON），保留原值");
        }
        return json;
    }

    private void maskObjectNode(ObjectNode node) {
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey().toLowerCase();
            if (SENSITIVE_FIELDS.contains(key) || SENSITIVE_FIELDS.stream().anyMatch(key::contains)) {
                String value = entry.getValue().asText("");
                entry.setValue(objectMapper.valueToTree(maskValue(value)));
            } else if (entry.getValue().isObject()) {
                maskObjectNode((ObjectNode) entry.getValue());
            } else if (entry.getValue().isArray()) {
                ArrayNode arr = (ArrayNode) entry.getValue();
                for (int i = 0; i < arr.size(); i++) {
                    if (arr.get(i).isObject()) {
                        maskObjectNode((ObjectNode) arr.get(i));
                    }
                }
            }
        });
    }

    private String maskValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
