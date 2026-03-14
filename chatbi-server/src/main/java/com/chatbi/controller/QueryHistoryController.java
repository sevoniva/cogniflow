package com.chatbi.controller;

import com.chatbi.common.PageResult;
import com.chatbi.common.Result;
import com.chatbi.entity.QueryHistory;
import com.chatbi.service.QueryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 查询历史控制器
 */
@RestController
@RequestMapping("/api/query-history")
@RequiredArgsConstructor
public class QueryHistoryController {

    private final QueryHistoryService queryHistoryService;

    /**
     * 分页查询查询历史
     */
    @GetMapping
    public Result<PageResult<QueryHistory>> page(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        var page = queryHistoryService.page(userId, current.longValue(), size.longValue());
        return Result.ok(PageResult.of(page.getRecords(), page.getTotal(), current.longValue(), size.longValue()));
    }

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public Result<QueryHistory> getById(@PathVariable Long id) {
        return Result.ok(queryHistoryService.getById(id));
    }

    /**
     * 保存查询历史
     */
    @PostMapping
    public Result<QueryHistory> save(@RequestBody QueryHistory queryHistory) {
        return Result.ok(queryHistoryService.save(queryHistory));
    }

    /**
     * 更新查询历史
     */
    @PutMapping("/{id}")
    public Result<QueryHistory> update(@PathVariable Long id, @RequestBody QueryHistory queryHistory) {
        return Result.ok(queryHistoryService.update(id, queryHistory));
    }

    /**
     * 删除查询历史
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        queryHistoryService.delete(id);
        return Result.ok();
    }

    /**
     * 收藏/取消收藏
     */
    @PatchMapping("/{id}/favorite")
    public Result<Void> toggleFavorite(
            @PathVariable Long id,
            @RequestParam Boolean isFavorite) {
        queryHistoryService.toggleFavorite(id, isFavorite);
        return Result.ok();
    }

    /**
     * 获取收藏列表
     */
    @GetMapping("/favorites")
    public Result<List<QueryHistory>> getFavorites(
            @RequestParam(required = false) Long userId) {
        return Result.ok(queryHistoryService.getFavorites(userId));
    }

    /**
     * 获取最近查询
     */
    @GetMapping("/recent")
    public Result<List<QueryHistory>> getRecent(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(queryHistoryService.getRecent(userId, limit));
    }
}
