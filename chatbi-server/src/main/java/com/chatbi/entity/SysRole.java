package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统角色实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role")
public class SysRole {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 描述
     */
    private String description;

    /**
     * 数据范围 (1 全部 2 本部门 3 本人)
     */
    private Integer dataScope;

    /**
     * 状态 (0 禁用 1 正常)
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 关联的权限 ID 列表
     */
    @TableField(exist = false)
    private List<Long> permissionIds;

    /**
     * 创建角色时调用
     */
    public static SysRole create(String roleCode, String roleName) {
        return SysRole.builder()
                .roleCode(roleCode)
                .roleName(roleName)
                .status(1)
                .dataScope(1)
                .sortOder(0)
                .build();
    }
}
