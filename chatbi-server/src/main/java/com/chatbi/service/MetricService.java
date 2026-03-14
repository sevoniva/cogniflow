package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 指标服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private final MetricMapper metricMapper;

    /**
     * 查询所有指标
     */
    public List<Metric> list() {
        return metricMapper.selectList(null);
    }

    /**
     * 查询启用的指标
     */
    public List<Metric> listActiveMetrics() {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getStatus, "active");
        return metricMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询指标
     */
    public Metric getById(Long id) {
        return metricMapper.selectById(id);
    }

    /**
     * 根据编码查询指标
     */
    public Metric getByCode(String code) {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getCode, code);
        return metricMapper.selectOne(wrapper);
    }

    /**
     * 创建指标
     */
    public Metric create(Metric metric) {
        metricMapper.insert(metric);
        log.info("创建指标成功：{}", metric.getCode());
        return metric;
    }

    /**
     * 更新指标
     */
    public Metric update(Long id, Metric metric) {
        Metric existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("指标不存在");
        }

        metric.setId(id);
        metricMapper.updateById(metric);
        log.info("更新指标成功：{}", metric.getCode());
        return metric;
    }

    /**
     * 删除指标
     */
    public void delete(Long id) {
        metricMapper.deleteById(id);
        log.info("删除指标成功：{}", id);
    }

    /**
     * 切换指标状态
     */
    public Metric toggleStatus(Long id) {
        Metric metric = getById(id);
        if (metric == null) {
            throw new RuntimeException("指标不存在");
        }

        String newStatus = "active".equals(metric.getStatus()) ? "inactive" : "active";
        metric.setStatus(newStatus);
        metricMapper.updateById(metric);
        log.info("切换指标状态成功：{} -> {}", metric.getCode(), newStatus);
        return metric;
    }
}
