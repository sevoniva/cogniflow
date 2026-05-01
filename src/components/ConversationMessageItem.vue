<template>
  <div
    class="message-row"
    :class="[`is-${msg.role}`, { 'has-data': Boolean(msg.data?.length) }]"
  >
    <div class="message-card" :class="`is-${msg.role}`">
      <div class="message-avatar">
        <el-icon v-if="msg.role === 'user'"><User /></el-icon>
        <el-icon v-else><Service /></el-icon>
      </div>

      <div class="message-content">
        <div class="message-meta">
          <strong>{{ msg.role === 'user' ? '你' : 'ChatBI' }}</strong>
          <span>{{ formatTime(msg.timestamp) }}</span>
          <el-tag v-if="msg.metric" size="small" effect="plain">{{ msg.metric }}</el-tag>
          <el-tag
            v-if="msg.disambiguation"
            size="small"
            type="warning"
            effect="dark"
          >
            指标待澄清
          </el-tag>
          <el-tag
            v-if="msg.role === 'assistant' && msg.source"
            size="small"
            effect="plain"
            :type="msg.source === 'llm' ? 'success' : 'info'"
          >
            {{ sourceLabel(msg.source) }}
          </el-tag>
        </div>

        <div class="message-text">{{ msg.content }}</div>

        <div
          v-if="msg.role === 'assistant' && msg.aiStatus"
          class="ai-trace"
          :class="{ 'is-llm': msg.source === 'llm' || msg.source === 'business-insight-ai' }"
        >
          {{ aiTraceLabel(msg) }}
        </div>

        <div v-if="msg.diagnosis && msg.diagnosis.code !== 'QUERY_EXECUTED'" class="diagnosis-panel">
          <div class="diagnosis-panel__head">
            <el-tag size="small" effect="plain" type="warning">
              诊断码：{{ msg.diagnosis.code }}
            </el-tag>
            <el-tag v-if="msg.diagnosis.recovered" size="small" effect="plain" type="success">
              已恢复
            </el-tag>
            <el-tag
              v-if="msg.diagnosis.guidanceScenario"
              size="small"
              effect="plain"
              type="info"
            >
              {{ msg.diagnosis.guidanceScenario }}
            </el-tag>
          </div>
          <p>{{ msg.diagnosis.reason }}</p>
          <div
            v-if="msg.diagnosis.intentTags?.length || msg.diagnosis.candidateMetricsPreview?.length"
            class="diagnosis-panel__evidence"
          >
            <span class="diagnosis-panel__evidence-label">识别依据</span>
            <el-tag
              v-for="tag in msg.diagnosis.intentTags || []"
              :key="`intent-${tag}`"
              size="small"
              effect="plain"
              type="info"
            >
              {{ tag }}
            </el-tag>
            <el-tag
              v-for="metric in msg.diagnosis.candidateMetricsPreview || []"
              :key="`metric-${metric}`"
              size="small"
              effect="plain"
              type="success"
            >
              指标候选：{{ metric }}
            </el-tag>
          </div>
          <div
            v-if="msg.diagnosis.slotEvidence"
            class="diagnosis-panel__evidence"
          >
            <span class="diagnosis-panel__evidence-label">槽位证据</span>
            <el-tag
              v-if="msg.diagnosis.slotEvidence.primaryMetric?.value"
              size="small"
              effect="plain"
              type="success"
            >
              主指标：{{ msg.diagnosis.slotEvidence.primaryMetric.value }}
            </el-tag>
            <el-tag
              v-if="msg.diagnosis.slotEvidence.secondaryMetric?.value"
              size="small"
              effect="plain"
              type="warning"
            >
              对比指标：{{ msg.diagnosis.slotEvidence.secondaryMetric.value }}
            </el-tag>
            <el-tag
              v-for="metric in msg.diagnosis.slotEvidence.secondaryMetric?.candidates || []"
              :key="`slot-candidate-${metric}`"
              size="small"
              effect="plain"
              type="info"
            >
              待确认：{{ metric }}
            </el-tag>
            <div
              v-if="rankedCandidateReasonOptionsList().length > 1"
              class="diagnosis-panel__rank-filters"
            >
              <span class="diagnosis-panel__evidence-label">证据分组</span>
              <el-button
                v-for="option in rankedCandidateReasonOptionsList()"
                :key="`slot-ranked-filter-${option.key}`"
                size="small"
                plain
                class="diagnosis-panel__rank-filter"
                :class="{ 'is-active': reasonFilter === option.key }"
                :type="reasonFilter === option.key ? 'warning' : undefined"
                @click="handleReasonFilterChange(option.key)"
              >
                {{ option.label }}（{{ option.count }}）
              </el-button>
            </div>
            <el-tag
              v-for="(candidate, rankedIndex) in visibleRankedCandidatesList()"
              :key="`slot-ranked-${candidate.metric || rankedIndex}`"
              size="small"
              effect="plain"
              :type="rankedIndex === 0 ? 'warning' : 'info'"
              :class="{ 'is-ranked-top': rankedIndex === 0 }"
              :title="`分类：${rankedCandidateReasonSummary(candidate)}`"
            >
              {{ rankedCandidateLabelLocal(candidate, rankedIndex) }}
            </el-tag>
            <el-button
              v-if="hiddenRankedCount() > 0 || expanded"
              link
              type="primary"
              class="diagnosis-panel__rank-toggle"
              @click="expanded = !expanded"
            >
              {{ expanded ? '收起排序证据' : `展开更多排序证据（+${hiddenRankedCount()}）` }}
            </el-button>
            <el-tag
              v-if="msg.diagnosis.slotEvidence.secondaryMetric?.source"
              size="small"
              effect="plain"
            >
              来源：{{ slotSourceLabel(msg.diagnosis.slotEvidence.secondaryMetric.source) }}
            </el-tag>
            <el-tag
              v-if="msg.diagnosis.slotEvidence.secondaryMetric?.confidence !== undefined"
              size="small"
              effect="plain"
            >
              置信度：{{ formatConfidence(msg.diagnosis.slotEvidence.secondaryMetric.confidence) }}
            </el-tag>
            <el-tag
              v-if="msg.diagnosis.slotConflict"
              size="small"
              effect="plain"
              type="danger"
            >
              槽位冲突
            </el-tag>
          </div>
          <div v-if="msg.diagnosis.actions?.length" class="diagnosis-panel__actions">
            <el-tag
              v-for="action in msg.diagnosis.actions"
              :key="action"
              effect="plain"
              class="suggestion-tag"
              :class="{ 'is-actionable': isActionableSuggestion(action) }"
              @click="emit('diagnosisAction', action)"
            >
              {{ action }}
            </el-tag>
          </div>
        </div>

        <div v-if="msg.disambiguation" class="disambiguation-panel">
          检测到多个高相似指标。请先点击一个指标继续查询，避免分析口径偏差。
        </div>

        <div v-if="msg.sql && msg.sql.trim()" class="sql-block">
          <div class="sql-block__header">
            <span>执行 SQL</span>
            <div class="sql-block__actions">
              <el-button size="small" text @click="emit('showLineage', msg.sql)">
                <el-icon><Share /></el-icon>
                血缘
              </el-button>
              <el-button size="small" text @click="emit('copySql', msg.sql)">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
            </div>
          </div>
          <pre class="sql-code">{{ msg.sql }}</pre>
        </div>

        <div v-if="msg.data?.length" class="data-block">
          <div class="data-block__header">
            <span>结果图表与明细</span>
            <div class="data-block__actions">
              <el-button
                size="small"
                text
                type="primary"
                @click="emit('openDataPreview', msg)"
              >
                <el-icon><FullScreen /></el-icon>
                全屏查看
              </el-button>
              <el-radio-group
                :model-value="msg.dataViewMode"
                size="small"
                class="data-view-mode"
                @change="(mode: any) => { msg.dataViewMode = mode; emit('toggleDataViewMode', mode) }"
              >
                <el-radio-button
                  label="chart"
                  :disabled="normalizedChartType(msg.renderChartType || msg.chartType, msg.data) === 'table'"
                >
                  图表
                </el-radio-button>
                <el-radio-button
                  label="split"
                  :disabled="normalizedChartType(msg.renderChartType || msg.chartType, msg.data) === 'table'"
                >
                  分栏
                </el-radio-button>
                <el-radio-button label="table">
                  明细
                </el-radio-button>
              </el-radio-group>
              <el-tag size="small" effect="plain">
                {{ chartTypeLabel(msg.renderChartType || msg.chartType, msg.data) }}
              </el-tag>
              <el-select
                :model-value="msg.renderChartType"
                size="small"
                class="chart-type-select"
                filterable
                @change="(type: any) => { msg.renderChartType = type; emit('chartTypeChange', index, String(type), msg) }"
              >
                <el-option
                  v-for="option in availableChartTypes()"
                  :key="option.type"
                  :label="option.label"
                  :value="option.type"
                />
              </el-select>
              <el-button text type="primary" @click="emit('openDataPreview', msg)">
                <el-icon><FullScreen /></el-icon>
                全屏查看
              </el-button>
            </div>
          </div>

          <div
            v-if="msg.dataViewMode !== 'table' && normalizedChartType(msg.renderChartType || msg.chartType, msg.data) !== 'table'"
            :ref="(element) => emit('registerChartRef', index, element as HTMLDivElement | null)"
            class="chart-container"
            @click="emit('chartClick', msg)"
          ></div>

          <el-table
            v-if="msg.dataViewMode !== 'chart'"
            :data="msg.data.slice(0, 8)"
            stripe
            border
            size="small"
            style="width: 100%"
            :header-cell-style="{ background: '#f5f8ff', fontWeight: 700 }"
          >
            <el-table-column
              v-for="column in columnsOf(msg.data)"
              :key="column"
              :prop="column"
              :label="column"
              min-width="120"
              show-overflow-tooltip
            />
          </el-table>
          <div v-if="msg.data.length > 8" class="data-summary">
            共 {{ msg.data.length }} 条数据，当前展示前 8 条。
          </div>
        </div>

        <!-- NL2SQL 反馈（Month 2 Week 2） -->
        <div v-if="msg.role === 'assistant' && msg.sql" class="feedback-bar">
          <div class="feedback-bar__actions">
            <el-button
              size="small"
              text
              :type="feedbackState.rating === 1 ? 'success' : undefined"
              @click="handleFeedback(1)"
            >
              <el-icon><CircleCheck /></el-icon>
              准确
            </el-button>
            <el-button
              size="small"
              text
              :type="feedbackState.rating === -1 ? 'danger' : undefined"
              @click="handleFeedback(-1)"
            >
              <el-icon><CircleClose /></el-icon>
              不准确
            </el-button>
          </div>
          <div v-if="feedbackState.showForm" class="feedback-form">
            <el-input
              v-model="feedbackState.correctSql"
              type="textarea"
              :rows="2"
              placeholder="请填写您认为正确的 SQL（可选）"
              size="small"
            />
            <el-input
              v-model="feedbackState.comment"
              type="textarea"
              :rows="1"
              placeholder="补充说明（可选）"
              size="small"
              style="margin-top: 6px;"
            />
            <div class="feedback-form__actions">
              <el-button size="small" @click="feedbackState.showForm = false">取消</el-button>
              <el-button size="small" type="primary" @click="submitFeedback">提交</el-button>
            </div>
          </div>
        </div>

        <div v-if="msg.suggestions?.length" class="suggestion-list">
          <span class="suggestion-list__label">下一步建议</span>
          <el-tag
            v-for="suggestion in msg.suggestions"
            :key="suggestion"
            effect="plain"
            class="suggestion-tag"
            @click="emit('suggestionClick', suggestion)"
          >
            {{ suggestion }}
          </el-tag>
        </div>

        <div
          v-if="resolveCandidateMetrics().length"
          class="candidate-list"
          :class="{ 'is-disambiguation': msg.disambiguation }"
        >
          <span class="suggestion-list__label">
            {{ msg.disambiguation ? '请选择一个指标继续' : '可识别指标' }}
          </span>
          <el-tag
            v-for="metric in resolveCandidateMetrics()"
            :key="metric"
            effect="light"
            :type="msg.disambiguation ? 'warning' : 'success'"
            class="suggestion-tag"
            @click="emit('candidateMetricClick', metric, msg.disambiguation || false)"
          >
            {{ metric }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CircleCheck, CircleClose, DocumentCopy, FullScreen, Service, Share, User } from '@element-plus/icons-vue'
