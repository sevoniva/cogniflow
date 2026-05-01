package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.rag.EmbeddingSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 向量同步管理接口
 */
@Tag(name = "向量同步管理", description = "Embedding 向量数据全量/增量同步与状态查看")
@RestController
@RequestMapping("/api/admin/embedding-sync")
@RequiredArgsConstructor
public class EmbeddingSyncController {

    private final EmbeddingSyncService embeddingSyncService;

    @Operation(summary = "查看同步状态")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<EmbeddingSyncService.SyncStatus> status() {
        return Result.ok(embeddingSyncService.getStatus());
    }

    @Operation(summary = "手动触发全量同步")
    @PostMapping("/full")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<EmbeddingSyncService.SyncResult> fullSync() {
        return Result.ok(embeddingSyncService.syncAll());
    }

    @Operation(summary = "手动触发增量同步")
    @PostMapping("/incremental")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<EmbeddingSyncService.SyncResult> incrementalSync() {
        return Result.ok(embeddingSyncService.syncIncremental());
    }
}
