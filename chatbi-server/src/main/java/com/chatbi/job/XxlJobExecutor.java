package com.chatbi.job;

import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.SubscriptionService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 分布式任务执行器
 *
 * 替代原 @Scheduled 单节点定时任务，支持分布式调度、失败重试、Web 可视化。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XxlJobExecutor {

    private final QueryExecutionService queryExecutionService;
    private final SubscriptionService subscriptionService;

    /**
     * 数据源连接池健康检查
     *
     * 每 5 分钟执行一次，清理已关闭的 HikariCP 连接池。
     */
    @XxlJob("dataSourceHealthCheckJob")
    public void dataSourceHealthCheckJob() {
        XxlJobHelper.log("开始执行数据源连接池健康检查");
        try {
            queryExecutionService.healthCheck();
            XxlJobHelper.log("数据源连接池健康检查完成");
        } catch (Exception e) {
            log.error("数据源连接池健康检查失败", e);
            XxlJobHelper.log("数据源连接池健康检查失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 订阅推送检查
     *
     * 每分钟执行一次，检查所有活跃订阅并推送到期消息。
     */
    @XxlJob("subscriptionPushJob")
    public void subscriptionPushJob() {
        XxlJobHelper.log("开始执行订阅推送检查");
        try {
            subscriptionService.checkAndPush();
            XxlJobHelper.log("订阅推送检查完成");
        } catch (Exception e) {
            log.error("订阅推送检查失败", e);
            XxlJobHelper.log("订阅推送检查失败: " + e.getMessage());
            throw e;
        }
    }
}
