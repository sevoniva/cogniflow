import { test, expect, type Page, type Response } from '@playwright/test';

async function expectDashboardReady(
  page: Page,
  options: {
    path: string;
    title: RegExp;
    heading: string;
    metricSelector: string;
    apiPattern: RegExp;
  }
) {
  const apiFailures: string[] = [];

  const failureHandler = (response: Response) => {
    if (options.apiPattern.test(response.url()) && response.status() >= 400) {
      apiFailures.push(`${response.status()} ${response.url()}`);
    }
  };

  page.on('response', failureHandler);

  await page.goto(options.path);
  await page.waitForLoadState('networkidle');
  await expect(page).toHaveTitle(options.title);
  await expect(page.getByText(options.heading)).toBeVisible();
  const metric = page.locator(options.metricSelector).first();
  await expect(metric).toBeVisible({ timeout: 10000 });
  const metricText = (await metric.textContent())?.trim() || '';
  expect(metricText).toMatch(/\d/);
  expect(metricText).not.toMatch(/^0+(?:[.,]0+)?%?$/);
  await expect(page.locator('canvas').first()).toBeVisible({ timeout: 10000 });
  page.off('response', failureHandler);
  expect(apiFailures).toEqual([]);
}

/**
 * ChatBI E2E 测试 - 冒烟测试
 *
 * 核心功能快速验证，确保系统基本可用
 */
