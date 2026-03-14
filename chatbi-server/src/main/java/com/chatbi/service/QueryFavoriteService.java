package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.QueryFavorite;
import com.chatbi.repository.QueryFavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 查询收藏服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryFavoriteService {

    private final QueryFavoriteMapper queryFavoriteMapper;

    /**
     * 获取用户收藏列表
     */
    public List<QueryFavorite> getUserFavorites(Long userId) {
        LambdaQueryWrapper<QueryFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QueryFavorite::getUserId, userId)
                .orderByDesc(QueryFavorite::getCreatedAt);
        return queryFavoriteMapper.selectList(wrapper);
    }

    /**
     * 添加收藏
     */
    @Transactional
    public QueryFavorite addFavorite(QueryFavorite queryFavorite) {
        queryFavoriteMapper.insert(queryFavorite);
        return queryFavorite;
    }

    /**
     * 删除收藏
     */
    @Transactional
    public void removeFavorite(Long id) {
        queryFavoriteMapper.deleteById(id);
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorite(Long userId, Long queryHistoryId) {
        LambdaQueryWrapper<QueryFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QueryFavorite::getUserId, userId)
                .eq(QueryFavorite::getQueryHistoryId, queryHistoryId);
        return queryFavoriteMapper.selectCount(wrapper) > 0;
    }
}
