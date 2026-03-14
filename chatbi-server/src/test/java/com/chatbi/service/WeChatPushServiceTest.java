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
class WeChatPushServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void shouldPushTextMessage() {
        String webhook = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test";
        when(restTemplate.postForEntity(eq(webhook), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("ok"));
        WeChatPushService service = new WeChatPushService(restTemplate);

        service.push(webhook, "日报", "今日指标更新完成");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhook), requestCaptor.capture(), eq(String.class));
        HttpEntity request = requestCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) request.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) body.get("text");
        org.junit.jupiter.api.Assertions.assertEquals("text", body.get("msgtype"));
        org.junit.jupiter.api.Assertions.assertTrue(String.valueOf(text.get("content")).contains("日报"));
    }

    @Test
    void shouldThrowWhenStatusNot2xx() {
        String webhook = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test";
        when(restTemplate.postForEntity(eq(webhook), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("err", HttpStatus.BAD_GATEWAY));
        WeChatPushService service = new WeChatPushService(restTemplate);

        assertThrows(IllegalStateException.class, () -> service.push(webhook, "日报", "内容"));
    }
}