import { normalizeEnterpriseChartType } from '@/components/Chart/chartCatalog'
import {
  buildRankedCandidateLabel,
  filterRankedCandidatesByReason,
  hiddenRankedCandidateCount,
  rankedCandidateDisplayIndex,
  rankedCandidateReasonOptions,
  rankedCandidateReasonSummary,
  type RankedCandidateReasonFilter,
  visibleRankedCandidates
} from '@/utils/diagnosis'

interface MessageAiStatus {
  mode?: string
  enabled?: boolean
  runtimeEnabled?: boolean
  reason?: string
  defaultProvider?: string | null
  providerName?: string | null
  model?: string | null
}

interface MessageDiagnosis {
  code: string
  reason: string
  recovered?: boolean
  actions?: string[]
  guidanceScenario?: string
  intentTags?: string[]
  candidateMetricsPreview?: string[]
  candidateMetricCount?: number
  slotConflict?: boolean
  slotEvidence?: {
    primaryMetric?: {
      value?: string
      source?: string
      confidence?: number
    }
    secondaryMetric?: {
      value?: string | null
      source?: string
      confidence?: number
      conflict?: boolean
      candidates?: string[]
      reason?: string | null
      rankedCandidates?: Array<{
        metric?: string
        score?: number
        position?: number
        reason?: string
      }>
    }
    timeContext?: {
      value?: string | null
      used?: boolean
    }
    timeExplicit?: {
      value?: string | null
      used?: boolean
    }
    timeComparison?: boolean
  }
}

