<template>
  <div class="conversation-page cb-page">
    <PageHeader subtitle="AI 对话分析" max-width="1320px">
      <template #actions>
        <el-button link @click="focusMode = !focusMode">
          <el-icon><FullScreen /></el-icon>
          <span class="btn-text">{{ focusMode ? '退出聚焦' : '聚焦工作区' }}</span>
        </el-button>
        <el-button v-if="!focusMode" link @click="sidePanelVisible = !sidePanelVisible">
          <el-icon><Fold v-if="sidePanelVisible" /><Expand v-else /></el-icon>
          <span class="btn-text">{{ sidePanelVisible ? '收起侧栏' : '展开侧栏' }}</span>
        </el-button>
        <el-button link @click="router.push('/chatbi/query')">
          <el-icon><Search /></el-icon>
          <span class="btn-text">智能查询</span>
        </el-button>
        <el-button link @click="router.push('/chatbi/chart-market')">
          <el-icon><Grid /></el-icon>
          <span class="btn-text">图表市场</span>
        </el-button>
        <el-button link @click="router.push('/admin/semantic')">
          <el-icon><Management /></el-icon>
          <span class="btn-text">同义词</span>
        </el-button>
        <el-button link @click="router.push('/admin/ai')">
          <el-icon><Setting /></el-icon>
          <span class="btn-text">AI 设置</span>
        </el-button>
        <el-button link :type="streamEnabled ? 'success' : ''" @click="streamEnabled = !streamEnabled" title="流式输出">
          <el-icon>
            <Loading v-if="streamEnabled" />
            <VideoPlay v-else />
          </el-icon>
          <span class="btn-text">{{ streamEnabled ? '流式中' : '流式' }}</span>
        </el-button>
        <el-button link type="primary" @click="startNewConversation">
          <el-icon><Plus /></el-icon>
          <span class="btn-text">新对话</span>
        </el-button>
      </template>
    </PageHeader>

    <main
      class="conversation-main cb-page-container cb-content"
      :class="{ 'focus-mode': focusMode, 'side-collapsed': !focusMode && !sidePanelVisible }"
      style="max-width: 1560px;"
    >
      <section class="conversation-primary">
        <Card v-if="messages.length === 0" padding="lg" shadow="md" class="intro-card">
          <AiRuntimeBanner
            :status="capabilities.ai"
            title="企业级对话分析工作台"
            description="对话页会先尝试识别业务指标和同义词，再决定走业务语义引擎还是外部大模型。无论哪种模式，都只查询真实后端数据。"
          >
            <template #actions>
              <el-button type="primary" plain @click="prefillAndSend('先给我一个经营总览')">
                经营总览
              </el-button>
              <el-button plain @click="router.push('/chatbi/result?q=本月销售额')">
                看结果页
              </el-button>
              <el-button plain @click="router.push('/admin/ai')">
                AI 设置
              </el-button>
              <el-button plain @click="router.push('/chatbi/chart-market')">
                图表应用市场
              </el-button>
            </template>
          </AiRuntimeBanner>

          <div class="guide-grid">
            <div class="guide-item">
              <span class="guide-item__step">01</span>
              <strong>先说指标</strong>
              <p>例如：销售额、毛利率、审批平均时长。</p>
            </div>
            <div class="guide-item">
              <span class="guide-item__step">02</span>
              <strong>再补时间和维度</strong>
              <p>例如：本月、上季度、按区域、按部门。</p>
            </div>
            <div class="guide-item">
              <span class="guide-item__step">03</span>
              <strong>继续追问</strong>
              <p>例如：那华东呢？趋势如何？哪个团队最高？</p>
            </div>
          </div>

          <div class="tips-row">
            <div v-for="tip in capabilities.usageTips" :key="tip" class="tip-chip">
              {{ tip }}
            </div>
          </div>
        </Card>

        <Card padding="none" shadow="md" class="workspace-card">
          <template #header>
            <div class="workspace-header">
              <div>
                <span class="card-title">对话工作区</span>
                <p class="workspace-subtitle">
                  当前支持 {{ capabilities.metricCount }} 个可查询指标，{{ capabilities.synonymCount }} 组同义词映射
                </p>
              </div>
              <div class="workspace-header__tools">
                <el-tag effect="plain" :type="capabilities.ai.runtimeEnabled ? 'success' : 'info'">
                  {{ capabilities.ai.runtimeEnabled ? '外部模型 + 语义引擎' : '语义引擎兜底' }}
                </el-tag>
                <el-select
                  v-model="preferredChartType"
                  clearable
                  filterable
                  size="small"
                  class="preferred-chart-select"
                  placeholder="默认图表（自动）"
                  @change="handlePreferredChartTypeChange"
                >
                  <el-option
                    v-for="option in preferredChartTypeOptions"
                    :key="option.type"
                    :label="option.label"
                    :value="option.type"
                  />
                </el-select>
                <el-button size="small" plain @click="chartLibraryVisible = true">
                  图表样式库
                </el-button>
              </div>
            </div>
          </template>

          <div class="messages-shell" ref="messagesContainer" @scroll.passive="handleMessagesScroll">
            <div v-if="messages.length === 0" class="welcome-panel">
              <div class="welcome-panel__icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <h2>从推荐问法开始，或直接输入业务问题</h2>
              <p>如果你输入的是泛化问题，系统会返回经营概览并提示可直接查询的指标。</p>

              <div class="starter-group">
                <button
                  v-for="question in starterQuestions"
                  :key="question"
                  type="button"
                  class="starter-chip"
                  @click="prefillAndSend(question)"
                >
                  {{ question }}
                </button>
              </div>

              <div class="metric-grid">
                <button
                  v-for="metric in capabilities.metrics.slice(0, 6)"
                  :key="metric.name"
                  type="button"
                  class="metric-tile"
                  @click="prefill(metric.examples[0] || `本月${metric.name}`)"
                >
                  <strong>{{ metric.name }}</strong>
                  <span>{{ metric.definition }}</span>
                  <div class="metric-tile__examples">
                    {{ metric.examples[0] || `本月${metric.name}` }}
                  </div>
                </button>
              </div>
            </div>

            <RecycleScroller
              class="message-scroller"
              :items="messages"
              :item-size="300"
              key-field="timestamp"
            >
              <template #default="{ item, index }">
                <ConversationMessageItem
                  :msg="item"
                  :index="index"
                  :capabilities="capabilities"
                  @copy-sql="copySql"
                  @suggestion-click="prefillAndSend"
                  @candidate-metric-click="handleCandidateMetric"
                  @toggle-data-view-mode="(mode: any) => item.dataViewMode = mode"
                  @chart-type-change="handleChartTypeChange"
                  @open-data-preview="openDataPreview"
                  @register-chart-ref="setChartRef"
                  @diagnosis-action="runDiagnosisAction"
                />
              </template>
            </RecycleScroller>

            <div v-if="loading" class="message-row is-assistant">
              <div class="message-card is-assistant">
                <div class="message-avatar">
                  <el-icon class="loading-icon"><Loading /></el-icon>
                </div>
                <div class="message-content">
                  <div class="message-meta">
                    <strong>ChatBI</strong>
                    <span>处理中</span>
                  </div>
                  <div class="message-text">正在解析问题、匹配指标并查询真实业务数据...</div>
                </div>
              </div>
            </div>
          </div>
          <el-button
            v-show="showScrollBottom"
            circle
            class="scroll-bottom-btn"
            @click="scrollToBottom(true)"
          >
            <el-icon><Bottom /></el-icon>
          </el-button>

          <div class="composer">
            <div class="metric-quick-panel">
              <div class="metric-quick-panel__head">
                <strong>指标速查</strong>
                <span>先点指标再发送，识别更稳定</span>
              </div>
              <el-input
                v-model.trim="metricQuickKeyword"
                size="small"
                clearable
                placeholder="搜索指标/同义词，例如 销售、毛利、投诉"
              />
              <div class="metric-quick-panel__tags">
                <el-tag
                  v-for="metric in quickMetricCandidates"
                  :key="metric.name"
                  effect="plain"
                  class="suggestion-tag is-actionable"
                  @click="prefill(`本月${metric.name}`)"
                >
                  {{ metric.name }}
                </el-tag>
              </div>
            </div>
            <el-input
              v-model="inputMessage"
              class="composer-input"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              :disabled="loading"
              placeholder="例如：本月销售额是多少？如果继续追问，可以直接说“那华东呢？”"
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="composer-actions">
              <span class="composer-tip">
                回车发送，Shift + 回车换行
              </span>
              <div class="composer-actions__buttons">
                <el-button plain @click="prefill('先给我一个经营总览')">插入总览问法</el-button>
                <el-button type="primary" :loading="loading" @click="handleSend">
                  <el-icon><Promotion /></el-icon>
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </Card>

        <el-dialog
          v-model="previewVisible"
          width="94%"
          top="3vh"
          class="preview-dialog"
          destroy-on-close
          @closed="disposePreviewChart"
        >
          <template #header>
            <div class="preview-header">
              <div class="preview-header__title">
                <strong>全屏图表分析</strong>
                <span>{{ previewMessage?.metric || '自由分析' }}</span>
              </div>
              <el-select v-model="previewChartType" size="small" class="chart-type-select" filterable>
                <el-option
                  v-for="option in previewChartTypeOptions"
                  :key="option.type"
                  :label="option.label"
                  :value="option.type"
                />
              </el-select>
            </div>
          </template>

          <div v-if="previewMessage" class="preview-body">
            <div v-if="previewChartType !== 'table'" ref="previewChartRef" class="preview-chart"></div>
            <el-table
              :data="previewMessage.data || []"
              stripe
              border
              size="small"
              style="width: 100%"
              :max-height="previewChartType === 'table' ? 640 : 340"
              :header-cell-style="{ background: '#f5f8ff', fontWeight: 700 }"
            >
              <el-table-column
                v-for="column in columnsOf(previewMessage.data || [])"
                :key="column"
                :prop="column"
                :label="column"
                min-width="140"
                show-overflow-tooltip
              />
            </el-table>
          </div>
        </el-dialog>

        <el-drawer
          v-model="chartLibraryVisible"
          title="企业图表样式库"
          size="520px"
          append-to-body
        >
          <div class="chart-library">
            <div class="chart-library__controls">
              <el-input
                v-model.trim="chartLibraryKeyword"
                clearable
                placeholder="搜索图表编码/名称，例如 line.enterprise"
              />
              <el-select v-model="chartLibraryFamily" style="width: 170px">
                <el-option label="全部家族" value="all" />
                <el-option
                  v-for="family in chartLibraryFamilies"
                  :key="family"
                  :label="family"
                  :value="family"
                />
              </el-select>
            </div>
            <div class="chart-library__summary">
              共 {{ chartLibraryCatalog.length }} 种，当前可选 {{ filteredChartLibraryCatalog.length }} 种
            </div>
            <div class="chart-library__list">
              <button
                v-for="chart in filteredChartLibraryCatalog"
                :key="chart.type"
                type="button"
                class="chart-library-item"
                :class="{ active: chart.type === preferredChartType }"
                @click="applyChartFromLibrary(chart.type)"
              >
                <div>
                  <strong>{{ chart.displayName || chart.type }}</strong>
                  <span>{{ chart.type }}</span>
                </div>
                <el-tag size="small" effect="plain">{{ chart.family }}</el-tag>
              </button>
            </div>
          </div>
        </el-drawer>
      </section>

      <aside v-if="!focusMode && sidePanelVisible" class="conversation-side">
        <Card padding="lg" class="side-card">
          <template #header>
            <span class="card-title">能力范围</span>
          </template>
          <div class="overview-grid">
            <div v-for="item in capabilities.overview.slice(0, 4)" :key="String(item.指标)" class="overview-item">
              <span>{{ item.指标 }}</span>
              <strong>{{ item.数值 }}{{ item.单位 }}</strong>
            </div>
          </div>
          <div class="capability-list">
            <div v-for="metric in capabilities.metrics.slice(0, 8)" :key="metric.name" class="capability-item">
              <div class="capability-item__head">
                <strong>{{ metric.name }}</strong>
                <el-tag v-if="metric.aliases.length" size="small" effect="plain">{{ metric.aliases.length }} 个同义词</el-tag>
              </div>
              <p>{{ metric.definition }}</p>
              <div class="capability-item__actions">
                <el-tag
                  v-for="example in metric.examples.slice(0, 2)"
                  :key="example"
                  effect="plain"
                  class="example-tag"
                  @click="prefill(example)"
                >
                  {{ example }}
                </el-tag>
              </div>
            </div>
          </div>
          <div class="chart-style-palette">
            <span class="palette-label">
              企业图表样式（{{ capabilities.chartTypeCount || capabilities.chartCatalog.length || 119 }} 种）
            </span>
            <el-button
              text
              type="primary"
              class="market-link-btn"
              @click="router.push('/chatbi/chart-market')"
            >
              打开图表应用市场（查看全部）
            </el-button>
            <div class="palette-tags">
              <el-tag
                v-for="chart in paletteChartTypes"
                :key="chart.type"
                effect="plain"
                class="palette-tag"
                @click="router.push({ path: '/chatbi/chart-market', query: { chartType: chart.type } })"
              >
                {{ chart.displayName || chart.type }}
              </el-tag>
            </div>
          </div>
        </Card>

        <Card padding="lg" class="side-card">
          <template #header>
            <span class="card-title">最近对话</span>
          </template>
          <div v-if="conversationList.length" class="conversation-list">
            <button
              v-for="conversation in conversationList"
              :key="conversation.id"
              type="button"
              class="conversation-item"
              :class="{ active: conversation.id === currentConversationId }"
              @click="loadConversation(conversation.id)"
            >
              <strong>{{ conversation.title }}</strong>
              <span>{{ formatTime(conversation.time) }}</span>
            </button>
          </div>
          <EmptyState
            v-else
            type="search"
            title="暂无历史对话"
            description="发起一次对话后，这里会显示最近记录"
          />
        </Card>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RecycleScroller } from 'vue-virtual-scroller'
