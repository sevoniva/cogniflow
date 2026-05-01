package com.chatbi.config;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Casbin RBAC 权限引擎配置
 *
 * 加载 RBAC 模型和策略，提供 Enforcer Bean。
 */
@Slf4j
@Configuration
public class CasbinConfig {

    @Bean
    public Enforcer enforcer() throws Exception {
        ClassPathResource modelResource = new ClassPathResource("casbin/rbac_model.conf");
        ClassPathResource policyResource = new ClassPathResource("casbin/rbac_policy.csv");

        Adapter adapter = new FileAdapter(policyResource.getFile().getAbsolutePath());
        Enforcer enforcer = new Enforcer(modelResource.getFile().getAbsolutePath(), adapter);

        log.info("Casbin Enforcer 初始化完成 - 策略数: {}, 角色数: {}",
                enforcer.getNamedPolicy("p").size(),
                enforcer.getNamedGroupingPolicy("g").size());
        return enforcer;
    }
}