interface ConversationMessage {
  role: 'user' | 'assistant'
  content: string
  sql?: string
  timestamp: number
  data?: Record<string, string | number>[]
  suggestions?: string[]
  chartType?: string
  source?: string
  metric?: string
  candidateMetrics?: string[]
  disambiguation?: boolean
  aiStatus?: MessageAiStatus
  diagnosis?: MessageDiagnosis
  renderChartType?: string
  dataViewMode?: 'chart' | 'split' | 'table'
}

interface ChartTypeOption {
  type: string
  label: string
}

interface CapabilityMetric {
  name: string
  definition: string
  examples: string[]
  aliases: string[]
}

interface ConversationCapabilities {
  chartCatalog: Array<{
    type: string
    family: string
    variant: string
    displayName?: string
  }>
  featuredChartTypes: Array<{
    type: string
    family: string
    variant: string
    displayName?: string
  }>
  quickStartMetrics?: string[]
  metrics: CapabilityMetric[]
}

const props = defineProps<{
  msg: ConversationMessage
  index: number
  capabilities?: ConversationCapabilities
}>()

const emit = defineEmits<{
  chartClick: [msg: ConversationMessage]
  candidateMetricClick: [metric: string, disambiguation: boolean]
  suggestionClick: [suggestion: string]
  copySql: [sql: string]
  toggleDataViewMode: [mode: 'chart' | 'split' | 'table']
  chartTypeChange: [index: number, type: string, msg: ConversationMessage]
  openDataPreview: [msg: ConversationMessage]
  registerChartRef: [index: number, element: HTMLDivElement | null]
  diagnosisAction: [action: string]
  feedback: [payload: { messageId: string; conversationId: string; question: string; generatedSql: string; rating: number; correctSql: string; comment: string }]
}>()

