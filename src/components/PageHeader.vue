<template>
  <header class="page-header" :class="{ 'has-back': showBack }">
    <div class="header-content" :style="contentStyle">
      <div class="left">
        <el-button v-if="showBack" link class="back-btn" @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </el-button>
        <div class="brand">
          <h1 class="title">Chat BI</h1>
          <span class="divider"></span>
          <span class="subtitle">{{ subtitle }}</span>
        </div>
      </div>
      <div v-if="$slots.actions" class="actions">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

interface Props {
  subtitle: string
  showBack?: boolean
  backPath?: string
  maxWidth?: string
}

const props = withDefaults(defineProps<Props>(), {
  showBack: false,
  backPath: '',
  maxWidth: '960px'
})

const emit = defineEmits<{
  back: []
}>()

const router = useRouter()

const contentStyle = computed(() => ({
  maxWidth: props.maxWidth
}))

function handleBack() {
  if (props.backPath) {
    router.push(props.backPath)
  } else {
    emit('back')
  }
}
</script>

<style scoped>
.page-header {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(111, 144, 255, 0.12);
  height: var(--cb-header-height);
  display: flex;
  align-items: center;
  padding: 0 var(--cb-space-lg);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  margin: 0 auto;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left {
  display: flex;
  align-items: center;
  gap: var(--cb-space-md);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: var(--cb-space-xs);
  color: var(--cb-text-regular);
  font-size: var(--cb-font-size-md);
  padding: var(--cb-space-xs) var(--cb-space-sm);
  border-radius: var(--cb-radius-sm);
  transition: all 0.2s;
}

.back-btn:hover {
  color: var(--cb-primary);
  background: var(--cb-primary-light);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--cb-space-sm);
}

.title {
  font-size: var(--cb-font-size-xl);
  font-weight: var(--cb-font-weight-bold);
  color: var(--cb-indigo);
  margin: 0;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.divider {
  width: 1px;
  height: 16px;
  background: var(--cb-border-base);
}

.subtitle {
  font-size: var(--cb-font-size-md);
  color: var(--cb-text-regular);
  font-weight: var(--cb-font-weight-medium);
  letter-spacing: 0.02em;
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--cb-space-sm);
}

@media (max-width: 640px) {
  .page-header {
    padding: 0 var(--cb-space-md);
  }
  
  .title {
    font-size: var(--cb-font-size-lg);
  }
  
  .subtitle {
    font-size: var(--cb-font-size-sm);
  }
}
</style>
