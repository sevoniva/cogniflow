<template>
  <ChatChart ref="chartRef" type="gauge" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ChatChart from './ChatChart.vue'

const chartRef = ref<any>(null)
defineExpose({
  resize: () => chartRef.value?.resize(),
  showLoading: () => chartRef.value?.showLoading(),
  hideLoading: () => chartRef.value?.hideLoading(),
  destroy: () => chartRef.value?.destroy()
})
</script>
