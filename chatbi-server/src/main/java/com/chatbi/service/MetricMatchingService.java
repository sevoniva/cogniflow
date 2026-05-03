package com.chatbi.service;

import com.chatbi.entity.Metric;
import com.chatbi.entity.Synonym;
import com.chatbi.repository.MetricMapper;
import com.chatbi.repository.SynonymMapper;
import com.chatbi.support.MetricSemanticMatcher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 指标语义匹配服务
 * 从 QueryController / ConversationController 中提取的公共匹配逻辑
 */
@Service
@RequiredArgsConstructor
public class MetricMatchingService {

    private final MetricMapper metricMapper;
    private final SynonymMapper synonymMapper;

    public static final Map<String, List<String>> METRIC_KEYWORDS = createMetricKeywords();
    public static final int AMBIGUITY_SCORE_DELTA_THRESHOLD = 20;
    public static final int AMBIGUITY_SECOND_SCORE_THRESHOLD = 100;
    public static final double AMBIGUITY_SIMILARITY_THRESHOLD = 0.88;
    public static final double AMBIGUITY_SIMILARITY_DELTA_THRESHOLD = 0.03;
    public static final double FUZZY_MATCH_THRESHOLD = 0.87;
    public static final double TYPO_FUZZY_MATCH_THRESHOLD = 0.66;
    public static final double TYPO_FUZZY_GAP_THRESHOLD = 0.10;

    // ─── 数据加载 ───

