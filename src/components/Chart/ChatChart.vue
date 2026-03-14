<template>
  <div ref="chartRef" class="chatbi-chart" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef } from 'vue';
import { init, use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import {
  BarChart,
  LineChart,
  PieChart,
  ScatterChart,
  RadarChart,
  GaugeChart,
  FunnelChart,
  MapChart,
  GraphChart,
  TreeChart,
  TreemapChart,
  SunburstChart,
  SankeyChart,
  HeatmapChart,
  CandlestickChart,
  BoxplotChart
} from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  VisualMapComponent,
  MarkPointComponent,
  MarkLineComponent,
  RadarComponent,
  GeoComponent
} from 'echarts/components';
import type { EChartsOption } from 'echarts';
import { getChartFamily, getEnterpriseChartType } from './chartCatalog';

// 注册必要的组件
use([
  CanvasRenderer,
  BarChart, LineChart, PieChart, ScatterChart, RadarChart, GaugeChart, FunnelChart, MapChart, GraphChart, TreeChart,
  TreemapChart, SunburstChart, SankeyChart, HeatmapChart, CandlestickChart, BoxplotChart,
  TitleComponent, TooltipComponent, GridComponent, LegendComponent,
  DataZoomComponent, ToolboxComponent, VisualMapComponent, MarkPointComponent, MarkLineComponent,
  RadarComponent, GeoComponent
]);

interface Props {
  width?: string;
  height?: string;
  type?: string;
  data?: any[];
  xData?: any[];
  seriesData?: any[];
  title?: string;
  legend?: string[];
  showLegend?: boolean;
  showDataZoom?: boolean;
  showToolbox?: boolean;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  type: 'line',
  data: () => [],
  xData: () => [],
  seriesData: () => [],
  title: '',
  legend: () => [],
  showLegend: true,
  showDataZoom: false,
  showToolbox: true,
  loading: false
});

const emit = defineEmits<{
  (e: 'chartClick', data: any): void;
  (e: 'chartReady', chart: any): void;
}>();

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<any>(null);
let resizeObserver: ResizeObserver | null = null;

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return;

  chartInstance.value = init(chartRef.value);

  // 点击事件
  chartInstance.value.on('click', (params: any) => {
    emit('chartClick', params);
  });

  updateChart();

  // 监听容器大小变化
  resizeObserver = new ResizeObserver(() => {
    chartInstance.value?.resize();
  });
  resizeObserver.observe(chartRef.value);

  emit('chartReady', chartInstance.value);
};

// 更新图表
const updateChart = () => {
  if (!chartInstance.value) return;

  const option: EChartsOption = getChartOption();
  chartInstance.value.setOption(option, true);
};

// 获取图表配置
const getChartOption = (): EChartsOption => {
  const chartType = getEnterpriseChartType(props.type);
  const family = getChartFamily(chartType.type);
  const isLightGrid = chartType.variant === 'light-grid';
  const isDarkGrid = chartType.variant === 'dark-grid';

  const baseOption: EChartsOption = {
    title: props.title ? { text: props.title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: props.showLegend && props.legend.length > 0 ? {
      data: props.legend,
      top: props.title ? 'bottom' : 'top'
    } : undefined,
    toolbox: props.showToolbox ? {
      feature: {
        saveAsImage: { show: true, title: '保存' },
        dataView: { show: true, title: '数据视图' },
        restore: { show: true, title: '还原' }
      },
      right: 20
    } : undefined,
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: props.title || (props.showLegend && props.legend.length > 0) ? '15%' : '10%',
      containLabel: true
    },
    backgroundColor: isDarkGrid ? '#0f172a' : '#ffffff'
  };

  // 根据图表类型生成配置
  switch (family) {
    case 'bar':
      return getBarOption(baseOption);
    case 'waterfall':
      return getWaterfallOption(baseOption);
    case 'line':
    case 'area':
      return getLineOption(baseOption);
    case 'pie':
      return getPieOption(baseOption);
    case 'scatter':
      return getScatterOption(baseOption);
    case 'radar':
      return getRadarOption(baseOption);
    case 'gauge':
      return getGaugeOption(baseOption);
    case 'funnel':
      return getFunnelOption(baseOption);
    case 'treemap':
      return getTreemapOption(baseOption);
    case 'sunburst':
      return getSunburstOption(baseOption);
    case 'sankey':
      return getSankeyOption(baseOption);
    case 'heatmap':
      return getHeatmapOption(baseOption, isLightGrid, isDarkGrid);
    case 'candlestick':
      return getCandlestickOption(baseOption);
    case 'boxplot':
      return getBoxplotOption(baseOption);
    case 'map':
      return getMapOption(baseOption);
    case 'graph':
      return getGraphOption(baseOption);
    case 'tree':
      return getTreeOption(baseOption);
    default:
      return getLineOption(baseOption);
  }
};

