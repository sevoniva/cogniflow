package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.DataSource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源 Mapper
 */
@Mapper
public interface DataSourceMapper extends BaseMapper<DataSource> {
}
