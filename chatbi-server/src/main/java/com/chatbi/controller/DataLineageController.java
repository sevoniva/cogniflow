package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.DataLineageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据血缘接口
 */
@Tag(name = "数据血缘", description = "SQL 血缘解析与可视化")
@RestController
@RequestMapping("/api/lineage")
@RequiredArgsConstructor
public class DataLineageController {

    private final DataLineageService dataLineageService;

    @Operation(summary = "解析 SQL 血缘")
    @PostMapping("/parse")
    public Result<DataLineageService.LineageGraph> parse(@RequestBody String sql) {
        return Result.ok(dataLineageService.parse(sql));
    }
}
