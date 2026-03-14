package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.QueryHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 查询历史 Mapper
 */
@Mapper
public interface QueryHistoryMapper extends BaseMapper<QueryHistory> {
}
