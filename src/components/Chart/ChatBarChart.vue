<template>
  <ChatChart
    ref="chartRef"
    type="bar"
    :width="width"
    :height="height"
    :title="title"
    :legend="legend"
    :x-data="xData"
    :series-data="seriesData"
    :show-legend="showLegend"
    :show-data-zoom="showDataZoom"
    :show-toolbox="showToolbox"
    :loading="loading"
    @chart-click="handleChartClick"
    @chart-ready="handleChartReady"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ChatChart from './ChatChart.vue';

interface Props {
  width?: string;
  height?: string;
  title?: string;
  legend?: string[];
  xData?: any[];
  seriesData?: any[];
  showLegend?: boolean;
  showDataZoom?: boolean;
  showToolbox?: boolean;
  loading?: boolean;
}

withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  title: '',
  legend: () => [],
  xData: () => [],
  seriesData: () => [],
  showLegend: true,
  showDataZoom: false,
  showToolbox: true,
  loading: false
});

const emit = defineEmits<{
  (e: 'chartClick', data: any): void;
  (e: 'chartReady', chart: any): void;
}>();

const chartRef = ref<any>(null);

const handleChartClick = (data: any) => {
  emit('chartClick', data);
};

const handleChartReady = (chart: any) => {
  emit('chartReady', chart);
};

defineExpose({
  resize: () => chartRef.value?.resize(),
  showLoading: () => chartRef.value?.showLoading(),
  hideLoading: () => chartRef.value?.hideLoading(),
  destroy: () => chartRef.value?.destroy()
});
</script>
