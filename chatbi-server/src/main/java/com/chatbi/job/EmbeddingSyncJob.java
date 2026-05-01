package com.chatbi.job;

import com.chatbi.service.rag.EmbeddingSyncService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 向量数据同步任务（XXL-JOB）
 *
 * 每天凌晨 2 点执行增量同步，将变更的指标定义同步到向量存储。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingSyncJob {

    private final EmbeddingSyncService embeddingSyncService;

    @XxlJob("embeddingSyncJob")
    public void execute() {
        XxlJobHelper.log("开始执行向量数据增量同步任务");
        try {
            EmbeddingSyncService.SyncResult result = embeddingSyncService.syncIncremental();
            XxlJobHelper.log(String.format("同步完成 - 新增: %d, 删除: %d, 总文档: %d, 时间: %s",
                    result.syncedCount(), result.deletedCount(), result.totalDocuments(), result.syncTime()));
            XxlJobHelper.handleSuccess(result.message());
        } catch (Exception e) {
            log.error("向量数据同步失败", e);
            XxlJobHelper.log("同步失败: " + e.getMessage());
            XxlJobHelper.handleFail("同步失败: " + e.getMessage());
        }
    }
}
