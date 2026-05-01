package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 反馈 Mapper
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("SELECT * FROM feedback WHERE exported = false AND created_at >= #{startTime} AND created_at < #{endTime} ORDER BY created_at")
    List<Feedback> findUnexportedByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE feedback SET exported = true WHERE exported = false AND created_at >= #{startTime} AND created_at < #{endTime}")
    int markExportedByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM feedback WHERE exported = false")
    long countUnexported();
}
