package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 用量记录 Mapper
 */
@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {

    @Select("SELECT COALESCE(SUM(cost), 0) FROM usage_record WHERE user_id = #{userId} AND resource_type = #{type} AND created_at >= #{since}")
    int sumCostByUserAndType(@Param("userId") Long userId, @Param("type") String type, @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM usage_record WHERE user_id = #{userId} AND resource_type = #{type} AND created_at >= #{since}")
    long countByUserAndType(@Param("userId") Long userId, @Param("type") String type, @Param("since") LocalDateTime since);
}