import { useLocalStorage } from '@vueuse/core'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  Bottom,
  ChatDotRound,
  Expand,
  Fold,
  Grid,
  Loading,
  Management,
  VideoPlay,
  FullScreen,
  Plus,
  Promotion,
  Search,
  Setting
} from '@element-plus/icons-vue'
import AiRuntimeBanner from '@/components/AiRuntimeBanner.vue'
import Card from '@/components/Card.vue'
import ConversationMessageItem from '@/components/ConversationMessageItem.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { getChartFamily, normalizeEnterpriseChartType } from '@/components/Chart/chartCatalog'
import type { AiRuntimeStatus } from '@/types'
import { request, streamRequest } from '@/utils/http'

interface CapabilityMetric {
  name: string
  definition: string
  examples: string[]
  aliases: string[]
}

interface OverviewRow {
  指标: string
  数值: number
  单位: string
}

interface ConversationCapabilities {
  ai: AiRuntimeStatus
  metricCount: number
  synonymCount: number
  chartTypeCount: number
  chartCatalog: Array<{
    type: string
    family: string
    variant: string
    displayName?: string
  }>
  metrics: CapabilityMetric[]
  starterQuestions: string[]
  fallbackPrompts?: string[]
  quickStartMetrics?: string[]
  overview: OverviewRow[]
  usageTips: string[]
  featuredChartTypes: Array<{
    type: string
    family: string
    variant: string
    displayName?: string
  }>
}

