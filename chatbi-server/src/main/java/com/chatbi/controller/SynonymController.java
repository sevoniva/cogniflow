package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.entity.Synonym;
import com.chatbi.service.SynonymService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同义词管理接口 - 对应前端 IAdminService 同义词部分
 */
@Tag(name = "同义词管理", description = "同义词管理接口 - 对应前端 IAdminService 同义词部分")
@RestController
@RequestMapping("/api/synonyms")
@RequiredArgsConstructor
public class SynonymController {

    private final SynonymService synonymService;

    @Operation(summary = "获取所有同义词")
    @GetMapping
    public Result<List<Synonym>> getSynonyms() {
        return Result.ok(synonymService.list());
    }

    @Operation(summary = "新增同义词")
    @PostMapping
    public Result<Synonym> addSynonym(@RequestBody Synonym request) {
        try {
            return Result.ok(synonymService.create(request));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除同义词")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSynonym(@PathVariable Long id) {
        synonymService.delete(id);
        return Result.ok();
    }
}
