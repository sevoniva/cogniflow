<template>
  <ChatChart
    ref="chartRef"
    type="gauge"
    :width="width"
    :height="height"
    :title="title"
    :data="gaugeData"
    :show-toolbox="showToolbox"
    :loading="loading"
    @chart-click="handleChartClick"
    @chart-ready="handleChartReady"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import ChatChart from './ChatChart.vue';

interface Props {
  width?: string;
  height?: string;
  title?: string;
  data?: any[];
  showToolbox?: boolean;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '300px',
  title: '',
  data: () => [],
  showToolbox: true,
  loading: false
});

const emit = defineEmits<{
  (e: 'chartClick', data: any): void;
  (e: 'chartReady', chart: any): void;
}>();

const chartRef = ref<any>(null);

const gaugeData = computed(() => {
  return props.data.length > 0 ? props.data : [{ value: 0, name: '' }];
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
