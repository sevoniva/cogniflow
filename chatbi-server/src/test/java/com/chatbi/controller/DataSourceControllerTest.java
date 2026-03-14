package com.chatbi.controller;

import com.chatbi.service.DataSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSourceController 测试")
class DataSourceControllerTest {

    @Mock
    private DataSourceService dataSourceService;

    @InjectMocks
    private DataSourceController dataSourceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dataSourceController).build();
    }

    @Test
    @DisplayName("获取数据表列表 - 成功")
    void testGetTablesSuccess() throws Exception {
        DataSourceService.DataSourceMetadata metadata = new DataSourceService.DataSourceMetadata();
        metadata.addTable("sales_order", "销售订单");
        metadata.addTable("customer", "客户");
        when(dataSourceService.getMetadata(1L)).thenReturn(metadata);

        mockMvc.perform(get("/api/datasources/{id}/tables", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("sales_order"))
                .andExpect(jsonPath("$.data[1].name").value("customer"));
    }

    @Test
    @DisplayName("获取字段列表 - 成功")
    void testGetColumnsSuccess() throws Exception {
        DataSourceService.TableSchema schema = new DataSourceService.TableSchema();
        schema.setTableName("sales_order");

        DataSourceService.ColumnInfo orderId = new DataSourceService.ColumnInfo();
        orderId.setColumnName("order_id");
        orderId.setDataType("BIGINT");
        orderId.setNullable(false);
        orderId.setRemarks("订单ID");

        DataSourceService.ColumnInfo amount = new DataSourceService.ColumnInfo();
        amount.setColumnName("amount");
        amount.setDataType("DECIMAL");
        amount.setNullable(true);
        amount.setRemarks("销售额");

        schema.setColumns(List.of(orderId, amount));
        when(dataSourceService.getTableSchema(1L, "sales_order")).thenReturn(schema);

        mockMvc.perform(get("/api/datasources/{id}/columns", 1L).param("table", "sales_order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("order_id"))
                .andExpect(jsonPath("$.data[0].type").value("BIGINT"))
                .andExpect(jsonPath("$.data[1].name").value("amount"))
                .andExpect(jsonPath("$.data[1].remarks").value("销售额"));
    }
}
