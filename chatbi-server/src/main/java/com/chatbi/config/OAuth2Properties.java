package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class OAuth2Properties {

    private Map<String, Provider> provider = new HashMap<>();
    private Map<String, Registration> registration = new HashMap<>();

    @Data
    public static class Provider {
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttribute;
        private String issuerUri;
        private String jwkSetUri;
    }

    @Data
    public static class Registration {
        private String clientId;
        private String clientSecret;
        private String authorizationGrantType;
        private String redirectUri;
        private String scope;
        private String provider;
    }
}