const expanded = ref(false)
const reasonFilter = ref<RankedCandidateReasonFilter>('all')

const feedbackState = ref({
  rating: 0 as number,
  showForm: false,
  correctSql: '',
  comment: ''
})

function handleFeedback(rating: number) {
  feedbackState.value.rating = rating
  feedbackState.value.showForm = true
  if (rating === 1) {
    // 点赞直接提交，无需表单
    submitFeedback()
  }
}

function submitFeedback() {
  emit('feedback', {
    messageId: props.msg.timestamp?.toString() || '',
    conversationId: '',
    question: '',
    generatedSql: props.msg.sql || '',
    rating: feedbackState.value.rating,
    correctSql: feedbackState.value.correctSql,
    comment: feedbackState.value.comment
  })
  feedbackState.value.showForm = false
  feedbackState.value.correctSql = ''
  feedbackState.value.comment = ''
}

const DEFAULT_CHART_TYPE_OPTIONS: ChartTypeOption[] = [
  { type: 'table', label: '明细表' },
  { type: 'bar.enterprise', label: '企业柱状图' },
  { type: 'line.enterprise', label: '企业折线图' },
  { type: 'area.enterprise', label: '企业面积图' },
  { type: 'pie.enterprise', label: '企业饼图' },
  { type: 'scatter.enterprise', label: '企业散点图' },
  { type: 'radar.enterprise', label: '企业雷达图' },
  { type: 'gauge.enterprise', label: '企业仪表盘' },
  { type: 'funnel.enterprise', label: '企业漏斗图' },
  { type: 'treemap.enterprise', label: '企业矩形树图' },
  { type: 'sunburst.enterprise', label: '企业旭日图' },
  { type: 'heatmap.enterprise', label: '企业热力图' },
  { type: 'candlestick.enterprise', label: '企业K线图' },
  { type: 'boxplot.enterprise', label: '企业箱线图' },
  { type: 'waterfall.enterprise', label: '企业瀑布图' },
  { type: 'sankey.enterprise', label: '企业桑基图' },
  { type: 'graph.enterprise', label: '企业关系图' },
  { type: 'tree.enterprise', label: '企业树图' }
]

