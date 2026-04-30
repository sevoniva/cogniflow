package com.chatbi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ChatBI 企业版启动类
 */
@SpringBootApplication
@EnableScheduling
public class ChatBiApplication {

    private static final Logger log = LoggerFactory.getLogger(ChatBiApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ChatBiApplication.class, args);
        ConfigurableEnvironment environment = context.getEnvironment();

        // 打印启动信息
        printStartupInfo(environment);
    }

    /**
     * 打印启动信息
     */
    private static void printStartupInfo(ConfigurableEnvironment environment) {
        String protocol = getProtocol(environment);
        String host = getHost();
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        log.info("");
        log.info("==========================================================");
        log.info("  ChatBI 企业版启动成功！");
        log.info("==========================================================");
        log.info("  访问地址：{}://{}:{}{}", protocol, host, port, contextPath);
        log.info("  API 文档：{}://{}:{}{}/swagger-ui/index.html", protocol, host, port, contextPath);
        log.info("  健康检查：{}://{}:{}{}/api/health", protocol, host, port, contextPath);
        log.info("==========================================================");
        log.info("");
    }

    private static String getProtocol(ConfigurableEnvironment environment) {
        String sslEnabled = environment.getProperty("server.ssl.enabled", "false");
        return "true".equals(sslEnabled) ? "https" : "http";
    }

    private static String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
