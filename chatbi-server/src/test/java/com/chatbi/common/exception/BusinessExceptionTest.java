package com.chatbi.common.exception;

import com.chatbi.common.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 单元测试
 */
@DisplayName("BusinessException 测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("构造异常 - 只有消息")
    void testConstructorWithMessage() {
        BusinessException exception = new BusinessException("测试异常消息");

        assertEquals(HttpStatus.ERROR, exception.getCode());
        assertEquals("测试异常消息", exception.getMessage());
    }

    @Test
    @DisplayName("构造异常 - 错误码和消息")
    void testConstructorWithCodeAndMessage() {
        BusinessException exception = new BusinessException(400, "自定义错误消息");

        assertEquals(400, exception.getCode());
        assertEquals("自定义错误消息", exception.getMessage());
    }

    @Test
    @DisplayName("构造异常 - 消息和原因")
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("原因");
        BusinessException exception = new BusinessException("测试异常", cause);

        assertEquals(HttpStatus.ERROR, exception.getCode());
        assertEquals("测试异常", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("快捷方法 - 用户不存在")
    void testUserNotFound() {
        BusinessException exception = BusinessException.userNotFound();

        assertEquals(HttpStatus.USER_NOT_FOUND, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 密码错误")
    void testUserPasswordError() {
        BusinessException exception = BusinessException.userPasswordError();

        assertEquals(HttpStatus.USER_PASSWORD_ERROR, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 用户已锁定")
    void testUserLocked() {
        BusinessException exception = BusinessException.userLocked();

        assertEquals(HttpStatus.USER_LOCKED, exception.getCode());
        assertEquals("用户已被锁定，请稍后再试", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 用户已禁用")
    void testUserDisabled() {
        BusinessException exception = BusinessException.userDisabled();

        assertEquals(HttpStatus.USER_DISABLED, exception.getCode());
        assertEquals("用户已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - Token 无效")
    void testTokenInvalid() {
        BusinessException exception = BusinessException.tokenInvalid();

        assertEquals(HttpStatus.TOKEN_INVALID, exception.getCode());
        assertEquals("Token 无效", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - Token 已过期")
    void testTokenExpired() {
        BusinessException exception = BusinessException.tokenExpired();

        assertEquals(HttpStatus.TOKEN_EXPIRED, exception.getCode());
        assertEquals("Token 已过期", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - Token 不能为空")
    void testTokenMissing() {
        BusinessException exception = BusinessException.tokenMissing();

        assertEquals(HttpStatus.TOKEN_MISSING, exception.getCode());
        assertEquals("Token 不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 权限不足")
    void testPermissionDenied() {
        BusinessException exception = BusinessException.permissionDenied();

        assertEquals(HttpStatus.PERMISSION_DENIED, exception.getCode());
        assertEquals("权限不足", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 数据不存在")
    void testDataNotFound() {
        BusinessException exception = BusinessException.dataNotFound();

        assertEquals(HttpStatus.DATA_NOT_FOUND, exception.getCode());
        assertEquals("数据不存在", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 数据重复")
    void testDataDuplicate() {
        BusinessException exception = BusinessException.dataDuplicate("邮箱");

        assertEquals(HttpStatus.DATA_DUPLICATE, exception.getCode());
        assertEquals("邮箱已存在", exception.getMessage());
    }

    @Test
    @DisplayName("快捷方法 - 查询执行失败")
    void testQueryExecuteError() {
        BusinessException exception = BusinessException.queryExecuteError("SQL 语法错误");

        assertEquals(HttpStatus.QUERY_EXECUTE_ERROR, exception.getCode());
        assertTrue(exception.getMessage().contains("查询执行失败"));
        assertTrue(exception.getMessage().contains("SQL 语法错误"));
    }

    @Test
    @DisplayName("快捷方法 - 业务错误")
    void testBusinessError() {
        BusinessException exception = BusinessException.businessError("自定义业务错误");

        assertEquals(HttpStatus.ERROR, exception.getCode());
        assertEquals("自定义业务错误", exception.getMessage());
    }

    @Test
    @DisplayName("序列化 ID 测试")
    void testSerialVersionUID() {
        // 验证 serialVersionUID 是否存在
        try {
            java.lang.reflect.Field field = BusinessException.class.getDeclaredField("serialVersionUID");
            field.setAccessible(true);
            assertNotNull(field.get(null));
        } catch (NoSuchFieldException e) {
            fail("serialVersionUID 字段不存在");
        } catch (IllegalAccessException e) {
            fail("无法访问 serialVersionUID 字段");
        }
    }
}
