package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 可观测性告警通知配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "chatbi.ai.observability.notify")
public class AiObservabilityNotifyProperties {

    /**
     * 是否启用告警通知。
     */
    private boolean enabled = false;

    /**
     * 通知通道，逗号分隔：EMAIL,DINGTALK,WECHAT。
     */
    private String channels = "";

    /**
     * 邮件接收人（多个邮箱逗号分隔）。
     */
    private String emailTo = "";

    /**
     * 钉钉 webhook。
     */
    private String dingtalkWebhook = "";

    /**
     * 企业微信 webhook。
     */
    private String wechatWebhook = "";

    /**
     * 通知标题前缀。
     */
    private String subjectPrefix = "[ChatBI AI告警]";

    /**
     * 同类告警最小发送间隔（秒），用于抑制告警风暴。
     */
    private int notifyMinIntervalSeconds = 300;
}
