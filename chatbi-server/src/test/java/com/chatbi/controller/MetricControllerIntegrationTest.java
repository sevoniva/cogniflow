package com.chatbi.controller;

import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MetricController 测试
 * 使用 standalone MockMvc，避免完整 Spring 上下文带来的外部依赖干扰
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricController 集成测试")
class MetricControllerIntegrationTest {

    @Mock
    private MetricMapper metricMapper;

    @InjectMocks
    private MetricController metricController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(metricController).build();
    }

    @Test
    @DisplayName("获取指标列表 - 成功")
    void testGetMetrics_Success() throws Exception {
        Metric metric = buildMetric(1L, "TEST_METRIC", "测试指标", "测试定义", "active");
        when(metricMapper.selectList(null)).thenReturn(List.of(metric));

        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("TEST_METRIC"));
    }

    @Test
    @DisplayName("根据 ID 获取指标 - 成功")
    void testGetMetricById_Success() throws Exception {
        Metric metric = buildMetric(2L, "TEST_METRIC_2", "测试指标 2", "测试定义 2", "active");
        when(metricMapper.selectById(2L)).thenReturn(metric);

        mockMvc.perform(get("/api/metrics/{id}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TEST_METRIC_2"));
    }

    @Test
    @DisplayName("新增指标 - 成功")
    void testAddMetric_Success() throws Exception {
        when(metricMapper.selectCount(any())).thenReturn(0L);
        when(metricMapper.insert(any(Metric.class))).thenAnswer(invocation -> {
            Metric metric = invocation.getArgument(0);
            metric.setId(3L);
            return 1;
        });

        mockMvc.perform(post("/api/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NEW_METRIC\",\"name\":\"新指标\",\"definition\":\"新定义\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("NEW_METRIC"))
                .andExpect(jsonPath("$.data.id").value(3L));
    }

    @Test
    @DisplayName("更新指标 - 成功")
    void testUpdateMetric_Success() throws Exception {
        Metric metric = buildMetric(4L, "UPDATE_METRIC", "原指标名称", "原定义", "active");
        when(metricMapper.selectById(4L)).thenReturn(metric);
        when(metricMapper.updateById(any(Metric.class))).thenReturn(1);

        mockMvc.perform(put("/api/metrics/{id}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新后的名称\",\"definition\":\"更新后的定义\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新后的名称"))
                .andExpect(jsonPath("$.data.definition").value("更新后的定义"));
    }

    @Test
    @DisplayName("删除指标 - 成功")
    void testDeleteMetric_Success() throws Exception {
        when(metricMapper.deleteById(5L)).thenReturn(1);

        mockMvc.perform(delete("/api/metrics/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(metricMapper).deleteById(5L);
    }

    @Test
    @DisplayName("获取启用的指标 - 成功")
    void testGetActiveMetrics_Success() throws Exception {
        Metric metric = buildMetric(6L, "ACTIVE_METRIC", "已启用指标", "定义", "active");
        when(metricMapper.selectList(any())).thenReturn(List.of(metric));

        mockMvc.perform(get("/api/metrics/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("ACTIVE_METRIC"));
    }

    @Test
    @DisplayName("切换指标状态 - 成功")
    void testToggleMetricStatus_Success() throws Exception {
        Metric metric = buildMetric(7L, "TOGGLE_METRIC", "状态切换指标", "定义", "active");
        when(metricMapper.selectById(7L)).thenReturn(metric);
        when(metricMapper.updateById(any(Metric.class))).thenReturn(1);

        mockMvc.perform(patch("/api/metrics/{id}/toggle", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("inactive"));
    }

    @Test
    @DisplayName("新增指标 - 编码重复")
    void testAddMetric_DuplicateCode() throws Exception {
        when(metricMapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(post("/api/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DUPLICATE\",\"name\":\"重复指标\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("指标编码已存在"));

        verify(metricMapper, never()).insert(ArgumentMatchers.any(Metric.class));
    }

    private Metric buildMetric(Long id, String code, String name, String definition, String status) {
        Metric metric = new Metric();
        metric.setId(id);
        metric.setCode(code);
        metric.setName(name);
        metric.setDefinition(definition);
        metric.setStatus(status);
        return metric;
    }
}