interface ConversationListItem {
  id: string
  title: string
  time: number
}

interface ConversationHistoryMessage {
  role: 'user' | 'assistant'
  content: string
  sql?: string
  timestamp: number
  metadata?: {
    data?: Record<string, string | number>[]
    suggestions?: string[]
    chartType?: string
    querySource?: string
    metricName?: string
    candidateMetrics?: string[]
    disambiguation?: boolean
    aiStatus?: MessageAiStatus
    diagnosis?: MessageDiagnosis
  }
}

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

const API_BASE = '/conversation'
const PREFERRED_CHART_STORAGE_KEY = 'chatbi.preferredChartType'

const router = useRouter()
const route = useRoute()

const messages = ref<ConversationMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const streamEnabled = ref(false)
const focusMode = ref(false)
const sidePanelVisible = ref(false)
const currentConversationId = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const showScrollBottom = ref(false)
const preferredChartType = useLocalStorage(PREFERRED_CHART_STORAGE_KEY, '')
const chartRefs = ref<Record<number, HTMLDivElement | null>>({})
const chartInstances = new Map<number, echarts.ECharts>()
const previewVisible = ref(false)
const previewMessage = ref<ConversationMessage | null>(null)
const previewChartType = ref('table')
const previewChartRef = ref<HTMLDivElement | null>(null)
const chartLibraryVisible = ref(false)
const chartLibraryKeyword = ref('')
const chartLibraryFamily = ref('all')

let previewChartInstance: echarts.ECharts | null = null
let scrollBottomTimer: ReturnType<typeof setTimeout> | null = null
const conversationList = ref<ConversationListItem[]>([])
const capabilities = ref<ConversationCapabilities>({
  ai: {
    mode: 'semantic',
    enabled: false,
    runtimeEnabled: false,
    reason: '正在检测 AI 运行状态',
    defaultProvider: 'kimi',
    providerName: 'Kimi',
    model: null
  },
  metricCount: 0,
  synonymCount: 0,
  chartTypeCount: 0,
  chartCatalog: [],
  metrics: [],
  starterQuestions: [],
  fallbackPrompts: [],
  quickStartMetrics: [],
  overview: [],
  usageTips: [],
  featuredChartTypes: []
})

const starterQuestions = ref<string[]>([
  '本月销售额是多少？',
  '销售额趋势如何？',
  '上月审批平均时长是多少？',
  '先给我一个经营总览'
])
const metricQuickKeyword = ref('')

interface ChartTypeOption {
  type: string
  label: string
}

const DEFAULT_CHART_TYPE_OPTIONS: ChartTypeOption[] = [
  { type: 'table', label: '明细表' },
  { type: 'bar.enterprise', label: '企业柱状图' },
  { type: 'line.enterprise', label: '企业折线图' },
  { type: 'area.enterprise', label: '企业面积图' },
  { type: 'pie.enterprise', label: '企业饼图' },
  { type: 'scatter.enterprise', label: '企业散点图' },
  { type: 'radar.enterprise', label: '企业雷达图' },
  { type: 'funnel.enterprise', label: '企业漏斗图' },
  { type: 'gauge.enterprise', label: '企业仪表盘' },
  { type: 'heatmap.enterprise', label: '企业热力图' },
  { type: 'treemap.enterprise', label: '企业矩形树图' },
  { type: 'sunburst.enterprise', label: '企业旭日图' },
  { type: 'waterfall.enterprise', label: '企业瀑布图' },
  { type: 'sankey.enterprise', label: '企业桑基图' },
  { type: 'graph.enterprise', label: '企业关系图' },
  { type: 'tree.enterprise', label: '企业树图' }
]

function columnsOf(data: Record<string, string | number>[]) {
  return data.length ? Object.keys(data[0]) : []
}

function toNumber(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
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

function normalizePreferredChartType(raw?: string | null) {
  if (!raw || !raw.trim()) {
    return ''
  }
  const normalized = normalizeEnterpriseChartType(raw)
  return normalized === 'table' ? '' : normalized
}

function syncPreferredChartTypeFromRoute() {
  const routeType = normalizePreferredChartType(String(route.query.chartType || ''))
  if (routeType) {
    preferredChartType.value = routeType
  }
}

// useLocalStorage 自动处理持久化，无需手动 restore/persist

function resolveRenderChartType(chartType: string | undefined, data: Record<string, string | number>[]) {
  const fallback = normalizedChartType(chartType, data)
  if (!preferredChartType.value || !data?.length) {
    return fallback
  }
  return preferredChartType.value
}

function resolveDefaultDataViewMode(chartType: string | undefined, data: Record<string, string | number>[]) {
  const resolvedType = normalizedChartType(chartType, data)
  if (!data?.length || resolvedType === 'table') {
    return 'table' as const
  }
  const columnCount = Object.keys(data[0] || {}).length
  if (columnCount >= 6 || data.length > 12) {
    return 'chart' as const
  }
  return 'split' as const
}

function availableChartTypes(message: ConversationMessage): ChartTypeOption[] {
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

  capabilities.value.chartCatalog.forEach(item => {
    push({
      type: item.type,
      label: item.displayName || `${item.family}.${item.variant}`
    })
  })

  capabilities.value.featuredChartTypes.forEach(item => {
    push({
      type: item.type,
      label: item.displayName || item.type
    })
  })

  const currentType = normalizedChartType(message.renderChartType || message.chartType, message.data)
  if (currentType !== 'table') {
    push({ type: currentType, label: currentType })
  }
  return merged
}

const preferredChartTypeOptions = computed<ChartTypeOption[]>(() => {
  const merged: ChartTypeOption[] = []
  const existed = new Set<string>()
  const push = (item: ChartTypeOption) => {
    if (!item.type || item.type === 'table' || existed.has(item.type)) {
      return
    }
    existed.add(item.type)
    merged.push(item)
  }

  DEFAULT_CHART_TYPE_OPTIONS.forEach(push)
  capabilities.value.chartCatalog.forEach(item => {
    push({
      type: item.type,
      label: item.displayName || `${item.family}.${item.variant}`
    })
  })
  capabilities.value.featuredChartTypes.forEach(item => {
    push({
      type: item.type,
      label: item.displayName || item.type
    })
  })
  return merged
})

const previewChartTypeOptions = computed(() =>
  previewMessage.value ? availableChartTypes(previewMessage.value) : DEFAULT_CHART_TYPE_OPTIONS
)

const paletteChartTypes = computed(() => {
  const source = capabilities.value.chartCatalog
  if (!source.length) {
    return capabilities.value.featuredChartTypes.slice(0, 18)
  }
  const enterpriseVariant = source.filter(item => item.variant === 'enterprise')
  return (enterpriseVariant.length ? enterpriseVariant : source).slice(0, 18)
})

const chartLibraryCatalog = computed(() => {
  if (capabilities.value.chartCatalog.length) {
    return capabilities.value.chartCatalog
  }
  return DEFAULT_CHART_TYPE_OPTIONS
    .filter(item => item.type !== 'table')
    .map(item => ({
      type: item.type,
      family: getChartFamily(item.type),
      variant: item.type.split('.')[1] || 'default',
      displayName: item.label
    }))
})

const chartLibraryFamilies = computed(() =>
  Array.from(new Set(chartLibraryCatalog.value.map(item => item.family))).sort()
)

const filteredChartLibraryCatalog = computed(() => {
  const keyword = chartLibraryKeyword.value.toLowerCase()
  return chartLibraryCatalog.value.filter(item => {
    const familyMatched = chartLibraryFamily.value === 'all' || item.family === chartLibraryFamily.value
    const keywordMatched = !keyword
      || item.type.toLowerCase().includes(keyword)
      || (item.displayName || '').toLowerCase().includes(keyword)
      || item.family.toLowerCase().includes(keyword)
    return familyMatched && keywordMatched
  })
})

const quickMetricCandidates = computed(() => {
  const keyword = metricQuickKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return capabilities.value.metrics.slice(0, 10)
  }
  return capabilities.value.metrics
    .filter(metric => {
      const haystack = [metric.name, metric.definition, ...(metric.aliases || [])]
        .join(' ')
        .toLowerCase()
      return haystack.includes(keyword)
    })
    .slice(0, 10)
})

