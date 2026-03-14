package com.chatbi.security;

import com.chatbi.common.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("JwtAuthenticationFilter 测试")
class JwtAuthenticationFilterTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

    @Test
    @DisplayName("开发态自动注入应放行普通 API")
    void shouldAllowDevAutoAuthForCommonApi() {
        assertTrue(filter.isDevAutoAuthAllowed("/api/query"));
        assertTrue(filter.isDevAutoAuthAllowed("/api/conversation/message"));
    }

    @Test
    @DisplayName("开发态自动注入应禁止高敏接口")
    void shouldBlockDevAutoAuthForSensitiveApi() {
        assertFalse(filter.isDevAutoAuthAllowed("/api/audit/logs"));
        assertFalse(filter.isDevAutoAuthAllowed("/api/system/permissions/tree"));
        assertFalse(filter.isDevAutoAuthAllowed("/api/audit/access-alerts"));
    }
}