function columnsOf(data: Record<string, string | number>[]) {
  return data.length ? Object.keys(data[0]) : []
}

function sourceLabel(source?: string) {
  switch (source) {
    case 'llm':
      return '外部模型'
    case 'business-insight-ai':
      return '业务语义 + AI'
    case 'business-insight':
      return '业务语义'
    case 'guided-discovery':
      return '指标引导'
    case 'guided-disambiguation':
      return '歧义澄清'
    default:
      return '系统回复'
  }
}

function aiTraceLabel(message: ConversationMessage) {
  const status = message.aiStatus
  if (!status) {
    return ''
  }

  if (message.source === 'llm' || message.source === 'business-insight-ai') {
    const provider = status.providerName || status.defaultProvider || '外部模型'
    return `AI链路：已调用 ${provider}${status.model ? ` / ${status.model}` : ''}`
  }

  if (!status.runtimeEnabled) {
    return `AI链路：未调用外部模型（${status.reason || '运行时未启用'}）`
  }

  return 'AI链路：本次命中业务语义引擎'
}

function slotSourceLabel(source?: string) {
  switch (source) {
    case 'semantic-candidates':
      return '语义候选'
    case 'semantic-candidates-priority':
      return '语义候选（优先级）'
    case 'direct-metric':
      return '指标直命中'
    case 'synonym':
      return '同义词映射'
    default:
      return source || '未标记'
  }
}

function formatConfidence(value?: number) {
  if (value === undefined || Number.isNaN(value)) {
    return '-'
  }
  return `${Math.round(value * 100)}%`
}

function rankedCandidateReasonOptionsList() {
  return rankedCandidateReasonOptions(
    props.msg.diagnosis?.slotEvidence?.secondaryMetric?.rankedCandidates
  )
}

function filteredRankedCandidates() {
  return filterRankedCandidatesByReason(
    props.msg.diagnosis?.slotEvidence?.secondaryMetric?.rankedCandidates,
    reasonFilter.value
  )
}

function visibleRankedCandidatesList() {
  return visibleRankedCandidates(filteredRankedCandidates(), expanded.value, 2)
}

function hiddenRankedCount() {
  return hiddenRankedCandidateCount(filteredRankedCandidates(), expanded.value, 2)
}

function rankedCandidateLabelLocal(candidate: {
  metric?: string
  score?: number
  position?: number
  reason?: string
}, idx: number) {
  return buildRankedCandidateLabel(
    candidate,
    rankedCandidateDisplayIndex(
      props.msg.diagnosis?.slotEvidence?.secondaryMetric?.rankedCandidates,
      candidate,
      idx
    )
  )
}

