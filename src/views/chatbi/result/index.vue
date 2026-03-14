<template>
  <div class="result-page cb-page">
    <PageHeader
      subtitle="查询结果"
      show-back
      @back="goBack"
      max-width="1120px"
    >
      <template #actions>
        <el-button v-if="result && result.total > 0" link @click="addToFavorite">
          <el-icon><Star /></el-icon>
          <span class="btn-text">收藏</span>
        </el-button>
        <el-button v-if="result && result.total > 0" link type="primary" @click="followUpWithAi">
          <el-icon><ChatDotRound /></el-icon>
          <span class="btn-text">AI追问</span>
        </el-button>
        <el-button v-if="result && result.total > 0" link @click="openChartMarket">
          <el-icon><Grid /></el-icon>
          <span class="btn-text">图表市场</span>
        </el-button>
        <el-button link @click="router.push('/chatbi/history')">
          <el-icon><Clock /></el-icon>
          <span class="btn-text">历史</span>
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-content result-main" style="max-width: 1120px;">
      <Card v-if="errorType === 'missing'" padding="lg">
        <EmptyState
          type="warning"
          title="查询条件缺失"
          description="未检测到查询参数，请返回首页重新输入"
        >
          <el-button type="primary" @click="goHome">
            <el-icon><HomeFilled /></el-icon>
            返回首页
          </el-button>
        </EmptyState>
      </Card>

      <Card v-else-if="errorType === 'api'" padding="lg">
        <EmptyState
          type="warning"
          title="查询失败"
          :description="errorMessage"
        >
          <el-button @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回修改
          </el-button>
          <el-button type="primary" plain @click="reload">
            <el-icon><Refresh /></el-icon>
            重试
          </el-button>
        </EmptyState>
      </Card>

      <Card v-else-if="loading" padding="lg">
        <div class="loading-state">
          <el-skeleton :rows="8" animated />
          <p class="loading-text">
            <el-icon class="loading-icon"><Loading /></el-icon>
            正在分析查询意图并获取数据...
          </p>
        </div>
      </Card>

      <template v-else-if="result && result.total > 0">
        <Card padding="lg" class="summary-card">
          <div class="summary-top">
            <div>
              <div class="eyebrow">Insight Report</div>
              <div class="query-display">
                <el-icon class="query-icon"><Search /></el-icon>
                <span class="query-text">{{ result.query }}</span>
              </div>
            </div>
            <div class="result-meta">
              <el-tag size="small" effect="plain">
                <el-icon><DataLine /></el-icon>
                {{ result.metric }}
              </el-tag>
              <el-tag size="small" effect="plain" type="info">
                <el-icon><Calendar /></el-icon>
                {{ result.timeRange }}
              </el-tag>
              <el-tag size="small" effect="plain" type="success">
                <el-icon><DocumentChecked /></el-icon>
                {{ result.total }} 条记录
              </el-tag>
            </div>
          </div>

          <div class="kpi-grid">
            <div class="kpi-item">
              <span>分析维度</span>
              <strong>{{ result.dimension }}</strong>
            </div>
            <div class="kpi-item">
              <span>主指标值</span>
              <strong>{{ primaryMetricValue }}</strong>
            </div>
            <div class="kpi-item">
              <span>维度数量</span>
              <strong>{{ xAxisData.length }}</strong>
            </div>
            <div class="kpi-item">
              <span>图表模板</span>
              <strong>4 种视图</strong>
            </div>
          </div>
        </Card>

        <Card v-if="result.summary" padding="lg" class="insight-card">
          <div class="insight-header">
            <div>
              <div class="eyebrow">Insight Narrative</div>
              <h3>智能解读</h3>
            </div>
            <div class="insight-tags">
              <el-tag :type="isAiDriven ? 'success' : 'info'" effect="plain">
                {{ analysisSourceLabel }}
              </el-tag>
              <el-tag v-if="result.aiStatus?.runtimeEnabled === false" type="warning" effect="plain">
                外部模型未启用
              </el-tag>
            </div>
          </div>

          <p class="insight-summary">{{ result.summary }}</p>

          <div v-if="result.diagnosis && result.diagnosis.code !== 'QUERY_EXECUTED'" class="diagnosis-box">
            <div class="diagnosis-head">
              <el-tag size="small" effect="plain" type="warning">
                诊断码：{{ result.diagnosis.code }}
              </el-tag>
              <el-tag v-if="result.diagnosis.recovered" size="small" effect="plain" type="success">
                已自动恢复
              </el-tag>
              <el-tag
                v-if="result.diagnosis.guidanceScenario"
                size="small"
                effect="plain"
                type="info"
              >
                {{ result.diagnosis.guidanceScenario }}
              </el-tag>
            </div>
            <p>{{ result.diagnosis.reason }}</p>
            <div v-if="result.diagnosis.slotEvidence" class="diagnosis-actions">
              <span>槽位证据</span>
              <div class="suggestion-list">
                <el-tag
                  v-if="result.diagnosis.slotEvidence.primaryMetric?.value"
                  effect="plain"
                  class="suggestion-tag"
                >
                  主指标：{{ result.diagnosis.slotEvidence.primaryMetric.value }}
                </el-tag>
                <el-tag
                  v-if="result.diagnosis.slotEvidence.secondaryMetric?.value"
                  effect="plain"
                  class="suggestion-tag"
                >
                  对比指标：{{ result.diagnosis.slotEvidence.secondaryMetric.value }}
                </el-tag>
                <el-tag
                  v-for="metric in result.diagnosis.slotEvidence.secondaryMetric?.candidates || []"
                  :key="`slot-metric-${metric}`"
                  effect="plain"
                  class="suggestion-tag"
                >
                  待确认：{{ metric }}
                </el-tag>
                <div
                  v-if="rankedCandidateReasonOptionsForResult.length > 1"
                  class="diagnosis-rank-filters"
                >
                  <span>证据分组</span>
                  <el-button
                    v-for="option in rankedCandidateReasonOptionsForResult"
                    :key="`slot-ranked-filter-${option.key}`"
                    size="small"
                    plain
                    class="diagnosis-rank-filter"
                    :class="{ 'is-active': rankedCandidateReasonFilter === option.key }"
                    :type="rankedCandidateReasonFilter === option.key ? 'warning' : undefined"
                    @click="setRankedCandidateReasonFilterForResult(option.key)"
                  >
                    {{ option.label }}（{{ option.count }}）
                  </el-button>
                </div>
                <el-tag
                  v-for="(candidate, rankedIndex) in visibleRankedCandidatesForResult"
                  :key="`slot-ranked-${candidate.metric || rankedIndex}`"
                  effect="plain"
                  class="suggestion-tag"
                  :type="rankedIndex === 0 ? 'warning' : 'info'"
                  :class="{ 'is-ranked-top': rankedIndex === 0 }"
                  :title="`分类：${rankedCandidateReasonSummary(candidate)}`"
                >
                  {{ rankedCandidateLabel(candidate, rankedIndex) }}
                </el-tag>
                <el-button
                  v-if="hiddenRankedCountForResult > 0 || rankedCandidatesExpanded"
                  link
                  type="primary"
                  class="diagnosis-rank-toggle"
                  @click="rankedCandidatesExpanded = !rankedCandidatesExpanded"
                >
                  {{ rankedCandidatesExpanded ? '收起排序证据' : `展开更多排序证据（+${hiddenRankedCountForResult}）` }}
                </el-button>
                <el-tag
                  v-if="result.diagnosis.slotEvidence.secondaryMetric?.source"
                  effect="plain"
                  class="suggestion-tag"
                >
                  来源：{{ slotSourceLabel(result.diagnosis.slotEvidence.secondaryMetric.source) }}
                </el-tag>
                <el-tag
                  v-if="result.diagnosis.slotEvidence.secondaryMetric?.confidence !== undefined"
                  effect="plain"
                  class="suggestion-tag"
                >
                  置信度：{{ formatConfidence(result.diagnosis.slotEvidence.secondaryMetric.confidence) }}
                </el-tag>
                <el-tag
                  v-if="result.diagnosis.slotConflict"
                  effect="plain"
                  class="suggestion-tag"
                >
                  槽位冲突
                </el-tag>
              </div>
            </div>
            <div v-if="result.diagnosis.actions?.length" class="diagnosis-actions">
              <span>建议动作</span>
              <div class="suggestion-list">
                <el-tag
                  v-for="action in result.diagnosis.actions"
                  :key="action"
                  effect="plain"
                  class="suggestion-tag"
                  :class="{ 'is-clickable': canRunAction(action) }"
                  @click="runAction(action)"
                >
                  {{ action }}
                </el-tag>
              </div>
            </div>
          </div>

          <div v-if="result.disambiguation && result.candidateMetrics?.length" class="disambiguation-box">
            <span>检测到多个可能指标，请先选择一个继续分析：</span>
            <div class="disambiguation-tags">
              <el-tag
                v-for="metric in result.candidateMetrics"
                :key="metric"
                effect="light"
                type="warning"
                class="metric-choice-tag"
                @click="runSuggestion(`本月${metric}`)"
              >
                {{ metric }}
              </el-tag>
            </div>
          </div>

          <div v-if="result.suggestions?.length" class="insight-actions">
            <span>推荐继续问</span>
            <div class="suggestion-list">
              <el-tag
                v-for="suggestion in result.suggestions"
                :key="suggestion"
                effect="plain"
                class="suggestion-tag"
                @click="runSuggestion(suggestion)"
              >
                {{ suggestion }}
              </el-tag>
            </div>
          </div>
        </Card>

        <Card v-if="result && result.total > 0" padding="md" class="view-mode-card">
          <div class="view-mode-toolbar">
            <div class="view-mode-toolbar__title">
              <div class="eyebrow">View Mode</div>
              <strong>分析视图模式</strong>
            </div>
            <div class="view-mode-toolbar__actions">
              <el-radio-group v-model="resultViewMode" size="small">
                <el-radio-button label="executive">图表优先</el-radio-button>
                <el-radio-button label="split">图表+明细</el-radio-button>
                <el-radio-button label="table">仅明细</el-radio-button>
              </el-radio-group>
              <el-button plain @click="openChartMarket">
                查看 100+ 图表样式
              </el-button>
            </div>
          </div>
        </Card>

        <section class="chart-grid" v-if="resultViewMode !== 'table' && xAxisData.length && numericSeries.length">
          <Card padding="md" class="chart-card chart-card--wide">
            <template #header>
              <span class="card-title">趋势分析</span>
            </template>
            <ChatLineChart
              :title="`${result.metric}趋势`"
              :x-data="xAxisData"
              :legend="[numericField]"
              :series-data="lineSeries"
              height="360px"
              :show-data-zoom="true"
            />
          </Card>

          <Card padding="md" class="chart-card">
            <template #header>
              <span class="card-title">结构对比</span>
            </template>
            <ChatBarChart
              :title="`${result.metric}对比`"
              :x-data="xAxisData"
              :legend="[numericField]"
              :series-data="barSeries"
              height="320px"
            />
          </Card>

          <Card padding="md" class="chart-card">
            <template #header>
              <span class="card-title">占比分布</span>
            </template>
            <ChatPieChart
              :title="`${result.metric}占比`"
              :data="pieData"
              height="320px"
            />
          </Card>

          <Card padding="md" class="chart-card">
            <template #header>
              <span class="card-title">达成看板</span>
            </template>
            <ChatGaugeChart
              :title="`${result.metric}健康度`"
              :data="gaugeData"
              height="320px"
            />
          </Card>
        </section>

        <Card v-if="resultViewMode !== 'executive'" padding="none">
          <el-table
            :data="result.data"
            border
            stripe
            size="small"
            style="width: 100%"
            :header-cell-style="{ background: '#f5f8ff', fontWeight: 700 }"
          >
            <el-table-column
              v-for="col in columns"
              :key="col"
              :prop="col"
              :label="col"
              min-width="140"
              show-overflow-tooltip
            />
          </el-table>
        </Card>

        <div class="cb-actions">
          <el-button @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回修改
          </el-button>
          <el-button type="primary" @click="followUpWithAi">
            <el-icon><ChatDotRound /></el-icon>
            进入 AI 对话追问
          </el-button>
          <el-button type="primary" plain @click="reload">
            <el-icon><Refresh /></el-icon>
            重新查询
          </el-button>
          <el-button type="warning" plain @click="addToFavorite">
            <el-icon><Star /></el-icon>
            加入收藏
          </el-button>
        </div>
      </template>

      <Card v-else-if="result && result.total === 0" padding="lg">
        <EmptyState
          type="search"
          title="未查询到数据"
          description="建议调整查询条件或时间范围后重试"
        >
          <el-button @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回修改
          </el-button>
          <el-button type="primary" plain @click="reload">
            <el-icon><Refresh /></el-icon>
            重试
          </el-button>
        </EmptyState>
      </Card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  ChatDotRound,
  Refresh,
  Star,
  Clock,
  HomeFilled,
  Search,
  DataLine,
  Calendar,
  DocumentChecked,
  Loading,
  Grid
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import ChatLineChart from '@/components/Chart/ChatLineChart.vue'
import ChatBarChart from '@/components/Chart/ChatBarChart.vue'
import ChatPieChart from '@/components/Chart/ChatPieChart.vue'
import ChatGaugeChart from '@/components/Chart/ChatGaugeChart.vue'
import { chatbiService } from '@/adapters'
import type { QueryResult } from '@/types'
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

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const errorType = ref<'missing' | 'api' | null>(null)
const errorMessage = ref('')
const result = ref<QueryResult | null>(null)
const resultViewMode = ref<'executive' | 'split' | 'table'>('executive')
const rankedCandidatesExpanded = ref(false)
const rankedCandidateReasonFilter = ref<RankedCandidateReasonFilter>('all')

