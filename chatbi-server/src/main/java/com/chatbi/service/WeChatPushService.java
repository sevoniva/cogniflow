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
 * 企业微信推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatPushService {

    private final RestTemplate restTemplate;

    /**
     * 发送企业微信消息
     * @param webhook 企业微信机器人 webhook
     * @param title 消息标题
     * @param content 消息内容
     */
    public void push(String webhook, String title, String content) {
        if (!StringUtils.hasText(webhook)) {
            throw new IllegalArgumentException("企业微信 webhook 不能为空");
        }

        String text = "【" + (StringUtils.hasText(title) ? title : "ChatBI 订阅推送") + "】\n" + content;
        Map<String, Object> contentBody = new LinkedHashMap<>();
        contentBody.put("content", text);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "text");
        payload.put("text", contentBody);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhook, request, String.class);
            HttpStatusCode statusCode = response.getStatusCode();
            if (!statusCode.is2xxSuccessful()) {
                throw new IllegalStateException("企业微信推送失败，状态码: " + statusCode.value());
            }
            log.info("企业微信推送成功：{} - {}", webhook, title);
        } catch (RestClientException ex) {
            throw new IllegalStateException("企业微信推送失败: " + ex.getMessage(), ex);
        }
    }
}
