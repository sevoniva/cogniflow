package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户 ID 查询角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色 ID 查询权限 ID 列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
