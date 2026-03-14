package com.chatbi.service;

import com.chatbi.common.exception.BusinessException;
import com.chatbi.common.utils.JwtUtils;
import com.chatbi.dto.auth.LoginRequest;
import com.chatbi.dto.auth.LoginResponse;
import com.chatbi.entity.SysUser;
import com.chatbi.entity.SysRole;
import com.chatbi.repository.SysRoleMapper;
import com.chatbi.repository.SysUserMapper;
import com.chatbi.security.user.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 测试")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private LoginUser mockLoginUser;
    private SysUser mockUser;

    @BeforeEach
    void setUp() {
        // 重置安全上下文
        SecurityContextHolder.clearContext();

        // 创建模拟用户
        mockUser = SysUser.builder()
                .id(1L)
                .username("testuser")
                .nickName("测试用户")
                .email("test@example.com")
                .phone("13800138000")
                .avatar("https://example.com/avatar.png")
                .status(1)
                .build();

        mockLoginUser = new LoginUser(
                1L,
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("USER:READ")),
                true,
                true,
                true,
                true,
                "test@example.com",
                "13800138000",
                1L
        );
    }

    @Test
    @DisplayName("登录成功测试")
    void testLogin_Success() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        String accessToken = "access_token_123";
        String refreshToken = "refresh_token_456";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        // 配置 Mock 行为
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockLoginUser);
        when(jwtUtils.generateAccessToken(eq(1L), eq("testuser"))).thenReturn(accessToken);
        when(jwtUtils.generateRefreshToken(eq(1L), eq("testuser"))).thenReturn(refreshToken);
        when(jwtUtils.getExpirationFromToken(accessToken)).thenReturn(expiration);
        when(sysUserMapper.selectById(1L)).thenReturn(mockUser);
        when(sysRoleMapper.selectRolesByUserId(1L)).thenReturn(List.of(
                SysRole.builder().roleCode("ADMIN").build()
        ));

        // 执行测试
        LoginResponse response = authService.login(request);

        // 验证结果
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("testuser", response.getUser().getUsername());
        assertEquals("测试用户", response.getUser().getNickName());

        // 验证方法调用
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateAccessToken(1L, "testuser");
        verify(jwtUtils).generateRefreshToken(1L, "testuser");
        verify(sysUserMapper).selectById(1L);
        verify(sysRoleMapper).selectRolesByUserId(1L);
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_BadCredentials() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        // 配置 Mock 行为
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().contains("密码"));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("登录失败 - 账户已禁用")
    void testLogin_Disabled() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        // 配置 Mock 行为
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("账户已禁用"));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().contains("禁用"));
    }

    @Test
    @DisplayName("登录失败 - 账户已锁定")
    void testLogin_Locked() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        // 配置 Mock 行为
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("账户已锁定"));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().contains("锁定"));
    }

    @Test
    @DisplayName("刷新 Token 测试")
    void testRefreshToken() {
        String refreshToken = "refresh_token_456";
        String newAccessToken = "new_access_token_789";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.refreshToken(refreshToken)).thenReturn(newAccessToken);

        String result = authService.refreshToken(refreshToken);

        assertEquals(newAccessToken, result);
        verify(jwtUtils).validateToken(refreshToken);
        verify(jwtUtils).refreshToken(refreshToken);
    }

    @Test
    @DisplayName("刷新 Token 失败 - Token 已过期")
    void testRefreshToken_Expired() {
        String refreshToken = "expired_refresh_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshToken(refreshToken)
        );

        assertTrue(exception.getMessage().contains("过期") || exception.getMessage().contains("Token"));
        verify(jwtUtils).validateToken(refreshToken);
        verify(jwtUtils, never()).refreshToken(anyString());
    }

    @Test
    @DisplayName("登出测试")
    void testLogout() {
        // 设置安全上下文
        SecurityContextHolder.setContext(securityContext);

        // 执行登出
        authService.logout();

        // 验证安全上下文已清空
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("获取当前用户测试")
    void testGetCurrentUser() {
        // 设置安全上下文
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockLoginUser);
        SecurityContextHolder.setContext(securityContext);

        // 执行测试
        LoginUser result = authService.getCurrentUser();

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("获取当前用户失败 - 未认证")
    void testGetCurrentUser_Unauthenticated() {
        // 设置空的安全上下文
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.getCurrentUser()
        );

        assertTrue(exception.getMessage().contains("Token") || exception.getMessage().contains("无效"));
    }

    @Test
    @DisplayName("获取当前用户 ID 测试")
    void testGetCurrentUserId() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockLoginUser);
        SecurityContextHolder.setContext(securityContext);

        Long userId = authService.getCurrentUserId();

        assertEquals(1L, userId);
    }

    @Test
    @DisplayName("获取当前用户名测试")
    void testGetCurrentUsername() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockLoginUser);
        SecurityContextHolder.setContext(securityContext);

        String username = authService.getCurrentUsername();

        assertEquals("testuser", username);
    }
}
