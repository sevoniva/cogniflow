package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统权限实体
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_permission")
public class SysPermission {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限编码
     */
    private String permCode;

    /**
     * 权限名称
     */
    private String permName;

    /**
     * 资源类型 (MENU/BUTTON/API)
     */
    private String resourceType;

    /**
     * 资源路径
     */
    private String resourcePath;

    /**
     * 父 ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sortOder;

    /**
     * 图标
     */
    private String icon;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 权限创建方法
     */
    public static SysPermission create(String permCode, String permName, String resourceType) {
        return SysPermission.builder()
                .permCode(permCode)
                .permName(permName)
                .resourceType(resourceType)
                .status(1)
                .parentId(0L)
                .sortOder(0)
                .build();
    }
}
