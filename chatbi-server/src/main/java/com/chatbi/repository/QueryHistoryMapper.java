package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.QueryHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 查询历史 Mapper
 */
@Mapper
public interface QueryHistoryMapper extends BaseMapper<QueryHistory> {

    /**
     * 查询指定时间范围内 Top N 高频查询（按 query_content 分组）
     */
    @Select("""
        SELECT * FROM query_history
        WHERE created_at >= #{since}
          AND status = 'SUCCESS'
          AND query_content IS NOT NULL
        ORDER BY duration ASC
        LIMIT #{limit}
        """)
    List<QueryHistory> findTopQueries(@Param("since") LocalDateTime since, @Param("limit") int limit);
}