function chartTypeLabel(chartType?: string, data?: Record<string, string | number>[]) {
  const normalized = normalizedChartType(chartType, data)
  const option = DEFAULT_CHART_TYPE_OPTIONS.find(item => item.type === normalized)
  if (option) {
    return option.label
  }
  const fromCatalog = [...capabilities.value.chartCatalog, ...capabilities.value.featuredChartTypes]
    .find(item => item.type === normalized)
  return fromCatalog?.displayName || normalized
}

function handleChartTypeChange(index: number, type: string, message: ConversationMessage) {
  message.renderChartType = type
  if (type === 'table') {
    message.dataViewMode = 'table'
  } else if (message.dataViewMode === 'table') {
    message.dataViewMode = resolveDefaultDataViewMode(type, message.data || [])
  }
  nextTick(() => renderSingleAssistantChart(index, message))
}

function handlePreferredChartTypeChange(value: string | undefined) {
  preferredChartType.value = normalizePreferredChartType(value || '')
}

function applyChartFromLibrary(type: string) {
  preferredChartType.value = normalizePreferredChartType(type)
  chartLibraryVisible.value = false
  ElMessage.success(`默认图表已切换为：${chartTypeLabel(type)}`)
}

function buildConversationSummary(conversationId: string, title: string, time = Date.now()): ConversationListItem {
  return {
    id: conversationId,
    title: title.length > 20 ? `${title.slice(0, 20)}...` : title,
    time
  }
}

function upsertConversation(conversationId: string, title: string, time = Date.now()) {
  const summary = buildConversationSummary(conversationId, title, time)
  const index = conversationList.value.findIndex(item => item.id === conversationId)
  if (index >= 0) {
    conversationList.value.splice(index, 1, summary)
  } else {
    conversationList.value.unshift(summary)
  }
}

function prefill(question: string) {
  inputMessage.value = question
}

function prefillAndSend(question: string) {
  inputMessage.value = question
  handleSend()
}

function handleCandidateMetric(metric: string, disambiguation = false) {
  const query = `本月${metric}`
  if (disambiguation) {
    prefillAndSend(query)
    return
  }
  prefill(query)
}

function isActionableSuggestion(text: string) {
  return /[？?]|本月|上月|总览|趋势|对比|分析/.test(text)
}

function runDiagnosisAction(action: string) {
  if (!isActionableSuggestion(action)) {
    return
  }
  prefillAndSend(action)
}

function appendAssistantGuidedFallback(message: string) {
  const candidateMetrics = (capabilities.value.quickStartMetrics?.length
    ? capabilities.value.quickStartMetrics
    : capabilities.value.metrics.map(item => item.name)
  ).slice(0, 4)
  const suggestions = (capabilities.value.fallbackPrompts?.length
    ? capabilities.value.fallbackPrompts
    : starterQuestions.value.length
      ? starterQuestions.value
      : ['先给我一个经营总览']
  )
  messages.value.push({
    role: 'assistant',
    content: message,
    timestamp: Date.now(),
    suggestions,
    candidateMetrics,
    source: 'guided-discovery',
    disambiguation: false,
    aiStatus: {
      runtimeEnabled: capabilities.value.ai.runtimeEnabled,
      reason: capabilities.value.ai.reason,
      providerName: capabilities.value.ai.providerName,
      model: capabilities.value.ai.model
    },
    diagnosis: {
      code: 'FRONTEND_GUARDED_FALLBACK',
      reason: '当前请求未返回可用结果，已切换到前端引导模式。',
      recovered: true,
      actions: suggestions.slice(0, 4),
      guidanceScenario: '综合经营场景'
    }
  })
}

function mapHistoryMessage(message: ConversationHistoryMessage): ConversationMessage {
  const data = message.metadata?.data || []
  const chartType = normalizedChartType(message.metadata?.chartType, data)
  return {
    role: message.role,
    content: message.content,
    sql: message.sql,
    timestamp: message.timestamp,
    data,
    suggestions: message.metadata?.suggestions || [],
    chartType: message.metadata?.chartType,
    source: message.metadata?.querySource,
    metric: message.metadata?.metricName,
    candidateMetrics: message.metadata?.candidateMetrics || [],
    disambiguation: Boolean(message.metadata?.disambiguation),
    aiStatus: message.metadata?.aiStatus,
    diagnosis: message.metadata?.diagnosis,
    renderChartType: chartType,
    dataViewMode: resolveDefaultDataViewMode(chartType, data)
  }
}

async function loadCapabilities() {
  const response = await request<ConversationCapabilities>(`${API_BASE}/capabilities`)
  if (!response.success || !response.data) {
    return
  }
  capabilities.value = {
    ...capabilities.value,
    ...response.data,
    chartCatalog: response.data.chartCatalog || [],
    featuredChartTypes: response.data.featuredChartTypes || [],
    fallbackPrompts: response.data.fallbackPrompts || [],
    quickStartMetrics: response.data.quickStartMetrics || []
  }
  starterQuestions.value = response.data.starterQuestions?.length
    ? response.data.starterQuestions
    : starterQuestions.value
}

async function loadConversationList() {
  const data = await request<ConversationListItem[]>(`${API_BASE}/list?userId=1`)
  if (data.success && data.data) {
    conversationList.value = data.data
  }
}

async function loadConversation(conversationId: string) {
  const data = await request<ConversationHistoryMessage[]>(`${API_BASE}/${conversationId}/history`)
  if (!data.success || !data.data) {
    ElMessage.error(data.error || '加载对话失败')
    return
  }

  disposeCharts()
  currentConversationId.value = conversationId
  messages.value = data.data.map(mapHistoryMessage)
  await nextTick()
  renderAssistantCharts()
  scrollToBottom()
}

