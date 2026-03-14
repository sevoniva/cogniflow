package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.QueryFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 查询收藏 Mapper
 */
@Mapper
public interface QueryFavoriteMapper extends BaseMapper<QueryFavorite> {
}