    public List<Metric> getActiveMetrics() {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getStatus, "active");
        return metricMapper.selectList(wrapper);
    }

    public List<Synonym> getAllSynonyms() {
        return synonymMapper.selectList(null);
    }

    // ─── 核心匹配 ───

    public MetricMatchAnalysis analyzeMetricMatch(String query, List<Metric> metrics, List<Synonym> synonyms) {
        if (metrics == null || metrics.isEmpty()) {
            return new MetricMatchAnalysis(null, List.of(), false);
        }
        List<Metric> uniqueMetrics = deduplicateMetrics(metrics);
        Map<String, MetricScore> scoreMap = buildMetricScores(query, metrics, synonyms);
        List<MetricRanking> ranking = uniqueMetrics.stream()
            .map(metric -> {
                MetricScore score = scoreMap.get(metric.getName());
                if (score == null) {
                    return new MetricRanking(metric, 0, 0, 0);
                }
                return new MetricRanking(metric, score.score, score.similarity, score.directHits);
            })
            .sorted(Comparator
                .comparingInt(MetricRanking::score).reversed()
                .thenComparing(Comparator.comparingDouble(MetricRanking::similarity).reversed())
                .thenComparing(item -> item.metric().getName()))
            .toList();

        List<String> candidateMetrics = ranking.stream()
            .filter(item -> item.score() > 0 || item.similarity() >= AMBIGUITY_SIMILARITY_THRESHOLD)
            .map(item -> item.metric().getName())
            .limit(4)
            .toList();

        boolean ambiguous = isAmbiguousMetricMatch(ranking);
        Metric matched = ambiguous ? null : matchMetric(query, metrics, synonyms);
        return new MetricMatchAnalysis(matched, candidateMetrics, ambiguous);
    }

    public Metric matchMetric(String query, List<Metric> metrics, List<Synonym> synonyms) {
        for (Metric m : metrics) {
            if (MetricSemanticMatcher.containsTerm(query, m.getName())) {
                return m;
            }
        }
        for (Synonym synonym : synonyms) {
            if (synonym.getAliases() == null) continue;
            for (String alias : synonym.getAliases()) {
                if (MetricSemanticMatcher.containsTerm(query, alias)) {
                    Metric mapped = findMetricByName(metrics, synonym.getStandardWord());
                    if (mapped != null) return mapped;
                }
            }
        }
        return resolveByFuzzySimilarity(query, metrics, synonyms);
    }

    public Map<String, MetricScore> buildMetricScores(String query, List<Metric> metrics, List<Synonym> synonyms) {
        Map<String, MetricScore> scores = new LinkedHashMap<>();
        for (Metric metric : metrics) {
            MetricScore score = new MetricScore();
            if (MetricSemanticMatcher.containsTerm(query, metric.getName())) {
                score.score += 100;
                score.directHits += 1;
            }
            double fuzzy = MetricSemanticMatcher.similarity(query, metric.getName());
            score.similarity = Math.max(score.similarity, fuzzy);
            if (fuzzy >= 0.82) {
                score.score += (int) Math.round(fuzzy * 20);
            } else if (fuzzy >= TYPO_FUZZY_MATCH_THRESHOLD && hasStrongMetricIntent(query, metric)) {
                score.score += (int) Math.round(fuzzy * 12);
            }
            if (metric.getDefinition() != null) {
                String[] queryTokens = query.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
                for (String token : queryTokens) {
                    if (token.length() >= 2 && metric.getDefinition().contains(token)) {
                        score.score += 20;
                    }
                }
            }
            scores.put(metric.getName(), score);
        }

        for (Synonym synonym : synonyms) {
            MetricScore score = scores.get(synonym.getStandardWord());
            if (score == null || synonym.getAliases() == null || synonym.getAliases().isEmpty()) continue;
            for (String alias : synonym.getAliases()) {
                if (MetricSemanticMatcher.containsTerm(query, alias)) {
                    score.score += 80;
                    score.directHits += 1;
                } else {
                    double fuzzy = MetricSemanticMatcher.similarity(query, alias);
                    if (fuzzy >= 0.87) {
                        score.score += (int) Math.round(fuzzy * 40);
                    } else if (fuzzy >= TYPO_FUZZY_MATCH_THRESHOLD && hasStrongMetricIntent(query, findMetricByName(metrics, synonym.getStandardWord()))) {
                        score.score += (int) Math.round(fuzzy * 20);
                    }
                }
                score.similarity = Math.max(score.similarity, MetricSemanticMatcher.similarity(query, alias));
            }
        }
        return scores;
    }

    public boolean isAmbiguousMetricMatch(List<MetricRanking> ranking) {
        List<MetricRanking> effectiveCandidates = ranking.stream()
            .filter(item -> item.score() > 0 || item.similarity() >= AMBIGUITY_SIMILARITY_THRESHOLD)
            .limit(3)
            .toList();
        if (effectiveCandidates.size() < 2) return false;

        MetricRanking top = effectiveCandidates.get(0);
        MetricRanking second = effectiveCandidates.get(1);
        if (top.directHits() > 0 && second.directHits() > 0) return true;
        if (second.score() >= AMBIGUITY_SECOND_SCORE_THRESHOLD
            && (top.score() - second.score()) <= AMBIGUITY_SCORE_DELTA_THRESHOLD) return true;
        return top.similarity() >= 0.9
            && second.similarity() >= AMBIGUITY_SIMILARITY_THRESHOLD
            && (top.similarity() - second.similarity()) <= AMBIGUITY_SIMILARITY_DELTA_THRESHOLD;
    }

    public Metric resolveByFuzzySimilarity(String query, List<Metric> metrics, List<Synonym> synonyms) {
        Metric bestMetric = null;
        double bestScore = 0;
        double secondBestScore = 0;

        for (Metric metric : metrics) {
            double score = MetricSemanticMatcher.similarity(query, metric.getName());
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                bestMetric = metric;
            } else if (score > secondBestScore) {
                secondBestScore = score;
            }
        }

        for (Synonym synonym : synonyms) {
            if (synonym.getAliases() == null || synonym.getAliases().isEmpty()) continue;
            Metric mapped = findMetricByName(metrics, synonym.getStandardWord());
            if (mapped == null) continue;
            for (String alias : synonym.getAliases()) {
                double score = MetricSemanticMatcher.similarity(query, alias);
                if (score > bestScore) {
                    secondBestScore = bestScore;
                    bestScore = score;
                    bestMetric = mapped;
                } else if (mapped != bestMetric && score > secondBestScore) {
                    secondBestScore = score;
                }
            }
        }

        if (bestScore >= FUZZY_MATCH_THRESHOLD) return bestMetric;
        if (bestScore >= TYPO_FUZZY_MATCH_THRESHOLD
            && (bestScore - secondBestScore) >= TYPO_FUZZY_GAP_THRESHOLD
            && hasStrongMetricIntent(query, bestMetric)) return bestMetric;
        return null;
    }

    public boolean hasStrongMetricIntent(String query, Metric metric) {
        if (metric == null) return false;
        List<String> actionHints = List.of(
            "多少", "趋势", "对比", "比较", "分析", "变化", "占比", "同比", "环比",
            "本月", "上月", "本周", "今年", "去年", "按", "排名",
            "看", "看看", "看下", "看一下", "帮我看", "瞅瞅", "瞅下",
            "情况", "如何", "怎么样", "咋样", "咋回事", "行不行", "盘下"
        );
        boolean actionHit = actionHints.stream().anyMatch(item -> MetricSemanticMatcher.containsTerm(query, item));
        boolean metricNameHit = metric.getName() != null && MetricSemanticMatcher.containsTerm(query, metric.getName());
        return actionHit || metricNameHit;
    }

    public Metric findMetricByName(List<Metric> metrics, String name) {
        return metrics.stream()
            .filter(m -> m.getName().equals(name))
            .findFirst().orElse(null);
    }

    public List<Metric> deduplicateMetrics(List<Metric> metrics) {
        Map<String, Metric> unique = new LinkedHashMap<>();
        for (Metric m : metrics) {
            if (m == null || m.getName() == null || m.getName().isBlank()) continue;
            unique.putIfAbsent(m.getName(), m);
        }
        return new ArrayList<>(unique.values());
    }

    public List<String> recommendMetricNames(String query, List<Metric> metrics, List<Synonym> synonyms) {
        if (metrics == null || metrics.isEmpty()) return List.of();
        List<Metric> uniqueMetrics = deduplicateMetrics(metrics);
        Map<String, MetricScore> scores = buildMetricScores(query, metrics, synonyms);
        return uniqueMetrics.stream()
            .map(m -> Map.entry(m.getName(), scores.get(m.getName())))
            .filter(e -> e.getValue() != null && e.getValue().score > 0)
            .sorted((a, b) -> {
                int cmp = Integer.compare(b.getValue().score, a.getValue().score);
                return cmp != 0 ? cmp : Double.compare(b.getValue().similarity, a.getValue().similarity);
            })
            .map(Map.Entry::getKey)
            .limit(4)
            .toList();
    }

    // ─── 意图识别 ───

    public boolean isGreetingIntent(String query) {
        return containsAny(query, List.of("你好", "您好", "hi", "hello", "在吗"));
    }

    public boolean isOverviewIntent(String query) {
        return containsAny(query, List.of(
            "数据概览", "数据情况", "业务情况", "整体情况", "总体情况",
            "整体分析", "现状", "概况", "总览", "最近怎么样",
            "有没有风险", "风险点", "分析一下", "帮我分析",
            "帮我看下", "帮我看看", "总结一下", "总结下", "情况怎么样"
        ));
    }

    public boolean containsAny(String query, List<String> keywords) {
        return keywords.stream().anyMatch(item -> MetricSemanticMatcher.containsTerm(query, item));
    }

    // ─── 建议生成 ───

    public List<String> buildGuidedSuggestions(String query, List<String> candidateMetrics) {
        if (isGreetingIntent(query) || isOverviewIntent(query)) {
            return List.of("先给我一个数据概览");
        }
        if (candidateMetrics == null || candidateMetrics.isEmpty()) {
            return inferFallbackMetrics(query).stream()
                .map(m -> buildMetricSuggestion(m, query))
                .distinct().limit(4).toList();
        }
        return candidateMetrics.stream()
            .map(m -> buildMetricSuggestion(m, query))
            .distinct().limit(4).toList();
    }

    public List<String> buildDisambiguationSuggestions(String query, List<String> candidateMetrics) {
        if (candidateMetrics == null || candidateMetrics.isEmpty()) {
            return buildGuidedSuggestions(query, List.of());
        }
        return candidateMetrics.stream()
            .map(m -> buildMetricSuggestion(m, query))
            .distinct().limit(4).toList();
    }

    public String buildMetricSuggestion(String metric, String query) {
        String timePrefix = inferTimePrefix(query, metric);
        if (containsAny(query, List.of("趋势", "变化", "走势"))) return timePrefix + metric + "趋势如何？";
        if (containsAny(query, List.of("对比", "比较", "排名", "按"))) return timePrefix + metric + "按区域对比";
        if (containsAny(query, List.of("占比", "构成", "结构"))) return timePrefix + metric + "占比如何？";
        return timePrefix + metric + "是多少？";
    }

    public String inferTimePrefix(String query, String metric) {
        if (containsAny(query, List.of("今日", "今天"))) return "今日";
        if (containsAny(query, List.of("昨日", "昨天"))) return "昨日";
        if (containsAny(query, List.of("本周", "这周", "周内"))) return "本周";
        if (containsAny(query, List.of("上周"))) return "上周";
        if (containsAny(query, List.of("本季度", "本季", "本q"))) return "本季度";
        if (containsAny(query, List.of("上季度", "上季"))) return "上季度";
        if (containsAny(query, List.of("本年", "今年", "年度"))) return "今年";
        if (containsAny(query, List.of("去年"))) return "去年";
        if (containsAny(query, List.of("上月"))) return "上月";
        if (containsAny(query, List.of("本月", "当月"))) return "本月";
        return "本月";
    }

    public List<String> inferFallbackMetrics(String query) {
        List<Metric> activeMetrics = getActiveMetrics();
        if (activeMetrics == null || activeMetrics.isEmpty()) {
            return List.of();
        }
        return activeMetrics.stream()
            .map(Metric::getName)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .limit(3)
            .toList();
    }

    public String detectGuidanceScenario(String query, List<String> candidateMetrics) {
        return "综合分析场景";
    }

    public List<String> buildIntentTags(String query) {
        if (query == null || query.isBlank()) return List.of();
        List<String> tags = new ArrayList<>();
        if (containsAny(query, List.of("看", "看下", "看一下", "看看", "帮我看", "帮我看看", "瞅瞅", "瞅下"))) tags.add("口语查看");
        if (containsAny(query, List.of("咋样", "咋回事", "行不行"))) tags.add("口语追问");
        if (containsAny(query, List.of("趋势", "走势", "变化"))) tags.add("趋势分析");
        if (containsAny(query, List.of("对比", "比较", "差异"))) tags.add("对比分析");
        if (containsAny(query, List.of("占比", "构成", "结构"))) tags.add("占比结构");
        if (containsAny(query, List.of("本月", "上月", "本周", "上周", "本季度", "上季度", "今年", "去年", "今日", "昨天"))) tags.add("时间范围");
        if (containsAny(query, List.of("排名", "Top", "top", "最高", "最低"))) tags.add("排序诉求");
        return tags;
    }

    public List<String> buildMetricExamples(String metricName) {
        return List.of("本月" + metricName + "是多少？", metricName + "趋势如何？");
    }

    // ─── 记录类型 ───

    public record MetricMatchAnalysis(Metric matchedMetric, List<String> candidateMetrics, boolean ambiguous) {}

    public record MetricRanking(Metric metric, int score, double similarity, int directHits) {}

    public static class MetricScore {
        public int score;
        public double similarity;
        public int directHits;
    }

    // ─── 内部 ───

    private static Map<String, List<String>> createMetricKeywords() {
        return new LinkedHashMap<>();
    }
}
