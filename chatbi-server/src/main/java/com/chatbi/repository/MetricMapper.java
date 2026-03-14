package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.Metric;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标 Mapper
 */
@Mapper
public interface MetricMapper extends BaseMapper<Metric> {
}
