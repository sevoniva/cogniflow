<template>
  <div class="ai-banner" :class="{ compact }">
    <div class="ai-banner__main">
      <div class="ai-banner__eyebrow">AI Runtime</div>
      <div class="ai-banner__title">
        {{ runtimeTitle }}
      </div>
      <p class="ai-banner__desc">
        {{ descriptionText }}
      </p>
      <div class="ai-banner__tags">
        <el-tag :type="status.runtimeEnabled ? 'success' : 'info'" effect="plain" round>
          {{ status.runtimeEnabled ? '外部模型已启用' : '业务语义引擎模式' }}
        </el-tag>
        <el-tag effect="plain" round>
          {{ status.mode === 'llm' ? '自由问答' : '指标语义问答' }}
        </el-tag>
        <el-tag v-if="status.providerName" effect="plain" round>
          {{ status.providerName }}
        </el-tag>
        <el-tag v-if="status.model" effect="plain" round>
          {{ status.model }}
        </el-tag>
      </div>
    </div>
    <div class="ai-banner__aside">
      <div class="ai-banner__state" :class="status.runtimeEnabled ? 'is-ready' : 'is-guided'">
        <strong>{{ status.runtimeEnabled ? 'Ready' : 'Guided' }}</strong>
        <span>{{ status.runtimeEnabled ? 'External LLM' : 'Semantic BI' }}</span>
      </div>
      <div v-if="$slots.actions" class="ai-banner__actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiRuntimeStatus } from '@/types'

interface Props {
  status: AiRuntimeStatus
  compact?: boolean
  title?: string
  description?: string
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
  title: '',
  description: ''
})

const runtimeTitle = computed(() => {
  if (props.title) {
    return props.title
  }

  if (props.status.runtimeEnabled) {
    return `当前由 ${props.status.providerName || props.status.defaultProvider} 提供外部 AI 分析能力`
  }

  return '当前未启用外部大模型，系统使用真实业务指标和同义词语义引擎'
})

const descriptionText = computed(() => {
  if (props.description) {
    return props.description
  }

  if (props.status.runtimeEnabled) {
    return `${props.status.providerName || props.status.defaultProvider} 已就绪，可处理未预先配置的自由问答，同时保留业务语义兜底。`
  }

  return props.status.reason
})
</script>

<style scoped>
.ai-banner {
  display: flex;
  justify-content: space-between;
  gap: var(--cb-space-lg);
  padding: var(--cb-space-lg);
  border-radius: var(--cb-radius-xl);
  border: 1px solid rgba(47, 107, 255, 0.14);
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.12), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.97), rgba(240, 247, 255, 0.92));
}

.ai-banner.compact {
  padding: var(--cb-space-md) var(--cb-space-lg);
}

.ai-banner__main {
  flex: 1;
}

.ai-banner__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--cb-primary);
}

.ai-banner__title {
  margin-top: 10px;
  font-size: 22px;
  line-height: 1.4;
  color: var(--cb-indigo);
  font-weight: 700;
}

.ai-banner.compact .ai-banner__title {
  font-size: 18px;
}

.ai-banner__desc {
  margin: 10px 0 0;
  color: var(--cb-text-regular);
  line-height: 1.7;
}

.ai-banner__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cb-space-sm);
  margin-top: var(--cb-space-md);
}

.ai-banner__aside {
  min-width: 180px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--cb-space-md);
}

.ai-banner__state {
  min-width: 148px;
  padding: 14px 16px;
  border-radius: 16px;
  color: #fff;
  box-shadow: var(--cb-shadow-sm);
}

.ai-banner__state strong,
.ai-banner__state span {
  display: block;
}

.ai-banner__state strong {
  font-size: 20px;
}

.ai-banner__state span {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.86;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ai-banner__state.is-ready {
  background: linear-gradient(135deg, #1f7a6c, #14b8a6);
}

.ai-banner__state.is-guided {
  background: linear-gradient(135deg, #2f6bff, #537ef5);
}

.ai-banner__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cb-space-sm);
}

@media (max-width: 900px) {
  .ai-banner {
    flex-direction: column;
  }

  .ai-banner__aside {
    min-width: 0;
    align-items: flex-start;
  }

  .ai-banner__actions {
    justify-content: flex-start;
  }
}
</style>
