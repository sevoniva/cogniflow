package com.chatbi.service;

import com.chatbi.common.exception.BusinessException;
import com.chatbi.common.utils.JwtUtils;
import com.chatbi.dto.auth.LoginRequest;
import com.chatbi.dto.auth.LoginResponse;
import com.chatbi.entity.SysRole;
import com.chatbi.entity.SysUser;
import com.chatbi.repository.SysRoleMapper;
import com.chatbi.repository.SysUserMapper;
import com.chatbi.security.user.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        try {
            // Spring Security 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 认证通过，获取用户信息
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 生成 Token
            String accessToken = jwtUtils.generateAccessToken(loginUser.getUserId(), loginUser.getUsername());
            String refreshToken = jwtUtils.generateRefreshToken(loginUser.getUserId(), loginUser.getUsername());

            // 查询用户详情
            SysUser user = sysUserMapper.selectById(loginUser.getUserId());

            // 查询用户角色
            List<SysRole> roles = sysRoleMapper.selectRolesByUserId(user.getId());
            List<String> roleCodes = roles.stream()
                    .map(SysRole::getRoleCode)
                    .toList();
            if (roleCodes.isEmpty()) {
                roleCodes = List.of("USER");
            }

            // 构建响应
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtUtils.getExpirationFromToken(accessToken).getTime() - System.currentTimeMillis())
                    .tokenType("Bearer")
                    .user(LoginResponse.UserInfoVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .nickName(user.getNickName())
                            .avatar(user.getAvatar())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .roles(roleCodes)
                            .permissions(loginUser.getAuthorities().stream()
                                    .map(a -> a.getAuthority())
                                    .toList())
                            .build())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("登录失败 - 用户名或密码错误：{}", request.getUsername());
            throw BusinessException.userPasswordError();
        } catch (DisabledException e) {
            log.warn("登录失败 - 账户已禁用：{}", request.getUsername());
            throw BusinessException.userDisabled();
        } catch (LockedException e) {
            log.warn("登录失败 - 账户已锁定：{}", request.getUsername());
            throw BusinessException.userLocked();
        } catch (Exception e) {
            log.error("登录失败：{}", e.getMessage(), e);
            throw new BusinessException("登录失败：" + e.getMessage());
        }
    }

    /**
     * 刷新 Token
     */
    public String refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw BusinessException.tokenExpired();
        }
        return jwtUtils.refreshToken(refreshToken);
    }

    /**
     * 用户登出
     */
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("用户登出成功");
    }

    /**
     * 获取当前登录用户
     */
    public LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw BusinessException.tokenInvalid();
        }
        return (LoginUser) authentication.getPrincipal();
    }

    /**
     * 获取当前用户 ID
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
}
