package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.Dashboard;
import com.chatbi.entity.EmbeddedAccessLog;
import com.chatbi.entity.Share;
import com.chatbi.repository.DashboardMapper;
import com.chatbi.repository.EmbeddedAccessLogMapper;
import com.chatbi.repository.ShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 嵌入式分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddedAnalyticsService {

    private final ShareMapper shareMapper;
    private final DashboardMapper dashboardMapper;
    private final EmbeddedAccessLogMapper embeddedAccessLogMapper;

    /**
     * 创建嵌入配置
     */
    @Transactional
    public Share createEmbed(EmbedConfig config) {
        // 验证仪表板是否存在
        Dashboard dashboard = dashboardMapper.selectById(config.getDashboardId());
        if (dashboard == null) {
            throw new IllegalArgumentException("仪表板不存在：" + config.getDashboardId());
        }

        // 生成嵌入码
        String embedToken = generateEmbedToken(config);

        // 创建分享记录
        Share share = Share.builder()
                .title(config.getTitle())
                .type("DASHBOARD")
                .resourceId(config.getDashboardId())
                .shareToken(embedToken)
                .expireTime(config.getExpireTime())
                .status(1)
                .createdAt(LocalDateTime.now())
                .build();

        shareMapper.insert(share);
        log.info("创建嵌入配置：{}", config.getTitle());

        return share;
    }

    /**
     * 获取嵌入配置
     */
    public Share getEmbed(String embedToken) {
        var wrapper = new LambdaQueryWrapper<Share>()
                .eq(Share::getShareToken, embedToken);
        Share share = shareMapper.selectOne(wrapper);

        if (share == null) {
            throw new IllegalArgumentException("嵌入配置不存在");
        }

        // 检查是否过期
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("嵌入配置已过期");
        }

        // 检查状态
        if (share.getStatus() != 1) {
            throw new IllegalArgumentException("嵌入配置已禁用");
        }

        if (share.getMaxVisits() != null && share.getCurrentVisits() != null
            && share.getCurrentVisits() >= share.getMaxVisits()) {
            throw new IllegalArgumentException("嵌入配置访问次数已用尽");
        }

        return share;
    }

    /**
     * 验证嵌入访问
     */
    public boolean validateEmbed(String embedToken, String origin) {
        getEmbed(embedToken);
        return true;
    }

    /**
     * 获取嵌入仪表板数据
     */
    @Transactional
    public Map<String, Object> getEmbedDashboardData(String embedToken, String origin, String ipAddress, String userAgent) {
        Share share = getEmbed(embedToken);
        recordAccess(share, origin, ipAddress, userAgent);

        Map<String, Object> result = new HashMap<>();
        result.put("shareId", share.getId());
        result.put("title", share.getTitle());
        result.put("dashboardId", share.getResourceId());

        // 获取仪表板配置
        Dashboard dashboard = dashboardMapper.selectById(share.getResourceId());
        if (dashboard != null) {
            result.put("dashboardName", dashboard.getName());
            result.put("chartsConfig", dashboard.getChartsConfig());
            result.put("layoutConfig", dashboard.getLayoutConfig());
        }

        return result;
    }

    public EmbedStats getEmbedStats(String embedToken) {
        Share share = getEmbed(embedToken);
        List<EmbeddedAccessLog> logs = embeddedAccessLogMapper.selectList(new LambdaQueryWrapper<EmbeddedAccessLog>()
            .eq(EmbeddedAccessLog::getShareId, share.getId())
            .orderByDesc(EmbeddedAccessLog::getCreatedAt));

        EmbedStats stats = new EmbedStats();
        stats.setViewCount(logs.isEmpty() ? Long.valueOf(share.getCurrentVisits() == null ? 0 : share.getCurrentVisits()) : (long) logs.size());
        stats.setUniqueVisitors(logs.stream()
            .map(EmbeddedAccessLog::getVisitorKey)
            .filter(Objects::nonNull)
            .filter(visitor -> !visitor.isBlank())
            .distinct()
            .count());
        stats.setViewHistory(logs.stream()
            .limit(20)
            .map(log -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("time", log.getCreatedAt());
                item.put("origin", log.getOrigin());
                item.put("ipAddress", log.getIpAddress());
                item.put("visitor", maskVisitor(log.getVisitorKey(), log.getIpAddress()));
                return item;
            })
            .toList());
        return stats;
    }

    /**
     * 更新嵌入配置
     */
    @Transactional
    public Share updateEmbed(Long shareId, EmbedConfig config) {
        Share share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new IllegalArgumentException("嵌入配置不存在：" + shareId);
        }

        share.setTitle(config.getTitle());
        share.setExpireTime(config.getExpireTime());

        shareMapper.updateById(share);
        return share;
    }

    /**
     * 删除嵌入配置
     */
    @Transactional
    public void deleteEmbed(Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share != null) {
            share.setStatus(0);
            shareMapper.updateById(share);
            log.info("删除嵌入配置：{}", shareId);
        }
    }

    private void recordAccess(Share share, String origin, String ipAddress, String userAgent) {
        EmbeddedAccessLog logEntry = EmbeddedAccessLog.builder()
            .shareId(share.getId())
            .shareToken(share.getShareToken())
            .origin(origin)
            .ipAddress(ipAddress)
            .userAgent(truncate(userAgent, 500))
            .visitorKey(buildVisitorKey(origin, ipAddress, userAgent))
            .build();
        embeddedAccessLogMapper.insert(logEntry);

        share.setCurrentVisits((share.getCurrentVisits() == null ? 0 : share.getCurrentVisits()) + 1);
        shareMapper.updateById(share);
    }

    private String buildVisitorKey(String origin, String ipAddress, String userAgent) {
        StringBuilder builder = new StringBuilder();
        if (origin != null && !origin.isBlank()) {
            builder.append(origin.trim());
        }
        builder.append("|");
        if (ipAddress != null && !ipAddress.isBlank()) {
            builder.append(ipAddress.trim());
        }
        builder.append("|");
        if (userAgent != null && !userAgent.isBlank()) {
            builder.append(userAgent.trim());
        }
        return truncate(builder.toString(), 255);
    }

    private String maskVisitor(String visitorKey, String ipAddress) {
        if (visitorKey != null && !visitorKey.isBlank()) {
            return truncate(visitorKey, 80);
        }
        return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * 生成嵌入 Token
     */
    private String generateEmbedToken(EmbedConfig config) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        return "emb_" + uuid + "_" + timestamp;
    }

    /**
     * 解析额外配置
     */
    private Map<String, Object> parseExtraConfig(String extraConfig) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(extraConfig, Map.class);
    }

    /**
     * 序列化额外配置
     */
    private String serializeExtraConfig(Map<String, Object> config) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(config);
        } catch (Exception e) {
            log.error("序列化配置失败：{}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 嵌入配置
     */
    public static class EmbedConfig {
        private Long dashboardId;
        private String title;
        private LocalDateTime expireTime;
        private Map<String, Object> extraConfig;

        public Long getDashboardId() { return dashboardId; }
        public void setDashboardId(Long dashboardId) { this.dashboardId = dashboardId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public LocalDateTime getExpireTime() { return expireTime; }
        public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
        public Map<String, Object> getExtraConfig() { return extraConfig; }
        public void setExtraConfig(Map<String, Object> extraConfig) { this.extraConfig = extraConfig; }
    }

    /**
     * 嵌入访问统计
     */
    public static class EmbedStats {
        private Long viewCount;
        private Long uniqueVisitors;
        private List<Map<String, Object>> viewHistory;

        public Long getViewCount() { return viewCount; }
        public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
        public Long getUniqueVisitors() { return uniqueVisitors; }
        public void setUniqueVisitors(Long uniqueVisitors) { this.uniqueVisitors = uniqueVisitors; }
        public List<Map<String, Object>> getViewHistory() { return viewHistory; }
        public void setViewHistory(List<Map<String, Object>> viewHistory) { this.viewHistory = viewHistory; }
    }
}