const columns = computed(() => {
  if (!result.value?.data.length) return []
  return Object.keys(result.value.data[0])
})

const xField = computed(() => columns.value[0] || '维度')
const numericField = computed(() => {
  const first = result.value?.data[0]
  if (!first) return '数值'
  return columns.value.find((column) => typeof first[column] === 'number') || columns.value[1] || columns.value[0] || '数值'
})

const xAxisData = computed(() => (result.value?.data || []).map((item) => String(item[xField.value])))
const numericSeries = computed(() => (result.value?.data || []).map((item) => Number(item[numericField.value]) || 0))
const primaryMetricValue = computed(() => {
  if (!numericSeries.value.length) return '-'
  return numericSeries.value.reduce((sum, current) => sum + current, 0).toLocaleString('zh-CN')
})

const lineSeries = computed(() => [{ name: numericField.value, data: numericSeries.value }])
const barSeries = computed(() => [{ name: numericField.value, data: numericSeries.value }])
const pieData = computed(() => xAxisData.value.map((label, index) => ({ name: label, value: numericSeries.value[index] || 0 })))
const gaugeData = computed(() => {
  if (!numericSeries.value.length) {
    return [{ value: 0, name: '完成度' }]
  }
  const max = Math.max(...numericSeries.value)
  const avg = numericSeries.value.reduce((sum, current) => sum + current, 0) / numericSeries.value.length
  const score = max === 0 ? 0 : Math.round((avg / max) * 100)
  return [{ value: score, name: '健康度' }]
})
const isAiDriven = computed(() => {
  const source = result.value?.source || ''
  return source.includes('ai') || source === 'llm'
})
const analysisSourceLabel = computed(() => {
  const source = result.value?.source || ''
  const labels: Record<string, string> = {
    llm: '外部大模型',
    'overview-ai': '经营总览 + AI',
    'business-insight-ai': '业务指标 + AI',
    overview: '经营总览',
    'business-insight': '业务语义引擎',
    'guided-discovery': '指标引导',
    'guided-disambiguation': '指标澄清',
    'guided-recovery': '稳定降级'
  }
  return labels[source] || '查询引擎'
})

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

