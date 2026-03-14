package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.QueryHistory;
import com.chatbi.repository.QueryHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 查询历史服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryHistoryService {

    private final QueryHistoryMapper queryHistoryMapper;

    /**
     * 分页查询查询历史
     */
    public Page<QueryHistory> page(Long userId, Long current, Long size) {
        LambdaQueryWrapper<QueryHistory> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(QueryHistory::getUserId, userId);
        }
        wrapper.orderByDesc(QueryHistory::getCreatedAt);
        return queryHistoryMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 根据 ID 查询
     */
    public QueryHistory getById(Long id) {
        return queryHistoryMapper.selectById(id);
    }

    /**
     * 保存查询历史
     */
    @Transactional
    public QueryHistory save(QueryHistory queryHistory) {
        queryHistoryMapper.insert(queryHistory);
        return queryHistory;
    }

    /**
     * 更新查询历史
     */
    @Transactional
    public QueryHistory update(Long id, QueryHistory queryHistory) {
        QueryHistory existing = queryHistoryMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("查询历史不存在");
        }

        queryHistory.setId(id);
        queryHistoryMapper.updateById(queryHistory);
        return queryHistoryMapper.selectById(id);
    }

    /**
     * 删除查询历史
     */
    @Transactional
    public void delete(Long id) {
        queryHistoryMapper.deleteById(id);
    }

    /**
     * 收藏/取消收藏
     */
    @Transactional
    public void toggleFavorite(Long id, Boolean isFavorite) {
        QueryHistory queryHistory = queryHistoryMapper.selectById(id);
        if (queryHistory != null) {
            queryHistory.setIsFavorite(isFavorite);
            queryHistoryMapper.updateById(queryHistory);
        }
    }

    /**
     * 查询收藏列表
     */
    public List<QueryHistory> getFavorites(Long userId) {
        LambdaQueryWrapper<QueryHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QueryHistory::getUserId, userId)
                .eq(QueryHistory::getIsFavorite, true)
                .orderByDesc(QueryHistory::getCreatedAt);
        return queryHistoryMapper.selectList(wrapper);
    }

    /**
     * 获取最近查询
     */
    public List<QueryHistory> getRecent(Long userId, Integer limit) {
        LambdaQueryWrapper<QueryHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QueryHistory::getUserId, userId)
                .orderByDesc(QueryHistory::getCreatedAt);
        Page<QueryHistory> page = queryHistoryMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords();
    }
}
