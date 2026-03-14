package com.chatbi.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 数据导出服务 - Excel/PDF
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    /**
     * 导出 Excel
     * @param data 数据
     * @param headers 表头
     * @param fileName 文件名
     * @return Excel 文件字节流
     */
    public byte[] exportExcel(List<Map<String, Object>> data, List<String> headers, String fileName) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 构建 head 列表
            List<List<String>> head = new java.util.ArrayList<>();
            for (String header : headers) {
                List<String> row = new java.util.ArrayList<>();
                row.add(header);
                head.add(row);
            }

            EasyExcel.write(outputStream)
                    .head(head)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("数据导出")
                    .doWrite(data);

            log.info("Excel 导出成功：{}，记录数：{}", fileName, data.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Excel 导出失败：{}", e.getMessage(), e);
            throw new RuntimeException("Excel 导出失败：" + e.getMessage());
        }
    }

    /**
     * 导出 PDF
     * @param data 数据
     * @param headers 表头
     * @param title 标题
     * @return PDF 文件字节流
     */
    public byte[] exportPdf(List<Map<String, Object>> data, List<String> headers, String title) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 添加标题
            document.add(new Paragraph(title)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // 创建表格
            float[] columnWidths = new float[headers.size()];
            for (int i = 0; i < headers.size(); i++) {
                columnWidths[i] = 1;
            }

            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .useAllAvailableWidth();

            // 添加表头
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
            }

            // 添加数据
            for (Map<String, Object> row : data) {
                for (String header : headers) {
                    Object value = row.get(header);
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(value != null ? value.toString() : "")));
                }
            }

            document.add(table);
            document.close();

            log.info("PDF 导出成功，记录数：{}", data.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("PDF 导出失败：{}", e.getMessage(), e);
            throw new RuntimeException("PDF 导出失败：" + e.getMessage());
        }
    }

    /**
     * 导出 Excel（带类类型）
     */
    public <T> byte[] exportExcelWithClass(List<T> data, Class<T> clazz, String fileName) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcel.write(outputStream, clazz)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("数据导出")
                    .doWrite(data);

            log.info("Excel 导出成功：{}，记录数：{}", fileName, data.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Excel 导出失败：{}", e.getMessage(), e);
            throw new RuntimeException("Excel 导出失败：" + e.getMessage());
        }
    }

    /**
     * 获取 Excel MIME 类型
     */
    public String getExcelMimeType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    /**
     * 获取 PDF MIME 类型
     */
    public String getPdfMimeType() {
        return "application/pdf";
    }

    /**
     * 生成下载文件名
     */
    public String generateFileName(String prefix, String extension) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return prefix + "_" + timestamp + "." + extension;
    }
}
