package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据导出控制器
 */
@Slf4j
@Tag(name = "数据导出", description = "Excel/PDF 导出相关接口")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * 导出 Excel
     */
    @PostMapping("/excel")
    @Operation(summary = "导出 Excel 文件")
    public ResponseEntity<byte[]> exportExcel(
            @RequestBody List<Map<String, Object>> data,
            @RequestParam String fileName,
            @RequestHeader List<String> headers) {

        byte[] excelData = exportService.exportExcel(data, headers, fileName);

        String downloadFileName = exportService.generateFileName(fileName, "xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType(exportService.getExcelMimeType()))
                .contentLength(excelData.length)
                .body(excelData);
    }

    /**
     * 导出 PDF
     */
    @PostMapping("/pdf")
    @Operation(summary = "导出 PDF 文件")
    public ResponseEntity<byte[]> exportPdf(
            @RequestBody List<Map<String, Object>> data,
            @RequestParam String title,
            @RequestHeader List<String> headers) {

        byte[] pdfData = exportService.exportPdf(data, headers, title);

        String downloadFileName = exportService.generateFileName(title, "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType(exportService.getPdfMimeType()))
                .contentLength(pdfData.length)
                .body(pdfData);
    }
}
