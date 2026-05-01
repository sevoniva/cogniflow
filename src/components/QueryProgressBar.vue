<template>
  <div v-if="progress && progress.stage !== 'DONE'" class="query-progress">
    <el-progress
      :percentage="progress.percent"
      :status="progress.stage === 'ERROR' ? 'exception' : undefined"
      :stroke-width="6"
      :show-text="false"
    />
    <div class="query-progress__label">
      <el-tag size="small" :type="tagType">{{ progress.message }}</el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QueryProgress } from '@/composables/useQueryProgress'

const props = defineProps<{
  progress: QueryProgress | null
}>()

const tagType = computed(() => {
  if (!props.progress) return 'info'
  switch (props.progress.stage) {
    case 'ERROR': return 'danger'
    case 'DONE': return 'success'
    default: return 'primary'
  }
})
</script>

<style scoped>
.query-progress {
  padding: 8px 16px;
  background: rgba(47, 107, 255, 0.04);
  border-radius: 12px;
  margin-bottom: 12px;
}
.query-progress__label {
  margin-top: 6px;
  font-size: 12px;
}
</style>
