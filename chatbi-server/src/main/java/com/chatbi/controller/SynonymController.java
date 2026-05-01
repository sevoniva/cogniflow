package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.ApiResponse;
import com.chatbi.entity.Synonym;
import com.chatbi.repository.SynonymMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同义词管理接口 - 对应前端 IAdminService 同义词部分
 */
@Tag(name = "同义词管理", description = "同义词管理接口 - 对应前端 IAdminService 同义词部分")
@RestController
@RequestMapping("/api/synonyms")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SynonymController {

    private final SynonymMapper synonymMapper;

    /**
     * 获取所有同义词
     */
    @Operation(summary = "获取所有同义词")
    @GetMapping
    public ApiResponse<List<Synonym>> getSynonyms() {
        return ApiResponse.ok(synonymMapper.selectList(null));
    }

    /**
     * 新增同义词
     */
    @Operation(summary = "新增同义词")
    @PostMapping
    public ApiResponse<Synonym> addSynonym(@RequestBody Synonym request) {
        // 检查是否已存在
        LambdaQueryWrapper<Synonym> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Synonym::getStandardWord, request.getStandardWord());
        if (synonymMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error("该标准词已存在");
        }

        synonymMapper.insert(request);
        return ApiResponse.ok(request);
    }

    /**
     * 删除同义词
     */
    @Operation(summary = "删除同义词")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSynonym(@PathVariable Long id) {
        synonymMapper.deleteById(id);
        return ApiResponse.ok();
    }
}
