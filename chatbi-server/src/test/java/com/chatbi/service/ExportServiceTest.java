package com.chatbi.service;

import com.alibaba.excel.EasyExcel;
import com.chatbi.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportService 单元测试
 */
@DisplayName("ExportService 测试")
class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
    }

    @Test
    @DisplayName("导出 Excel 测试")
    void testExportExcel() {
        // 准备测试数据
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "测试 1");
        row1.put("value", 100);
        data.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("name", "测试 2");
        row2.put("value", 200);
        data.add(row2);

        List<String> headers = Arrays.asList("id", "name", "value");
        String fileName = "test_export";

        // 执行测试
        byte[] excelData = exportService.exportExcel(data, headers, fileName);

        // 验证结果
        assertNotNull(excelData);
        assertTrue(excelData.length > 0);
    }

    @Test
    @DisplayName("导出 Excel - 空数据测试")
    void testExportExcel_EmptyData() {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> headers = Arrays.asList("id", "name", "value");
        String fileName = "test_export_empty";

        byte[] excelData = exportService.exportExcel(data, headers, fileName);

        assertNotNull(excelData);
        assertTrue(excelData.length > 0);
    }

    @Test
    @DisplayName("导出 PDF 测试")
    void testExportPdf() {
        // 准备测试数据
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("name", "测试 1");
        row1.put("value", "100");
        data.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", "2");
        row2.put("name", "测试 2");
        row2.put("value", "200");
        data.add(row2);

        List<String> headers = Arrays.asList("id", "name", "value");
        String title = "测试报表";

        // 执行测试
        byte[] pdfData = exportService.exportPdf(data, headers, title);

        // 验证结果
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
    }

    @Test
    @DisplayName("获取 Excel MIME 类型测试")
    void testGetExcelMimeType() {
        String mimeType = exportService.getExcelMimeType();
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", mimeType);
    }

    @Test
    @DisplayName("获取 PDF MIME 类型测试")
    void testGetPdfMimeType() {
        String mimeType = exportService.getPdfMimeType();
        assertEquals("application/pdf", mimeType);
    }

    @Test
    @DisplayName("生成文件名测试")
    void testGenerateFileName() {
        String fileName = exportService.generateFileName("test", "xlsx");

        assertNotNull(fileName);
        assertTrue(fileName.startsWith("test_"));
        assertTrue(fileName.endsWith(".xlsx"));
    }

    @Test
    @DisplayName("生成文件名 - PDF 测试")
    void testGenerateFileName_Pdf() {
        String fileName = exportService.generateFileName("report", "pdf");

        assertNotNull(fileName);
        assertTrue(fileName.startsWith("report_"));
        assertTrue(fileName.endsWith(".pdf"));
    }
}
