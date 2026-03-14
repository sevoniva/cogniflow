package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.EmbeddedAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 嵌入访问日志 Mapper。
 */
@Mapper
public interface EmbeddedAccessLogMapper extends BaseMapper<EmbeddedAccessLog> {
}
