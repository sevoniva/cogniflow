package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.config.AiConfig;
import com.chatbi.entity.AiProviderSetting;
import com.chatbi.entity.AiRuntimeSetting;
import com.chatbi.repository.AiProviderSettingMapper;
import com.chatbi.repository.AiRuntimeSettingMapper;
import com.chatbi.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 配置持久化服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderSettingService {

    private static final long RUNTIME_SETTING_ID = 1L;

    private final AiRuntimeSettingMapper runtimeSettingMapper;
    private final AiProviderSettingMapper providerSettingMapper;
    private final AiConfig aiConfig;

    @Value("${app.jwt.secret:chatbi-enterprise-secret-key-min-256-bit-for-jwt-signing}")
    private String encryptionSecret;

    public record ProviderMutation(
        String name,
        String apiUrl,
        String model,
        Boolean enabled,
        String apiKey,
        Boolean clearApiKey,
        Double temperature,
        Integer maxTokens
    ) {}

    public synchronized void initializeRuntimeSettings() {
        ensureRuntimeSetting();
        ensureProviderSettings();
        applyPersistedSettings();
    }

    public synchronized void updateRuntime(Boolean enabled) {
        initializeRuntimeSettings();
        AiRuntimeSetting runtimeSetting = getRuntimeSetting();
        if (enabled != null) {
            runtimeSetting.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        }
        saveRuntimeSetting(runtimeSetting);
        applyPersistedSettings();
    }

    public synchronized void switchDefaultProvider(String provider) {
        initializeRuntimeSettings();
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("默认提供商不能为空");
        }

        AiProviderSetting target = getProviderSetting(provider);
        if (target == null) {
            throw new IllegalArgumentException("不支持的AI模型提供商: " + provider);
        }

        AiRuntimeSetting runtimeSetting = getRuntimeSetting();
        runtimeSetting.setDefaultProvider(provider);
        saveRuntimeSetting(runtimeSetting);
        applyPersistedSettings();
    }

    public synchronized void updateProvider(String providerKey, ProviderMutation mutation) {
        initializeRuntimeSettings();
        if (!StringUtils.hasText(providerKey)) {
            throw new IllegalArgumentException("提供商标识不能为空");
        }

        AiProviderSetting current = getProviderSetting(providerKey);
        if (current == null) {
            current = AiProviderSetting.builder()
                .providerKey(providerKey)
                .providerName(providerKey)
                .enabled(0)
                .build();
        }

        if (mutation != null) {
            if (mutation.name() != null) {
                current.setProviderName(mutation.name().trim());
            }
            if (mutation.apiUrl() != null) {
                current.setApiUrl(mutation.apiUrl().trim());
            }
            if (mutation.model() != null) {
                current.setModel(mutation.model().trim());
            }
            if (mutation.enabled() != null) {
                current.setEnabled(Boolean.TRUE.equals(mutation.enabled()) ? 1 : 0);
            }
            if (mutation.temperature() != null) {
                current.setTemperature(mutation.temperature());
            }
            if (mutation.maxTokens() != null) {
                current.setMaxTokens(mutation.maxTokens());
            }
            if (Boolean.TRUE.equals(mutation.clearApiKey())) {
                current.setApiKeyEncrypted(null);
            } else if (StringUtils.hasText(mutation.apiKey())) {
                current.setApiKeyEncrypted(EncryptionUtils.encrypt(mutation.apiKey().trim(), encryptionSecret));
            }
        }

        if (current.getId() == null) {
            providerSettingMapper.insert(current);
        } else {
            providerSettingMapper.updateById(current);
        }
        applyPersistedSettings();
    }

    public synchronized AiProviderSetting getProviderSetting(String providerKey) {
        if (!StringUtils.hasText(providerKey)) {
            return null;
        }
        LambdaQueryWrapper<AiProviderSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderSetting::getProviderKey, providerKey).last("LIMIT 1");
        return providerSettingMapper.selectOne(wrapper);
    }

    public synchronized List<AiProviderSetting> listProviderSettings() {
        initializeRuntimeSettings();
        return providerSettingMapper.selectList(new LambdaQueryWrapper<AiProviderSetting>()
            .orderByAsc(AiProviderSetting::getId));
    }

    private void ensureRuntimeSetting() {
        AiRuntimeSetting runtimeSetting = runtimeSettingMapper.selectById(RUNTIME_SETTING_ID);
        if (runtimeSetting != null) {
            return;
        }

        runtimeSettingMapper.insert(AiRuntimeSetting.builder()
            .id(RUNTIME_SETTING_ID)
            .enabled(aiConfig.isEnabled() ? 1 : 0)
            .defaultProvider(aiConfig.getDefaultProvider())
            .build());
    }

    private void ensureProviderSettings() {
        Map<String, AiConfig.ProviderConfig> configuredProviders = aiConfig.getProviders();
        if (configuredProviders == null || configuredProviders.isEmpty()) {
            return;
        }

        configuredProviders.forEach((providerKey, config) -> {
            if (getProviderSetting(providerKey) != null) {
                return;
            }
            providerSettingMapper.insert(AiProviderSetting.builder()
                .providerKey(providerKey)
                .providerName(StringUtils.hasText(config.getName()) ? config.getName() : providerKey)
                .apiUrl(config.getApiUrl())
                .apiKeyEncrypted(StringUtils.hasText(config.getApiKey())
                    ? EncryptionUtils.encrypt(config.getApiKey(), encryptionSecret)
                    : null)
                .model(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .enabled(config.isEnabled() ? 1 : 0)
                .build());
        });
    }

    private void applyPersistedSettings() {
        AiRuntimeSetting runtimeSetting = getRuntimeSetting();
        List<AiProviderSetting> settings = providerSettingMapper.selectList(new LambdaQueryWrapper<AiProviderSetting>()
            .orderByAsc(AiProviderSetting::getId));

        Map<String, AiConfig.ProviderConfig> mergedProviders = new LinkedHashMap<>();
        if (aiConfig.getProviders() != null) {
            aiConfig.getProviders().forEach((key, value) -> mergedProviders.put(key, cloneProvider(value)));
        }

        for (AiProviderSetting setting : settings) {
            AiConfig.ProviderConfig target = mergedProviders.computeIfAbsent(setting.getProviderKey(), key -> new AiConfig.ProviderConfig());
            if (StringUtils.hasText(setting.getProviderName())) {
                target.setName(setting.getProviderName());
            }
            if (StringUtils.hasText(setting.getApiUrl())) {
                target.setApiUrl(setting.getApiUrl());
            }
            if (StringUtils.hasText(setting.getModel())) {
                target.setModel(setting.getModel());
            }
            if (setting.getTemperature() != null) {
                target.setTemperature(setting.getTemperature());
            }
            if (setting.getMaxTokens() != null) {
                target.setMaxTokens(setting.getMaxTokens());
            }
            target.setEnabled(Objects.equals(setting.getEnabled(), 1));
            target.setApiKey(decrypt(setting.getApiKeyEncrypted()));
        }

        aiConfig.setProviders(mergedProviders);
        aiConfig.setEnabled(Objects.equals(runtimeSetting.getEnabled(), 1));

        String defaultProvider = runtimeSetting.getDefaultProvider();
        if (!StringUtils.hasText(defaultProvider) || !mergedProviders.containsKey(defaultProvider)) {
            defaultProvider = mergedProviders.keySet().stream().findFirst().orElse(aiConfig.getDefaultProvider());
            runtimeSetting.setDefaultProvider(defaultProvider);
            saveRuntimeSetting(runtimeSetting);
        }
        aiConfig.setDefaultProvider(defaultProvider);
    }

    private AiRuntimeSetting getRuntimeSetting() {
        AiRuntimeSetting runtimeSetting = runtimeSettingMapper.selectById(RUNTIME_SETTING_ID);
        if (runtimeSetting == null) {
            throw new IllegalStateException("AI 运行配置不存在");
        }
        return runtimeSetting;
    }

    private void saveRuntimeSetting(AiRuntimeSetting runtimeSetting) {
        if (runtimeSettingMapper.selectById(runtimeSetting.getId()) == null) {
            runtimeSettingMapper.insert(runtimeSetting);
        } else {
            runtimeSettingMapper.updateById(runtimeSetting);
        }
    }

    private AiConfig.ProviderConfig cloneProvider(AiConfig.ProviderConfig source) {
        AiConfig.ProviderConfig target = new AiConfig.ProviderConfig();
        target.setName(source.getName());
        target.setApiUrl(source.getApiUrl());
        target.setApiKey(source.getApiKey());
        target.setModel(source.getModel());
        target.setTemperature(source.getTemperature());
        target.setMaxTokens(source.getMaxTokens());
        target.setEnabled(source.isEnabled());
        target.setExtraParams(source.getExtraParams() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getExtraParams()));
        return target;
    }

    private String decrypt(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            return "";
        }
        try {
            return EncryptionUtils.decrypt(encryptedValue, encryptionSecret);
        } catch (Exception ex) {
            log.warn("AI 提供商密钥解密失败，providerKey 配置将按未配置处理");
            return "";
        }
    }
}