// 柱状图配置
const getBarOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    xAxis: {
      type: 'category',
      data: props.xData || [],
      axisLabel: { interval: 0, rotate: 0 }
    },
    yAxis: { type: 'value' },
    series: (props.seriesData || []).map((item: any) => ({
      type: 'bar',
      name: item.name || '',
      data: item.data || [],
      barMaxWidth: 50,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      showBackground: true,
      backgroundStyle: { color: 'rgba(180, 180, 180, 0.2)' }
    })),
    dataZoom: props.showDataZoom ? [{ type: 'slider' }, { type: 'inside' }] : undefined
  };
};

// 折线图配置
const getLineOption = (baseOption: EChartsOption): EChartsOption => {
  const family = getChartFamily(props.type);
  const isArea = family === 'area';
  return {
    ...baseOption,
    xAxis: {
      type: 'category',
      data: props.xData || [],
      boundaryGap: false,
      axisLabel: { interval: 0, rotate: 0 }
    },
    yAxis: { type: 'value', scale: true },
    series: (props.seriesData || []).map((item: any) => ({
      type: 'line',
      name: item.name || '',
      data: item.data || [],
      smooth: true,
      areaStyle: isArea ? (item.areaStyle || { opacity: 0.3 }) : undefined,
      lineStyle: { width: 3 },
      symbol: 'circle',
      symbolSize: 8
    })),
    dataZoom: props.showDataZoom ? [{ type: 'slider' }, { type: 'inside' }] : undefined
  };
};

// 饼图配置
const getPieOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '60%'],
      data: props.data || [],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  };
};

// 散点图配置
const getScatterOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    xAxis: { type: 'value', scale: true, splitLine: { show: true } },
    yAxis: { type: 'value', scale: true, splitLine: { show: true } },
    series: (props.seriesData || []).map((item: any) => ({
      type: 'scatter',
      name: item.name || '',
      data: item.data || [],
      symbolSize: 10,
      itemStyle: { opacity: 0.8 }
    }))
  };
};

// 雷达图配置
const getRadarOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    radar: {
      indicator: props.data?.[0]?.indicators || [],
      radius: '65%',
      splitNumber: 5
    },
    series: (props.seriesData || []).map((item: any) => ({
      type: 'radar',
      name: item.name || '',
      data: item.data || [],
      emphasis: { lineStyle: { width: 4 } }
    }))
  };
};

// 仪表盘配置
const getGaugeOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    series: [{
      type: 'gauge',
      progress: { show: true, width: 18 },
      axisLine: { lineStyle: { width: 18 } },
      axisTick: { show: false },
      splitLine: { length: 15, lineStyle: { width: 2, color: '#999' } },
      axisLabel: { distance: 25, color: '#999', fontSize: 10 },
      anchor: { show: true, showAbove: true, size: 25, itemStyle: { borderWidth: 10 } },
      title: { show: true },
      detail: { valueAnimation: true, fontSize: 30, offsetCenter: [0, '70%'] },
      data: props.data || [{ value: 70, name: '完成度' }]
    }]
  };
};

// 漏斗图配置
const getFunnelOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    series: [{
      type: 'funnel',
      left: '10%',
      top: 60,
      bottom: 60,
      width: '80%',
      min: 0,
      max: 100,
      minSize: '0%',
      maxSize: '100%',
      sort: 'descending',
      gap: 2,
      label: { show: true, position: 'inside', formatter: '{b}: {d}%' },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      data: props.data || []
    }]
  };
};

const getTreemapOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    tooltip: { trigger: 'item' },
    series: [{
      type: 'treemap',
      roam: false,
      nodeClick: 'zoomToNode',
      data: props.data || [
        { name: '华东', value: 320, children: [{ name: '上海', value: 180 }, { name: '杭州', value: 140 }] },
        { name: '华北', value: 260, children: [{ name: '北京', value: 160 }, { name: '天津', value: 100 }] }
      ]
    }]
  };
};

const getSunburstOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    series: [{
      type: 'sunburst',
      radius: [0, '88%'],
      sort: undefined,
      emphasis: { focus: 'ancestor' },
      data: props.data || [
        { name: '大客户', value: 40, children: [{ name: '华东', value: 18 }, { name: '华南', value: 22 }] },
        { name: '中小客户', value: 60, children: [{ name: '线上', value: 35 }, { name: '线下', value: 25 }] }
      ]
    }]
  };
};

const getSankeyOption = (baseOption: EChartsOption): EChartsOption => {
  const sankeyData = !Array.isArray(props.data) && props.data ? props.data as { nodes?: any[]; links?: any[] } : {};
  const nodes = sankeyData.nodes || [
    { name: '线索' }, { name: '商机' }, { name: '签约' }, { name: '回款' }
  ];
  const links = sankeyData.links || [
    { source: '线索', target: '商机', value: 120 },
    { source: '商机', target: '签约', value: 72 },
    { source: '签约', target: '回款', value: 51 }
  ];
  return {
    ...baseOption,
    tooltip: { trigger: 'item', triggerOn: 'mousemove' },
    series: [{
      type: 'sankey',
      data: nodes,
      links,
      emphasis: { focus: 'adjacency' },
      lineStyle: { color: 'gradient', curveness: 0.5 }
    }]
  };
};

const getHeatmapOption = (baseOption: EChartsOption, isLightGrid: boolean, isDarkGrid: boolean): EChartsOption => {
  const xAxisData = props.xData?.length ? props.xData : ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  const yAxisData = ['0-6', '6-12', '12-18', '18-24'];
  const matrix = (props.data || []).length
    ? (props.data as any[])
    : xAxisData.flatMap((_, x) => yAxisData.map((__, y) => [x, y, (x + 2) * (y + 5) * 3]));
  return {
    ...baseOption,
    tooltip: { position: 'top' },
    grid: { height: '62%', top: '14%' },
    xAxis: { type: 'category', data: xAxisData, splitArea: { show: true } },
    yAxis: { type: 'category', data: yAxisData, splitArea: { show: true } },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '2%',
      inRange: {
        color: isDarkGrid
          ? ['#0b253a', '#125d98', '#37a6e6', '#8be9fd']
          : isLightGrid
            ? ['#f7fbff', '#deebf7', '#9ecae1', '#3182bd']
            : ['#fff7ec', '#fdd49e', '#fc8d59', '#d7301f']
      }
    },
    series: [{ type: 'heatmap', data: matrix, label: { show: false } }]
  };
};

const getCandlestickOption = (baseOption: EChartsOption): EChartsOption => {
  const xAxisData = props.xData?.length ? props.xData : ['周一', '周二', '周三', '周四', '周五'];
  const seriesData = props.data?.length
    ? (props.data as any[])
    : [[20, 28, 18, 32], [28, 24, 22, 30], [24, 26, 21, 29], [26, 34, 25, 36], [34, 31, 29, 38]];
  return {
    ...baseOption,
    xAxis: { type: 'category', data: xAxisData },
    yAxis: { scale: true },
    series: [{ type: 'candlestick', data: seriesData }]
  };
};