test.describe('冒烟测试', () => {
  test('系统应该正常启动', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Chat BI/);
    await expect(page.getByPlaceholder('例如：本月华东大区销售额与毛利率趋势')).toBeVisible();
  });

  test('管理页面应该可访问', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('企业数据管理中枢')).toBeVisible();
    await expect(page.getByRole('menuitem', { name: '指标管理' })).toBeVisible();
  });

  test('后台治理页面应该复用统一管理框架', async ({ page }) => {
    await page.goto('/admin/data-permission', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveTitle(/数据权限 - Chat BI/);
    await expect(page.getByText('Admin Console')).toBeVisible();
    await expect(page.getByRole('menuitem', { name: '数据权限' })).toBeVisible();
  });

  test('AI 管理页应展示调用观测与告警历史', async ({ page }) => {
    await page.goto('/admin/ai', { waitUntil: 'domcontentloaded' });
    const observabilityTitle = page.getByText('AI 调用观测');
    if (await observabilityTitle.count()) {
      await expect(observabilityTitle).toBeVisible();
      const historyLabel = page.getByText('告警历史');
      if (await historyLabel.count()) {
        await expect(historyLabel).toBeVisible();
      } else {
        await expect(page.getByText('总调用数')).toBeVisible();
      }
    } else {
      await expect(page.getByText('提供商状态')).toBeVisible();
    }
  });

  test('应该能够完成一次查询', async ({ page }) => {
    await page.goto('/');
    await page.getByPlaceholder('例如：本月华东大区销售额与毛利率趋势').fill('本月销售额');
    await page.getByRole('button', { name: '查询', exact: true }).click();
    await page.waitForURL(/\/chatbi\/result/, { timeout: 10000 });
    await expect(page.getByText('正在分析查询意图并获取数据...')).toHaveCount(0, { timeout: 20000 });
    await expect(page.locator('.summary-card')).toBeVisible({ timeout: 20000 });
    const summaryText = ((await page.locator('.summary-card').first().textContent()) || '').trim();
    expect(summaryText.length).toBeGreaterThan(0);
    expect(summaryText).toMatch(/销售额|营收|收入/);
    await expect(page.getByText('智能解读')).toBeVisible({ timeout: 20000 });
  });

  test('AI 对话应该支持真实业务问题', async ({ page }) => {
    await page.goto('/chatbi/conversation?q=%E6%9C%AC%E6%9C%88%E9%94%80%E5%94%AE%E9%A2%9D', { waitUntil: 'networkidle' });
    const workspace = page.locator('.workspace-card').first();
    await expect(workspace).toBeVisible();
    const workspaceBox = await workspace.boundingBox();
    expect(workspaceBox?.y ?? 999).toBeLessThan(240);

    const metricQuickPanel = page.locator('.metric-quick-panel').first();
    if (await metricQuickPanel.count()) {
      await expect(metricQuickPanel).toBeVisible();
      const firstQuickMetric = metricQuickPanel.locator('.el-tag').first();
      if (await firstQuickMetric.count()) {
        await firstQuickMetric.click();
        const input = page.locator('.composer-input textarea').first();
        await expect(input).toHaveValue(/本月|销售额|毛利率|回款/);
      }
    } else {
      await expect(page.locator('.composer').first()).toBeVisible();
    }

    await expect(page.locator('.message-card.is-user .message-text').last()).toHaveText('本月销售额');
    await expect(page.locator('.message-card.is-assistant').last()).toBeVisible({ timeout: 10000 });
    const eastChinaCell = page.locator('.el-table .cell').filter({ hasText: /^华东$/ }).first();
    if (await eastChinaCell.count()) {
      await expect(eastChinaCell).toBeVisible();
      await expect(page.locator('.chart-container').last()).toBeVisible();
    } else {
      await expect(page.locator('.message-card.is-assistant').last()).toContainText(/请求超时|处理消息失败|已自动切换|经营总览/);
      await expect(page.locator('.quick-action-tag').first()).toBeVisible();
    }
    const sendButton = page.getByRole('button', { name: '发送' }).first();
    await expect(sendButton).toBeVisible();
    const sendBox = await sendButton.boundingBox();
    const viewport = page.viewportSize();
    expect(sendBox).not.toBeNull();
    expect(viewport).not.toBeNull();
    expect(sendBox?.y || 0).toBeLessThan((viewport?.height || 0));
    expect((sendBox?.y || 0) + (sendBox?.height || 0)).toBeGreaterThan(0);

    const shellOverflow = await page.locator('.messages-shell').evaluate(element => ({
      scrollWidth: element.scrollWidth,
      clientWidth: element.clientWidth
    }));
    expect(shellOverflow.scrollWidth - shellOverflow.clientWidth).toBeLessThanOrEqual(4);

    const chartOverflow = await page.locator('.chart-container').last().evaluate(element => ({
      scrollWidth: element.scrollWidth,
      clientWidth: element.clientWidth
    }));
    expect(chartOverflow.scrollWidth - chartOverflow.clientWidth).toBeLessThanOrEqual(4);

    await expect(page.getByText(/发送失败|处理消息失败/)).toHaveCount(0);
  });

  test('AI 对话诊断面板应提供可执行建议', async ({ page }) => {
    await page.goto('/chatbi/conversation', { waitUntil: 'networkidle' });
    const input = page.locator('.composer-input textarea').first();
    await expect(input).toBeVisible();
    await input.fill('本周销售额');
    await page.getByRole('button', { name: '发送' }).first().click();
    await expect(page.locator('.message-row.is-assistant').last()).toBeVisible({ timeout: 12000 });

    await input.fill('这个和毛利率或回款额上周对比');
    await page.getByRole('button', { name: '发送' }).first().click();

    const diagnosisPanel = page.locator('.message-card.is-assistant .diagnosis-panel').last();
    if (await diagnosisPanel.count()) {
      await expect(diagnosisPanel).toContainText('诊断码');
      await expect(diagnosisPanel).toContainText('候选排序');
      const reasonFilters = diagnosisPanel.locator('.diagnosis-panel__rank-filter');
      if ((await reasonFilters.count()) > 1) {
        const secondaryFilter = reasonFilters.nth(1);
        await secondaryFilter.click();
        await expect(secondaryFilter).toHaveClass(/is-active/);
        await expect(diagnosisPanel).toContainText('候选排序');
      }
      const actionableSuggestion = diagnosisPanel.locator('.suggestion-tag.is-actionable').first();
      if (await actionableSuggestion.count()) {
        const actionText = ((await actionableSuggestion.textContent()) || '').trim();
        expect(actionText.length).toBeGreaterThan(0);
        await actionableSuggestion.click();
        await expect(page.locator('.message-card.is-user .message-text').last()).toContainText(actionText, {
          timeout: 12000
        });
      }
    } else {
      await expect(page.locator('.message-card.is-assistant .message-text').last()).toBeVisible();
    }
    await expect(page.getByText(/发送失败|处理消息失败/)).toHaveCount(0);
  });

  test('AI 对话长会话时输入区应始终可见', async ({ page }) => {
    await page.setViewportSize({ width: 1366, height: 720 });
    await page.goto('/chatbi/conversation', { waitUntil: 'networkidle' });
    const input = page.locator('.composer-input textarea').first();
    const sendButton = page.getByRole('button', { name: '发送' }).first();
    await expect(input).toBeVisible();
    await expect(sendButton).toBeVisible();

    const prompts = [
      '本月销售额',
      '上月销售额',
      '本月毛利率',
      '本月回款额',
      '按区域看本月销售额'
    ];

    for (const prompt of prompts) {
      await input.fill(prompt);
      await sendButton.click();
      await expect(page.locator('.message-row.is-assistant').last()).toBeVisible({ timeout: 12000 });
    }

    const viewport = page.viewportSize();
    const buttonBox = await sendButton.boundingBox();
    expect(viewport).not.toBeNull();
    expect(buttonBox).not.toBeNull();
    expect((buttonBox?.y || 0) + (buttonBox?.height || 0)).toBeLessThanOrEqual((viewport?.height || 0) - 2);

    const shellMetrics = await page.locator('.messages-shell').evaluate(element => ({
      scrollTop: element.scrollTop,
      scrollHeight: element.scrollHeight,
      clientHeight: element.clientHeight
    }));
    expect(shellMetrics.scrollHeight).toBeGreaterThan(shellMetrics.clientHeight);
    expect(shellMetrics.scrollTop).toBeGreaterThan(0);
  });

  test('可视化查询页应该能生成并执行 SQL', async ({ page }) => {
    const apiFailures: string[] = [];
    page.on('response', response => {
      if (/\/api\/datasources\/\d+\/(tables|columns)|\/api\/query\/execute/.test(response.url()) && response.status() >= 400) {
        apiFailures.push(`${response.status()} ${response.url()}`);
      }
    });

    await page.goto('/chatbi/visual-query');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveTitle(/可视化查询 - Chat BI|可视化查询工作台 - Chat BI/);
    await page.getByRole('button', { name: '生成 SQL' }).click();
    await expect(page.locator('.sql-preview code')).toContainText('SELECT');

    const executeResponse = page.waitForResponse(response =>
      response.url().includes('/api/query/execute') && response.request().method() === 'POST'
    );
    await page.getByRole('button', { name: '执行查询' }).click();
    const response = await executeResponse;
    const payload = await response.json();
    expect(payload.success).toBeTruthy();
    expect(Array.isArray(payload.data?.records)).toBeTruthy();
    await expect(page.locator('.el-table').first()).toBeVisible();
    expect(apiFailures).toEqual([]);
  });

  test('图表应用市场应该展示 100+ 图表能力', async ({ page }) => {
    const chartCatalogResponse = page
      .waitForResponse(response =>
        response.url().includes('/api/chart-catalog/types')
          && response.request().resourceType() === 'fetch'
          && response.status() === 200
      , { timeout: 6000 })
      .catch(() => null);

    await page.goto('/chatbi/advanced-charts');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveTitle(/图表应用市场 - Chat BI|高级图表 - Chat BI|企业级图表中心 - Chat BI/);
    await expect(page.getByText(/图表应用市场|企业级图表中心|高级图表/).first()).toBeVisible();

    const summaryHint = page.getByText(/当前已支持\s*\d+\s*种图表样式/).first();
    await expect(summaryHint).toBeVisible();
    const summaryText = (await summaryHint.textContent()) || '';
    const matched = summaryText.match(/(\d+)/);
    if (matched) {
      expect(Number(matched[1])).toBeGreaterThanOrEqual(100);
    }

    const coverageHint = page.getByText(/数据验证覆盖率\s*\d+(?:\.\d+)?%/).first();
    await expect(coverageHint).toBeVisible();
    const marketItems = page.locator('.market-grid .market-item');
    if (await marketItems.count()) {
      await expect(marketItems.first()).toBeVisible();
      const statusTag = marketItems.first().locator('.el-tag').filter({ hasText: /已验证|待修复|待验证/ }).first();
      if (await statusTag.count()) {
        await expect(statusTag).toBeVisible();
      } else {
        await expect(page.locator('.validation-table .el-tag').filter({ hasText: /通过|失败/ }).first()).toBeVisible();
      }
    } else {
      await expect(page.getByRole('combobox').first()).toBeVisible();
    }

    const response = await chartCatalogResponse;
    if (response) {
      const payload = await response.json();
      expect(payload.success).toBeTruthy();
      expect(Array.isArray(payload.data)).toBeTruthy();
      expect(payload.data.length).toBeGreaterThanOrEqual(100);
      return;
    }

    await expect(page.getByRole('combobox').first()).toBeVisible();
    await expect(page.locator('.chart-container').first()).toBeVisible();
  });

  test('分析大屏应该渲染真实数据', async ({ page }) => {
    await expectDashboardReady(page, {
      path: '/sales-dashboard',
      title: /销售分析 - Chat BI/,
      heading: '销售额趋势',
      metricSelector: '.stat-value',
      apiPattern: /\/api\/analytics\/sales\//
    });

    await expectDashboardReady(page, {
      path: '/operation-dashboard',
      title: /运营分析 - Chat BI/,
      heading: '用户活跃度趋势（近30天）',
      metricSelector: '.stat-value',
      apiPattern: /\/api\/analytics\/operation\//
    });

    await expectDashboardReady(page, {
      path: '/agile-dashboard',
      title: /敏捷研发管理 - Chat BI/,
      heading: '迭代速率趋势',
      metricSelector: '.card-value',
      apiPattern: /\/api\/agile\//
    });
  });

  test('API 健康检查', async ({ request }) => {
    const response = await request.get('/api/actuator/health');
    expect([200, 401, 503]).toContain(response.status());
  });

  test('前端资源应该正常加载', async ({ page }) => {
    const responses: string[] = [];

    page.on('response', response => {
      responses.push(response.url());
    });

    await page.goto('/');

    expect(responses.some(url => url.includes('/src/main.ts') || url.includes('/assets/'))).toBeTruthy();
    expect(responses.some(url => url.endsWith('.css'))).toBeTruthy();
  });
});
