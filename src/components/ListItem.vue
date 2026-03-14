<template>
  <div 
    class="cb-list-item" 
    :class="{ 
      'is-clickable': clickable, 
      'is-active': active,
      'has-actions': $slots.actions 
    }"
    @click="handleClick"
  >
    <div class="item-main">
      <div v-if="$slots.icon" class="item-icon">
        <slot name="icon" />
      </div>
      <div class="item-content">
        <div v-if="title" class="item-title">{{ title }}</div>
        <div v-if="description" class="item-desc">{{ description }}</div>
        <slot name="content" />
      </div>
    </div>
    <div v-if="$slots.actions || meta" class="item-meta">
      <span v-if="meta" class="meta-text">{{ meta }}</span>
      <div v-if="$slots.actions" class="item-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  description?: string
  meta?: string
  clickable?: boolean
  active?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  clickable: false,
  active: false
})

const emit = defineEmits<{
  click: []
}>()

function handleClick() {
  if (props.clickable) {
    emit('click')
  }
}
</script>

<style scoped>
.cb-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--cb-space-md) var(--cb-space-lg);
  border-bottom: 1px solid var(--cb-border-lighter);
  transition: all 0.15s ease;
  gap: var(--cb-space-md);
}

.cb-list-item:last-child {
  border-bottom: none;
}

.cb-list-item.is-clickable {
  cursor: pointer;
}

.cb-list-item.is-clickable:hover {
  background: var(--cb-bg-hover);
}

.cb-list-item.is-active {
  background: var(--cb-bg-active);
  border-left: 3px solid var(--cb-primary);
}

.item-main {
  display: flex;
  align-items: center;
  gap: var(--cb-space-md);
  flex: 1;
  min-width: 0;
}

.item-icon {
  color: var(--cb-text-secondary);
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: var(--cb-font-size-md);
  font-weight: var(--cb-font-weight-medium);
  color: var(--cb-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}

.item-desc {
  font-size: var(--cb-font-size-sm);
  color: var(--cb-text-secondary);
  margin-top: var(--cb-space-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: var(--cb-space-md);
  flex-shrink: 0;
}

.meta-text {
  font-size: var(--cb-font-size-sm);
  color: var(--cb-text-secondary);
}

.item-actions {
  display: flex;
  gap: var(--cb-space-xs);
}

@media (max-width: 640px) {
  .cb-list-item.has-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--cb-space-md);
  }
  
  .cb-list-item.has-actions .item-meta {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
