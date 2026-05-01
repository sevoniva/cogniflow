package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.dto.auth.LoginRequest;
import com.chatbi.dto.auth.LoginResponse;
import com.chatbi.security.user.LoginUser;
import com.chatbi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@Tag(name = "认证", description = "认证控制器")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.ok("登录成功", response);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok("登出成功", null);
    }

    /**
     * 刷新 Token
     */
    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestParam String refreshToken) {
        String newToken = authService.refreshToken(refreshToken);
        return Result.ok("刷新成功", newToken);
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<LoginResponse.UserInfoVO> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        LoginUser currentUser = loginUser;
        if (currentUser == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser principal) {
                currentUser = principal;
            }
        }

        if (currentUser == null || currentUser.getUserId() == null || currentUser.getUsername() == null) {
            return Result.unauthorized("未认证，请先登录");
        }
        List<String> permissions = currentUser.getAuthorities() == null
            ? List.of()
            : currentUser.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        LoginResponse.UserInfoVO userInfo = LoginResponse.UserInfoVO.builder()
                .id(currentUser.getUserId())
                .username(currentUser.getUsername())
                .nickName(currentUser.getUsername())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .avatar("")
                .permissions(permissions)
                .build();
        return Result.ok(userInfo);
    }
}