const rankedCandidatesForResult = computed(
  () => result.value?.diagnosis?.slotEvidence?.secondaryMetric?.rankedCandidates || []
)

const rankedCandidateReasonOptionsForResult = computed(() =>
  rankedCandidateReasonOptions(rankedCandidatesForResult.value)
)

const filteredRankedCandidatesForResult = computed(() =>
  filterRankedCandidatesByReason(rankedCandidatesForResult.value, rankedCandidateReasonFilter.value)
)

const visibleRankedCandidatesForResult = computed(() =>
  visibleRankedCandidates(filteredRankedCandidatesForResult.value, rankedCandidatesExpanded.value, 2)
)

const hiddenRankedCountForResult = computed(() =>
  hiddenRankedCandidateCount(filteredRankedCandidatesForResult.value, rankedCandidatesExpanded.value, 2)
)

function rankedCandidateLabel(candidate: {
  metric?: string
  score?: number
  position?: number
  reason?: string
}, index: number) {
  return buildRankedCandidateLabel(
    candidate,
    rankedCandidateDisplayIndex(rankedCandidatesForResult.value, candidate, index)
  )
}

function setRankedCandidateReasonFilterForResult(filter: RankedCandidateReasonFilter) {
  rankedCandidateReasonFilter.value = filter
  rankedCandidatesExpanded.value = false
}

