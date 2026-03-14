package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.Share;
import com.chatbi.repository.ShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 分享服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareMapper shareMapper;

    /**
     * 分页查询分享列表
     */
    public Page<Share> page(Long createdBy, int current, int size) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getCreatedBy, createdBy);
        wrapper.orderByDesc(Share::getCreatedAt);
        return shareMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询所有分享
     */
    public List<Share> list() {
        return shareMapper.selectList(null);
    }

    /**
     * 根据 ID 查询分享
     */
    public Share getById(Long id) {
        return shareMapper.selectById(id);
    }

    /**
     * 根据 Token 查询分享
     */
    public Share getByToken(String token) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getShareToken, token);
        return shareMapper.selectOne(wrapper);
    }

    /**
     * 创建分享
     */
    @Transactional
    public Share create(Share share) {
        // 生成分享 Token
        share.setShareToken(generateShareToken());

        // 初始化访问次数
        if (share.getCurrentVisits() == null) {
            share.setCurrentVisits(0);
        }

        // 计算过期时间
        if ("DAYS".equals(share.getValidityType()) && share.getValidityDays() != null) {
            share.setExpireTime(LocalDateTime.now().plusDays(share.getValidityDays()));
        }

        shareMapper.insert(share);
        log.info("创建分享成功：{}", share.getShareToken());
        return share;
    }

    /**
     * 更新分享
     */
    @Transactional
    public Share update(Long id, Share share) {
        Share existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("分享不存在");
        }

        share.setId(id);
        shareMapper.updateById(share);
        log.info("更新分享成功：{}", share.getShareToken());
        return share;
    }

    /**
     * 删除分享
     */
    @Transactional
    public void delete(Long id) {
        shareMapper.deleteById(id);
        log.info("删除分享成功：{}", id);
    }

    /**
     * 增加访问次数
     */
    @Transactional
    public void incrementVisits(Long id) {
        Share share = getById(id);
        if (share != null) {
            share.setCurrentVisits(share.getCurrentVisits() + 1);
            shareMapper.updateById(share);
        }
    }

    /**
     * 检查分享是否有效
     */
    public boolean isValid(Share share) {
        if (share == null || share.getStatus() != 1) {
            return false;
        }

        // 检查过期时间
        if (share.getExpireTime() != null && LocalDateTime.now().isAfter(share.getExpireTime())) {
            return false;
        }

        // 检查访问次数限制
        if (share.getMaxVisits() != null && share.getCurrentVisits() >= share.getMaxVisits()) {
            return false;
        }

        return true;
    }

    /**
     * 生成分享 Token
     */
    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
