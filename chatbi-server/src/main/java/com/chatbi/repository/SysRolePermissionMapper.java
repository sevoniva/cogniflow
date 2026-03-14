package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联 Mapper
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色 ID 删除角色权限关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关联
     */
    int batchInsert(@Param("list") List<SysRolePermission> list);
}
