package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.common.exception.BusinessException;
import com.chatbi.entity.SysRole;
import com.chatbi.entity.SysRolePermission;
import com.chatbi.entity.SysUserRole;
import com.chatbi.repository.SysRoleMapper;
import com.chatbi.repository.SysRolePermissionMapper;
import com.chatbi.repository.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    /**
     * 分页查询角色列表
     */
    public Page<SysRole> page(String keyword, Integer status, int current, int size) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(SysRole::getRoleCode, keyword)
                    .or()
                    .like(SysRole::getRoleName, keyword)
            );
        }

        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }

        wrapper.orderByAsc(SysRole::getSortOder);

        return sysRoleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询所有启用状态的角色
     */
    public List<SysRole> listActive() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        return sysRoleMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询角色
     */
    public SysRole getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.dataNotFound();
        }
        return role;
    }

    /**
     * 根据用户 ID 查询角色列表
     */
    public List<SysRole> listByUserId(Long userId) {
        return sysRoleMapper.selectRolesByUserId(userId);
    }

    /**
     * 创建角色
     */
    @Transactional(rollbackFor = Exception.class)
    public SysRole create(SysRole role) {
        // 检查角色编码是否已存在
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw BusinessException.dataDuplicate("角色编码");
        }

        sysRoleMapper.insert(role);
        log.info("创建角色成功：{}", role.getRoleCode());
        return role;
    }

    /**
     * 更新角色
     */
    @Transactional(rollbackFor = Exception.class)
    public SysRole update(Long id, SysRole role) {
        SysRole existing = getById(id);

        role.setId(id);
        role.setUpdatedAt(null); // 由 MyBatis Plus 自动填充

        sysRoleMapper.updateById(role);
        log.info("更新角色成功：{}", role.getRoleCode());

        return role;
    }

    /**
     * 删除角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getById(id);

        // 检查是否有用户使用该角色
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getRoleId, id);
        long count = sysUserRoleMapper.selectCount(userRoleWrapper);
        if (count > 0) {
            throw BusinessException.businessError("该角色已被 " + count + " 个用户使用，无法删除");
        }

        sysRoleMapper.deleteById(id);
        log.info("删除角色成功：{}", role.getRoleCode());
    }

    /**
     * 分配权限给角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 检查角色是否存在
        SysRole role = getById(roleId);

        // 删除旧的权限关联
        sysRolePermissionMapper.deleteByRoleId(roleId);

        // 批量插入新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<SysRolePermission> permissions = new ArrayList<>();
            for (Long permissionId : permissionIds) {
                permissions.add(SysRolePermission.builder()
                        .roleId(roleId)
                        .permissionId(permissionId)
                        .build());
            }
            sysRolePermissionMapper.batchInsert(permissions);
        }

        log.info("分配权限给角色成功：roleId={}, permissions={}", roleId, permissionIds);
    }

    /**
     * 查询角色的权限 ID 列表
     */
    public List<Long> getPermissionIds(Long roleId) {
        return sysRoleMapper.selectPermissionIdsByRoleId(roleId);
    }
}
