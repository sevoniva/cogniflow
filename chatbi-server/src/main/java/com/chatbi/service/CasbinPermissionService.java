package com.chatbi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.stereotype.Service;

/**
 * Casbin RBAC 权限服务
 *
 * 封装 Casbin Enforcer，提供域名（domain）级别的 RBAC 权限校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasbinPermissionService {

    private final Enforcer enforcer;

    private static final String DEFAULT_DOMAIN = "default";

    /**
     * 检查用户是否有权限执行指定操作
     *
     * @param user   用户标识（用户名或角色）
     * @param domain 域（如租户 ID，默认 default）
     * @param obj    资源路径（如 /api/query）
     * @param act    操作（GET/POST/PUT/DELETE）
     */
    public boolean check(String user, String domain, String obj, String act) {
        if (user == null || obj == null || act == null) {
            return false;
        }
        String effectiveDomain = domain == null || domain.isBlank() ? DEFAULT_DOMAIN : domain;
        boolean allowed = enforcer.enforce(user, effectiveDomain, obj, act);
        log.debug("Casbin 权限检查 - user: {}, domain: {}, obj: {}, act: {}, allowed: {}",
                user, effectiveDomain, obj, act, allowed);
        return allowed;
    }

    public boolean check(String user, String obj, String act) {
        return check(user, DEFAULT_DOMAIN, obj, act);
    }

    /**
     * 为用户添加角色
     */
    public void assignRole(String user, String role, String domain) {
        String effectiveDomain = domain == null || domain.isBlank() ? DEFAULT_DOMAIN : domain;
        enforcer.addGroupingPolicy(user, role, effectiveDomain);
        log.info("Casbin 角色分配 - user: {} -> role: {} in domain: {}", user, role, effectiveDomain);
    }

    /**
     * 添加策略
     */
    public void addPolicy(String role, String domain, String obj, String act) {
        enforcer.addPolicy(role, domain, obj, act);
        log.info("Casbin 策略添加 - role: {}, domain: {}, obj: {}, act: {}", role, domain, obj, act);
    }

    /**
     * 删除策略
     */
    public void removePolicy(String role, String domain, String obj, String act) {
        enforcer.removePolicy(role, domain, obj, act);
        log.info("Casbin 策略删除 - role: {}, domain: {}, obj: {}, act: {}", role, domain, obj, act);
    }
}
