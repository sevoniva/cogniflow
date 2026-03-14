package com.chatbi.config;

import org.apache.catalina.Context;
import org.apache.catalina.Container;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 配置 - 禁用 JSP
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addContextCustomizers((Context context) -> {
            // 移除 JSP Servlet
            Container jspWrapper = context.findChild("jsp");
            if (jspWrapper != null) {
                context.removeChild(jspWrapper);
            }

            // 禁用 JSP
            context.setJspConfigDescriptor(null);
        });
    }
}
