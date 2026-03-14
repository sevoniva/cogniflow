import { test, expect } from '@playwright/test';

/**
 * ChatBI E2E 测试 - 路由导航
 */
test.describe('导航测试', () => {
  test('关键页面路由应该可访问', async ({ page }) => {
    const routes = [
      '/chatbi/query',
      '/chatbi/conversation',
      '/chatbi/visual-query',
      '/chatbi/advanced-charts',
      '/admin/metric',
      '/admin/ai'
    ];

    for (const route of routes) {
      await page.goto(route, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveURL(new RegExp(route.replace('/', '\\/')));
    }
  });

  test('管理台应显示统一侧边导航', async ({ page }) => {
    await page.goto('/admin/metric', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Admin Console')).toBeVisible();
    await expect(page.getByRole('menuitem', { name: '指标管理' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'AI 设置' })).toBeVisible();
  });
});
