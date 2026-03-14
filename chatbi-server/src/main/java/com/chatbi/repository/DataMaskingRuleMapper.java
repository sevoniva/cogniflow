package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.DataMaskingRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据脱敏规则 Mapper
 */
@Mapper
public interface DataMaskingRuleMapper extends BaseMapper<DataMaskingRule> {
}
