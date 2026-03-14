<template>
  <ChatChart
    ref="chartRef"
    type="pie"
    :width="width"
    :height="height"
    :title="title"
    :data="data"
    :show-legend="showLegend"
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
  data?: any[];
  showLegend?: boolean;
  showToolbox?: boolean;
  loading?: boolean;
}

withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  title: '',
  data: () => [],
  showLegend: true,
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
