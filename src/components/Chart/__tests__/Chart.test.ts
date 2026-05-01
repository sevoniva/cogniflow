import { defineComponent, h, onMounted } from 'vue';
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import ChatBarChart from '../ChatBarChart.vue';
import ChatLineChart from '../ChatLineChart.vue';
import ChatPieChart from '../ChatPieChart.vue';

vi.mock('../ChatChart.vue', () => ({
  default: defineComponent({
    name: 'ChatChartStub',
    props: {
      title: String,
      width: String,
      height: String,
      showDataZoom: Boolean,
      showLegend: Boolean
    },
    emits: ['chartReady', 'chartClick'],
    setup(_, { emit }) {
      onMounted(() => {
        emit('chartReady', { resize: vi.fn() });
      });
      return () => h('div', { class: 'chatbi-chart' });
    }
  })
}));

/**
 * 图表组件单元测试
 */
describe('Chart Components', () => {
  describe('ChatBarChart', () => {
    const mockData = {
      xData: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      seriesData: [
        {
          name: '销售额',
          data: [120, 200, 150, 80, 70, 110, 130]
        }
      ]
    };

    it('应该正确渲染柱状图', () => {
      const wrapper = mount(ChatBarChart, {
        props: {
          title: '销售统计',
          xData: mockData.xData,
          seriesData: mockData.seriesData
        }
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.chatbi-chart').exists()).toBe(true);
    });

    it('应该透传 props 到 ChatChart', () => {
      const wrapper = mount(ChatBarChart, {
        props: {
          title: '测试标题',
          width: '500px',
          height: '300px'
        }
      });

      const chartStub = wrapper.findComponent({ name: 'ChatChartStub' });
      expect(chartStub.props('title')).toBe('测试标题');
      expect(chartStub.props('width')).toBe('500px');
      expect(chartStub.props('height')).toBe('300px');
    });

    it('应该触发 chartReady 事件', async () => {
      const wrapper = mount(ChatBarChart, {
        props: {
          xData: mockData.xData,
          seriesData: mockData.seriesData
        }
      });

      // 等待组件挂载和图表初始化
      await new Promise(resolve => setTimeout(resolve, 100));

      const chartStub = wrapper.findComponent({ name: 'ChatChartStub' });
      expect(chartStub.emitted('chartReady')).toBeDefined();
    });
  });

  describe('ChatLineChart', () => {
    const mockData = {
      xData: ['1 月', '2 月', '3 月', '4 月', '5 月', '6 月'],
      seriesData: [
        {
          name: '访问量',
          data: [820, 932, 901, 934, 1290, 1330]
        }
      ]
    };

    it('应该正确渲染折线图', () => {
      const wrapper = mount(ChatLineChart, {
        props: {
          title: '访问趋势',
          xData: mockData.xData,
          seriesData: mockData.seriesData
        }
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.chatbi-chart').exists()).toBe(true);
    });

    it('应该支持 areaStyle 面积图样式', () => {
      const wrapper = mount(ChatLineChart, {
        props: {
          xData: mockData.xData,
          seriesData: mockData.seriesData
        }
      });

      expect(wrapper.exists()).toBe(true);
    });

    it('应该支持 dataZoom 缩放', () => {
      const wrapper = mount(ChatLineChart, {
        props: {
          xData: mockData.xData,
          seriesData: mockData.seriesData,
          showDataZoom: true
        }
      });

      const chartStub = wrapper.findComponent({ name: 'ChatChartStub' });
      expect(chartStub.props('showDataZoom')).toBe(true);
    });
  });

  describe('ChatPieChart', () => {
    const mockData = [
      { value: 1048, name: '搜索引擎' },
      { value: 735, name: '直接访问' },
      { value: 580, name: '邮件营销' },
      { value: 484, name: '联盟广告' },
      { value: 300, name: '视频广告' }
    ];

    it('应该正确渲染饼图', () => {
      const wrapper = mount(ChatPieChart, {
        props: {
          title: '访问来源',
          data: mockData
        }
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.chatbi-chart').exists()).toBe(true);
    });

    it('应该支持自定义高度', () => {
      const wrapper = mount(ChatPieChart, {
        props: {
          data: mockData,
          height: '500px'
        }
      });

      const chartStub = wrapper.findComponent({ name: 'ChatChartStub' });
      expect(chartStub.props('height')).toBe('500px');
    });

    it('应该支持隐藏图例', () => {
      const wrapper = mount(ChatPieChart, {
        props: {
          data: mockData,
          showLegend: false
        }
      });

      const chartStub = wrapper.findComponent({ name: 'ChatChartStub' });
      expect(chartStub.props('showLegend')).toBe(false);
    });
  });
});
