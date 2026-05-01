package com.chatbi.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询进度 WebSocket 处理器
 *
 * 推送阶段：ANALYZING → GENERATING_SQL → EXECUTING → BUILDING_RESULT → DONE
 * Month 4 Week 1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryProgressWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket 连接建立 - session: {}, 当前连接数: {}", sessionId, sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket 连接关闭 - session: {}, 当前连接数: {}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可发送查询 ID 进行订阅
        String payload = message.getPayload();
        log.debug("收到 WebSocket 消息 - session: {}, payload: {}", session.getId(), payload);
    }

    /**
     * 向所有连接的客户端推送进度
     */
    public void broadcastProgress(String stage, String message, int percent) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "stage", stage,
                    "message", message,
                    "percent", percent,
                    "timestamp", System.currentTimeMillis()
            ));
            TextMessage textMessage = new TextMessage(json);
            sessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("WebSocket 消息发送失败 - session: {}", session.getId(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("进度广播序列化失败", e);
        }
    }

    /**
     * 向指定 session 推送进度
     */
    public void sendProgress(String sessionId, String stage, String message, int percent) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "stage", stage,
                    "message", message,
                    "percent", percent,
                    "timestamp", System.currentTimeMillis()
            ));
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("WebSocket 单播失败 - session: {}", sessionId, e);
        }
    }
}
