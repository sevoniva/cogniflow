package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.Dashboard;
import com.chatbi.repository.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仪表板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    /**
     * 分页查询仪表板
     */
    public List<Dashboard> list(Long createdBy, Boolean isPublic) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        if (createdBy != null) {
            wrapper.eq(Dashboard::getCreatedBy, createdBy);
        }
        if (isPublic != null && isPublic) {
            wrapper.eq(Dashboard::getIsPublic, true);
        }
        wrapper.orderByDesc(Dashboard::getCreatedAt);
        return dashboardMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询
     */
    public Dashboard getById(Long id) {
        return dashboardMapper.selectById(id);
    }

    /**
     * 创建仪表板
     */
    @Transactional
    public Dashboard create(Dashboard dashboard) {
        dashboardMapper.insert(dashboard);
        return dashboard;
    }

    /**
     * 更新仪表板
     */
    @Transactional
    public Dashboard update(Long id, Dashboard dashboard) {
        Dashboard existing = dashboardMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("仪表板不存在");
        }
        dashboard.setId(id);
        dashboardMapper.updateById(dashboard);
        return dashboard;
    }

    /**
     * 删除仪表板
     */
    @Transactional
    public void delete(Long id) {
        dashboardMapper.deleteById(id);
    }

    /**
     * 发布/取消发布
     */
    @Transactional
    public void togglePublish(Long id, Integer status) {
        Dashboard dashboard = dashboardMapper.selectById(id);
        if (dashboard != null) {
            dashboard.setStatus(status);
            dashboardMapper.updateById(dashboard);
        }
    }

    /**
     * 更新布局配置
     */
    @Transactional
    public Dashboard updateLayout(Long id, String layoutConfig, String chartsConfig) {
        Dashboard dashboard = dashboardMapper.selectById(id);
        if (dashboard != null) {
            dashboard.setLayoutConfig(layoutConfig);
            dashboard.setChartsConfig(chartsConfig);
            dashboardMapper.updateById(dashboard);
        }
        return dashboard;
    }
}
