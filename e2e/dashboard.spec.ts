import { test, expect } from '@playwright/test';

/**
 * ChatBI E2E 测试 - 分析大屏
 */
test.describe('分析大屏', () => {
  const chartContainer = 'div[style*="height: 350px"], div[style*="height: 300px"]';

  test('销售大屏应展示趋势图与指标卡', async ({ page }) => {
    await page.goto('/sales-dashboard');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveTitle(/销售分析 - Chat BI/);
    await expect(page.getByText('销售额趋势')).toBeVisible();
    await expect(page.locator('.stat-value').first()).toContainText(/\d/);
    await expect(page.locator(chartContainer).first()).toBeVisible();
  });

  test('运营大屏应展示活跃趋势图', async ({ page }) => {
    await page.goto('/operation-dashboard');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveTitle(/运营分析 - Chat BI/);
    await expect(page.getByText('用户活跃度趋势（近30天）')).toBeVisible();
    await expect(page.locator(chartContainer).first()).toBeVisible();
  });

  test('敏捷大屏应展示研发效能图', async ({ page }) => {
    await page.goto('/agile-dashboard');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveTitle(/敏捷研发管理 - Chat BI/);
    await expect(page.getByText('迭代速率趋势')).toBeVisible();
    await expect(page.locator(chartContainer).first()).toBeVisible();
  });
});