async function sendMessage(message: string) {
  if (loading.value) return

  loading.value = true
  messages.value.push({
    role: 'user',
    content: message,
    timestamp: Date.now()
  })
  scrollToBottom()

  try {
    const data = await request<any>(`${API_BASE}/message`, {
      method: 'POST',
      body: JSON.stringify({
        conversationId: currentConversationId.value || undefined,
        message,
        userId: 1
      })
    })

    if (!data.success || !data.data) {
      const fallbackMessage = data.error
        ? `处理消息失败：${data.error}。已为你切换到可继续分析的引导模式。`
        : '处理消息失败，已切换到可继续分析的引导模式。'
      appendAssistantGuidedFallback(fallbackMessage)
      ElMessage.error(data.error || '处理消息失败')
      return
    }

    const result = data.data
    if (!currentConversationId.value && result.conversationId) {
      currentConversationId.value = result.conversationId
    }
    upsertConversation(result.conversationId, message)

    messages.value.push({
      role: 'assistant',
      content: result.message,
      sql: result.sql,
      data: result.data || [],
      chartType: result.chartType,
      renderChartType: resolveRenderChartType(result.chartType, result.data || []),
      suggestions: result.suggestions || [],
      timestamp: Date.now(),
      source: result.source,
      metric: result.metric,
      candidateMetrics: result.candidateMetrics || [],
      disambiguation: Boolean(result.disambiguation),
      aiStatus: result.aiStatus,
      diagnosis: result.diagnosis,
      dataViewMode: resolveDefaultDataViewMode(result.chartType, result.data || [])
    })

    await nextTick()
    renderAssistantCharts()
    scrollToBottom()
  } catch (error: any) {
    console.error('发送消息失败', error)
    appendAssistantGuidedFallback(`发送失败：${error?.message || '网络异常'}。已切换到可继续分析的引导模式。`)
    ElMessage.error(`发送消息失败：${error?.message || '网络异常'}`)
  } finally {
    loading.value = false
  }
}

async function sendMessageStream(message: string) {
  if (loading.value) return

  loading.value = true
  messages.value.push({
    role: 'user',
    content: message,
    timestamp: Date.now()
  })
  scrollToBottom()

  const assistantMsgId = Date.now()
  messages.value.push({
    role: 'assistant',
    content: '',
    timestamp: assistantMsgId
  })

  try {
    let result: any = null
    await streamRequest(
      `${API_BASE}/message/stream`,
      {
        method: 'POST',
        body: JSON.stringify({
          conversationId: currentConversationId.value || undefined,
          message,
          userId: 1
        })
      },
      (chunk) => {
        if (chunk.event === 'status') {
          // 可在此更新状态指示器
          const status = JSON.parse(chunk.data)
          const assistantMsg = messages.value[messages.value.length - 1]
          if (assistantMsg.role === 'assistant') {
            assistantMsg.content = getStatusText(status.step)
          }
        } else if (chunk.event === 'result') {
          const parsed = JSON.parse(chunk.data)
          if (parsed.success && parsed.data) {
            result = parsed.data
          }
        } else if (chunk.event === 'error') {
          const parsed = JSON.parse(chunk.data)
          throw new Error(parsed.error || '流式处理失败')
        }
      }
    )

    if (!result) {
      throw new Error('未收到有效结果')
    }

    if (!currentConversationId.value && result.conversationId) {
      currentConversationId.value = result.conversationId
    }
    upsertConversation(result.conversationId, message)

    const assistantMsg = messages.value[messages.value.length - 1]
    if (assistantMsg.role === 'assistant') {
      assistantMsg.content = result.message
      assistantMsg.sql = result.sql
      assistantMsg.data = result.data || []
      assistantMsg.chartType = result.chartType
      assistantMsg.renderChartType = resolveRenderChartType(result.chartType, result.data || [])
      assistantMsg.suggestions = result.suggestions || []
      assistantMsg.source = result.source
      assistantMsg.metric = result.metric
      assistantMsg.candidateMetrics = result.candidateMetrics || []
      assistantMsg.disambiguation = Boolean(result.disambiguation)
      assistantMsg.aiStatus = result.aiStatus
      assistantMsg.diagnosis = result.diagnosis
      assistantMsg.dataViewMode = resolveDefaultDataViewMode(result.chartType, result.data || [])
    }

    await nextTick()
    renderAssistantCharts()
    scrollToBottom()
  } catch (error: any) {
    console.error('流式发送消息失败', error)
    const assistantMsg = messages.value[messages.value.length - 1]
    if (assistantMsg.role === 'assistant') {
      assistantMsg.content = `发送失败：${error?.message || '网络异常'}`
    }
    ElMessage.error(`发送消息失败：${error?.message || '网络异常'}`)
  } finally {
    loading.value = false
  }
}

function getStatusText(step: string): string {
  const map: Record<string, string> = {
    CREATING_CONVERSATION: '正在创建对话...',
    ANALYZING_INTENT: '正在分析意图...',
    GENERATING_SQL: '正在生成 SQL...',
    GENERATING_INTERPRETATION: '正在生成解读...',
    FINALIZING: '正在整理结果...'
  }
  return map[step] || '正在处理...'
}

function handleSend() {
  const message = inputMessage.value.trim()
  if (!message) {
    ElMessage.warning('请输入问题')
    return
  }
  inputMessage.value = ''
  if (streamEnabled.value) {
    sendMessageStream(message)
  } else {
    sendMessage(message)
  }
}

function setChartRef(index: number, element: any) {
  chartRefs.value[index] = (element as HTMLDivElement | null) || null
}

function renderSingleAssistantChart(index: number, message: ConversationMessage) {
  if (message.role !== 'assistant' || !message.data?.length) {
    return
  }
  const chartType = normalizedChartType(message.renderChartType || message.chartType, message.data)
  if (chartType === 'table') {
    chartInstances.get(index)?.dispose()
    chartInstances.delete(index)
    return
  }
  const chartElement = chartRefs.value[index]
  if (!chartElement) {
    return
  }
  const existing = chartInstances.get(index)
  if (existing) {
    existing.dispose()
  }

  const chart = echarts.init(chartElement)
  chart.setOption(buildChartOption(message.data, chartType))
  requestAnimationFrame(() => chart.resize())
  chartInstances.set(index, chart)
}

function renderAssistantCharts() {
  messages.value.forEach((message, index) => renderSingleAssistantChart(index, message))
  scrollToBottomDeferred()
}

function openDataPreview(message: ConversationMessage) {
  previewMessage.value = message
  previewChartType.value = normalizedChartType(message.renderChartType || message.chartType, message.data)
  previewVisible.value = true
}

async function renderPreviewChart() {
  if (!previewVisible.value || !previewMessage.value || previewChartType.value === 'table') {
    disposePreviewChart()
    return
  }

  const data = previewMessage.value.data || []
  if (!data.length) {
    disposePreviewChart()
    return
  }

  await nextTick()
  if (!previewChartRef.value) {
    return
  }

  disposePreviewChart()
  previewChartInstance = echarts.init(previewChartRef.value)
  previewChartInstance.setOption(buildChartOption(data, previewChartType.value))
  requestAnimationFrame(() => previewChartInstance?.resize())
}

