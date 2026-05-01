package com.chatbi.service;

import com.chatbi.websocket.QueryProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 查询进度服务
 *
 * 封装查询各阶段的进度推送，解耦业务逻辑与 WebSocket。
 * Month 4 Week 1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProgressService {

    private final QueryProgressWebSocketHandler webSocketHandler;

    public void analyzing(String query) {
        broadcast("ANALYZING", "正在分析您的问题: " + query, 10);
    }

    public void generatingSql() {
        broadcast("GENERATING_SQL", "正在生成 SQL 查询语句...", 30);
    }

    public void executing() {
        broadcast("EXECUTING", "正在执行数据库查询...", 60);
    }

    public void buildingResult() {
        broadcast("BUILDING_RESULT", "正在构建结果与图表...", 85);
    }

    public void done() {
        broadcast("DONE", "查询完成", 100);
    }

    public void error(String reason) {
        broadcast("ERROR", "查询失败: " + reason, 0);
    }

    private void broadcast(String stage, String message, int percent) {
        try {
            webSocketHandler.broadcastProgress(stage, message, percent);
        } catch (Exception e) {
            log.debug("进度推送失败（WebSocket 可能未连接）- stage: {}", stage);
        }
    }
}
