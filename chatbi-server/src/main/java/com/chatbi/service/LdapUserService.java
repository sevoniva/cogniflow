package com.chatbi.service;

import com.chatbi.entity.SysUser;
import com.chatbi.repository.SysUserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LDAP 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LdapUserService {

    private final LdapTemplate ldapTemplate;
    private final SysUserMapper sysUserMapper;

    /**
     * LDAP 用户属性
     */
    public static class LdapUser {
        private String dn;
        private String uid;
        private String cn;
        private String sn;
        private String mail;
        private String telephoneNumber;
        private String title;
        private String department;

        public String getDn() { return dn; }
        public void setDn(String dn) { this.dn = dn; }
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getCn() { return cn; }
        public void setCn(String cn) { this.cn = cn; }
        public String getSn() { return sn; }
        public void setSn(String sn) { this.sn = sn; }
        public String getMail() { return mail; }
        public void setMail(String mail) { this.mail = mail; }
        public String getTelephoneNumber() { return telephoneNumber; }
        public void setTelephoneNumber(String telephoneNumber) { this.telephoneNumber = telephoneNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }

    /**
     * LDAP 认证 - 验证用户名密码
     */
    public boolean authenticate(String username, String password) {
        try {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", "person"))
                  .and(new EqualsFilter("uid", username));

            LdapQuery query = LdapQueryBuilder.query().filter(filter);
            List<LdapUser> users = ldapTemplate.find(query, LdapUser.class);

            if (users.isEmpty()) {
                log.warn("LDAP 用户不存在：{}", username);
                return false;
            }

            LdapUser ldapUser = users.get(0);
            log.info("找到 LDAP 用户：{}, {}", ldapUser.getUid(), ldapUser.getCn());

            // 尝试使用提供的密码进行绑定认证
            ldapTemplate.authenticate(
                LdapQueryBuilder.query().where("uid").is(username),
                password
            );

            log.info("LDAP 用户认证成功：{}", username);
            return true;

        } catch (Exception e) {
            log.warn("LDAP 认证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据用户名获取 LDAP 用户信息
     */
    public LdapUser getUserByUsername(String username) {
        try {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", "person"))
                  .and(new EqualsFilter("uid", username));

            LdapQuery query = LdapQueryBuilder.query().filter(filter);
            List<LdapUser> users = ldapTemplate.find(query, LdapUser.class);

            return users.isEmpty() ? null : users.get(0);

        } catch (Exception e) {
            log.error("获取 LDAP 用户失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 搜索 LDAP 用户
     */
    public List<LdapUser> searchUsers(String filter) {
        try {
            LdapQuery query = LdapQueryBuilder.query()
                .filter(new EqualsFilter("objectclass", "person"));
            return ldapTemplate.find(query, LdapUser.class);
        } catch (Exception e) {
            log.error("搜索 LDAP 用户失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 同步 LDAP 用户到本地数据库
     */
    public SysUser syncUserToDatabase(LdapUser ldapUser) {
        // 先查找是否存在
        SysUser existingUser = sysUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, ldapUser.getUid())
        );

        if (existingUser != null) {
            // 更新用户信息
            existingUser.setNickName(ldapUser.getCn());
            existingUser.setEmail(ldapUser.getMail());
            existingUser.setPhone(ldapUser.getTelephoneNumber());
            sysUserMapper.updateById(existingUser);
            log.info("更新本地用户：{}", ldapUser.getUid());
            return existingUser;
        }

        // 创建新用户
        SysUser newUser = new SysUser();
        newUser.setUsername(ldapUser.getUid());
        newUser.setNickName(ldapUser.getCn() != null ? ldapUser.getCn() : ldapUser.getUid());
        newUser.setEmail(ldapUser.getMail());
        newUser.setPhone(ldapUser.getTelephoneNumber());
        newUser.setStatus(1);

        sysUserMapper.insert(newUser);
        log.info("创建本地用户：{}", ldapUser.getUid());

        return newUser;
    }

    /**
     * LDAP 登录 - 认证并同步用户
     */
    public SysUser ldapLogin(String username, String password) {
        // 1. LDAP 认证
        if (!authenticate(username, password)) {
            return null;
        }

        // 2. 获取用户信息
        LdapUser ldapUser = getUserByUsername(username);
        if (ldapUser == null) {
            return null;
        }

        // 3. 同步到本地数据库
        return syncUserToDatabase(ldapUser);
    }
}
