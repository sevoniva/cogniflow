package com.chatbi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChartDataValidationService 测试")
class ChartDataValidationServiceTest {

    @Mock
    private EnterpriseChartCatalogService enterpriseChartCatalogService;
    @Mock
    private BusinessInsightService businessInsightService;

    @InjectMocks
    private ChartDataValidationService chartDataValidationService;

    @Test
    @DisplayName("验证摘要应返回 100% 覆盖")
    void testValidationSummarySuccess() {
        when(enterpriseChartCatalogService.getCatalog()).thenReturn(List.of(
            Map.of("type", "bar.enterprise", "family", "bar", "variant", "enterprise"),
            Map.of("type", "line.enterprise", "family", "line", "variant", "enterprise")
        ));
        when(enterpriseChartCatalogService.getFamilies()).thenReturn(List.of("bar", "line"));
        when(businessInsightService.getChartData("sales", "区域")).thenReturn(List.of(
            Map.of("区域", "华东", "销售额", 1200),
            Map.of("区域", "华北", "销售额", 980)
        ));
        when(businessInsightService.getChartData("timeseries", "月份")).thenReturn(List.of(
            Map.of("月份", "2026-01", "销售额", 1000),
            Map.of("月份", "2026-02", "销售额", 1200),
            Map.of("月份", "2026-03", "销售额", 1300)
        ));

        Map<String, Object> summary = chartDataValidationService.getValidationSummary(0, false);
        assertEquals(2, summary.get("totalTypes"));
        assertEquals(2, summary.get("validatedTypes"));
        assertEquals(100.0, summary.get("coverageRate"));
        assertTrue(((List<?>) summary.get("typeValidation")).size() == 2);
    }

    @Test
    @DisplayName("仅失败过滤应生效")
    void testValidationSummaryOnlyFailed() {
        when(enterpriseChartCatalogService.getCatalog()).thenReturn(List.of(
            Map.of("type", "bar.enterprise", "family", "bar", "variant", "enterprise")
        ));
        when(enterpriseChartCatalogService.getFamilies()).thenReturn(List.of("bar"));
        when(businessInsightService.getChartData("sales", "区域")).thenReturn(List.of());

        Map<String, Object> summary = chartDataValidationService.getValidationSummary(0, true);
        assertEquals(1, summary.get("totalTypes"));
        assertEquals(0, summary.get("validatedTypes"));
        List<?> failures = (List<?>) summary.get("typeValidation");
        assertEquals(1, failures.size());
    }
}
