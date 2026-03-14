<template>
  <div class="ai-admin-page">
    <div class="hero-grid">
      <Card padding="lg" shadow="md">
        <AiRuntimeBanner
          :status="status"
          title="AI 运行配置"
          description="这里可以直接管理外部模型开关、默认提供商和 API 配置。保存后立即生效，用户侧会继续只查询真实业务数据。"
        >
          <template #actions>
            <el-button type="primary" plain @click="loadAll">刷新状态</el-button>
            <el-button plain @click="router.push('/chatbi/conversation')">打开对话页</el-button>
          </template>
        </AiRuntimeBanner>

        <div class="runtime-panel">
          <div>
            <strong>外部 AI 总开关</strong>
            <p>关闭时，所有用户问题都会走业务语义引擎；打开后，未命中指标的问题可交给默认外部模型。</p>
          </div>
          <div class="runtime-panel__actions">
            <el-switch
              v-model="runtimeEnabled"
              inline-prompt
              active-text="开启"
              inactive-text="关闭"
            />
            <el-button type="primary" :loading="runtimeSaving" @click="saveRuntime">
              保存运行开关
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="!status.runtimeEnabled"
          type="warning"
          :closable="false"
          show-icon
          class="runtime-alert"
          title="当前没有真实外部 AI 调用"
          :description="status.reason || '请先开启总开关，并为默认提供商配置 API Key 与启用状态。'"
        />

        <div class="command-panel">
          <div class="command-panel__header">
            <strong>当前生效条件</strong>
            <span>无需重启，可直接保存并测试连接</span>
          </div>
          <div class="check-list">
            <div class="check-item" :class="{ ready: status.enabled }">
              <span class="check-item__dot"></span>
              <span>外部 AI 总开关已开启</span>
            </div>
            <div class="check-item" :class="{ ready: status.providerEnabled }">
              <span class="check-item__dot"></span>
              <span>默认提供商已启用</span>
            </div>
            <div class="check-item" :class="{ ready: status.apiKeyConfigured }">
              <span class="check-item__dot"></span>
              <span>默认提供商已配置 API Key</span>
            </div>
          </div>
        </div>
      </Card>

      <Card padding="lg">
        <template #header>
          <span class="card-title">当前能力说明</span>
        </template>
        <div class="facts-grid">
          <div class="fact-item">
            <span>运行模式</span>
            <strong>{{ status.runtimeEnabled ? '外部模型 + 语义引擎' : '语义引擎' }}</strong>
          </div>
          <div class="fact-item">
            <span>默认提供商</span>
            <strong>{{ status.providerName || status.defaultProvider }}</strong>
          </div>
          <div class="fact-item">
            <span>模型</span>
            <strong>{{ status.model || '-' }}</strong>
          </div>
          <div class="fact-item">
            <span>API Key</span>
            <strong>{{ status.apiKeyConfigured ? '已配置' : '未配置' }}</strong>
          </div>
        </div>

        <div class="usage-list">
          <div class="usage-item">
            <span class="usage-item__index">1</span>
            <div>
              <strong>经营指标问答</strong>
              <p>像“本月营收”“投诉量按区域分布”这类问题，始终优先查询真实业务表，不依赖外部模型。</p>
            </div>
          </div>
          <div class="usage-item">
            <span class="usage-item__index">2</span>
            <div>
              <strong>泛化自由问答</strong>
              <p>像“帮我总结一下经营情况”“风险点有哪些”这类开放问题，启用外部模型后会优先走默认提供商。</p>
            </div>
          </div>
          <div class="usage-item">
            <span class="usage-item__index">3</span>
            <div>
              <strong>验证方法</strong>
              <p>先在本页保存并测试连接，再到对话页提问。如果这里显示语义模式，就不会产生 Kimi 调用记录。</p>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <Card padding="lg" shadow="md" class="provider-card-shell">
      <template #header>
        <div class="provider-header">
          <span class="card-title">提供商状态</span>
          <el-tag effect="plain">{{ providerList.length }} 个提供商</el-tag>
        </div>
      </template>

      <div class="provider-grid">
        <div v-for="provider in providerList" :key="provider.key" class="provider-card">
          <div class="provider-card__head">
            <div>
              <strong>{{ provider.name }}</strong>
              <p>{{ provider.key }}</p>
            </div>
            <div class="provider-card__tags">
              <el-tag :type="provider.selected ? 'success' : 'info'" effect="plain">
                {{ provider.selected ? '当前默认' : '可切换' }}
              </el-tag>
              <el-tag :type="provider.enabled ? 'success' : 'warning'" effect="plain">
                {{ provider.enabled ? '已启用' : '未启用' }}
              </el-tag>
            </div>
          </div>

          <div class="provider-card__meta">
            <span>模型：{{ provider.model || '-' }}</span>
            <span>地址：{{ provider.apiUrl || '-' }}</span>
            <span>温度：{{ provider.temperature ?? 0.7 }}</span>
            <span>Token：{{ provider.maxTokens ?? 2000 }}</span>
            <span>Key：{{ provider.apiKeyConfigured ? '已配置' : '未配置' }}</span>
          </div>

          <div class="provider-card__actions">
            <el-button size="small" plain :disabled="provider.selected" @click="switchProvider(provider.key)">
              设为默认
            </el-button>
            <el-button size="small" plain @click="openProviderDialog(provider)">
              配置
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="!provider.enabled || !provider.apiKeyConfigured || testingProvider === provider.key"
              :loading="testingProvider === provider.key"
              @click="testConnection(provider.key)"
            >
              测试连接
            </el-button>
          </div>
        </div>
      </div>
    </Card>

    <Card padding="lg" shadow="md">
      <template #header>
        <div class="provider-header">
          <span class="card-title">AI 调用观测</span>
          <div class="observability-head-actions">
            <el-tag :type="healthTagType(observability.healthStatus)" effect="dark">
              {{ healthLabel(observability.healthStatus) }}
            </el-tag>
            <el-button text @click="loadObservability">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="health-strip">
        <div class="health-strip__item">
          <span>健康评分</span>
          <strong>{{ observability.healthScore }}</strong>
        </div>
        <div class="health-strip__item">
          <span>观测窗口</span>
          <strong>{{ observability.windowMinutes }} 分钟</strong>
        </div>
        <div class="health-strip__item">
          <span>状态说明</span>
          <strong>{{ healthDescription(observability.healthStatus) }}</strong>
        </div>
      </div>

      <div class="threshold-panel">
        <div class="threshold-panel__header">
          <strong>观测阈值策略</strong>
          <div class="threshold-panel__actions">
            <el-button size="small" @click="loadObservabilityThresholds">刷新阈值</el-button>
            <el-button type="primary" size="small" :loading="thresholdSaving" @click="saveObservabilityThresholds">
              保存阈值
            </el-button>
          </div>
        </div>
        <div class="threshold-grid">
          <el-form-item label="窗口(分钟)">
            <el-input-number v-model="thresholdForm.windowMinutes" :min="1" :max="120" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最小样本">
            <el-input-number v-model="thresholdForm.minSampleSizeForAlert" :min="1" :max="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="失败率预警">
            <el-input-number v-model="thresholdForm.failureRateWarning" :min="0" :max="1" :step="0.01" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="失败率严重">
            <el-input-number v-model="thresholdForm.failureRateCritical" :min="0" :max="1" :step="0.01" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="延迟预警(ms)">
            <el-input-number v-model="thresholdForm.latencyWarningMs" :min="1" :max="120000" :step="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="延迟严重(ms)">
            <el-input-number v-model="thresholdForm.latencyCriticalMs" :min="1" :max="120000" :step="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="连续失败预警">
            <el-input-number v-model="thresholdForm.consecutiveFailureWarning" :min="1" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="连续失败严重">
            <el-input-number v-model="thresholdForm.consecutiveFailureCritical" :min="1" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="分类峰值预警">
            <el-input-number v-model="thresholdForm.categorySpikeWarning" :min="1" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="分类峰值严重">
            <el-input-number v-model="thresholdForm.categorySpikeCritical" :min="1" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最近调用保留">
            <el-input-number v-model="thresholdForm.recentCallLimit" :min="1" :max="500" style="width: 100%" />
          </el-form-item>
          <el-form-item label="切换事件保留">
            <el-input-number v-model="thresholdForm.switchEventLimit" :min="1" :max="500" style="width: 100%" />
          </el-form-item>
        </div>
      </div>

      <div class="facts-grid observability-grid">
        <div class="fact-item">
          <span>总调用数</span>
          <strong>{{ observability.totalCalls }}</strong>
        </div>
        <div class="fact-item">
          <span>成功率</span>
          <strong>{{ observability.successRate.toFixed(2) }}%</strong>
        </div>
        <div class="fact-item">
          <span>失败调用</span>
          <strong>{{ observability.failedCalls }}</strong>
        </div>
        <div class="fact-item">
          <span>平均耗时</span>
          <strong>{{ observability.avgLatencyMs }} ms</strong>
        </div>
      </div>

      <div class="alert-list">
        <div
          v-for="alert in observability.alerts"
          :key="`${alert.code}-${alert.timestamp}`"
          class="alert-item"
          :class="`is-${alert.level}`"
        >
          <div class="alert-item__head">
            <strong>{{ alert.title }}</strong>
            <el-tag :type="alert.level === 'critical' ? 'danger' : 'warning'" effect="plain" size="small">
              {{ alert.level === 'critical' ? '严重' : '预警' }}
            </el-tag>
          </div>
          <p>{{ alert.message }}</p>
        </div>
        <span v-if="!observability.alerts.length" class="form-tip">当前窗口内无可观测性告警</span>
      </div>

      <div class="failure-tags">
        <el-tag
          v-for="item in observability.failureCategoryEntries"
          :key="item.key"
          effect="plain"
          type="danger"
        >
          {{ item.key }}：{{ item.value }}
        </el-tag>
        <span v-if="!observability.failureCategoryEntries.length" class="form-tip">暂无失败分类记录</span>
      </div>

      <el-table
        :data="observability.providerRows"
        stripe
        border
        size="small"
        style="margin-top: 12px"
      >
        <el-table-column prop="provider" label="提供商" min-width="120" />
        <el-table-column prop="totalCalls" label="总调用" min-width="90" />
        <el-table-column prop="successCalls" label="成功" min-width="90" />
        <el-table-column prop="failedCalls" label="失败" min-width="90" />
        <el-table-column prop="successRate" label="成功率(%)" min-width="100" />
        <el-table-column prop="avgLatencyMs" label="平均耗时(ms)" min-width="120" />
      </el-table>

      <el-table
        :data="observability.recentRows"
        stripe
        border
        size="small"
        style="margin-top: 16px"
        max-height="320"
      >
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatTimestamp(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="提供商" min-width="100" />
        <el-table-column prop="operation" label="操作" min-width="110" />
        <el-table-column label="结果" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="plain">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" min-width="100" />
        <el-table-column prop="attempts" label="重试次数" min-width="90" />
        <el-table-column prop="category" label="分类" min-width="110" />
      </el-table>

      <el-table
        :data="observability.alertHistoryRows"
        stripe
        border
        size="small"
        style="margin-top: 16px"
        max-height="260"
      >
        <template #header>
          <div class="alert-history-toolbar">
            <span>告警历史</span>
            <div class="alert-history-toolbar__controls">
              <el-select v-model="alertHistoryFilter.status" size="small" style="width: 120px">
                <el-option label="全部" value="all" />
                <el-option label="告警" value="alert" />
                <el-option label="恢复" value="recovery" />
              </el-select>
              <el-input
                v-model.trim="alertHistoryFilter.keyword"
                size="small"
                clearable
                placeholder="搜索摘要"
                style="width: 180px"
              />
              <el-select v-model="alertHistoryFilter.limit" size="small" style="width: 110px">
                <el-option label="20 条" :value="20" />
                <el-option label="50 条" :value="50" />
                <el-option label="100 条" :value="100" />
              </el-select>
              <el-button size="small" @click="loadObservabilityAlertHistory">
                刷新历史
              </el-button>
            </div>
          </div>
        </template>
        <el-table-column label="告警时间" min-width="170">
          <template #default="{ row }">
            {{ formatTimestamp(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="事件" min-width="170" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'recovery' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'recovery' ? '恢复' : '告警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="280" show-overflow-tooltip />
      </el-table>

      <el-table
        :data="observability.thresholdHistoryRows"
        stripe
        border
        size="small"
        style="margin-top: 16px"
        max-height="220"
      >
        <template #header>
          <div class="alert-history-toolbar">
            <span>阈值变更历史</span>
            <div class="alert-history-toolbar__controls">
              <el-select v-model="thresholdHistoryLimit" size="small" style="width: 110px">
                <el-option label="20 条" :value="20" />
                <el-option label="50 条" :value="50" />
                <el-option label="100 条" :value="100" />
              </el-select>
              <el-button size="small" @click="loadObservabilityThresholdHistory">刷新历史</el-button>
            </div>
          </div>
        </template>
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatTimestamp(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" min-width="100" />
        <el-table-column label="阈值摘要" min-width="360" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatThresholdSummary(row.thresholds) }}
          </template>
        </el-table-column>
      </el-table>
    </Card>

    <el-dialog
      v-model="providerDialogVisible"
      width="680px"
      :title="activeProvider ? `配置 ${activeProvider.name}` : '配置提供商'"
      destroy-on-close
    >
      <el-form label-position="top" class="provider-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="展示名称">
              <el-input v-model="providerForm.name" placeholder="例如：Kimi" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型">
              <el-input v-model="providerForm.model" placeholder="例如：moonshot-v1-32k" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="API 地址">
          <el-input v-model="providerForm.apiUrl" placeholder="例如：https://api.moonshot.cn/v1" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="温度系数">
              <el-input-number v-model="providerForm.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大 Token">
              <el-input-number v-model="providerForm.maxTokens" :min="128" :max="32768" :step="128" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="API Key">
          <el-input
            v-model="providerForm.apiKey"
            type="password"
            show-password
            :placeholder="activeProvider?.apiKeyConfigured ? '已保存，留空则保持不变' : '请输入真实 API Key'"
          />
          <div class="form-tip">
            Key 仅在后端持久化保存并即时生效，前端不会回显原值。
          </div>
        </el-form-item>

        <div class="form-row-inline">
          <el-switch v-model="providerForm.enabled" inline-prompt active-text="启用" inactive-text="停用" />
          <el-checkbox v-model="providerForm.clearApiKey">清空已保存的 API Key</el-checkbox>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="providerDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingProvider" @click="saveProvider">
            保存配置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Card from '@/components/Card.vue'
import AiRuntimeBanner from '@/components/AiRuntimeBanner.vue'
import type { AiRuntimeStatus } from '@/types'
import { request } from '@/utils/http'

interface AiProviderInfo {
  key: string
  name: string
  model?: string | null
  enabled: boolean
  apiUrl?: string | null
  apiKeyConfigured: boolean
  selected: boolean
  temperature?: number | null
  maxTokens?: number | null
}

interface AiProviderPayload extends AiRuntimeStatus {
  providers?: Record<string, {
    name?: string
    model?: string | null
    enabled?: boolean
    apiUrl?: string | null
    apiKeyConfigured?: boolean
    selected?: boolean
    temperature?: number | null
    maxTokens?: number | null
  }>
}

interface ProviderFormState {
  name: string
  apiUrl: string
  model: string
  enabled: boolean
  apiKey: string
  clearApiKey: boolean
  temperature: number
  maxTokens: number
}

interface AiProviderObservabilityRow {
  provider: string
  totalCalls: number
  successCalls: number
  failedCalls: number
  successRate: number
  avgLatencyMs: number
}

interface AiRecentCallRow {
  timestamp: number
  provider: string
  operation: string
  success: boolean
  durationMs: number
  attempts: number
  category: string
}

interface AiObservabilityResponse {
  totalCalls: number
  successCalls: number
  failedCalls: number
  successRate: number
  avgLatencyMs: number
  healthStatus?: string
  healthScore?: number
  windowMinutes?: number
  failureCategories?: Record<string, number>
  providers?: AiProviderObservabilityRow[]
  recentCalls?: AiRecentCallRow[]
  alerts?: Array<{
    code: string
    level: 'critical' | 'warning'
    title: string
    message: string
    timestamp: number
  }>
  thresholds?: AiObservabilityThresholds
}

interface AiObservabilityAlertHistoryRow {
  id: number
  timestamp: number
  action: string
  status: 'alert' | 'recovery'
  summary: string
}

interface AiObservabilityThresholds {
  windowMinutes: number
  minSampleSizeForAlert: number
  failureRateWarning: number
  failureRateCritical: number
  latencyWarningMs: number
  latencyCriticalMs: number
  consecutiveFailureWarning: number
  consecutiveFailureCritical: number
  categorySpikeWarning: number
  categorySpikeCritical: number
  recentCallLimit: number
  switchEventLimit: number
}

interface AiObservabilityThresholdHistoryRow {
  id: number
  timestamp: number
  operator: string
  thresholds: Record<string, string>
}

const router = useRouter()
const status = ref<AiRuntimeStatus>({
  mode: 'semantic',
  enabled: false,
  runtimeEnabled: false,
  reason: '正在加载 AI 运行状态',
  defaultProvider: 'kimi',
  providerName: 'Kimi',
  model: null
})
const runtimeEnabled = ref(false)
const providers = ref<Record<string, AiProviderInfo>>({})
const testingProvider = ref('')
const runtimeSaving = ref(false)
const savingProvider = ref(false)
const thresholdSaving = ref(false)
const providerDialogVisible = ref(false)
const editingProviderKey = ref('')
const thresholdHistoryLimit = ref(20)
const observability = ref({
  totalCalls: 0,
  successCalls: 0,
  failedCalls: 0,
  successRate: 100,
  avgLatencyMs: 0,
  healthStatus: 'no-traffic',
  healthScore: 100,
  windowMinutes: 10,
  alerts: [] as Array<{
    code: string
    level: 'critical' | 'warning'
    title: string
    message: string
    timestamp: number
  }>,
  failureCategoryEntries: [] as Array<{ key: string; value: number }>,
  providerRows: [] as AiProviderObservabilityRow[],
  recentRows: [] as AiRecentCallRow[],
  alertHistoryRows: [] as AiObservabilityAlertHistoryRow[],
  thresholdHistoryRows: [] as AiObservabilityThresholdHistoryRow[]
})
const alertHistoryFilter = reactive({
  status: 'all',
  keyword: '',
  limit: 30
})
const providerForm = reactive<ProviderFormState>({
  name: '',
  apiUrl: '',
  model: '',
  enabled: false,
  apiKey: '',
  clearApiKey: false,
  temperature: 0.7,
  maxTokens: 2000
})
const thresholdForm = reactive<AiObservabilityThresholds>({
  windowMinutes: 10,
  minSampleSizeForAlert: 5,
  failureRateWarning: 0.2,
  failureRateCritical: 0.4,
  latencyWarningMs: 3000,
  latencyCriticalMs: 6000,
  consecutiveFailureWarning: 3,
  consecutiveFailureCritical: 6,
  categorySpikeWarning: 2,
  categorySpikeCritical: 4,
  recentCallLimit: 50,
  switchEventLimit: 30
})

const providerList = computed(() => Object.values(providers.value))
const activeProvider = computed(() => providers.value[editingProviderKey.value] || null)

async function loadStatus() {
  const response = await request<AiRuntimeStatus>('/ai-model/status')
  if (response.success && response.data) {
    status.value = response.data
    runtimeEnabled.value = response.data.enabled
  }
}

async function loadProviders() {
  const response = await request<AiProviderPayload>('/ai-model/providers')
  if (!response.success || !response.data?.providers) {
    return
  }

  const next: Record<string, AiProviderInfo> = {}
  Object.entries(response.data.providers).forEach(([key, item]) => {
    next[key] = {
      key,
      name: item.name || key,
      model: item.model || null,
      enabled: Boolean(item.enabled),
      apiUrl: item.apiUrl || null,
      apiKeyConfigured: Boolean(item.apiKeyConfigured),
      selected: Boolean(item.selected),
      temperature: item.temperature ?? 0.7,
      maxTokens: item.maxTokens ?? 2000
    }
  })
  providers.value = next
}

async function loadAll() {
  await Promise.all([
    loadStatus(),
    loadProviders(),
    loadObservability(),
    loadObservabilityAlertHistory(),
    loadObservabilityThresholds(),
    loadObservabilityThresholdHistory()
  ])
}

async function loadObservability() {
  const response = await request<AiObservabilityResponse>('/ai-model/observability')
  if (!response.success || !response.data) {
    return
  }
  const failureCategories = response.data.failureCategories || {}
  observability.value = {
    totalCalls: response.data.totalCalls ?? 0,
    successCalls: response.data.successCalls ?? 0,
    failedCalls: response.data.failedCalls ?? 0,
    successRate: response.data.successRate ?? 100,
    avgLatencyMs: response.data.avgLatencyMs ?? 0,
    healthStatus: response.data.healthStatus || 'no-traffic',
    healthScore: response.data.healthScore ?? 100,
    windowMinutes: response.data.windowMinutes ?? 10,
    alerts: response.data.alerts || [],
    failureCategoryEntries: Object.entries(failureCategories).map(([key, value]) => ({ key, value })),
    providerRows: response.data.providers || [],
    recentRows: response.data.recentCalls || [],
    alertHistoryRows: observability.value.alertHistoryRows,
    thresholdHistoryRows: observability.value.thresholdHistoryRows
  }
  if (response.data.thresholds) {
    applyThresholdForm(response.data.thresholds)
  }
}

async function loadObservabilityAlertHistory() {
  const params = new URLSearchParams()
  params.set('limit', String(alertHistoryFilter.limit))
  if (alertHistoryFilter.status && alertHistoryFilter.status !== 'all') {
    params.set('status', alertHistoryFilter.status)
  }
  if (alertHistoryFilter.keyword) {
    params.set('keyword', alertHistoryFilter.keyword)
  }
  const response = await request<AiObservabilityAlertHistoryRow[]>(`/ai-model/observability/alerts/history?${params.toString()}`)
  if (!response.success || !response.data) {
    return
  }
  observability.value.alertHistoryRows = response.data
}

async function loadObservabilityThresholds() {
  const response = await request<AiObservabilityThresholds>('/ai-model/observability/thresholds')
  if (!response.success || !response.data) {
    return
  }
  applyThresholdForm(response.data)
}

async function loadObservabilityThresholdHistory() {
  const response = await request<AiObservabilityThresholdHistoryRow[]>(
    `/ai-model/observability/thresholds/history?limit=${thresholdHistoryLimit.value}`
  )
  if (!response.success || !response.data) {
    return
  }
  observability.value.thresholdHistoryRows = response.data
}

function applyThresholdForm(thresholds: Partial<AiObservabilityThresholds>) {
  thresholdForm.windowMinutes = Number(thresholds.windowMinutes ?? thresholdForm.windowMinutes)
  thresholdForm.minSampleSizeForAlert = Number(thresholds.minSampleSizeForAlert ?? thresholdForm.minSampleSizeForAlert)
  thresholdForm.failureRateWarning = Number(thresholds.failureRateWarning ?? thresholdForm.failureRateWarning)
  thresholdForm.failureRateCritical = Number(thresholds.failureRateCritical ?? thresholdForm.failureRateCritical)
  thresholdForm.latencyWarningMs = Number(thresholds.latencyWarningMs ?? thresholdForm.latencyWarningMs)
  thresholdForm.latencyCriticalMs = Number(thresholds.latencyCriticalMs ?? thresholdForm.latencyCriticalMs)
  thresholdForm.consecutiveFailureWarning = Number(thresholds.consecutiveFailureWarning ?? thresholdForm.consecutiveFailureWarning)
  thresholdForm.consecutiveFailureCritical = Number(thresholds.consecutiveFailureCritical ?? thresholdForm.consecutiveFailureCritical)
  thresholdForm.categorySpikeWarning = Number(thresholds.categorySpikeWarning ?? thresholdForm.categorySpikeWarning)
  thresholdForm.categorySpikeCritical = Number(thresholds.categorySpikeCritical ?? thresholdForm.categorySpikeCritical)
  thresholdForm.recentCallLimit = Number(thresholds.recentCallLimit ?? thresholdForm.recentCallLimit)
  thresholdForm.switchEventLimit = Number(thresholds.switchEventLimit ?? thresholdForm.switchEventLimit)
}

async function saveObservabilityThresholds() {
  thresholdSaving.value = true
  try {
    const response = await request<AiObservabilityThresholds>('/ai-model/observability/thresholds', {
      method: 'PUT',
      body: JSON.stringify({ ...thresholdForm })
    })
    if (!response.success || !response.data) {
      ElMessage.error(response.error || '保存阈值配置失败')
      return
    }
    applyThresholdForm(response.data)
    ElMessage.success('观测阈值已保存')
    await Promise.all([loadObservability(), loadObservabilityThresholdHistory()])
  } finally {
    thresholdSaving.value = false
  }
}

async function saveRuntime() {
  runtimeSaving.value = true
  try {
    const response = await request<void>('/ai-model/runtime', {
      method: 'PUT',
      body: JSON.stringify({ enabled: runtimeEnabled.value })
    })
    if (!response.success) {
      ElMessage.error(response.error || '保存运行开关失败')
      return
    }
    ElMessage.success('AI 运行开关已保存')
    await loadAll()
  } finally {
    runtimeSaving.value = false
  }
}

function openProviderDialog(provider: AiProviderInfo) {
  editingProviderKey.value = provider.key
  providerForm.name = provider.name
  providerForm.apiUrl = provider.apiUrl || ''
  providerForm.model = provider.model || ''
  providerForm.enabled = provider.enabled
  providerForm.apiKey = ''
  providerForm.clearApiKey = false
  providerForm.temperature = provider.temperature ?? 0.7
  providerForm.maxTokens = provider.maxTokens ?? 2000
  providerDialogVisible.value = true
}

async function saveProvider() {
  if (!editingProviderKey.value) {
    return
  }

  savingProvider.value = true
  try {
    const response = await request<void>(`/ai-model/providers/${encodeURIComponent(editingProviderKey.value)}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: providerForm.name,
        apiUrl: providerForm.apiUrl,
        model: providerForm.model,
        enabled: providerForm.enabled,
        apiKey: providerForm.apiKey,
        clearApiKey: providerForm.clearApiKey,
        temperature: providerForm.temperature,
        maxTokens: providerForm.maxTokens
      })
    })
    if (!response.success) {
      ElMessage.error(response.error || '保存提供商配置失败')
      return
    }
    ElMessage.success('提供商配置已保存并生效')
    providerDialogVisible.value = false
    await loadAll()
  } finally {
    savingProvider.value = false
  }
}

async function switchProvider(provider: string) {
  const response = await request<void>(`/ai-model/default-provider?provider=${encodeURIComponent(provider)}`, {
    method: 'PUT'
  })
  if (!response.success) {
    ElMessage.error(response.error || '切换默认提供商失败')
    return
  }
  ElMessage.success(`已切换默认提供商为 ${providers.value[provider]?.name || provider}`)
  await loadAll()
}

async function testConnection(provider: string) {
  testingProvider.value = provider
  try {
    const response = await request<boolean>(`/ai-model/test/${encodeURIComponent(provider)}`, {
      method: 'POST'
    })
    if (!response.success) {
      ElMessage.error(response.error || '连接测试失败')
      return
    }
    ElMessage.success(response.data ? '连接测试成功' : '连接测试失败')
  } finally {
    testingProvider.value = ''
  }
}

onMounted(loadAll)

function formatTimestamp(timestamp: number) {
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function healthTagType(status: string) {
  if (status === 'critical') {
    return 'danger'
  }
  if (status === 'degraded') {
    return 'warning'
  }
  if (status === 'healthy') {
    return 'success'
  }
  return 'info'
}

function healthLabel(status: string) {
  if (status === 'critical') {
    return '状态: 严重'
  }
  if (status === 'degraded') {
    return '状态: 退化'
  }
  if (status === 'healthy') {
    return '状态: 健康'
  }
  return '状态: 无流量'
}

function healthDescription(status: string) {
  if (status === 'critical') {
    return '当前链路存在明显异常'
  }
  if (status === 'degraded') {
    return '可用但需持续关注'
  }
  if (status === 'healthy') {
    return '调用稳定'
  }
  return '尚未检测到调用'
}

function formatThresholdSummary(thresholds: Record<string, string>) {
  if (!thresholds) {
    return '-'
  }
  const keys = ['windowMinutes', 'failureRateWarning', 'failureRateCritical', 'latencyWarningMs', 'latencyCriticalMs']
  return keys
    .filter((key) => thresholds[key] !== undefined)
    .map((key) => `${key}=${thresholds[key]}`)
    .join(', ')
}
</script>

<style scoped>
.ai-admin-page {
  display: grid;
  gap: 24px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 24px;
}

.runtime-panel {
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid rgba(47, 107, 255, 0.12);
  background: linear-gradient(180deg, rgba(47, 107, 255, 0.05), rgba(15, 23, 42, 0.02));
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.runtime-panel strong {
  color: var(--cb-text-primary);
}

.runtime-panel p {
  margin: 8px 0 0;
  color: var(--cb-text-secondary);
  line-height: 1.7;
}

.runtime-panel__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.runtime-alert {
  margin-top: 18px;
}

.command-panel {
  margin-top: 18px;
  border: 1px solid rgba(47, 107, 255, 0.12);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(47, 107, 255, 0.05), rgba(10, 37, 64, 0.02));
  overflow: hidden;
}

.command-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px 12px;
  color: var(--cb-text-secondary);
  font-size: 13px;
}

.check-list {
  display: grid;
  gap: 10px;
  padding: 0 18px 18px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--cb-text-secondary);
}

.check-item.ready {
  color: #1b5e20;
}

.check-item__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d0d7e6;
}

.check-item.ready .check-item__dot {
  background: #67c23a;
}

.facts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.fact-item {
  padding: 16px;
  border-radius: 16px;
  background: #f6f8fc;
  border: 1px solid #e7edf7;
}

.fact-item span {
  display: block;
  font-size: 13px;
  color: var(--cb-text-secondary);
  margin-bottom: 8px;
}

.fact-item strong {
  font-size: 17px;
  color: var(--cb-text-primary);
}

.usage-list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.usage-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.usage-item__index {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #2f6bff;
  color: #fff;
  font-weight: 700;
}

.alert-history-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.alert-history-toolbar__controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.usage-item strong {
  display: block;
  color: var(--cb-text-primary);
  margin-bottom: 6px;
}

.usage-item p {
  margin: 0;
  color: var(--cb-text-secondary);
  line-height: 1.7;
}

.provider-card-shell {
  overflow: hidden;
}

.observability-grid {
  margin-bottom: 12px;
}

.observability-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.health-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.health-strip__item {
  border: 1px solid #e6edf9;
  border-radius: 14px;
  background: #fbfcff;
  padding: 12px 14px;
}

.health-strip__item span {
  display: block;
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.health-strip__item strong {
  display: block;
  margin-top: 6px;
  color: var(--cb-text-primary);
}

.threshold-panel {
  margin-bottom: 12px;
  border: 1px solid #e6edf9;
  border-radius: 14px;
  padding: 14px;
  background: #fcfdff;
}

.threshold-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.threshold-panel__actions {
  display: inline-flex;
  gap: 8px;
}

.threshold-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px 12px;
}

.threshold-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.alert-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.alert-item {
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid #e6edf9;
  background: #fafcff;
}

.alert-item.is-warning {
  border-color: rgba(230, 162, 60, 0.34);
  background: rgba(230, 162, 60, 0.08);
}

.alert-item.is-critical {
  border-color: rgba(245, 108, 108, 0.34);
  background: rgba(245, 108, 108, 0.08);
}

.alert-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.alert-item p {
  margin: 6px 0 0;
  color: var(--cb-text-secondary);
  line-height: 1.6;
}

.failure-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.provider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.provider-card {
  padding: 20px;
  border-radius: 18px;
  border: 1px solid #e7edf7;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  display: grid;
  gap: 16px;
}

.provider-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.provider-card__head strong {
  color: var(--cb-text-primary);
}

.provider-card__head p {
  margin: 6px 0 0;
  color: var(--cb-text-secondary);
}

.provider-card__tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.provider-card__meta {
  display: grid;
  gap: 8px;
  color: var(--cb-text-secondary);
  font-size: 13px;
}

.provider-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.provider-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.form-tip {
  margin-top: 8px;
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.form-row-inline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding-top: 6px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.card-title {
  color: var(--cb-text-primary);
  font-weight: 700;
}

@media (max-width: 1200px) {
  .provider-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-grid,
  .provider-grid {
    grid-template-columns: 1fr;
  }

  .runtime-panel,
  .form-row-inline {
    flex-direction: column;
    align-items: stretch;
  }

  .runtime-panel__actions {
    justify-content: space-between;
  }

  .health-strip {
    grid-template-columns: 1fr;
  }

  .threshold-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
