package com.chatbi.controller;

import com.chatbi.repository.DataSourceMapper;
import com.chatbi.service.AiQueryService;
import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.QueryGovernanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiQueryController 测试")
class AiQueryControllerTest {

    @Mock
    private AiQueryService aiQueryService;
    @Mock
    private QueryExecutionService queryExecutionService;
    @Mock
    private DataSourceMapper dataSourceMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiQueryController controller = new AiQueryController(aiQueryService, queryExecutionService, dataSourceMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        com.chatbi.entity.DataSource ds = new com.chatbi.entity.DataSource();
        ds.setId(1L);
        ds.setName("测试数据源");
        when(dataSourceMapper.selectById(1L)).thenReturn(ds);

        AiQueryService.TableSchema schema = new AiQueryService.TableSchema();
        schema.setTableName("sales_order");
        AiQueryService.TableSchema.Column column = new AiQueryService.TableSchema.Column();
        column.setName("sales_amount");
        column.setType("DECIMAL");
        schema.setColumns(List.of(column));
        when(queryExecutionService.extractTableSchemas(any())).thenReturn(List.of(schema));
    }

    @Test
    @DisplayName("SQL 校验失败时返回降级成功响应而不是错误")
    void testQuery_ShouldFallbackOnValidationFailure() throws Exception {
        when(aiQueryService.generateSqlWithLLM(anyString(), any())).thenReturn("SELECT * FROM sales_order");
        when(aiQueryService.validateSqlDetail(anyString()))
            .thenReturn(QueryGovernanceService.ValidationResult.invalid("未识别到可查询的数据表"));

        mockMvc.perform(post("/api/ai-query/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"帮我分析平台数据\",\"datasourceId\":1,\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.success").value(true))
            .andExpect(jsonPath("$.data.source").value("guided-recovery"))
            .andExpect(jsonPath("$.data.recovered").value(true))
            .andExpect(jsonPath("$.data.data.length()").value(4));
    }

    @Test
    @DisplayName("AI 查询异常时返回经营总览兜底")
    void testQuery_ShouldFallbackOnException() throws Exception {
        when(aiQueryService.generateSqlWithLLM(anyString(), any())).thenThrow(new IllegalStateException("模拟 AI 异常"));

        mockMvc.perform(post("/api/ai-query/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"本月销售额趋势\",\"datasourceId\":1,\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.success").value(true))
            .andExpect(jsonPath("$.data.source").value("guided-recovery"))
            .andExpect(jsonPath("$.data.recovered").value(true))
            .andExpect(jsonPath("$.data.suggestions.length()").value(4));
    }

    @Test
    @DisplayName("查询成功时返回真实查询结果")
    void testQuery_ShouldReturnSuccessResult() throws Exception {
        when(aiQueryService.generateSqlWithLLM(anyString(), any())).thenReturn("SELECT region, sales_amount FROM sales_order");
        when(aiQueryService.validateSqlDetail(anyString())).thenReturn(QueryGovernanceService.ValidationResult.passed());
        when(queryExecutionService.executeQuery(any(), anyString(), anyLong()))
            .thenReturn(List.of(Map.of("region", "华东", "sales_amount", 60000)));

        mockMvc.perform(post("/api/ai-query/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"本月销售额\",\"datasourceId\":1,\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.success").value(true))
            .andExpect(jsonPath("$.data.message").value("查询成功，共 1 条结果"))
            .andExpect(jsonPath("$.data.data[0].region").value("华东"));
    }
}
