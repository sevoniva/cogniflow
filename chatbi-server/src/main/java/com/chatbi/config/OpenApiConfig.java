package com.chatbi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger 配置
 */
@Configuration
public class OpenApiConfig {

    @Value("${chatbi.server.baseUrl:}")
    private String serverUrl;

    @Bean
    public OpenAPI chatbiOpenAPI() {
        // 创建服务器列表
        var servers = List.of(
            new Server().url("http://localhost:8080").description("本地开发环境"),
            new Server().url("https://api.chatbi.com").description("生产环境")
        );

        // 如果有配置 baseUrl，添加到服务器列表
        if (serverUrl != null && !serverUrl.isBlank()) {
            servers = new java.util.ArrayList<>(servers);
            ((List<Server>) servers).add(0, new Server().url(serverUrl).description("自定义服务器"));
        }

        return new OpenAPI()
                .servers(servers)
                .info(new Info()
                        .title("ChatBI API 文档")
                        .description("""
                                ## ChatBI 企业级 BI 平台 API

                                提供完整的用户认证授权、数据源管理、指标管理、查询分析、仪表板管理等功能。

                                ### 认证说明
                                所有需要认证的接口都需要在请求头中携带 JWT Token：
                                `Authorization: Bearer {token}`

                                ### 响应格式
                                所有接口统一返回 JSON 格式：
                                - code: 状态码 (200 表示成功)
                                - message: 响应消息
                                - data: 响应数据
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("ChatBI Team")
                                .email("support@chatbi.com")
                                .url("https://www.chatbi.com"))
                        .license(new License()
                                .name("Enterprise License")
                                .url("https://www.chatbi.com/license")))
                .schemaRequirement("BearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Token 认证"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
