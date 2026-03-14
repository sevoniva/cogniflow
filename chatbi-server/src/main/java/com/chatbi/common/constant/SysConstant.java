package com.chatbi.common.constant;

/**
 * 系统常量
 */
public class SysConstant {

    private SysConstant() {
        throw new IllegalStateException("Constant class");
    }

    // ========== 通用常量 ==========

    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 默认分页大小
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    /**
     * 默认页码
     */
    public static final Integer DEFAULT_PAGE_NUM = 1;

    // ========== 状态常量 ==========

    /**
     * 正常状态
     */
    public static final Integer STATUS_NORMAL = 1;

    /**
     * 禁用状态
     */
    public static final Integer STATUS_DISABLED = 0;

    /**
     * 成功状态
     */
    public static final Integer SUCCESS = 1;

    /**
     * 失败状态
     */
    public static final Integer FAIL = 0;

    // ========== 用户相关常量 ==========

    /**
     * 默认密码（重置密码时使用）
     */
    public static final String DEFAULT_PASSWORD = "Password@123";

    /**
     * 超级管理员 ID
     */
    public static final Long SUPER_ADMIN_ID = 1L;

    /**
     * 超级管理员用户名
     */
    public static final String SUPER_ADMIN_USERNAME = "admin";

    /**
     * 匿名用户 ID
     */
    public static final Long ANONYMOUS_ID = 0L;

    /**
     * 匿名用户名称
     */
    public static final String ANONYMOUS_USERNAME = "anonymous";

    // ========== 性别常量 ==========

    /**
     * 未知性别
     */
    public static final Integer GENDER_UNKNOWN = 0;

    /**
     * 男性
     */
    public static final Integer GENDER_MALE = 1;

    /**
     * 女性
     */
    public static final Integer GENDER_FEMALE = 2;

    // ========== 资源类型常量 ==========

    /**
     * 菜单
     */
    public static final String RESOURCE_TYPE_MENU = "MENU";

    /**
     * 按钮
     */
    public static final String RESOURCE_TYPE_BUTTON = "BUTTON";

    /**
     * API 接口
     */
    public static final String RESOURCE_TYPE_API = "API";

    // ========== 数据范围常量 ==========

    /**
     * 全部数据权限
     */
    public static final Integer DATA_SCOPE_ALL = 1;

    /**
     * 本部门数据权限
     */
    public static final Integer DATA_SCOPE_DEPT = 2;

    /**
     * 本部门及以下数据权限
     */
    public static final Integer DATA_SCOPE_DEPT_AND_CHILD = 3;

    /**
     * 仅本人数据权限
     */
    public static final Integer DATA_SCOPE_SELF = 4;

    // ========== Token 相关常量 ==========

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token 请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token 缓存前缀
     */
    public static final String TOKEN_CACHE_PREFIX = "token:";

    /**
     * 登录用户信息 Key
     */
    public static final String LOGIN_USER_KEY = "loginUser";

    // ========== 缓存相关常量 ==========

    /**
     * 用户信息缓存
     */
    public static final String CACHE_USER_INFO = "user:info:";

    /**
     * 用户权限缓存
     */
    public static final String CACHE_USER_PERMISSIONS = "user:permissions:";

    /**
     * 验证码缓存
     */
    public static final String CACHE_CAPTCHA = "captcha:";

    /**
     * 登录失败次数缓存
     */
    public static final String CACHE_LOGIN_FAIL = "login:fail:";

    // ========== 审计日志常量 ==========

    /**
     * 操作类型 - 登录
     */
    public static final String ACTION_LOGIN = "LOGIN";

    /**
     * 操作类型 - 登出
     */
    public static final String ACTION_LOGOUT = "LOGOUT";

    /**
     * 操作类型 - 新增
     */
    public static final String ACTION_ADD = "ADD";

    /**
     * 操作类型 - 修改
     */
    public static final String ACTION_UPDATE = "UPDATE";

    /**
     * 操作类型 - 删除
     */
    public static final String ACTION_DELETE = "DELETE";

    /**
     * 操作类型 - 查询
     */
    public static final String ACTION_QUERY = "QUERY";

    /**
     * 操作类型 - 导出
     */
    public static final String ACTION_EXPORT = "EXPORT";

    /**
     * 操作类型 - 导入
     */
    public static final String ACTION_IMPORT = "IMPORT";

    // ========== 审计结果常量 ==========

    /**
     * 操作成功
     */
    public static final String RESULT_SUCCESS = "SUCCESS";

    /**
     * 操作失败
     */
    public static final String RESULT_FAILED = "FAILED";
}
