package com.chatbi.interceptor;

import com.chatbi.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器
 *
 * 对登录接口和查询接口进行请求频率限制：
 * - 登录：5 次/60 秒（IP + 用户名维度）
 * - 查询：100 次/60 秒（用户维度）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 登录接口限流
        if (isLoginEndpoint(uri)) {
            String ip = getClientIp(request);
            String username = request.getParameter("username");
            if (!rateLimitService.tryConsumeLogin(ip, username)) {
                writeRateLimitResponse(response, "登录尝试过于频繁，请 60 秒后再试");
                return false;
            }
        }

        // 查询接口限流（POST/GET 的查询类接口）
        if (isQueryEndpoint(uri, method)) {
            String userId = resolveUserId(request);
            if (!rateLimitService.tryConsumeQuery(userId)) {
                writeRateLimitResponse(response, "查询请求过于频繁，请稍后再试");
                return false;
            }
        }

        return true;
    }

    private boolean isLoginEndpoint(String uri) {
        return uri.contains("/auth/login") || uri.contains("/auth/token");
    }

    private boolean isQueryEndpoint(String uri, String method) {
        if (!"POST".equals(method) && !"GET".equals(method)) {
            return false;
        }
        return uri.startsWith("/api/query") || uri.startsWith("/api/conversation/message");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserId(HttpServletRequest request) {
        // 从 SecurityContext 或 Header 中解析用户 ID
        // 简化：先尝试从 header 中读取
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        // 兜底：使用 session id 或 IP
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        String json = objectMapper.writeValueAsString(Result.error(message));
        response.getWriter().write(json);
    }
}
