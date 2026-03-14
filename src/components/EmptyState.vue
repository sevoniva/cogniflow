<template>
  <div class="empty-state" :class="`empty-${type}`">
    <div class="empty-icon">
      <el-icon :size="iconSize">
        <component :is="iconComponent" />
      </el-icon>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { 
  Search, 
  FolderOpened, 
  Document, 
  DataLine, 
  Warning,
  Star
} from '@element-plus/icons-vue'

type EmptyType = 'search' | 'data' | 'document' | 'chart' | 'warning' | 'favorite' | 'default'

interface Props {
  type?: EmptyType
  title?: string
  description?: string
  iconSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  title: '暂无数据',
  description: '',
  iconSize: 64
})

const iconMap: Record<EmptyType, any> = {
  search: Search,
  data: DataLine,
  document: Document,
  chart: DataLine,
  warning: Warning,
  favorite: Star,
  default: FolderOpened
}

const iconComponent = computed(() => iconMap[props.type])

const defaultTitles: Record<EmptyType, string> = {
  search: '未找到匹配结果',
  data: '暂无数据',
  document: '暂无文档',
  chart: '暂无图表',
  warning: '出错了',
  favorite: '暂无收藏',
  default: '暂无数据'
}

const title = computed(() => props.title || defaultTitles[props.type])
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--cb-space-xxl) var(--cb-space-lg);
  text-align: center;
}

.empty-icon {
  color: var(--cb-border-base);
  margin-bottom: var(--cb-space-md);
}

.empty-search .empty-icon {
  color: var(--cb-primary);
  opacity: 0.5;
}

.empty-favorite .empty-icon {
  color: var(--cb-warning);
  opacity: 0.5;
}

.empty-warning .empty-icon {
  color: var(--cb-danger);
  opacity: 0.5;
}

.empty-title {
  font-size: var(--cb-font-size-lg);
  font-weight: var(--cb-font-weight-medium);
  color: var(--cb-text-primary);
  margin: 0 0 var(--cb-space-sm);
}

.empty-desc {
  font-size: var(--cb-font-size-md);
  color: var(--cb-text-secondary);
  margin: 0 0 var(--cb-space-lg);
  line-height: 1.6;
  max-width: 320px;
}

.empty-actions {
  display: flex;
  gap: var(--cb-space-sm);
}
</style>
