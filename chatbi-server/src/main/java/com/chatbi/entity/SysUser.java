package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt 加密）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 性别 (0 女 1 男 2 未知)
     */
    private Integer gender;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 状态 (0 禁用 1 正常)
     */
    private Integer status;

    /**
     * 是否管理员 (0 否 1 是)
     */
    private Integer isAdmin;

    /**
     * 最后登录 IP
     */
    private String lastLoginIp;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 密码最后修改时间
     */
    private LocalDateTime pwdResetTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

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
     * 删除时间（软删除）
     */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;

    /**
     * 创建用户时调用
     */
    public static SysUser create(String username, String password, String nickName) {
        return SysUser.builder()
                .username(username)
                .password(password)
                .nickName(nickName)
                .status(1)
                .gender(0)
                .isAdmin(0)
                .build();
    }
}
