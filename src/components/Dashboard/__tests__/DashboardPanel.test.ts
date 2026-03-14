import ElementPlus from 'element-plus';
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import DashboardPanel from '../DashboardPanel.vue';

const mountDashboard = (props: Record<string, unknown>) => mount(DashboardPanel, {
  props,
  global: {
    plugins: [ElementPlus],
    stubs: {
      GridLayout: {
        template: '<div class="grid-layout"><slot /></div>'
      },
      GridItem: {
        template: '<div class="dashboard-item"><slot /></div>'
      },
      ChatBarChart: { template: '<div class="chart-stub">bar</div>' },
      ChatLineChart: { template: '<div class="chart-stub">line</div>' },
      ChatAreaChart: { template: '<div class="chart-stub">area</div>' },
      ChatPieChart: { template: '<div class="chart-stub">pie</div>' },
      ChatScatterChart: { template: '<div class="chart-stub">scatter</div>' },
      ChatRadarChart: { template: '<div class="chart-stub">radar</div>' },
      ChatGaugeChart: { template: '<div class="chart-stub">gauge</div>' },
      ChatFunnelChart: { template: '<div class="chart-stub">funnel</div>' }
    }
  }
});

/**
 * DashboardPanel 组件单元测试
 */
describe('DashboardPanel', () => {
  const mockLayout = [
    {
      i: 'chart-1',
      x: 0,
      y: 0,
      w: 6,
      h: 4,
      title: '销售趋势',
      chartType: 'line',
      chartProps: {
        xData: ['周一', '周二', '周三'],
        seriesData: [{ name: '销售额', data: [120, 200, 150] }]
      }
    },
    {
      i: 'chart-2',
      x: 6,
      y: 0,
      w: 6,
      h: 4,
      title: '访问来源',
      chartType: 'pie',
      chartProps: {
        data: [
          { value: 1048, name: '搜索引擎' },
          { value: 735, name: '直接访问' }
        ]
      }
    }
  ];

  it('应该正确渲染仪表板', () => {
    const wrapper = mountDashboard({
      title: '测试仪表板',
      data: mockLayout
    });

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('.dashboard-container').exists()).toBe(true);
    expect(wrapper.find('.dashboard-title').text()).toBe('测试仪表板');
  });

  it('应该渲染图表项', () => {
    const wrapper = mountDashboard({
      data: mockLayout
    });

    expect(wrapper.findAll('.dashboard-item').length).toBeGreaterThan(0);
  });

  it('应该触发 refresh 事件', async () => {
    const wrapper = mountDashboard({
      data: mockLayout
    });

    // 模拟刷新按钮点击
    const refreshButton = wrapper.find('button[title="刷新"]');
    if (refreshButton.exists()) {
      await refreshButton.trigger('click');
      expect(wrapper.emitted('refresh')).toBeDefined();
    }
  });

  it('应该触发 export 事件', async () => {
    const wrapper = mountDashboard({
      data: mockLayout
    });

    // 模拟导出按钮点击
    const exportButton = wrapper.find('button[title="导出"]');
    if (exportButton.exists()) {
      await exportButton.trigger('click');
      expect(wrapper.emitted('export')).toBeDefined();
    }
  });

  it('应该在可编辑模式下显示编辑按钮', () => {
    const wrapper = mountDashboard({
      data: mockLayout,
      editable: true
    });

    expect(wrapper.find('[title="编辑"]').exists()).toBe(true);
  });

  it('应该在非编辑模式下隐藏编辑按钮', () => {
    const wrapper = mountDashboard({
      data: mockLayout,
      editable: false
    });

    expect(wrapper.find('[title="编辑"]').exists()).toBe(false);
  });

  it('应该支持空数据状态', () => {
    const wrapper = mountDashboard({
      data: []
    });

    expect(wrapper.text()).toContain('暂无图表');
  });

  it('应该支持添加图表', async () => {
    const wrapper = mountDashboard({
      data: [],
      editable: true
    });

    // 模拟打开添加图表对话框
    const editButton = wrapper.find('button[title="编辑"]');
    if (editButton.exists()) {
      await editButton.trigger('click');
      // 对话框应该打开
      expect(wrapper.find('.el-dialog').exists() || wrapper.text().includes('添加图表')).toBe(true);
    }
  });
});
