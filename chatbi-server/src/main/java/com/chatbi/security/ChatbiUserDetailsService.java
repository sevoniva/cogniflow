package com.chatbi.security;

import com.chatbi.entity.SysUser;
import com.chatbi.entity.SysPermission;
import com.chatbi.repository.SysPermissionMapper;
import com.chatbi.repository.SysUserMapper;
import com.chatbi.security.user.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户详情服务
 */
@Service
@RequiredArgsConstructor
public class ChatbiUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(user.getId());
        if ((permissionCodes == null || permissionCodes.isEmpty()) && Integer.valueOf(1).equals(user.getIsAdmin())) {
            LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysPermission::getStatus, 1);
            permissionCodes = sysPermissionMapper.selectList(wrapper).stream()
                    .map(SysPermission::getPermCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (permissionCodes == null || permissionCodes.isEmpty()) {
            permissionCodes = List.of("ROLE_USER");
        }

        List<SimpleGrantedAuthority> authorities = permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                true,                           // accountNonExpired
                user.getStatus() == 1,         // accountNonLocked
                true,                           // credentialsNonExpired
                user.getStatus() == 1,         // enabled
                user.getEmail(),
                user.getPhone(),
                user.getDeptId()
        );
    }
}
