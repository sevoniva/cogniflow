package com.chatbi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DingTalkPushServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void shouldPushMarkdownMessage() {
        String webhook = "https://oapi.dingtalk.com/robot/send?access_token=test";
        when(restTemplate.postForEntity(eq(webhook), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("ok"));
        DingTalkPushService service = new DingTalkPushService(restTemplate);

        service.push(webhook, "日报", "今日指标更新完成");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhook), requestCaptor.capture(), eq(String.class));
        HttpEntity request = requestCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) request.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> markdown = (Map<String, Object>) body.get("markdown");
        org.junit.jupiter.api.Assertions.assertEquals("markdown", body.get("msgtype"));
        org.junit.jupiter.api.Assertions.assertEquals("日报", markdown.get("title"));
    }

    @Test
    void shouldThrowWhenStatusNot2xx() {
        String webhook = "https://oapi.dingtalk.com/robot/send?access_token=test";
        when(restTemplate.postForEntity(eq(webhook), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("err", HttpStatus.BAD_GATEWAY));
        DingTalkPushService service = new DingTalkPushService(restTemplate);

        assertThrows(IllegalStateException.class, () -> service.push(webhook, "日报", "内容"));
    }
}
