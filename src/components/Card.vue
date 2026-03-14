<template>
  <div class="cb-card" :class="{ 'has-header': $slots.header, 'is-hoverable': hoverable }" :style="cardStyle">
    <div v-if="$slots.header" class="cb-card-header">
      <slot name="header" />
    </div>
    <div class="cb-card-body" :class="bodyClass">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  padding?: 'none' | 'sm' | 'md' | 'lg'
  hoverable?: boolean
  shadow?: 'none' | 'sm' | 'md'
}

const props = withDefaults(defineProps<Props>(), {
  padding: 'md',
  hoverable: false,
  shadow: 'sm'
})

const shadowMap = {
  none: 'none',
  sm: 'var(--cb-shadow-sm)',
  md: 'var(--cb-shadow-md)'
}

const cardStyle = computed(() => ({
  boxShadow: shadowMap[props.shadow]
}))

const bodyClass = computed(() => `padding-${props.padding}`)
</script>

<style scoped>
.cb-card {
  background: var(--cb-bg-card);
  border: 1px solid rgba(129, 157, 219, 0.12);
  border-radius: var(--cb-radius-xl);
  overflow: hidden;
  transition: all 0.2s ease;
  backdrop-filter: blur(12px);
}

.cb-card.is-hoverable:hover {
  box-shadow: var(--cb-shadow-md);
  transform: translateY(-3px);
}

.cb-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--cb-space-md) var(--cb-space-lg);
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.95), rgba(242, 247, 255, 0.92));
  border-bottom: 1px solid rgba(129, 157, 219, 0.12);
}

.cb-card-header :deep(.card-title) {
  font-size: var(--cb-font-size-md);
  font-weight: var(--cb-font-weight-bold);
  color: var(--cb-text-primary);
}

.cb-card-body.padding-none {
  padding: 0;
}

.cb-card-body.padding-sm {
  padding: var(--cb-space-md);
}

.cb-card-body.padding-md {
  padding: var(--cb-space-lg);
}

.cb-card-body.padding-lg {
  padding: var(--cb-space-xl);
}
</style>
