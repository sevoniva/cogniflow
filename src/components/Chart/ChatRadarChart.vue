<template>
  <ChatChart
    ref="chartRef"
    type="radar"
    :width="width"
    :height="height"
    :title="title"
    :legend="legend"
    :data="radarData"
    :series-data="seriesData"
    :show-legend="showLegend"
    :show-toolbox="showToolbox"
    :loading="loading"
    @chart-click="handleChartClick"
    @chart-ready="handleChartReady"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import ChatChart from './ChatChart.vue';

interface Props {
  width?: string;
  height?: string;
  title?: string;
  legend?: string[];
  data?: any[];
  seriesData?: any[];
  showLegend?: boolean;
  showToolbox?: boolean;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  title: '',
  legend: () => [],
  data: () => [],
  seriesData: () => [],
  showLegend: true,
  showToolbox: true,
  loading: false
});

const emit = defineEmits<{
  (e: 'chartClick', data: any): void;
  (e: 'chartReady', chart: any): void;
}>();

const chartRef = ref<any>(null);

const radarData = computed(() => {
  return props.data.length > 0 ? props.data : [{ indicators: [] }];
});

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
