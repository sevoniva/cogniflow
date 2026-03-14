package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.DataSource;
import com.chatbi.service.DataSourceService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理控制器
 */
@Tag(name = "数据源管理", description = "提供数据源的 CRUD、连接测试、元数据获取等功能")
@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 分页查询数据源列表
     */
    @Operation(summary = "分页查询数据源列表", description = "支持按关键词和状态筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('datasource:query')")
    public Result<Page<DataSource>> list(
            @Parameter(description = "关键词（名称/编码）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size
    ) {
        Page<DataSource> page = dataSourceService.page(keyword, status, current, size);
        page.setRecords(page.getRecords().stream().map(this::sanitizeDataSource).toList());
        return Result.ok(page);
    }

    /**
     * 查询所有数据源
     */
    @Operation(summary = "查询所有数据源", description = "返回全部数据源列表")
    @GetMapping("/all")
    public Result<List<DataSource>> listAll() {
        List<DataSource> list = dataSourceService.list().stream().map(this::sanitizeDataSource).toList();
        return Result.ok(list);
    }

    /**
     * 根据 ID 查询数据源
     */
    @Operation(summary = "根据 ID 查询数据源", description = "返回指定 ID 的数据源详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('datasource:query')")
    public Result<DataSource> get(@PathVariable Long id) {
        DataSource dataSource = sanitizeDataSource(dataSourceService.getById(id));
        return Result.ok(dataSource);
    }

    /**
     * 根据编码查询数据源
     */
    @Operation(summary = "根据编码查询数据源", description = "返回指定编码的数据源详情")
    @GetMapping("/code/{code}")
    public Result<DataSource> getByCode(@PathVariable String code) {
        DataSource dataSource = sanitizeDataSource(dataSourceService.getByCode(code));
        return Result.ok(dataSource);
    }

    /**
     * 创建数据源
     */
    @Operation(summary = "创建数据源", description = "新增数据源配置")
    @PostMapping
    @PreAuthorize("hasAuthority('datasource:add')")
    public Result<DataSource> create(@RequestBody DataSource dataSource) {
        DataSource created = sanitizeDataSource(dataSourceService.create(dataSource));
        return Result.ok("创建成功", created);
    }

    /**
     * 更新数据源
     */
    @Operation(summary = "更新数据源", description = "更新数据源配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('datasource:update')")
    public Result<DataSource> update(@PathVariable Long id, @RequestBody DataSource dataSource) {
        DataSource updated = sanitizeDataSource(dataSourceService.update(id, dataSource));
        return Result.ok("更新成功", updated);
    }

    /**
     * 删除数据源
     */
    @Operation(summary = "删除数据源", description = "删除指定 ID 的数据源")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('datasource:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 测试数据源连接
     */
    @Operation(summary = "测试数据源连接", description = "测试数据源连接是否可用")
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('datasource:test')")
    public Result<Map<String, Object>> test(@RequestBody DataSource dataSource) {
        DataSourceService.ConnectionTestResult result = dataSourceService.testConnection(dataSource);

        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "message", result.getMessage(),
                "responseTime", result.getResponseTime()
        );

        return result.isSuccess() ? Result.ok(response) : Result.error(result.getMessage());
    }

    /**
     * 获取数据源元数据
     */
    @Operation(summary = "获取数据源元数据", description = "获取数据源中的表、视图等元数据信息")
    @GetMapping("/{id}/metadata")
    @PreAuthorize("hasAuthority('datasource:query')")
    public Result<DataSourceService.DataSourceMetadata> getMetadata(@PathVariable Long id) {
        try {
            DataSourceService.DataSourceMetadata metadata = dataSourceService.getMetadata(id);
            return Result.ok(metadata);
        } catch (Exception e) {
            return Result.error("获取元数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取数据源下的表列表
     */
    @Operation(summary = "获取数据表列表", description = "返回指定数据源中可访问的数据表")
    @GetMapping("/{id}/tables")
    public Result<List<DataSourceService.DataSourceMetadata.TableInfo>> getTables(@PathVariable Long id) {
        try {
            DataSourceService.DataSourceMetadata metadata = dataSourceService.getMetadata(id);
            return Result.ok(metadata.getTables());
        } catch (Exception e) {
            return Result.error("获取数据表失败：" + e.getMessage());
        }
    }

    /**
     * 获取表结构
     */
    @Operation(summary = "获取表结构", description = "获取指定表的列结构信息")
    @GetMapping("/{id}/tables/{tableName}")
    @PreAuthorize("hasAuthority('datasource:query')")
    public Result<DataSourceService.TableSchema> getTableSchema(
            @PathVariable Long id,
            @PathVariable String tableName
    ) {
        try {
            DataSourceService.TableSchema schema = dataSourceService.getTableSchema(id, tableName);
            return Result.ok(schema);
        } catch (Exception e) {
            return Result.error("获取表结构失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定表的字段列表
     */
    @Operation(summary = "获取字段列表", description = "返回指定数据表的列信息")
    @GetMapping("/{id}/columns")
    public Result<List<Map<String, Object>>> getColumns(
            @PathVariable Long id,
            @RequestParam String table
    ) {
        try {
            DataSourceService.TableSchema schema = dataSourceService.getTableSchema(id, table);
            List<Map<String, Object>> columns = schema.getColumns() == null
                    ? List.of()
                    : schema.getColumns().stream()
                        .map(column -> Map.<String, Object>of(
                                "name", column.getColumnName(),
                                "type", column.getDataType(),
                                "nullable", column.isNullable(),
                                "remarks", column.getRemarks() == null ? "" : column.getRemarks()
                        ))
                        .toList();
            return Result.ok(columns);
        } catch (Exception e) {
            return Result.error("获取字段失败：" + e.getMessage());
        }
    }

    /**
     * 获取支持的数据源类型
     */
    @Operation(summary = "获取支持的数据源类型", description = "返回系统支持的所有数据库类型")
    @GetMapping("/types")
    public Result<List<Map<String, String>>> getSupportedTypes() {
        List<Map<String, String>> types = java.util.Arrays.stream(com.chatbi.enums.DataSourceType.values())
                .map(t -> Map.of(
                        "type", t.name(),
                        "name", t.getName(),
                        "driver", t.getDriverClass()
                ))
                .toList();
        return Result.ok(types);
    }

    private DataSource sanitizeDataSource(DataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        return DataSource.builder()
                .id(dataSource.getId())
                .name(dataSource.getName())
                .code(dataSource.getCode())
                .type(dataSource.getType())
                .host(dataSource.getHost())
                .port(dataSource.getPort())
                .database(dataSource.getDatabase())
                .service(dataSource.getService())
                .url(dataSource.getUrl())
                .username(dataSource.getUsername())
                .passwordEncrypted(dataSource.getPasswordEncrypted() == null || dataSource.getPasswordEncrypted().isBlank() ? null : "***已配置***")
                .driverClass(dataSource.getDriverClass())
                .configJson(dataSource.getConfigJson())
                .status(dataSource.getStatus())
                .healthStatus(dataSource.getHealthStatus())
                .lastCheckTime(dataSource.getLastCheckTime())
                .createdBy(dataSource.getCreatedBy())
                .createdAt(dataSource.getCreatedAt())
                .updatedAt(dataSource.getUpdatedAt())
                .deletedAt(dataSource.getDeletedAt())
                .build();
    }
}
