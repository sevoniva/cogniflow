package com.chatbi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.common.Result;
import com.chatbi.entity.Feedback;
import com.chatbi.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * NL2SQL 反馈接口
 */
@Tag(name = "NL2SQL 反馈", description = "用户对 AI 生成 SQL 的反馈收集与数据集导出")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping
    public Result<Feedback> submit(@RequestBody Feedback feedback) {
        return Result.ok(feedbackService.submit(feedback));
    }

    @Operation(summary = "更新反馈")
    @PutMapping("/{id}")
    public Result<Feedback> update(@PathVariable Long id, @RequestBody Feedback feedback) {
        return Result.ok(feedbackService.update(id, feedback));
    }

    @Operation(summary = "根据 ID 查询")
    @GetMapping("/{id}")
    public Result<Feedback> getById(@PathVariable Long id) {
        return Result.ok(feedbackService.getById(id));
    }

    @Operation(summary = "分页查询")
    @GetMapping
    public Result<Page<Feedback>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean exported) {
        return Result.ok(feedbackService.page(current, size, rating, exported));
    }

    @Operation(summary = "反馈统计")
    @GetMapping("/statistics")
    public Result<Map<String, Long>> statistics() {
        return Result.ok(feedbackService.statistics());
    }

    @Operation(summary = "未导出数量")
    @GetMapping("/unexported-count")
    public Result<Long> unexportedCount() {
        return Result.ok(feedbackService.countUnexported());
    }
}
