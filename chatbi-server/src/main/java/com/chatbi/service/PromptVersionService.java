package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.PromptVersion;
import com.chatbi.repository.PromptVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 版本管理服务
 *
 * 支持 CRUD、版本切换、灰度 A/B 测试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptVersionService {

    private final PromptVersionMapper promptVersionMapper;

    /**
     * 创建 Prompt 版本
     */
    @Transactional
    public PromptVersion create(PromptVersion version) {
        if (version.getStatus() == null) {
            version.setStatus("draft");
        }
        if (version.getGrayScalePercent() == null) {
            version.setGrayScalePercent(0);
        }
        promptVersionMapper.insert(version);
        log.info("创建 Prompt 版本 - id: {}, name: {}, tag: {}", version.getId(), version.getName(), version.getVersionTag());
        return version;
    }

    /**
     * 更新 Prompt 版本
     */
    @Transactional
    public PromptVersion update(Long id, PromptVersion version) {
        PromptVersion existing = promptVersionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("Prompt 版本不存在: " + id);
        }
        version.setId(id);
        promptVersionMapper.updateById(version);
        log.info("更新 Prompt 版本 - id: {}, name: {}", id, version.getName());
        return promptVersionMapper.selectById(id);
    }

    /**
     * 删除 Prompt 版本（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        promptVersionMapper.deleteById(id);
        log.info("删除 Prompt 版本 - id: {}", id);
    }

    /**
     * 根据 ID 查询
     */
    public PromptVersion getById(Long id) {
        return promptVersionMapper.selectById(id);
    }

    /**
     * 分页查询
     */
    public Page<PromptVersion> page(int current, int size, String status) {
        LambdaQueryWrapper<PromptVersion> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(PromptVersion::getStatus, status);
        }
        wrapper.orderByDesc(PromptVersion::getUpdatedAt);
        return promptVersionMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询所有非废弃版本
     */
    public List<PromptVersion> listAllActiveOrDraft() {
        return promptVersionMapper.findAllActiveOrDraft();
    }

    /**
     * 切换生效版本
     *
     * 将指定版本置为 active，其他 active 版本自动置为 deprecated。
     */
    @Transactional
    public PromptVersion activate(Long id) {
        PromptVersion version = promptVersionMapper.selectById(id);
        if (version == null) {
            throw new RuntimeException("Prompt 版本不存在: " + id);
        }
        // 将其他 active 版本置为 deprecated
        promptVersionMapper.deprecateOthers(id);
        // 将当前版本置为 active
        version.setStatus("active");
        promptVersionMapper.updateById(version);
        log.info("激活 Prompt 版本 - id: {}, tag: {}", id, version.getVersionTag());
        return version;
    }

    /**
     * 复制版本（快速迭代）
     */
    @Transactional
    public PromptVersion duplicate(Long id) {
        PromptVersion source = promptVersionMapper.selectById(id);
        if (source == null) {
            throw new RuntimeException("Prompt 版本不存在: " + id);
        }
        PromptVersion copy = PromptVersion.builder()
                .name(source.getName() + " (副本)")
                .versionTag(source.getVersionTag() + "-copy-" + System.currentTimeMillis())
                .template(source.getTemplate())
                .variables(source.getVariables())
                .status("draft")
                .grayScalePercent(0)
                .description(source.getDescription())
                .build();
        promptVersionMapper.insert(copy);
        log.info("复制 Prompt 版本 - from: {}, to: {}", id, copy.getId());
        return copy;
    }

    /**
     * 根据用户 ID 获取应使用的 Prompt 版本
     *
     * 灰度策略：userId % 100 < grayScalePercent 则使用灰度版本，否则使用稳定版本。
     */
    public Optional<PromptVersion> resolveForUser(Long userId) {
        List<PromptVersion> versions = promptVersionMapper.findAllActiveOrDraft();
        if (versions.isEmpty()) {
            return Optional.empty();
        }

        // 找 active 状态且灰度比例 > 0 的版本（A/B 测试版本）
        Optional<PromptVersion> grayVersion = versions.stream()
                .filter(v -> "active".equals(v.getStatus()) && v.getGrayScalePercent() != null && v.getGrayScalePercent() > 0)
                .findFirst();

        // 找 active 状态且灰度比例 = 0 的版本（稳定版本）
        Optional<PromptVersion> stableVersion = versions.stream()
                .filter(v -> "active".equals(v.getStatus()) && (v.getGrayScalePercent() == null || v.getGrayScalePercent() == 0))
                .findFirst();

        if (grayVersion.isEmpty()) {
            return stableVersion;
        }
        if (stableVersion.isEmpty()) {
            return grayVersion;
        }

        // 灰度命中判断
        long hash = Math.abs(userId % 100);
        boolean inGray = hash < grayVersion.get().getGrayScalePercent();
        if (inGray) {
            log.debug("用户命中灰度 Prompt - userId: {}, version: {}", userId, grayVersion.get().getVersionTag());
            return grayVersion;
        }
        return stableVersion;
    }

    /**
     * 获取最新的 active Prompt（不区分用户，用于系统内部调用）
     */
    public Optional<PromptVersion> getLatestActive() {
        return promptVersionMapper.findLatestActive();
    }
}
