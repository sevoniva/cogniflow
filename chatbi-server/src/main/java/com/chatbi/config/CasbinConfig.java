package com.chatbi.config;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
        Path modelPath = extractToTemp("casbin/rbac_model.conf", ".conf");
        Path policyPath = extractToTemp("casbin/rbac_policy.csv", ".csv");

        Adapter adapter = new FileAdapter(policyPath.toString());
        Enforcer enforcer = new Enforcer(modelPath.toString(), adapter);

        log.info("Casbin Enforcer 初始化完成 - 策略数: {}, 角色数: {}",
                enforcer.getNamedPolicy("p").size(),
                enforcer.getNamedGroupingPolicy("g").size());
        return enforcer;
    }

    private Path extractToTemp(String classpath, String suffix) throws Exception {
        Path temp = Files.createTempFile("casbin-", suffix);
        temp.toFile().deleteOnExit();
        try (InputStream is = new ClassPathResource(classpath).getInputStream()) {
            Files.copy(is, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }
}