async function fetchResult() {
  const q = route.query.q as string
  if (!q) {
    errorType.value = 'missing'
    return
  }

  errorType.value = null
  errorMessage.value = ''
  loading.value = true
  result.value = null
  rankedCandidatesExpanded.value = false
  rankedCandidateReasonFilter.value = 'all'

  try {
    result.value = await chatbiService.executeQuery({ text: decodeURIComponent(q) })
  } catch (e: any) {
    errorType.value = 'api'
    errorMessage.value = e?.message || '查询失败，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}

function openChartMarket() {
  const chartType = numericSeries.value.length > 8 ? 'line.enterprise' : 'bar.enterprise'
  router.push({
    path: '/chatbi/chart-market',
    query: { chartType }
  })
}

function goHome() {
  router.push('/chatbi/query')
}

function goBack() {
  const text = result.value?.query || (route.query.q as string)
  if (text) {
    router.push({ path: '/chatbi/query', query: { backQuery: encodeURIComponent(text) } })
  } else {
    goHome()
  }
}

function reload() {
  fetchResult()
}

function runSuggestion(text: string) {
  router.push({
    path: '/chatbi/result',
    query: { q: encodeURIComponent(text) }
  })
}

function canRunAction(action: string) {
  return /[？?]|本月|上月|总览|趋势|对比|分析/.test(action)
}

function runAction(action: string) {
  if (!canRunAction(action)) {
    return
  }
  runSuggestion(action)
}

function followUpWithAi() {
  const text = result.value?.query || (route.query.q as string)
  if (!text) {
    ElMessage.warning('暂无查询内容')
    return
  }

  router.push({
    path: '/chatbi/conversation',
    query: { q: encodeURIComponent(decodeURIComponent(text)) }
  })
}

async function addToFavorite() {
  const text = result.value?.query || (route.query.q as string)
  if (!text) {
    ElMessage.warning('暂无查询内容')
    return
  }
  const success = await chatbiService.addFavorite(decodeURIComponent(text))
  if (success) {
    ElMessage.success('已加入收藏')
  } else {
    ElMessage.info('该查询已在收藏中')
  }
}

onMounted(fetchResult)
watch(() => route.query.q, () => {
  fetchResult()
})

watch(result, (current) => {
  if (!current?.data?.length) {
    resultViewMode.value = 'table'
    return
  }
  const hasNumeric = current.data.some(row =>
    Object.values(row).some(value => typeof value === 'number')
  )
  resultViewMode.value = hasNumeric ? 'executive' : 'table'
})
</script>

<style scoped>
.result-main {
  gap: 24px;
}

.summary-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(240, 247, 255, 0.9));
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--cb-primary);
}

