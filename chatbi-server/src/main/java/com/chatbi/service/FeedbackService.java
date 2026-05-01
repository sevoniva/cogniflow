package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.Feedback;
import com.chatbi.repository.FeedbackMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NL2SQL 反馈服务
 *
 * 收集用户反馈、统计、导出 fine-tuning 数据集。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.feedback.export-dir:./data/feedback}")
    private String exportDir;

    @Transactional
    public Feedback submit(Feedback feedback) {
        if (feedback.getRating() == null) {
            feedback.setRating(0);
        }
        if (feedback.getExported() == null) {
            feedback.setExported(false);
        }
        feedbackMapper.insert(feedback);
        log.info("提交反馈 - id: {}, messageId: {}, rating: {}", feedback.getId(), feedback.getMessageId(), feedback.getRating());
        return feedback;
    }

    @Transactional
    public Feedback update(Long id, Feedback feedback) {
        Feedback existing = feedbackMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("反馈不存在: " + id);
        }
        feedback.setId(id);
        feedbackMapper.updateById(feedback);
        return feedbackMapper.selectById(id);
    }

    public Feedback getById(Long id) {
        return feedbackMapper.selectById(id);
    }

    public Page<Feedback> page(int current, int size, Integer rating, Boolean exported) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (rating != null) {
            wrapper.eq(Feedback::getRating, rating);
        }
        if (exported != null) {
            wrapper.eq(Feedback::getExported, exported);
        }
        wrapper.orderByDesc(Feedback::getCreatedAt);
        return feedbackMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public long countUnexported() {
        return feedbackMapper.countUnexported();
    }

    public Map<String, Long> statistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", feedbackMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.put("thumbsUp", feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>().eq(Feedback::getRating, 1)));
        stats.put("thumbsDown", feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>().eq(Feedback::getRating, -1)));
        stats.put("unrated", feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>().eq(Feedback::getRating, 0)));
        stats.put("unexported", feedbackMapper.countUnexported());
        return stats;
    }

    /**
     * 导出指定时间范围内的反馈为 JSONL（OpenAI fine-tuning 格式）
     *
     * @return 导出文件路径
     */
    @Transactional
    public String exportToJsonl(LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        List<Feedback> feedbacks = feedbackMapper.findUnexportedByTimeRange(startTime, endTime);
        if (feedbacks.isEmpty()) {
            log.info("无可导出的反馈数据 - {} ~ {}", startTime, endTime);
            return null;
        }

        Path dir = Paths.get(exportDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String filename = "feedback_" + startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".jsonl";
        Path filePath = dir.resolve(filename);

        List<String> lines = feedbacks.stream()
                .map(this::toFineTuningRecord)
                .collect(Collectors.toList());

        Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 标记为已导出
        int marked = feedbackMapper.markExportedByTimeRange(startTime, endTime);
        log.info("导出反馈数据集 - 文件: {}, 条数: {}, 标记已导出: {}", filePath, lines.size(), marked);
        return filePath.toString();
    }

    private String toFineTuningRecord(Feedback feedback) {
        try {
            Map<String, Object> record = new HashMap<>();
            record.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a SQL expert. Generate SQL based on the user's question and table schema."),
                    Map.of("role", "user", "content", "Question: " + feedback.getQuestion() + "\nSQL: " + feedback.getGeneratedSql()),
                    Map.of("role", "assistant", "content", feedback.getCorrectSql() != null ? feedback.getCorrectSql() : feedback.getGeneratedSql())
            ));
            return objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.warn("转换 fine-tuning 记录失败 - id: {}", feedback.getId(), e);
            return "{}";
        }
    }
}
