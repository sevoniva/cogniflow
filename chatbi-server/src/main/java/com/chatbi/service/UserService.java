package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.common.exception.BusinessException;
import com.chatbi.entity.SysUser;
import com.chatbi.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据 ID 查询用户
     */
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        return user;
    }

    /**
     * 根据用户名查询用户
     */
    public SysUser getByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }

    /**
     * 创建用户
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(SysUser user) {
        // 检查用户名是否已存在
        if (sysUserMapper.selectByUsername(user.getUsername()) != null) {
            throw BusinessException.dataDuplicate("用户名");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        sysUserMapper.insert(user);
        log.info("创建用户成功：{}", user.getUsername());
        return user;
    }

    /**
     * 更新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(Long id, SysUser user) {
        SysUser existing = getById(id);

        user.setId(id);
        user.setPassword(null); // 密码不通过此接口更新
        user.setUpdatedAt(null);

        sysUserMapper.updateById(user);
        log.info("更新用户成功：{}", user.getUsername());

        return user;
    }

    /**
     * 删除用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getById(id);
        sysUserMapper.deleteById(id);
        log.info("删除用户成功：{}", user.getUsername());
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);

        SysUser updateUser = new SysUser();
        updateUser.setId(id);
        updateUser.setPassword(passwordEncoder.encode(newPassword));

        sysUserMapper.updateById(updateUser);
        log.info("重置用户密码成功：{}", user.getUsername());
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.userPasswordError();
        }

        // 更新密码
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(passwordEncoder.encode(newPassword));

        sysUserMapper.updateById(updateUser);
        log.info("修改用户密码成功：{}", user.getUsername());
    }

    /**
     * 更新用户状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysUser user = getById(id);

        SysUser updateUser = new SysUser();
        updateUser.setId(id);
        updateUser.setStatus(status);

        sysUserMapper.updateById(updateUser);
        log.info("更新用户状态成功：{} -> {}", user.getUsername(), status);
    }
}
