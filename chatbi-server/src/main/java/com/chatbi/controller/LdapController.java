package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.common.utils.JwtUtils;
import com.chatbi.entity.SysUser;
import com.chatbi.service.LdapUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LDAP 认证控制器
 */
@Slf4j
@Tag(name = "LDAP 认证", description = "LDAP 单点登录相关接口")
@RestController
@RequestMapping("/api/ldap")
@RequiredArgsConstructor
public class LdapController {

    private final LdapUserService ldapUserService;
    private final JwtUtils jwtUtils;

    /**
     * LDAP 登录
     */
    @Operation(summary = "LDAP 登录", description = "使用 LDAP 用户名密码登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Parameter(description = "登录请求") @RequestBody LoginRequest request) {

        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }

        try {
            // LDAP 登录
            SysUser user = ldapUserService.ldapLogin(username.trim(), password);

            if (user == null) {
                return Result.error("LDAP 认证失败，用户名或密码错误");
            }

            // 生成 Token
            String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
            String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .user(buildUserInfo(user))
                    .build();

            log.info("LDAP 用户登录成功：{}", username);
            return Result.ok(response);

        } catch (Exception e) {
            log.error("LDAP 登录失败：{}", e.getMessage(), e);
            return Result.error("LDAP 登录失败：" + e.getMessage());
        }
    }

    /**
     * 测试 LDAP 连接
     */
    @Operation(summary = "测试 LDAP 连接", description = "测试 LDAP 服务器连接是否正常")
    @PostMapping("/test-connection")
    public Result<Map<String, Object>> testConnection(
            @Parameter(description = "测试请求") @RequestBody TestConnectionRequest request) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 使用提供的配置测试连接
            boolean authenticated = ldapUserService.authenticate(request.getUsername(), request.getPassword());
            result.put("success", authenticated);
            result.put("message", authenticated ? "LDAP 连接成功" : "LDAP 认证失败");

            if (authenticated) {
                var ldapUser = ldapUserService.getUserByUsername(request.getUsername());
                if (ldapUser != null) {
                    result.put("user", Map.of(
                            "uid", ldapUser.getUid(),
                            "cn", ldapUser.getCn(),
                            "mail", ldapUser.getMail()
                    ));
                }
            }

            return Result.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "LDAP 连接失败：" + e.getMessage());
            return Result.ok(result);
        }
    }

    /**
     * 搜索 LDAP 用户
     */
    @Operation(summary = "搜索 LDAP 用户", description = "搜索 LDAP 目录中的用户")
    @GetMapping("/search")
    public Result<List<Map<String, String>>> searchUsers(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {

        try {
            var ldapUsers = ldapUserService.searchUsers(keyword);

            List<Map<String, String>> users = ldapUsers.stream()
                    .map(user -> {
                        Map<String, String> info = new HashMap<>();
                        info.put("uid", user.getUid());
                        info.put("cn", user.getCn());
                        info.put("mail", user.getMail());
                        info.put("telephoneNumber", user.getTelephoneNumber());
                        info.put("department", user.getDepartment());
                        info.put("title", user.getTitle());
                        return info;
                    })
                    .collect(Collectors.toList());

            return Result.ok(users);

        } catch (Exception e) {
            log.error("搜索 LDAP 用户失败：{}", e.getMessage(), e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 同步 LDAP 用户到本地
     */
    @Operation(summary = "同步 LDAP 用户", description = "将 LDAP 用户同步到本地数据库")
    @PostMapping("/sync/{username}")
    public Result<SysUser> syncUser(@PathVariable String username) {
        try {
            var ldapUser = ldapUserService.getUserByUsername(username);
            if (ldapUser == null) {
                return Result.error("LDAP 用户不存在：" + username);
            }

            SysUser user = ldapUserService.syncUserToDatabase(ldapUser);
            return Result.ok(user);

        } catch (Exception e) {
            log.error("同步 LDAP 用户失败：{}", e.getMessage(), e);
            return Result.error("同步失败：" + e.getMessage());
        }
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

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Map<String, Object> user;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class TestConnectionRequest {
        private String username;
        private String password;
    }
}
