package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅 Mapper
 */
@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {
}
