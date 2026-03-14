package com.chatbi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.HashMap;
import java.util.Map;

/**
 * LDAP 配置
 */
@Configuration
@Profile("ldap")
public class LdapConfig {

    @Value("${ldap.url:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${ldap.base:dc=example,dc=com}")
    private String ldapBase;

    @Value("${ldap.username:cn=admin,dc=example,dc=com}")
    private String ldapUsername;

    @Value("${ldap.password:admin}")
    private String ldapPassword;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBase);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);

        // 配置连接池
        Map<String, Object> config = new HashMap<>();
        config.put("com.sun.jndi.ldap.connect.pool", "true");
        config.put("com.sun.jndi.ldap.connect.pool.timeout", "300000");
        config.put("com.sun.jndi.ldap.connect.timeout", "5000");
        contextSource.setBaseEnvironmentProperties(config);

        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(ldapContextSource());
    }
}