function disposePreviewChart() {
  if (previewChartInstance) {
    previewChartInstance.dispose()
    previewChartInstance = null
  }
}

function resizeCharts() {
  chartInstances.forEach(instance => instance.resize())
  previewChartInstance?.resize()
}

function buildChartOption(data: Record<string, string | number>[], chartType: string): echarts.EChartsOption {
  const safeType = normalizedChartType(chartType, data)
  const family = getChartFamily(safeType)
  const variant = safeType.split('.')[1] || 'classic'

  const firstRow = data[0] || {}
  const keys = Object.keys(firstRow)
  const numericKeys = keys.filter(key => data.some(row => Number.isFinite(Number(row[key]))))
  const categoryKey = keys.find(key => !numericKeys.includes(key)) || keys[0]
  const valueKeys = (numericKeys.length ? numericKeys : keys.slice(1, 2)).slice(0, 4)
  const categories = data.map((row, index) => String(row[categoryKey] ?? `项${index + 1}`))
  const seriesList = valueKeys.map(key => ({
    name: key,
    data: data.map(row => toNumber(row[key]))
  }))
  const primarySeries = seriesList[0] || { name: valueKeys[0] || '数值', data: data.map(() => 0) }
  const pieData = categories.map((name, index) => ({
    name,
    value: primarySeries.data[index] || 0
  }))

  const themeByVariant: Record<string, { colors: string[]; axis: string; split: string; bg: string }> = {
    classic: { colors: ['#2f6bff', '#14b8a6', '#f59e0b', '#ef4444'], axis: '#c7d2fe', split: '#eef2ff', bg: '#ffffff' },
    enterprise: { colors: ['#1d4ed8', '#0f766e', '#c2410c', '#7c3aed'], axis: '#bfdbfe', split: '#dbeafe', bg: '#f8fbff' },
    minimal: { colors: ['#334155', '#64748b', '#94a3b8', '#0f172a'], axis: '#e2e8f0', split: '#f1f5f9', bg: '#ffffff' },
    contrast: { colors: ['#0f172a', '#16a34a', '#f97316', '#be123c'], axis: '#94a3b8', split: '#e2e8f0', bg: '#ffffff' },
    soft: { colors: ['#60a5fa', '#34d399', '#fbbf24', '#f472b6'], axis: '#bfdbfe', split: '#dbeafe', bg: '#f8fafc' },
    'dark-grid': { colors: ['#60a5fa', '#2dd4bf', '#fbbf24', '#fb7185'], axis: '#334155', split: '#1e293b', bg: '#0f172a' },
    'light-grid': { colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'], axis: '#cbd5e1', split: '#e2e8f0', bg: '#f8fafc' }
  }
  const theme = themeByVariant[variant] || themeByVariant.classic
  const textColor = variant === 'dark-grid' ? '#e2e8f0' : '#475569'

  const baseOption: echarts.EChartsOption = {
    backgroundColor: theme.bg,
    color: theme.colors,
    animationDuration: 520,
    tooltip: { trigger: 'axis' },
    grid: { top: 36, left: 52, right: 24, bottom: 36 },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: theme.axis } },
      axisLabel: { color: textColor, interval: 0, rotate: categories.length > 6 ? 20 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: theme.split } },
      axisLabel: { color: textColor }
    }
  }

  if (family === 'line' || family === 'area') {
    return {
      ...baseOption,
      series: seriesList.map((series, index) => ({
        type: 'line',
        name: series.name,
        smooth: true,
        symbolSize: 7,
        data: series.data,
        lineStyle: { width: 3, color: theme.colors[index % theme.colors.length] },
        areaStyle: family === 'area'
          ? {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: `${theme.colors[index % theme.colors.length]}66` },
                { offset: 1, color: `${theme.colors[index % theme.colors.length]}0f` }
              ])
            }
          : undefined
      }))
    }
  }

  if (family === 'pie') {
    return {
      backgroundColor: theme.bg,
      color: theme.colors,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, icon: 'circle', textStyle: { color: textColor } },
      series: [{
        type: 'pie',
        radius: ['36%', '68%'],
        roseType: variant === 'contrast' ? 'radius' : undefined,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { color: textColor, formatter: '{b}\n{d}%' },
        data: pieData
      }]
    }
  }

  if (family === 'scatter') {
    const scatterPairs = data.map((row, idx) => {
      const x = valueKeys.length > 1 ? toNumber(row[valueKeys[0]]) : idx + 1
      const y = valueKeys.length > 1 ? toNumber(row[valueKeys[1]]) : toNumber(row[valueKeys[0]])
      return [x, y, categories[idx]]
    })
    return {
      backgroundColor: theme.bg,
      color: [theme.colors[0]],
      tooltip: {
        trigger: 'item',
        formatter: (params: any) =>
          `${params?.value?.[2] || '数据点'}<br/>X: ${params?.value?.[0]}<br/>Y: ${params?.value?.[1]}`
      },
      xAxis: { type: 'value', axisLine: { lineStyle: { color: theme.axis } }, splitLine: { lineStyle: { color: theme.split } } },
      yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: theme.split } } },
      series: [{
        type: 'scatter',
        data: scatterPairs,
        symbolSize: (params: number[]) => Math.max(10, Math.min(36, Math.abs(params[1]) / 5 + 10)),
        itemStyle: { opacity: 0.82 }
      }]
    }
  }

  if (family === 'radar') {
    const radarValues = pieData.slice(0, 8)
    const maxVal = Math.max(...radarValues.map(item => item.value), 1)
    return {
      backgroundColor: theme.bg,
      color: [theme.colors[0]],
      tooltip: {},
      radar: {
        radius: '65%',
        indicator: radarValues.map(item => ({ name: item.name, max: maxVal * 1.2 })),
        splitArea: { areaStyle: { color: ['rgba(59,130,246,0.06)', 'rgba(59,130,246,0.02)'] } }
      },
      series: [{
        type: 'radar',
        data: [{ value: radarValues.map(item => item.value), name: primarySeries.name }],
        areaStyle: { opacity: 0.2 }
      }]
    }
  }

  if (family === 'funnel') {
    const sorted = [...pieData].sort((a, b) => b.value - a.value)
    return {
      backgroundColor: theme.bg,
      color: theme.colors,
      tooltip: { trigger: 'item' },
      series: [{
        type: 'funnel',
        top: 20,
        bottom: 20,
        left: '10%',
        width: '80%',
        sort: 'descending',
        label: { color: textColor, formatter: '{b}: {c}' },
        data: sorted
      }]
    }
  }

  if (family === 'gauge') {
    const average = primarySeries.data.length
      ? primarySeries.data.reduce((sum, value) => sum + value, 0) / primarySeries.data.length
      : 0
    return {
      backgroundColor: theme.bg,
      series: [{
        type: 'gauge',
        min: 0,
        max: Math.max(100, Math.ceil(average * 1.2)),
        progress: { show: true, width: 16 },
        axisLine: { lineStyle: { width: 16 } },
        detail: { valueAnimation: true, formatter: '{value}', color: textColor },
        data: [{ value: Number(average.toFixed(2)), name: primarySeries.name }]
      }]
    }
  }

  if (family === 'heatmap') {
    const matrixData: number[][] = []
    seriesList.forEach((series, yIndex) => {
      series.data.forEach((value, xIndex) => matrixData.push([xIndex, yIndex, value]))
    })
    const maxVal = Math.max(...matrixData.map(item => item[2]), 1)
    return {
      backgroundColor: theme.bg,
      tooltip: {},
      grid: { top: 30, left: 56, right: 20, bottom: 42 },
      xAxis: { type: 'category', data: categories, axisLabel: { color: textColor, rotate: categories.length > 6 ? 20 : 0 } },
      yAxis: { type: 'category', data: seriesList.map(item => item.name), axisLabel: { color: textColor } },
      visualMap: {
        min: 0,
        max: maxVal,
        orient: 'horizontal',
        left: 'center',
        bottom: 0,
        inRange: { color: ['#dbeafe', theme.colors[0], '#0f172a'] }
      },
      series: [{
        type: 'heatmap',
        data: matrixData,
        label: { show: false }
      }]
    }
  }

  if (family === 'treemap') {
    return {
      backgroundColor: theme.bg,
      series: [{
        type: 'treemap',
        roam: false,
        breadcrumb: { show: false },
        label: { color: textColor },
        data: pieData.slice(0, 18)
      }]
    }
  }

  if (family === 'sunburst') {
    return {
      backgroundColor: theme.bg,
      series: [{
        type: 'sunburst',
        radius: [0, '92%'],
        data: [{
          name: primarySeries.name,
          children: pieData.slice(0, 12)
        }]
      }]
    }
  }

  if (family === 'sankey') {
    const limited = pieData.slice(0, 8)
    const nodes = limited.map(item => ({ name: item.name }))
    const links = limited.slice(1).map((item, index) => ({
      source: limited[index].name,
      target: item.name,
      value: Math.max(item.value, 1)
    }))
    return {
      backgroundColor: theme.bg,
      series: [{
        type: 'sankey',
        nodeAlign: 'justify',
        data: nodes,
        links
      }]
    }
  }

  if (family === 'graph') {
    const limited = pieData.slice(0, 10)
    return {
      backgroundColor: theme.bg,
      series: [{
        type: 'graph',
        layout: 'circular',
        roam: true,
        label: { show: true, color: textColor },
        data: limited.map((item, index) => ({
          id: String(index),
          name: item.name,
          value: item.value,
          symbolSize: Math.max(18, Math.min(54, item.value / 2 + 18))
        })),
        links: limited.slice(1).map((item, index) => ({
          source: String(index),
          target: String(index + 1),
          value: item.value
        }))
      }]
    }
  }

  if (family === 'tree') {
    return {
      backgroundColor: theme.bg,
      series: [{
        type: 'tree',
        top: '6%',
        left: '8%',
        right: '22%',
        bottom: '8%',
        symbolSize: 10,
        orient: 'LR',
        label: { position: 'left', verticalAlign: 'middle', align: 'right', color: textColor },
        leaves: { label: { position: 'right', align: 'left', color: textColor } },
        data: [{
          name: primarySeries.name,
          children: pieData.slice(0, 12).map(item => ({ name: `${item.name} (${item.value})` }))
        }]
      }]
    }
  }

  if (family === 'waterfall') {
    const assist: number[] = []
    const change: number[] = []
    let cumulative = 0
    primarySeries.data.forEach(value => {
      assist.push(cumulative)
      change.push(value)
      cumulative += value
    })
    return {
      ...baseOption,
      legend: { data: ['变化值'], textStyle: { color: textColor } },
      series: [
        {
          type: 'bar',
          stack: 'total',
          silent: true,
          itemStyle: { color: 'transparent', borderColor: 'transparent' },
          emphasis: { itemStyle: { color: 'transparent', borderColor: 'transparent' } },
          data: assist
        },
        {
          type: 'bar',
          name: '变化值',
          stack: 'total',
          data: change,
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: (params: any) => toNumber(params?.value) >= 0 ? theme.colors[1] : theme.colors[3] || '#ef4444'
          }
        }
      ] as any[]
    }
  }

  return {
    ...baseOption,
    series: seriesList.map((series, index) => ({
      type: 'bar',
      name: series.name,
      data: series.data,
      barMaxWidth: 32,
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: theme.colors[index % theme.colors.length] },
          { offset: 1, color: `${theme.colors[index % theme.colors.length]}80` }
        ])
      }
    }))
  }
}

