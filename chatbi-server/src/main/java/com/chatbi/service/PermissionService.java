package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.common.exception.BusinessException;
import com.chatbi.entity.SysPermission;
import com.chatbi.repository.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    /**
     * 查询权限树
     */
    public List<SysPermission> tree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, 1);
        wrapper.orderByAsc(SysPermission::getSortOder);
        return sysPermissionMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询权限
     */
    public SysPermission getById(Long id) {
        SysPermission permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw BusinessException.dataNotFound();
        }
        return permission;
    }

    /**
     * 创建权限
     */
    @Transactional(rollbackFor = Exception.class)
    public SysPermission create(SysPermission permission) {
        // 检查权限编码是否已存在
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermCode, permission.getPermCode());
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw BusinessException.dataDuplicate("权限编码");
        }

        sysPermissionMapper.insert(permission);
        log.info("创建权限成功：{}", permission.getPermCode());
        return permission;
    }

    /**
     * 更新权限
     */
    @Transactional(rollbackFor = Exception.class)
    public SysPermission update(Long id, SysPermission permission) {
        SysPermission existing = getById(id);

        permission.setId(id);

        sysPermissionMapper.updateById(permission);
        log.info("更新权限成功：{}", permission.getPermCode());

        return permission;
    }

    /**
     * 删除权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysPermission permission = getById(id);
        sysPermissionMapper.deleteById(id);
        log.info("删除权限成功：{}", permission.getPermCode());
    }
}
