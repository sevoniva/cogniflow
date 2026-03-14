# Language | 语言

[English](./TESTING_GUIDE.md) | [中文](./TESTING_GUIDE.zh-CN.md)

# ChatBI 测试指南

## 📋 目录

- [快速开始](#快速开始)
- [后端测试](#后端测试)
- [前端测试](#前端测试)
- [E2E 测试](#e2e-测试)
- [测试报告](#测试报告)

---

## 🚀 快速开始

### 运行所有测试

```bash
# 后端测试
cd chatbi-server
./mvnw test

# 前端测试
cd ..
npm run test

# E2E 测试
npm run test:e2e
```

### 运行测试并生成覆盖率报告

```bash
# 前端覆盖率
npm run test:coverage

# 后端覆盖率
cd chatbi-server
./mvnw test jacoco:report
```

---

## 🔧 后端测试

### 运行单个测试类

```bash
# 运行 AuthService 测试
./mvnw test -Dtest=AuthServiceTest

# 运行 JwtUtils 测试
./mvnw test -Dtest=JwtUtilsTest
```

### 运行指定包下的测试

```bash
# 运行所有服务层测试
./mvnw test -Dtest="com.chatbi.service.**"

# 运行所有工具类测试
./mvnw test -Dtest="com.chatbi.common.utils.**"
```

### 测试配置

测试配置文件位于 `src/test/resources/application-test.yml`

```yaml
spring:
  application:
    name: chatbi-server-test
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=MySQL
  jpa:
    hibernate:
      ddl-auto: create-drop
```

### 编写测试用例

```java
package com.chatbi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    @DisplayName("登录成功测试")
    void testLogin_Success() {
        // 准备数据
        // 执行测试
        // 验证结果
    }
}
```

---

## 🎨 前端测试

### 运行单个测试文件

```bash
# 运行图表组件测试
npm run test -- src/components/Chart/__tests__/Chart.test.ts

# 运行仪表板组件测试
npm run test -- src/components/Dashboard/__tests__/DashboardPanel.test.ts
```

### 运行测试并监听文件变化

```bash
# 开发模式
npm run test -- --watch

# UI 模式
npm run test:ui
```

### 测试配置

测试配置文件位于 `vitest.config.ts`

```typescript
export default defineConfig({
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      threshold: {
        lines: 70,
        functions: 70,
        branches: 70,
        statements: 70
      }
    }
  }
});
```

### 编写组件测试

```typescript
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import ChatBarChart from '../src/components/Chart/ChatBarChart.vue';

describe('ChatBarChart', () => {
  it('应该正确渲染柱状图', () => {
    const wrapper = mount(ChatBarChart, {
      props: {
        title: '销售统计',
        xData: ['周一', '周二', '周三'],
        seriesData: [
          { name: '销售额', data: [120, 200, 150] }
        ]
      }
    });

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('.chatbi-chart').exists()).toBe(true);
  });

  it('应该触发 chartReady 事件', async () => {
    const wrapper = mount(ChatBarChart, {
      props: {
        xData: ['周一', '周二', '周三'],
        seriesData: [{ name: '销售额', data: [120, 200, 150] }]
      }
    });

    await new Promise(resolve => setTimeout(resolve, 100));
    expect(wrapper.emitted('chartReady')).toBeDefined();
  });
});
```

### Mock 工具

测试全局配置提供了以下 mock 工具：

```typescript
// Mock 成功的 fetch 响应
globalThis.mockFetch({ data: 'test' });

// Mock 失败的 fetch 响应
globalThis.mockFetchError(500, 'Internal Server Error');
```

---

## 🌐 E2E 测试

### 运行 E2E 测试

```bash
# 运行所有 E2E 测试
npm run test:e2e

# 运行指定测试文件
npx playwright test e2e/login.spec.ts

# 运行指定浏览器测试
npx playwright test --project=chromium

# 有头模式（显示浏览器）
npx playwright test --headed

# 调试模式
npx playwright test --debug
```

### 测试配置

测试配置文件位于 `playwright.config.ts`

```typescript
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'test-results/e2e' }],
    ['junit', { outputFile: 'test-results/e2e/results.xml' }]
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  }
});
```

### 编写 E2E 测试

```typescript
import { test, expect } from '@playwright/test';

test.describe('登录流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('应该成功登录', async ({ page }) => {
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'Admin@123');
    await page.click('button[type="submit"]');

    await page.waitForURL(/\/dashboard/);
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('应该显示错误消息 - 密码错误', async ({ page }) => {
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'WrongPassword');
    await page.click('button[type="submit"]');

    await expect(page.locator('.el-message--error')).toBeVisible();
  });
});
```

### 认证状态管理

```bash
# 保存认证状态
npx playwright auth:save

# 使用保存的认证状态
test.use({
  storageState: 'e2e/.auth/admin.json'
});
```

---

## 📊 测试报告

### 查看覆盖率报告

```bash
# 前端覆盖率报告（HTML）
npm run test:coverage
# 报告位置：coverage/index.html

# 后端覆盖率报告（HTML）
cd chatbi-server
./mvnw test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

### 查看 E2E 测试报告

```bash
# HTML 报告
npx playwright show-report test-results/e2e

# JUnit 报告
# 文件位置：test-results/e2e/results.xml
```

### 测试报告位置

| 报告类型 | 位置 |
|----------|------|
| 前端单元测试 | `coverage/index.html` |
| 后端单元测试 | `chatbi-server/target/site/jacoco/index.html` |
| E2E 测试报告 | `test-results/e2e/index.html` |
| 测试汇总报告 | `docs/TEST_REPORT.md` |

---

## 🔍 调试技巧

### 后端调试

```bash
# 调试模式运行测试
./mvnw test -Dmaven.surefire.debug
```

### 前端调试

```bash
# 单个测试调试
npm run test -- --reporter=verbose

# VSCode 调试配置
# .vscode/launch.json
{
  "type": "node",
  "request": "launch",
  "name": "Debug Vitest",
  "runtimeArgs": ["--inspect-brk", "node_modules/vitest/vitest.js", "run"],
  "console": "integratedTerminal",
  "skipFiles": ["<node_internals>/**"]
}
```

### E2E 调试

```bash
# Playwright Inspector
npx playwright test --debug

# 追踪查看
npx playwright show-trace trace.zip
```

---

## 📝 最佳实践

### 测试命名规范

- 后端：`test[Feature]_[Scenario]_[ExpectedResult]`
- 前端：`应该 [ 功能]`
- E2E: `应该 [ 用户操作]`

### 测试组织结构

```
src/
├── test/
│   ├── setup.ts              # 全局配置
│   └── mocks/                # Mock 数据
└── components/
    └── Chart/
        └── __tests__/
            └── Chart.test.ts # 测试文件
```

### 测试数据管理

```typescript
// 使用 factory 函数创建测试数据
const createMockUser = (overrides = {}) => ({
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  ...overrides
});

// 在测试中使用
const user = createMockUser({ username: 'custom' });
```

---

## ⚠️ 常见问题

### 测试失败

1. **环境问题**: 确保安装所有依赖
   ```bash
   npm install
   cd chatbi-server && ./mvnw install
   ```

2. **数据库问题**: 确保测试数据库配置正确
   ```bash
   # 检查 application-test.yml
   ```

3. **端口冲突**: 确保测试端口未被占用

### 覆盖率不达标

1. 检查是否有未测试的代码路径
2. 添加边界条件测试
3. 添加异常场景测试

---

*文档版本：1.0.0*
*更新日期：2026-03-10*
