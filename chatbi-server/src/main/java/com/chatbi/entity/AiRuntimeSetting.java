package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 运行时设置。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_runtime_setting")
public class AiRuntimeSetting {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Integer enabled;

    private String defaultProvider;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
