package com.chatbi.security;

import com.chatbi.common.constant.SysConstant;
import com.chatbi.common.utils.JwtUtils;
import com.chatbi.security.user.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 拦截请求，验证 Token 有效性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    @Value("${app.security.dev-permit-all-api:false}")
    private boolean devPermitAllApi;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从请求中获取 Token
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 验证 Token
                if (jwtUtils.validateToken(token)) {
                    // 从 Token 中获取用户名
                    String username = jwtUtils.getUsernameFromToken(token);
                    authenticate(request, username);
                } else {
                    log.debug("Token 无效或已过期");
                }
            } else if (devPermitAllApi &&
                request.getRequestURI().startsWith("/api/") &&
                isDevAutoAuthAllowed(request.getRequestURI())) {
                authenticate(request, "admin");
            }
        } catch (Exception e) {
            log.error("JWT 认证失败：{}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String username) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (userDetails instanceof LoginUser loginUser) {
            request.setAttribute("userId", loginUser.getUserId());
        }

        log.debug("用户 [{}] 认证成功", username);
    }

    /**
     * 从请求中获取 Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从 Header 中获取
        String bearerToken = request.getHeader(SysConstant.TOKEN_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SysConstant.TOKEN_PREFIX)) {
            return bearerToken.substring(SysConstant.TOKEN_PREFIX.length());
        }

        // 从参数中获取（不推荐）
        String accessToken = request.getParameter("access_token");
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }

        return null;
    }

    /**
     * 跳过认证的 URL
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 跳过静态资源
        if (path.startsWith("/static/") || path.startsWith("/assets/") ||
            path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".ico")) {
            return true;
        }

        // 跳过公开接口
        // 仅跳过登录与刷新，/api/auth/me 仍需解析 JWT 以返回当前用户信息
        if (path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/refresh") ||
            path.startsWith("/api/public/") ||
            path.startsWith("/api/chart-catalog/") ||
            path.startsWith("/api/health") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui")) {
            return true;
        }

        return false;
    }

    /**
     * 本地开发态自动注入 admin 身份时，必须排除高敏感接口，
     * 防止发布前误判鉴权有效性。
     */
    boolean isDevAutoAuthAllowed(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return false;
        }
        return !(requestUri.equals("/api/audit") ||
            requestUri.startsWith("/api/audit/") ||
            requestUri.equals("/api/system/permissions") ||
            requestUri.startsWith("/api/system/permissions/"));
    }
}