function copySql(sql: string) {
  navigator.clipboard.writeText(sql)
  ElMessage.success('SQL 已复制到剪贴板')
}

function handleMessagesScroll() {
  const container = messagesContainer.value
  if (!container) {
    showScrollBottom.value = false
    return
  }
  const distanceToBottom = container.scrollHeight - container.scrollTop - container.clientHeight
  showScrollBottom.value = distanceToBottom > 180
}

function scrollToBottom(smooth = false) {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
      showScrollBottom.value = false
    }
  })
}

function scrollToBottomDeferred() {
  if (scrollBottomTimer) {
    clearTimeout(scrollBottomTimer)
  }
  scrollBottomTimer = setTimeout(() => {
    scrollToBottom()
    scrollBottomTimer = null
  }, 120)
}

function startNewConversation() {
  currentConversationId.value = ''
  messages.value = []
  inputMessage.value = ''
  disposeCharts()
  ElMessage.success('已开始新对话')
}

function disposeCharts() {
  chartInstances.forEach(instance => instance.dispose())
  chartInstances.clear()
}

function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  window.addEventListener('resize', resizeCharts)
  if (window.innerWidth < 1440) {
    sidePanelVisible.value = false
  }
  syncPreferredChartTypeFromRoute()
  await Promise.all([loadCapabilities(), loadConversationList()])
  const initialQuery = route.query.q as string | undefined
  if (initialQuery) {
    const decodedQuery = decodeURIComponent(initialQuery)
    inputMessage.value = decodedQuery
    handleSend()
  }
  nextTick(() => handleMessagesScroll())
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  if (scrollBottomTimer) {
    clearTimeout(scrollBottomTimer)
    scrollBottomTimer = null
  }
  disposeCharts()
  disposePreviewChart()
})

watch([previewVisible, previewChartType], ([visible]) => {
  if (visible) {
    renderPreviewChart()
    return
  }
  disposePreviewChart()
  previewMessage.value = null
})

watch(() => route.query.chartType, () => {
  syncPreferredChartTypeFromRoute()
})

// useLocalStorage 自动持久化 preferredChartType，无需 watch
</script>

<style scoped>
.conversation-page {
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conversation-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 2.3fr) minmax(300px, 0.78fr);
  gap: 24px;
  align-items: stretch;
  overflow: hidden;
  padding-top: 16px;
  padding-bottom: 14px;
}

.conversation-main.focus-mode {
  grid-template-columns: 1fr;
}

.conversation-main.side-collapsed {
  grid-template-columns: 1fr;
}

.conversation-primary,
.conversation-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 0;
}

.conversation-primary {
  overflow: hidden;
}

.workspace-card {
  order: -1;
  min-height: 0;
  height: calc(100dvh - 130px);
  max-height: 1180px;
}

