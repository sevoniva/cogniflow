package com.chatbi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EnterpriseChartCatalogService 测试")
class EnterpriseChartCatalogServiceTest {

    private final EnterpriseChartCatalogService service = new EnterpriseChartCatalogService();

    @Test
    @DisplayName("图表目录数量应达到 100+")
    void testCatalogCount() {
        assertTrue(service.getCatalog().size() >= 100);
        assertEquals(119, service.getCatalog().size());
    }

    @Test
    @DisplayName("图表类型应规范化到企业编码")
    void testNormalizeType() {
        assertEquals("bar.enterprise", service.toEnterpriseType("bar"));
        assertEquals("line.enterprise", service.toEnterpriseType("unknown"));
        assertEquals("table", service.toEnterpriseType("table"));
    }
}

