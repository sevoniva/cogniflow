import { test, expect } from '@playwright/test';

/**
 * ChatBI E2E 测试 - 首页入口流程
 * 说明：当前系统默认无登录拦截，保留此文件作为用户核心入口回归测试。
 */
test.describe('首页入口流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('应该展示首页核心信息', async ({ page }) => {
    await expect(page).toHaveTitle(/Chat BI/);
    await expect(page.getByText('ChatBI 智能数据分析平台')).toBeVisible();
    await expect(page.getByPlaceholder('例如：本月华东大区销售额与毛利率趋势')).toBeVisible();
  });

  test('应该从首页发起查询并跳转结果页', async ({ page }) => {
    await page.getByPlaceholder('例如：本月华东大区销售额与毛利率趋势').fill('本月销售额');
    await page.getByRole('button', { name: '查询', exact: true }).click();
    await page.waitForURL(/\/chatbi\/result/, { timeout: 15000 });
    await expect(page.locator('main')).toContainText(/智能解读|查询失败/, { timeout: 20000 });
  });

  test('应该从首页进入 AI 对话页', async ({ page }) => {
    await page.getByRole('button', { name: 'AI 对话分析' }).click();
    await page.waitForURL(/\/chatbi\/conversation/, { timeout: 10000 });
    await expect(page.getByText('对话工作区')).toBeVisible();
    await expect(page.getByPlaceholder(/例如：本月销售额是多少/)).toBeVisible();
  });
});
