package com.chatbi.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricSemanticMatcherTest {

    @Test
    void testContainsTerm_WithSpacesAndPunctuation() {
        assertTrue(MetricSemanticMatcher.containsTerm("请看 审批-平均时长？", "审批平均时长"));
    }

    @Test
    void testSimilarity_WithContainment() {
        double similarity = MetricSemanticMatcher.similarity("本月销售额趋势", "销售额");
        assertTrue(similarity >= 0.9);
    }

    @Test
    void testContainsTerm_WithSingleCharacterTypo() {
        assertTrue(MetricSemanticMatcher.containsTerm("本月销受额趋势如何", "销售额"));
    }

    @Test
    void testContainsTerm_WithBusinessPhraseTypos() {
        assertTrue(MetricSemanticMatcher.containsTerm("毛利律本月咋样", "毛利率"));
        assertTrue(MetricSemanticMatcher.containsTerm("帮我看下回宽额趋势", "回款额"));
    }

    @Test
    void testContainsTerm_WithEnglishTypoAlias() {
        assertTrue(MetricSemanticMatcher.containsTerm("show me revennue trend", "revenue"));
        assertTrue(MetricSemanticMatcher.containsTerm("inventory and fullfilment health", "fulfillment"));
    }

    @Test
    void testContainsTerm_WithDifferentMetricShouldNotMatch() {
        assertFalse(MetricSemanticMatcher.containsTerm("本月库存周转天数", "销售额"));
    }

    @Test
    void testContainsTerm_Blank() {
        assertFalse(MetricSemanticMatcher.containsTerm("", "销售额"));
        assertFalse(MetricSemanticMatcher.containsTerm("销售额", ""));
    }
}
