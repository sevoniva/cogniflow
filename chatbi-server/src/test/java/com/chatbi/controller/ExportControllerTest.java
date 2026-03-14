package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExportController 单元测试
 */
@DisplayName("ExportController 测试")
@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    private List<Map<String, Object>> testData;
    private List<String> testHeaders;

    @BeforeEach
    void setUp() {
        testData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("name", "测试");
        testData.add(row);

        testHeaders = Arrays.asList("id", "name");
    }

    @Test
    @DisplayName("导出 Excel 测试")
    void testExportExcel() {
        byte[] mockExcelData = new byte[]{1, 2, 3, 4, 5};
        when(exportService.exportExcel(anyList(), anyList(), anyString())).thenReturn(mockExcelData);
        when(exportService.generateFileName(anyString(), anyString())).thenReturn("test_20260310_120000.xlsx");
        when(exportService.getExcelMimeType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        ResponseEntity<byte[]> response = exportController.exportExcel(testData, "test", testHeaders);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(mockExcelData, response.getBody());
        verify(exportService).exportExcel(testData, testHeaders, "test");
    }

    @Test
    @DisplayName("导出 PDF 测试")
    void testExportPdf() {
        byte[] mockPdfData = new byte[]{1, 2, 3, 4, 5};
        when(exportService.exportPdf(anyList(), anyList(), anyString())).thenReturn(mockPdfData);
        when(exportService.generateFileName(anyString(), anyString())).thenReturn("test_20260310_120000.pdf");
        when(exportService.getPdfMimeType()).thenReturn("application/pdf");

        ResponseEntity<byte[]> response = exportController.exportPdf(testData, "测试报表", testHeaders);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(mockPdfData, response.getBody());
        verify(exportService).exportPdf(testData, testHeaders, "测试报表");
    }
}
