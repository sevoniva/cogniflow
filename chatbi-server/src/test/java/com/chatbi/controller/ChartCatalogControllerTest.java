package com.chatbi.controller;

import com.chatbi.service.EnterpriseChartCatalogService;
import com.chatbi.service.ChartDataValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChartCatalogController 测试")
class ChartCatalogControllerTest {

    @Mock
    private EnterpriseChartCatalogService enterpriseChartCatalogService;
    @Mock
    private ChartDataValidationService chartDataValidationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChartCatalogController controller = new ChartCatalogController(enterpriseChartCatalogService, chartDataValidationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("摘要接口返回图表总量和维度")
    void testGetSummary() throws Exception {
        when(enterpriseChartCatalogService.getSummary()).thenReturn(Map.of(
            "total", 119,
            "familyCount", 17
        ));

        mockMvc.perform(get("/api/chart-catalog/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.total").value(119))
            .andExpect(jsonPath("$.data.familyCount").value(17));
    }

    @Test
    @DisplayName("类型接口支持 limit 参数")
    void testGetTypesWithLimit() throws Exception {
        when(enterpriseChartCatalogService.getFeaturedTypes(5)).thenReturn(List.of(
            Map.of("type", "bar.enterprise", "family", "bar", "variant", "enterprise"),
            Map.of("type", "line.enterprise", "family", "line", "variant", "enterprise")
        ));

        mockMvc.perform(get("/api/chart-catalog/types").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].type").value("bar.enterprise"))
            .andExpect(jsonPath("$.data[1].type").value("line.enterprise"));
    }

    @Test
    @DisplayName("验证接口返回覆盖率摘要")
    void testGetValidationSummary() throws Exception {
        when(chartDataValidationService.getValidationSummary(10, false)).thenReturn(Map.of(
            "validatedTypes", 119,
            "totalTypes", 119,
            "coverageRate", 100.0
        ));

        mockMvc.perform(get("/api/chart-catalog/validation")
                .param("limit", "10")
                .param("onlyFailed", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.validatedTypes").value(119))
            .andExpect(jsonPath("$.data.coverageRate").value(100.0));
    }
}
