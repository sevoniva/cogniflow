<template>
  <div class="sunburst-chart" ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { SunburstChart as EChartsSunburst } from 'echarts/charts';
import { TitleComponent, TooltipComponent } from 'echarts/components';

use([CanvasRenderer, EChartsSunburst, TitleComponent, TooltipComponent]);

interface Props {
  width?: string;
  height?: string;
  title?: string;
  data?: any[];
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  title: '',
  data: () => [],
  loading: false
});

const emit = defineEmits<{
  (e: 'chartClick', data: any): void;
  (e: 'chartReady', chart: any): void;
}>();

const chartRef = ref<HTMLElement | null>(null);
const chartInstance = shallowRef<any>(null);

const initChart = () => {
  if (!chartRef.value) return;

  chartInstance.value = (window as any).echarts.init(chartRef.value);

  chartInstance.value.on('click', (params: any) => {
    emit('chartClick', params);
  });

  updateChart();
  emit('chartReady', chartInstance.value);
};

const updateChart = () => {
  if (!chartInstance.value) return;

  chartInstance.value.setOption({
    title: props.title ? { text: props.title, left: 'center' } : undefined,
    tooltip: { trigger: 'item' },
    series: {
      type: 'sunburst',
      data: props.data || [],
      radius: [0, '90%'],
      label: { rotate: 'radial' },
      emphasis: { focus: 'ancestor' }
    }
  }, true);
};

const resize = () => chartInstance.value?.resize();
const showLoading = () => chartInstance.value?.showLoading();
const hideLoading = () => chartInstance.value?.hideLoading();
const destroy = () => {
  chartInstance.value?.dispose();
  chartInstance.value = null;
};

defineExpose({ resize, showLoading, hideLoading, destroy });

onMounted(() => {
  setTimeout(initChart, 0);
});

onUnmounted(() => {
  destroy();
});

watch(() => [props.data, props.title], updateChart, { deep: true });
</script>

<style scoped>
.sunburst-chart {
  display: inline-block;
}
</style>
