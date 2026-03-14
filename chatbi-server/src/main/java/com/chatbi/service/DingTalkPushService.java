package com.chatbi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 钉钉推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkPushService {

    private final RestTemplate restTemplate;

    /**
     * 发送钉钉消息
     * @param webhook 钉钉机器人 webhook
     * @param title 消息标题
     * @param content 消息内容
     */
    public void push(String webhook, String title, String content) {
        if (!StringUtils.hasText(webhook)) {
            throw new IllegalArgumentException("钉钉 webhook 不能为空");
        }

        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("title", StringUtils.hasText(title) ? title : "ChatBI 订阅推送");
        markdown.put("text", "### " + (StringUtils.hasText(title) ? title : "ChatBI 订阅推送") + "\n\n" + content);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhook, request, String.class);
            HttpStatusCode statusCode = response.getStatusCode();
            if (!statusCode.is2xxSuccessful()) {
                throw new IllegalStateException("钉钉推送失败，状态码: " + statusCode.value());
            }
            log.info("钉钉推送成功：{} - {}", webhook, title);
        } catch (RestClientException ex) {
            throw new IllegalStateException("钉钉推送失败: " + ex.getMessage(), ex);
        }
    }
}
