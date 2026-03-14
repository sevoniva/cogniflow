package com.chatbi.service;

import com.chatbi.entity.SysRole;
import com.chatbi.repository.SysRoleMapper;
import com.chatbi.repository.SysUserRoleMapper;
import com.chatbi.repository.SysRolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoleService 单元测试
 */
@DisplayName("RoleService 测试")
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    @InjectMocks
    private RoleService roleService;

    private SysRole mockRole;

    @BeforeEach
    void setUp() {
        mockRole = SysRole.builder()
                .id(1L)
                .roleCode("ADMIN")
                .roleName("管理员")
                .status(1)
                .build();
    }

    @Test
    @DisplayName("获取所有角色测试")
    void testListAllRoles() {
        List<SysRole> roles = Arrays.asList(mockRole);
        when(sysRoleMapper.selectList(any())).thenReturn(roles);

        List<SysRole> result = roleService.listActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).getRoleCode());
        verify(sysRoleMapper).selectList(any());
    }

    @Test
    @DisplayName("根据 ID 获取角色测试")
    void testGetById() {
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);

        SysRole result = roleService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ADMIN", result.getRoleCode());
        verify(sysRoleMapper).selectById(1L);
    }

    @Test
    @DisplayName("新增角色测试")
    void testCreate() {
        when(sysRoleMapper.insert(mockRole)).thenReturn(1);
        when(sysRoleMapper.selectCount(any())).thenReturn(0L);

        SysRole result = roleService.create(mockRole);

        assertNotNull(result);
        verify(sysRoleMapper).insert(mockRole);
    }

    @Test
    @DisplayName("更新角色测试")
    void testUpdate() {
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);
        when(sysRoleMapper.updateById(mockRole)).thenReturn(1);

        SysRole result = roleService.update(1L, mockRole);

        assertNotNull(result);
        verify(sysRoleMapper).updateById(mockRole);
    }

    @Test
    @DisplayName("删除角色测试")
    void testDelete() {
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);
        when(sysRoleMapper.deleteById(1L)).thenReturn(1);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);

        roleService.delete(1L);

        verify(sysRoleMapper).deleteById(1L);
    }

    @Test
    @DisplayName("分配权限测试")
    void testAssignPermissions() {
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);

        roleService.assignPermissions(1L, permissionIds);

        verify(sysRolePermissionMapper).deleteByRoleId(1L);
        verify(sysRolePermissionMapper).batchInsert(anyList());
    }

    @Test
    @DisplayName("分配权限 - 空权限列表测试")
    void testAssignPermissions_Empty() {
        List<Long> permissionIds = Arrays.asList();
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);

        roleService.assignPermissions(1L, permissionIds);

        verify(sysRolePermissionMapper).deleteByRoleId(1L);
        verify(sysRolePermissionMapper, never()).batchInsert(anyList());
    }

    @Test
    @DisplayName("获取用户角色测试")
    void testListByUserId() {
        List<SysRole> roles = Arrays.asList(mockRole);
        when(sysRoleMapper.selectRolesByUserId(1L)).thenReturn(roles);

        List<SysRole> result = roleService.listByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sysRoleMapper).selectRolesByUserId(1L);
    }

    @Test
    @DisplayName("获取角色权限 ID 列表测试")
    void testGetPermissionIds() {
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        when(sysRoleMapper.selectPermissionIdsByRoleId(1L)).thenReturn(permissionIds);

        List<Long> result = roleService.getPermissionIds(1L);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(sysRoleMapper).selectPermissionIdsByRoleId(1L);
    }
}
