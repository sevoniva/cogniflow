package com.chatbi.job;

import com.chatbi.service.FeedbackService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 反馈数据集导出任务（XXL-JOB）
 *
 * 每周自动将上周未导出的反馈导出为 JSONL fine-tuning 数据集。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackExportJob {

    private final FeedbackService feedbackService;

    @XxlJob("feedbackExportJob")
    public void execute() {
        XxlJobHelper.log("开始执行反馈数据集导出任务");
        try {
            // 默认导出上周数据
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(7);

            String filePath = feedbackService.exportToJsonl(startTime, endTime);
            if (filePath != null) {
                XxlJobHelper.log("导出成功: " + filePath);
                XxlJobHelper.handleSuccess("导出成功: " + filePath);
            } else {
                XxlJobHelper.log("无可导出数据");
                XxlJobHelper.handleSuccess("无可导出数据");
            }
        } catch (Exception e) {
            log.error("反馈数据集导出失败", e);
            XxlJobHelper.log("导出失败: " + e.getMessage());
            XxlJobHelper.handleFail("导出失败: " + e.getMessage());
        }
    }
}
