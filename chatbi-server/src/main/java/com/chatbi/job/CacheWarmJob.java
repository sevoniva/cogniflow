package com.chatbi.job;

import com.chatbi.entity.QueryHistory;
import com.chatbi.repository.QueryHistoryMapper;
import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.QueryCacheService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 查询缓存预热任务（XXL-JOB）
 *
 * 每天凌晨 3 点预热 Top 20 高频查询，提升白天高峰期的缓存命中率。
 * Month 3 Week 2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmJob {

    private final QueryHistoryMapper queryHistoryMapper;
    private final QueryExecutionService queryExecutionService;
    private final QueryCacheService queryCacheService;

    @XxlJob("cacheWarmJob")
    public void execute() {
        XxlJobHelper.log("开始执行缓存预热任务");
        try {
            // 查询最近 7 天 Top 20 高频查询
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<QueryHistory> topQueries = queryHistoryMapper.findTopQueries(since, 20);

            if (topQueries.isEmpty()) {
                XxlJobHelper.log("无可预热的高频查询");
                XxlJobHelper.handleSuccess("无可预热数据");
                return;
            }

            int warmed = 0;
            for (QueryHistory history : topQueries) {
                try {
                    if (!queryCacheService.shouldCache(history.getQueryContent())) {
                        continue;
                    }
                    // 如果缓存已存在则跳过
                    var cached = queryCacheService.getCachedResult(
                            history.getQueryContent(), history.getDatasourceId());
                    if (cached != null) {
                        continue;
                    }
                    // 执行查询并缓存
                    var result = queryExecutionService.execute(history.getQueryContent());
                    long ttl = queryCacheService.decideCacheTtl(history.getQueryContent());
                    queryCacheService.cacheResult(
                            history.getQueryContent(), history.getDatasourceId(), result, ttl);
                    warmed++;
                } catch (Exception e) {
                    log.warn("预热单条查询失败 - query: {}", history.getQueryName(), e);
                }
            }

            String msg = String.format("缓存预热完成 - 预热: %d/%d 条", warmed, topQueries.size());
            XxlJobHelper.log(msg);
            XxlJobHelper.handleSuccess(msg);
        } catch (Exception e) {
            log.error("缓存预热任务失败", e);
            XxlJobHelper.log("预热失败: " + e.getMessage());
            XxlJobHelper.handleFail("预热失败: " + e.getMessage());
        }
    }
}
