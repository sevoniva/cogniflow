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
 * AI 提供商配置。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_provider_setting")
public class AiProviderSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerKey;

    private String providerName;

    private String apiUrl;

    private String apiKeyEncrypted;

    private String model;

    private Double temperature;

    private Integer maxTokens;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
