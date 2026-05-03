package com.chatbi.service;

import com.chatbi.entity.QueryHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能推荐服务
 * 基于用户行为和数据特征推荐查询
 */
@Slf4j
@Service
public class SmartRecommendationService {

    private final QueryHistoryService queryHistoryService;
    private final Map<String, Map<String, Double>> localQueryScores = new ConcurrentHashMap<>();

    private static final String RECOMMENDATION_PREFIX = "chatbi:recommendation:";

    private StringRedisTemplate redisTemplate;
    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled = false;

    @Autowired
    public SmartRecommendationService(QueryHistoryService queryHistoryService) {
        this.queryHistoryService = queryHistoryService;
    }

    SmartRecommendationService(StringRedisTemplate redisTemplate, QueryHistoryService queryHistoryService) {
        this.queryHistoryService = queryHistoryService;
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取推荐查询（基于数据源）
     */
    public List<String> getRecommendationsByDataSource(Long dataSourceId) {
        List<String> recommendations = new ArrayList<>();

        // 1. 基于表结构的推荐
        recommendations.addAll(getTableBasedRecommendations(dataSourceId));

        // 2. 基于热门查询的推荐
        recommendations.addAll(getPopularQueries(dataSourceId));

        // 3. 去重并限制数量
        return recommendations.stream()
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取个性化推荐（基于用户历史）
     */
    public List<String> getPersonalizedRecommendations(Long userId) {
        List<String> recommendations = new ArrayList<>();

        try {
            // 1. 获取用户最近的查询历史
            List<QueryHistory> recentQueries = queryHistoryService.getRecent(userId, 20);

            if (recentQueries.isEmpty()) {
                // 新用户，返回通用推荐
                return getDefaultRecommendations();
            }

            // 2. 分析用户查询模式
            Map<String, Integer> queryPatterns = analyzeQueryPatterns(recentQueries);

            // 3. 基于查询模式生成推荐
            recommendations.addAll(generateRecommendationsFromPatterns(queryPatterns));

            // 4. 基于协同过滤的推荐
            recommendations.addAll(getCollaborativeRecommendations(userId));

            // 5. 去重并限制数量
            return recommendations.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取个性化推荐失败 - userId: {}", userId, e);
            return getDefaultRecommendations();
        }
    }

    /**
     * 获取下一步推荐（基于当前查询）
     */
    public List<String> getNextStepRecommendations(String currentQuery, List<Map<String, Object>> queryResult) {
        List<String> recommendations = new ArrayList<>();

        // 1. 基于查询内容的推荐
        if (currentQuery.contains("核心指标") || currentQuery.contains("金额")) {
            recommendations.add("和上月对比如何？");
            recommendations.add("哪个地区贡献最大？");
            recommendations.add("增长趋势如何？");
            recommendations.add("排名前5的是哪些？");
        } else if (currentQuery.contains("数量") || currentQuery.contains("个数")) {
            recommendations.add("占比是多少？");
            recommendations.add("同比增长多少？");
            recommendations.add("哪个类别最多？");
        } else if (currentQuery.contains("趋势")) {
            recommendations.add("预测下月数据");
            recommendations.add("哪个时间段最高？");
            recommendations.add("波动原因是什么？");
        } else if (currentQuery.contains("对比") || currentQuery.contains("比较")) {
            recommendations.add("详细数据是什么？");
            recommendations.add("差异原因是什么？");
            recommendations.add("如何改进？");
        } else {
            // 通用推荐
            recommendations.add("详细数据是什么？");
            recommendations.add("有什么异常吗？");
            recommendations.add("给出分析建议");
        }

        // 2. 基于查询结果的推荐
        if (queryResult != null && !queryResult.isEmpty()) {
            // 如果结果只有一条，推荐查看详细数据
            if (queryResult.size() == 1) {
                recommendations.add("查看详细明细数据");
            }
            // 如果结果很多，推荐筛选
            else if (queryResult.size() > 20) {
                recommendations.add("筛选前10名");
                recommendations.add("按条件过滤");
            }
        }

        return recommendations.stream()
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * 获取相似查询推荐
     */
    public List<String> getSimilarQueries(String query) {
        List<String> similar = new ArrayList<>();

        // 维度变化优先返回，避免在数量限制内被其他推荐挤掉。
        if (query.contains("地区")) {
            similar.add(query.replace("地区", "产品"));
            similar.add(query.replace("地区", "客户"));
            similar.add(query.replace("地区", "渠道"));
        }

        if (query.contains("核心指标")) {
            similar.add(query.replace("核心指标", "订单数"));
            similar.add(query.replace("核心指标", "客单价"));
            similar.add(query.replace("核心指标", "利润"));
        }

        if (query.contains("本月")) {
            similar.add(query.replace("本月", "上月"));
            similar.add(query.replace("本月", "本季度"));
            similar.add(query.replace("本月", "今年"));
        }

        return similar.stream()
            .distinct()
            .limit(6)
            .collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 基于表结构的推荐
     */
    private List<String> getTableBasedRecommendations(Long dataSourceId) {
        List<String> recommendations = new ArrayList<>();

        // 这里应该分析表结构，生成推荐
        // 简化实现，返回通用推荐
        recommendations.add("本月核心指标是多少？");
        recommendations.add("哪个维度数据最高？");
        recommendations.add("核心指标趋势如何？");

        return recommendations;
    }

    /**
     * 获取热门查询
     */
    private List<String> getPopularQueries(Long dataSourceId) {
        List<String> popular = new ArrayList<>();
        String key = RECOMMENDATION_PREFIX + "popular:" + dataSourceId;

        try {
            if (redisEnabled && redisTemplate != null) {
                Set<String> queries = redisTemplate.opsForZSet().reverseRange(key, 0, 9);
                if (queries != null) {
                    popular.addAll(queries);
                    return popular;
                }
            }
        } catch (Exception e) {
            log.warn("Redis 热门查询读取失败，已降级为本地存储", e);
        }

        popular.addAll(getTopLocalQueries(key, 10));
        return popular;
    }

    /**
     * 分析用户查询模式
     */
    private Map<String, Integer> analyzeQueryPatterns(List<QueryHistory> queries) {
        Map<String, Integer> patterns = new HashMap<>();

        for (QueryHistory query : queries) {
            String content = query.getQueryContent();

            // 提取关键词
            if (content.contains("核心指标")) patterns.merge("核心指标", 1, Integer::sum);
            if (content.contains("地区")) patterns.merge("地区", 1, Integer::sum);
            if (content.contains("产品")) patterns.merge("产品", 1, Integer::sum);
            if (content.contains("趋势")) patterns.merge("趋势", 1, Integer::sum);
            if (content.contains("对比")) patterns.merge("对比", 1, Integer::sum);
            if (content.contains("排名")) patterns.merge("排名", 1, Integer::sum);
        }

        return patterns;
    }

    /**
     * 基于查询模式生成推荐
     */
    private List<String> generateRecommendationsFromPatterns(Map<String, Integer> patterns) {
        List<String> recommendations = new ArrayList<>();

        // 根据用户最常查询的内容生成推荐
        if (patterns.getOrDefault("核心指标", 0) > 3) {
            recommendations.add("本月同比增长多少？");
            recommendations.add("哪个产品核心指标最高？");
        }

        if (patterns.getOrDefault("地区", 0) > 3) {
            recommendations.add("各地区核心指标对比");
            recommendations.add("哪个地区增长最快？");
        }

        if (patterns.getOrDefault("趋势", 0) > 2) {
            recommendations.add("预测下月核心指标");
            recommendations.add("核心指标波动分析");
        }

        return recommendations;
    }

    /**
     * 协同过滤推荐
     */
    private List<String> getCollaborativeRecommendations(Long userId) {
        List<String> recommendations = new ArrayList<>();

        // 简化实现：找到相似用户的查询
        // 实际应该使用协同过滤算法
        recommendations.add("热门查询：本季度核心指标");
        recommendations.add("热门查询：用户流失率");

        return recommendations;
    }

    /**
     * 获取默认推荐
     */
    private List<String> getDefaultRecommendations() {
        return List.of(
            "本月核心指标是多少？",
            "哪个维度数据最高？",
            "核心指标趋势如何？",
            "哪个分类表现最好？",
            "总体数量是多少？",
            "本月数据总量是多少？",
            "平均值是多少？",
            "同比增长多少？"
        );
    }

    /**
     * 记录查询（用于推荐算法）
     */
    public void recordQuery(Long userId, Long dataSourceId, String query) {
        String popularKey = RECOMMENDATION_PREFIX + "popular:" + dataSourceId;
        String userKey = RECOMMENDATION_PREFIX + "user:" + userId;

        try {
            incrementLocalScore(popularKey, query);
            incrementLocalScore(userKey, query);

            if (redisEnabled && redisTemplate != null) {
                redisTemplate.opsForZSet().incrementScore(popularKey, query, 1);
                redisTemplate.opsForZSet().incrementScore(userKey, query, 1);
            }
            log.info("记录查询 - userId: {}, dataSourceId: {}, query: {}", userId, dataSourceId, query);
        } catch (Exception e) {
            log.warn("Redis 推荐打点失败，已降级为本地存储", e);
        }
    }

    private void incrementLocalScore(String key, String query) {
        localQueryScores
            .computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
            .merge(query, 1D, Double::sum);
    }

    private List<String> getTopLocalQueries(String key, int limit) {
        return localQueryScores.getOrDefault(key, Map.of()).entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
}