.workspace-card :deep(.cb-card-body.padding-none) {
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.intro-card {
  order: 1;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.guide-item {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(129, 157, 219, 0.14);
}

.guide-item__step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: var(--cb-primary-light);
  color: var(--cb-primary);
  font-weight: 700;
}

.guide-item strong {
  display: block;
  margin-top: 14px;
  color: var(--cb-indigo);
  font-size: 16px;
}

.guide-item p {
  margin: 10px 0 0;
  color: var(--cb-text-regular);
  line-height: 1.7;
}

.tips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.tip-chip {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(47, 107, 255, 0.08);
  color: var(--cb-primary-dark);
  font-size: 13px;
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.workspace-header__tools {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.preferred-chart-select {
  width: 220px;
}

.workspace-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--cb-text-secondary);
}

.messages-shell {
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px 24px 20px;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  background:
    radial-gradient(circle at top left, rgba(47, 107, 255, 0.06), transparent 24%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(255, 255, 255, 0.98));
  contain: layout paint;
}

.message-scroller {
  min-height: 0;
  height: 100%;
}

.welcome-panel {
  text-align: center;
  padding: 32px 12px 18px;
}

.welcome-panel__icon {
  width: 82px;
  height: 82px;
  margin: 0 auto;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.12), rgba(20, 184, 166, 0.12));
  color: var(--cb-primary);
  font-size: 42px;
}

.welcome-panel h2 {
  margin: 20px 0 12px;
  color: var(--cb-indigo);
}

.welcome-panel p {
  margin: 0;
  color: var(--cb-text-regular);
}

.starter-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.starter-chip,
.metric-tile,
.conversation-item {
  border: none;
  cursor: pointer;
  font: inherit;
}

.starter-chip {
  padding: 12px 16px;
  border-radius: 999px;
  background: rgba(47, 107, 255, 0.08);
  color: var(--cb-primary-dark);
  transition: transform 0.2s ease, background 0.2s ease;
}

.starter-chip:hover,
.metric-tile:hover,
.conversation-item:hover {
  transform: translateY(-2px);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
}

.metric-tile {
  padding: 18px;
  text-align: left;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(129, 157, 219, 0.14);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.metric-tile strong,
.metric-tile span,
.metric-tile__examples {
  display: block;
}

.metric-tile strong {
  color: var(--cb-indigo);
  font-size: 16px;
}

.metric-tile span {
  margin-top: 10px;
  color: var(--cb-text-regular);
  line-height: 1.7;
}

.metric-tile__examples {
  margin-top: 12px;
  color: var(--cb-primary);
  font-size: 13px;
}

.composer {
  position: relative;
  flex-shrink: 0;
  padding: 14px 24px 18px;
  border-top: 1px solid rgba(129, 157, 219, 0.14);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  z-index: 2;
  box-shadow: 0 -8px 20px rgba(15, 23, 42, 0.06);
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
}

.metric-quick-panel {
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(129, 157, 219, 0.16);
  background: #f8fbff;
  display: grid;
  gap: 8px;
}

.metric-quick-panel__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.metric-quick-panel__head strong {
  color: var(--cb-indigo);
  font-size: 13px;
}

.metric-quick-panel__head span {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.metric-quick-panel__tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 2px;
}

.composer-input :deep(.el-textarea__inner) {
  min-height: 96px !important;
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fbff;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.composer-tip {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.composer-actions__buttons {
  display: flex;
  gap: 10px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.preview-header__title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-header__title strong {
  color: var(--cb-indigo);
  font-size: 16px;
}

.preview-header__title span {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.preview-body {
  display: grid;
  gap: 14px;
}

.preview-chart {
  width: 100%;
  height: 420px;
  border-radius: 14px;
  border: 1px solid rgba(129, 157, 219, 0.14);
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.96), rgba(244, 248, 255, 0.96));
}

.chart-library {
  display: grid;
  gap: 12px;
}

.chart-library__controls {
  display: flex;
  gap: 10px;
}

.chart-library__summary {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.chart-library__list {
  max-height: calc(100dvh - 220px);
  overflow-y: auto;
  display: grid;
  gap: 8px;
  padding-right: 4px;
}

.chart-library-item {
  border: 1px solid rgba(129, 157, 219, 0.16);
  background: rgba(247, 250, 255, 0.96);
  border-radius: 14px;
  padding: 10px 12px;
  text-align: left;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.chart-library-item:hover {
  transform: translateY(-1px);
}

.chart-library-item.active {
  border-color: rgba(47, 107, 255, 0.46);
  box-shadow: var(--cb-shadow-sm);
}

.chart-library-item strong,
.chart-library-item span {
  display: block;
}

.chart-library-item strong {
  color: var(--cb-indigo);
  font-size: 14px;
}

.chart-library-item span {
  margin-top: 4px;
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  padding: 14px;
  border-radius: 16px;
  background: rgba(47, 107, 255, 0.06);
}

.overview-item span,
.overview-item strong {
  display: block;
}

.overview-item span {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.overview-item strong {
  margin-top: 8px;
  color: var(--cb-indigo);
  font-size: 20px;
}

.capability-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.capability-item {
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(129, 157, 219, 0.12);
}

.capability-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.capability-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.capability-item__head strong {
  color: var(--cb-indigo);
}

.capability-item p {
  margin: 8px 0 0;
  color: var(--cb-text-regular);
  line-height: 1.7;
  font-size: 13px;
}

.capability-item__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.example-tag {
  cursor: pointer;
}

.chart-style-palette {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed rgba(129, 157, 219, 0.2);
}

.palette-label {
  display: block;
  margin-bottom: 6px;
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.market-link-btn {
  padding: 0;
  margin-bottom: 8px;
}

.palette-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.palette-tag {
  max-width: 100%;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conversation-side {
  position: sticky;
  top: 24px;
  overflow-y: auto;
}

.scroll-bottom-btn {
  position: absolute;
  right: 22px;
  bottom: 126px;
  z-index: 4;
  box-shadow: var(--cb-shadow-md);
}

.conversation-item {
  padding: 14px 16px;
  border-radius: 16px;
  text-align: left;
  background: rgba(247, 250, 255, 0.9);
  border: 1px solid rgba(129, 157, 219, 0.12);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.conversation-item.active {
  border-color: rgba(47, 107, 255, 0.32);
  background: rgba(47, 107, 255, 0.08);
  box-shadow: var(--cb-shadow-sm);
}

.conversation-item strong,
.conversation-item span {
  display: block;
}

.conversation-item strong {
  color: var(--cb-indigo);
}

.conversation-item span {
  margin-top: 6px;
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.loading-icon {
  animation: rotating 1s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1100px) {
  .conversation-main {
    grid-template-columns: 1fr;
    overflow: hidden;
  }

  .conversation-side {
    position: static;
    overflow: visible;
  }

  .workspace-card {
    min-height: 0;
    height: calc(100dvh - 150px);
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .conversation-page {
    height: 100dvh;
    min-height: 100dvh;
    overflow: hidden;
  }

  .conversation-main {
    overflow: hidden;
  }

  .workspace-card {
    height: calc(100dvh - 104px);
  }

  .guide-grid,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .messages-shell {
    padding: 16px;
  }

  .composer {
    padding: 16px;
    padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  }

  .metric-quick-panel__head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .composer-actions__buttons {
    width: 100%;
    justify-content: space-between;
  }

  .workspace-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .workspace-header__tools {
    width: 100%;
  }

  .preferred-chart-select {
    width: 100%;
  }

  .preview-chart {
    height: 320px;
  }

  .data-view-mode {
    width: 100%;
  }

  .scroll-bottom-btn {
    right: 14px;
    bottom: calc(104px + env(safe-area-inset-bottom, 0px));
  }

  .btn-text {
    display: none;
  }
}
</style>