.query-display {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.query-icon {
  color: var(--cb-primary);
}

.query-text {
  font-size: 28px;
  font-weight: 700;
  color: var(--cb-indigo);
}

.result-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.result-meta .el-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.kpi-item {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(129, 157, 219, 0.16);
}

.kpi-item span,
.kpi-item strong {
  display: block;
}

.kpi-item span {
  color: var(--cb-text-secondary);
  margin-bottom: 8px;
  font-size: 12px;
}

.kpi-item strong {
  color: var(--cb-indigo);
  font-size: 24px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.view-mode-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 255, 0.94));
}

.view-mode-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.view-mode-toolbar__title strong {
  display: block;
  margin-top: 8px;
  color: var(--cb-indigo);
  font-size: 18px;
}

.view-mode-toolbar__actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.insight-card {
  background:
    radial-gradient(circle at top right, rgba(47, 107, 255, 0.14), transparent 28%),
    linear-gradient(135deg, rgba(15, 23, 42, 0.98), rgba(24, 53, 112, 0.96));
  color: #f8fbff;
}

.insight-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.insight-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.insight-header h3 {
  margin: 10px 0 0;
  font-size: 24px;
  color: #f8fbff;
}

.insight-summary {
  margin: 20px 0 0;
  line-height: 1.9;
  font-size: 15px;
  color: rgba(248, 251, 255, 0.92);
  white-space: pre-line;
}

