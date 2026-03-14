package com.chatbi.support;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 指标语义匹配工具。
 * 目标：统一“空格/标点噪声清理 + 包含匹配 + 轻量模糊匹配”。
 */
public final class MetricSemanticMatcher {

    private static final Map<String, String> BUSINESS_TYPO_CORRECTIONS = buildTypoCorrections();

    private MetricSemanticMatcher() {
    }

    public static String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text
            .toLowerCase(Locale.ROOT)
            .replaceAll("[\\s\\p{Punct}，。！？；：、“”‘’（）()【】\\[\\]《》<>·~`!@#$%^&*_+=|\\\\/-]+", "");
        return applyTypoCorrections(normalized);
    }

    public static boolean containsTerm(String text, String term) {
        String normalizedText = normalize(text);
        String normalizedTerm = normalize(term);
        if (normalizedText.isBlank() || normalizedTerm.isBlank()) {
            return false;
        }
        if (normalizedText.contains(normalizedTerm)) {
            return true;
        }
        return fuzzyContains(normalizedText, normalizedTerm);
    }

    /**
     * 计算 text 与 term 的相似度（0-1）。
     * 规则：
     * 1) 归一化后包含 => 1.0
     * 2) 子序列命中 => 0.9
     * 3) 滑窗编辑距离相似度 => [0,1]
     */
    public static double similarity(String text, String term) {
        String normalizedText = normalize(text);
        String normalizedTerm = normalize(term);
        if (normalizedText.isBlank() || normalizedTerm.isBlank()) {
            return 0;
        }
        if (normalizedText.contains(normalizedTerm)) {
            return 1.0;
        }
        if (isSubsequence(normalizedTerm, normalizedText)) {
            return 0.9;
        }
        return bestWindowSimilarity(normalizedText, normalizedTerm);
    }

    private static double bestWindowSimilarity(String text, String term) {
        int termLength = term.length();
        if (termLength == 0 || text.isEmpty()) {
            return 0;
        }
        int minWindowLength = Math.max(1, termLength - 1);
        int maxWindowLength = Math.min(text.length(), termLength + 2);
        double best = levenshteinSimilarity(text, term);

        for (int start = 0; start < text.length(); start++) {
            for (int len = minWindowLength; len <= maxWindowLength; len++) {
                int end = start + len;
                if (end > text.length()) {
                    break;
                }
                String candidate = text.substring(start, end);
                best = Math.max(best, levenshteinSimilarity(candidate, term));
                if (best >= 0.98) {
                    return best;
                }
            }
        }
        return best;
    }

    /**
     * 近似包含匹配：
     * 对中文业务指标允许 1 个字符级错别字容错，降低“销售额/销受额”类问法的识别失败率。
     */
    private static boolean fuzzyContains(String text, String term) {
        if (term.length() < 3 || !containsChinese(term)) {
            return false;
        }
        int minWindowLength = Math.max(1, term.length() - 1);
        int maxWindowLength = Math.min(text.length(), term.length() + 1);
        for (int start = 0; start < text.length(); start++) {
            for (int len = minWindowLength; len <= maxWindowLength; len++) {
                int end = start + len;
                if (end > text.length()) {
                    break;
                }
                String candidate = text.substring(start, end);
                int distance = levenshteinDistance(candidate, term);
                if (distance <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                return true;
            }
        }
        return false;
    }

    private static String applyTypoCorrections(String text) {
        if (text.isBlank()) {
            return text;
        }
        String corrected = text;
        for (Map.Entry<String, String> entry : BUSINESS_TYPO_CORRECTIONS.entrySet()) {
            corrected = corrected.replace(entry.getKey(), entry.getValue());
        }
        return corrected;
    }

    private static Map<String, String> buildTypoCorrections() {
        Map<String, String> corrections = new LinkedHashMap<>();
        corrections.put("销受额", "销售额");
        corrections.put("销瘦额", "销售额");
        corrections.put("消售额", "销售额");
        corrections.put("营页额", "营业额");
        corrections.put("毛利律", "毛利率");
        corrections.put("毛力率", "毛利率");
        corrections.put("回宽额", "回款额");
        corrections.put("回筐额", "回款额");
        corrections.put("库寸周转", "库存周转");
        corrections.put("履约绿", "履约率");
        corrections.put("revennue", "revenue");
        corrections.put("reveune", "revenue");
        corrections.put("margn", "margin");
        corrections.put("inventroy", "inventory");
        corrections.put("fullfilment", "fulfillment");
        return corrections;
    }

    private static boolean isSubsequence(String target, String text) {
        if (target.length() > text.length()) {
            return false;
        }
        int i = 0;
        int j = 0;
        while (i < target.length() && j < text.length()) {
            if (target.charAt(i) == text.charAt(j)) {
                i++;
            }
            j++;
        }
        return i == target.length();
    }

    private static double levenshteinSimilarity(String left, String right) {
        int max = Math.max(left.length(), right.length());
        if (max == 0) {
            return 1.0;
        }
        int distance = levenshteinDistance(left, right);
        return Math.max(0, 1.0 - ((double) distance / max));
    }

    private static int levenshteinDistance(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];

        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[left.length()][right.length()];
    }
}