function handleReasonFilterChange(filter: RankedCandidateReasonFilter) {
  reasonFilter.value = filter
  expanded.value = false
}

function recommendChartTypeFromData(data?: Record<string, string | number>[]) {
  if (!data?.length) {
    return 'table'
  }
  const first = data[0]
  const keys = Object.keys(first)
  const numericKeys = keys.filter(key => data.some(row => Number.isFinite(Number(row[key]))))
  const categoryKey = keys.find(key => !numericKeys.includes(key)) ?? keys[0]
  const looksLikeTime = /date|time|month|year|周|日|月|季度|时间/i.test(categoryKey)
  if (numericKeys.length >= 2) {
    return 'scatter.enterprise'
  }
  if (looksLikeTime) {
    return 'line.enterprise'
  }
  if (data.length <= 6) {
    return 'pie.enterprise'
  }
  return 'bar.enterprise'
}

function normalizedChartType(chartType?: string, data?: Record<string, string | number>[]) {
  if (!data?.length) {
    return 'table'
  }
  if (!chartType || !chartType.trim()) {
    return recommendChartTypeFromData(data)
  }
  if (chartType === 'number') {
    return 'gauge.enterprise'
  }
  if (chartType === 'table') {
    return 'table'
  }
  return normalizeEnterpriseChartType(chartType)
}

function chartTypeLabel(chartType?: string, data?: Record<string, string | number>[]) {
  const normalized = normalizedChartType(chartType, data)
  const option = DEFAULT_CHART_TYPE_OPTIONS.find(item => item.type === normalized)
  if (option) {
    return option.label
  }
  const caps = props.capabilities
  if (caps) {
    const fromCatalog = [...caps.chartCatalog, ...caps.featuredChartTypes]
      .find(item => item.type === normalized)
    if (fromCatalog?.displayName) {
      return fromCatalog.displayName
    }
  }
  return normalized
}

function availableChartTypes(): ChartTypeOption[] {
  const merged: ChartTypeOption[] = []
  const existed = new Set<string>()

  const push = (item: ChartTypeOption) => {
    if (!item.type || existed.has(item.type)) {
      return
    }
    existed.add(item.type)
    merged.push(item)
  }

  DEFAULT_CHART_TYPE_OPTIONS.forEach(push)

  const caps = props.capabilities
  if (caps) {
    caps.chartCatalog?.forEach(item => {
      push({
        type: item.type,
        label: item.displayName || `${item.family}.${item.variant}`
      })
    })
    caps.featuredChartTypes?.forEach(item => {
      push({
        type: item.type,
        label: item.displayName || item.type
      })
    })
  }

  const currentType = normalizedChartType(props.msg.renderChartType || props.msg.chartType, props.msg.data)
  if (currentType !== 'table') {
    push({ type: currentType, label: currentType })
  }
  return merged
}

function resolveCandidateMetrics() {
  if (props.msg.candidateMetrics?.length) {
    return props.msg.candidateMetrics
  }
  if (props.msg.role !== 'assistant') {
    return []
  }
  if (props.msg.diagnosis?.code !== 'METRIC_NOT_RECOGNIZED') {
    return []
  }
  const caps = props.capabilities
  const quickStart = caps?.quickStartMetrics || []
  if (quickStart.length) {
    return quickStart.slice(0, 4)
  }
  return (caps?.metrics || []).slice(0, 4).map(item => item.name)
}

function isActionableSuggestion(text: string) {
  return /[？?]|本月|上月|总览|趋势|对比|分析/.test(text)
}

function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.message-row {
  display: flex;
  margin-bottom: 20px;
}

.message-row.is-user {
  justify-content: flex-end;
}

.message-card {
  display: flex;
  gap: 14px;
  width: 100%;
  max-width: 100%;
}

.message-card.is-user {
  flex-direction: row-reverse;
  width: auto;
  max-width: min(720px, 82%);
}

.message-row.is-assistant .message-card {
  max-width: 100%;
}

.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22px;
  color: #fff;
  background: linear-gradient(135deg, #2f6bff, #537ef5);
  box-shadow: var(--cb-shadow-sm);
}