const getBoxplotOption = (baseOption: EChartsOption): EChartsOption => {
  const xAxisData = props.xData?.length ? props.xData : ['A组', 'B组', 'C组', 'D组'];
  const seriesData = props.data?.length
    ? (props.data as any[])
    : [[10, 20, 25, 30, 36], [12, 22, 28, 33, 39], [8, 16, 20, 27, 35], [14, 23, 29, 35, 43]];
  return {
    ...baseOption,
    xAxis: { type: 'category', data: xAxisData },
    yAxis: { type: 'value' },
    series: [{ type: 'boxplot', data: seriesData }]
  };
};

const getWaterfallOption = (baseOption: EChartsOption): EChartsOption => {
  const categories = props.xData?.length ? props.xData : ['期初', '销售增长', '成本上升', '折扣', '期末'];
  const values = props.seriesData?.[0]?.data || [100, 45, -20, -12, 113];
  let cumulative = 0;
  const help = values.map((value: number, index: number) => {
    if (index === 0) {
      cumulative = Number(value);
      return 0;
    }
    const prev = cumulative;
    cumulative += Number(value);
    return value >= 0 ? prev : cumulative;
  });
  return {
    ...baseOption,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value' },
    series: [
      { type: 'bar', stack: 'all', itemStyle: { color: 'transparent' }, emphasis: { itemStyle: { color: 'transparent' } }, data: help },
      { type: 'bar', stack: 'all', data: values, itemStyle: { color: '#409EFF' } }
    ]
  };
};

// 地图配置（需要额外的地图数据）
const getMapOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    geo: {
      map: 'china',
      roam: true,
      label: { show: true, color: '#fff' },
      itemStyle: {
        areaColor: '#323c48',
        borderColor: '#111'
      },
      emphasis: {
        itemStyle: { areaColor: '#2a333d' },
        label: { show: true, color: '#fff' }
      }
    },
    series: [{
      type: 'map',
      map: 'china',
      data: props.data || [],
      label: { show: true }
    }]
  };
};

// 关系图配置
const getGraphOption = (baseOption: EChartsOption): EChartsOption => {
  const graphData = !Array.isArray(props.data) && props.data ? props.data as { nodes?: any[]; links?: any[] } : {};
  return {
    ...baseOption,
    tooltip: {},
    animationDurationUpdate: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [{
      type: 'graph',
      layout: 'force',
      symbolSize: 50,
      roam: true,
      label: { show: true, position: 'right' },
      edgeSymbol: ['circle', 'arrow'],
      edgeSymbolSize: [4, 10],
      data: graphData.nodes || [],
      links: graphData.links || [],
      lineStyle: { color: 'source', curveness: 0.3 },
      emphasis: { focus: 'adjacency', lineStyle: { width: 10 } }
    }]
  };
};

// 树图配置
const getTreeOption = (baseOption: EChartsOption): EChartsOption => {
  return {
    ...baseOption,
    series: [{
      type: 'tree',
      data: props.data || [],
      top: '10%',
      bottom: '10%',
      left: '8%',
      right: '8%',
      symbolSize: 14,
      label: { position: 'left', verticalAlign: 'middle', align: 'right', fontSize: 12 },
      leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
      emphasis: { focus: 'descendant' },
      expandAndCollapse: true,
      animationDuration: 550,
      animationDurationUpdate: 750
    }]
  };
};

// 公开方法：手动调整大小
const resize = () => {
  chartInstance.value?.resize();
};

// 公开方法：显示加载
const showLoading = () => {
  chartInstance.value?.showLoading();
};

// 公开方法：隐藏加载
const hideLoading = () => {
  chartInstance.value?.hideLoading();
};

// 公开方法：销毁图表
const destroy = () => {
  resizeObserver?.disconnect();
  chartInstance.value?.dispose();
  chartInstance.value = null;
};

defineExpose({ resize, showLoading, hideLoading, destroy });

onMounted(() => {
  // 等待 DOM 渲染
  setTimeout(() => {
    initChart();
  }, 0);
});

onUnmounted(() => {
  destroy();
});

watch(() => [props.type, props.data, props.xData, props.seriesData, props.title, props.legend], () => {
  updateChart();
}, { deep: true });
</script>

<style scoped>
.chatbi-chart {
  display: inline-block;
}
</style>
