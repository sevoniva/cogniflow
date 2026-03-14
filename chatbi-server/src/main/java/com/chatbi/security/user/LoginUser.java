package com.chatbi.security.user;

import com.chatbi.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录用户信息
 * 实现 Spring Security 的 UserDetails 接口
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 权限列表
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 账号是否过期
     */
    private boolean accountNonExpired;

    /**
     * 账号是否锁定
     */
    private boolean accountNonLocked;

    /**
     * 凭证是否过期
     */
    private boolean credentialsNonExpired;

    /**
     * 账号是否启用
     */
    private boolean enabled;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 从 SysUser 创建 LoginUser
     */
    public static LoginUser fromUser(SysUser user, List<String> permissions) {
        List<GrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                true,
                user.getStatus() == 1,
                true,
                user.getStatus() == 1,
                user.getEmail(),
                user.getPhone(),
                user.getDeptId()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
