package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.Synonym;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同义词 Mapper
 */
@Mapper
public interface SynonymMapper extends BaseMapper<Synonym> {
}