.message-card.is-assistant .message-avatar {
  background: linear-gradient(135deg, #14b8a6, #2f6bff);
}

.message-content {
  flex: 1;
  min-width: 0;
  padding: 18px 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(129, 157, 219, 0.14);
  box-shadow: var(--cb-shadow-sm);
}

.message-card.is-user .message-content {
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.94), rgba(83, 126, 245, 0.94));
  color: #fff;
}

.message-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.message-card.is-user .message-meta {
  color: rgba(255, 255, 255, 0.82);
}

.message-text {
  margin-top: 10px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-trace {
  margin-top: 10px;
  font-size: 12px;
  color: #6b7280;
}

.ai-trace.is-llm {
  color: #0f766e;
}

.diagnosis-panel {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px dashed rgba(250, 204, 21, 0.42);
  background: rgba(255, 251, 235, 0.92);
  color: #92400e;
  display: grid;
  gap: 8px;
}

.diagnosis-panel p {
  margin: 0;
  line-height: 1.6;
  font-size: 13px;
}

.diagnosis-panel__head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-panel__evidence {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-panel__rank-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-panel__evidence-label {
  color: #854d0e;
  font-size: 12px;
  font-weight: 600;
}

.disambiguation-panel {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(245, 158, 11, 0.35);
  background: rgba(255, 251, 235, 0.95);
  color: #92400e;
  font-size: 13px;
  line-height: 1.6;
}

.sql-block {
  margin-top: 18px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(129, 157, 219, 0.16);
  background: #f8fbff;
}

.sql-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(47, 107, 255, 0.08);
  color: var(--cb-indigo);
  font-size: 13px;
  font-weight: 600;
}

.sql-code {
  margin: 0;
  padding: 14px;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  overflow-x: auto;
}

.data-block {
  margin-top: 18px;
  display: grid;
  gap: 16px;
  overflow: hidden;
}

.data-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(129, 157, 219, 0.14);
}

.data-block__header > span {
  color: var(--cb-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.data-block__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.data-view-mode :deep(.el-radio-button__inner) {
  min-width: 56px;
}

.chart-container {
  width: 100%;
  min-width: 0;
  height: clamp(360px, 46vh, 520px);
  border-radius: 18px;
  border: 1px solid rgba(129, 157, 219, 0.12);
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.96), rgba(244, 248, 255, 0.96));
  padding: 10px 6px 4px;
  box-sizing: border-box;
}

.chart-type-select {
  width: 220px;
}

.data-summary {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.data-block :deep(.el-table) {
  width: 100%;
}

.data-block :deep(.el-table .cell) {
  line-height: 1.6;
}

.data-block :deep(.el-table__body-wrapper),
.data-block :deep(.el-table__header-wrapper) {
  overflow-x: auto;
}

.suggestion-list {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(129, 157, 219, 0.14);
}

.candidate-list {
  margin-top: 10px;
}

.candidate-list.is-disambiguation {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(245, 158, 11, 0.35);
}

.suggestion-list__label {
  display: block;
  margin-bottom: 10px;
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.suggestion-tag {
  margin-right: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.suggestion-tag:not(.is-actionable) {
  cursor: default;
}

.suggestion-tag.is-ranked-top {
  border-color: rgba(245, 158, 11, 0.4);
  background: rgba(255, 247, 237, 0.95);
}

.diagnosis-panel__rank-filter {
  margin: 0;
}

.diagnosis-panel__rank-filter.is-active {
  box-shadow: 0 6px 14px rgba(245, 158, 11, 0.18);
}

.diagnosis-panel__rank-toggle {
  margin: 2px 0 8px;
  padding: 0;
}

@media (max-width: 768px) {
  .message-card,
  .message-card.is-user {
    max-width: 100%;
  }

  .chart-container {
    height: 320px;
    padding: 6px 0 0;
  }

  .chart-type-select {
    width: 100%;
  }

  .data-view-mode {
    width: 100%;
  }
}

.feedback-bar {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(129, 157, 219, 0.14);
}

.feedback-bar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feedback-form {
  margin-top: 8px;
}

.feedback-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
