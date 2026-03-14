package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.common.exception.BusinessException;
import com.chatbi.common.utils.JwtUtils;
import com.chatbi.entity.SysUser;
import com.chatbi.repository.SysUserMapper;
import com.chatbi.security.user.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SSO 认证控制器
 */
@Slf4j
@Tag(name = "SSO 认证", description = "单点登录相关接口")
@RestController
@RequestMapping("/api/sso")
@RequiredArgsConstructor
public class SsoController {

    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;
    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取支持的 OAuth2 Provider 列表
     */
    @GetMapping("/providers")
    @Operation(summary = "获取支持的 OAuth2 提供商")
    public Result<Map<String, String>> getOAuth2Providers() {
        Map<String, String> providers = new HashMap<>();

        // 从配置中获取可用的 provider
        String[] supportedProviders = {"google", "github", "wechat", "dingtalk"};

        if (clientRegistrationRepository.isPresent()) {
            ClientRegistrationRepository repo = clientRegistrationRepository.get();
            for (String provider : supportedProviders) {
                ClientRegistration registration = repo.findByRegistrationId(provider);
                if (registration != null) {
                    String authorizationUri = registration.getProviderDetails().getAuthorizationUri();
                    providers.put(provider, authorizationUri);
                }
            }
        }

        return Result.ok(providers);
    }

    /**
     * OAuth2 回调处理
     */
    @GetMapping("/callback/{registrationId}")
    @Operation(summary = "OAuth2 回调")
    public ResponseEntity<?> oauth2Callback(
            @PathVariable String registrationId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {

        log.info("OAuth2 回调 - Provider: {}", registrationId);

        if (oauth2User == null) {
            throw BusinessException.businessError("OAuth2 认证失败");
        }

        // 获取用户信息
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String avatar = oauth2User.getAttribute("picture");

        // 如果 email 为空，尝试其他属性
        if (email == null && "github".equals(registrationId)) {
            email = oauth2User.getAttribute("login");
        }

        log.info("OAuth2 用户信息 - Email: {}, Name: {}", email, name);

        // 查找或创建用户
        SysUser user = findOrCreateUser(registrationId, oauth2User);

        // 生成 JWT Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("user", buildUserInfo(user));

        // 重定向到前端页面（带 token）
        String redirectUrl = buildRedirectUrl(accessToken, refreshToken);
        return ResponseEntity.status(307).location(URI.create(redirectUrl)).build();
    }

    /**
     * 查找或创建用户
     */
    private SysUser findOrCreateUser(String registrationId, OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            email = oauth2User.getAttribute("login") + "@" + registrationId + ".com";
        }

        // 先查找是否存在
        SysUser existingUser = sysUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email));

        if (existingUser != null) {
            return existingUser;
        }

        // 创建新用户
        SysUser newUser = new SysUser();
        newUser.setUsername(email.split("@")[0]);
        newUser.setEmail(email);
        newUser.setNickName(name != null ? name : email.split("@")[0]);
        newUser.setAvatar(oauth2User.getAttribute("picture"));
        newUser.setStatus(1);

        sysUserMapper.insert(newUser);
        log.info("创建新用户：{}", newUser.getEmail());

        return newUser;
    }

    /**
     * 构建用户信息
     */
    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickName", user.getNickName());
        info.put("email", user.getEmail());
        info.put("avatar", user.getAvatar());
        info.put("phone", user.getPhone());
        return info;
    }

    /**
     * 构建重定向 URL
     */
    private String buildRedirectUrl(String accessToken, String refreshToken) {
        // 重定向到前端的回调页面，携带 token 参数
        return ServletUriComponentsBuilder.fromUriString("http://localhost:5173/sso/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("tokenType", "Bearer")
                .build()
                .toUriString();
    }

    /**
     * 获取当前 SSO 登录用户信息
     */
    @GetMapping("/user")
    @Operation(summary = "获取当前 SSO 用户信息")
    public Result<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw BusinessException.tokenInvalid();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", oauth2User.getAttribute("email"));
        userInfo.put("name", oauth2User.getAttribute("name"));
        userInfo.put("attributes", oauth2User.getAttributes());

        return Result.ok(userInfo);
    }
}