.insight-actions {
  margin-top: 20px;
  display: grid;
  gap: 12px;
}

.disambiguation-box {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(245, 158, 11, 0.35);
  background: rgba(255, 251, 235, 0.92);
  color: #92400e;
  display: grid;
  gap: 10px;
}

.diagnosis-box {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px dashed rgba(250, 204, 21, 0.42);
  background: rgba(254, 252, 232, 0.1);
  color: rgba(255, 255, 255, 0.9);
  display: grid;
  gap: 10px;
}

.diagnosis-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-box p {
  margin: 0;
  line-height: 1.7;
}

.diagnosis-actions {
  display: grid;
  gap: 8px;
}

.diagnosis-actions > span {
  color: rgba(248, 251, 255, 0.72);
  font-size: 13px;
}

.disambiguation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metric-choice-tag {
  cursor: pointer;
}

.insight-actions > span {
  color: rgba(248, 251, 255, 0.72);
  font-size: 13px;
}

.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.diagnosis-rank-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.diagnosis-rank-filters > span {
  color: rgba(248, 251, 255, 0.72);
  font-size: 12px;
  font-weight: 600;
}

.suggestion-tag {
  cursor: pointer;
  border-color: rgba(255, 255, 255, 0.16);
  color: #f8fbff;
  background: rgba(255, 255, 255, 0.08);
}

.suggestion-tag:not(.is-clickable) {
  cursor: default;
}

.suggestion-tag.is-ranked-top {
  border-color: rgba(245, 158, 11, 0.4);
  background: rgba(255, 247, 237, 0.95);
}

.diagnosis-rank-filter {
  margin: 0;
}

.diagnosis-rank-filter.is-active {
  box-shadow: 0 6px 14px rgba(245, 158, 11, 0.18);
}

.diagnosis-rank-toggle {
  padding: 0;
}

.chart-card--wide {
  grid-column: span 2;
}

.card-title {
  color: var(--cb-indigo);
  font-weight: 700;
}

.loading-state {
  padding: var(--cb-space-lg);
}

.loading-text {
  text-align: center;
  color: var(--cb-text-secondary);
  margin-top: var(--cb-space-lg);
  font-size: var(--cb-font-size-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--cb-space-sm);
}

.loading-icon {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 960px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .chart-card--wide {
    grid-column: span 1;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-top {
    flex-direction: column;
  }

  .insight-header {
    flex-direction: column;
  }

  .query-text {
    font-size: 22px;
  }

  .view-mode-toolbar__actions {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .btn-text {
    display: none;
  }
}
</style>
