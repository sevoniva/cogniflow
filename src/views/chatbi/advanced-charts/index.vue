<template>
  <div class="advanced-charts-page cb-page">
    <PageHeader subtitle="图表应用市场" max-width="1320px">
      <template #actions>
        <el-button link @click="router.push('/chatbi/query')">
          <el-icon><Search /></el-icon>
          <span class="btn-text">智能查询</span>
        </el-button>
        <el-button link @click="router.push('/chatbi/conversation')">
          <el-icon><ChatDotRound /></el-icon>
          <span class="btn-text">AI 对话</span>
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-content advanced-main" style="max-width: 1320px;">
      <el-card class="page-card" shadow="never">
      <template #header>
        <div class="header-row">
          <div>
            <h2>图表应用市场</h2>
            <p>当前已支持 {{ chartCatalog.length }} 种图表样式（含主题变体），可直接筛选、预览和应用</p>
          </div>
          <div class="header-tags">
            <el-tag type="success" effect="light">100+ 图表已启用</el-tag>
            <el-tag effect="light" :type="validation.validatedTypes >= 100 ? 'success' : 'warning'">
              已验证 {{ validation.validatedTypes }} 种
            </el-tag>
            <el-tag :type="validation.coverageRate >= 99 ? 'success' : 'warning'" effect="light">
              数据验证覆盖率 {{ validation.coverageRate.toFixed(2) }}%
            </el-tag>
            <el-button link type="primary" @click="openConversationWithChart()">
              去 AI 对话页应用
            </el-button>
          </div>
        </div>
      </template>

      <el-card shadow="never" class="validation-card">
        <template #header>
          <div class="validation-header">
            <span>图表数据验证状态</span>
            <el-button text :loading="validationLoading" @click="loadValidation">
              刷新验证
            </el-button>
          </div>
        </template>
        <div class="validation-summary">
          <div class="validation-item">
            <span>已验证类型</span>
            <strong>{{ validation.validatedTypes }}/{{ validation.totalTypes }}</strong>
          </div>
          <div class="validation-item">
            <span>已验证家族</span>
            <strong>{{ validation.validatedFamilies }}/{{ validation.totalFamilies }}</strong>
          </div>
          <div class="validation-item">
            <span>覆盖率</span>
            <strong>{{ validation.coverageRate.toFixed(2) }}%</strong>
          </div>
        </div>
        <div class="family-tags">
          <el-tag
            v-for="item in validation.familyValidation"
            :key="item.family"
            size="small"
            :type="item.validated ? 'success' : 'danger'"
            effect="plain"
          >
            {{ item.family }} · {{ item.dataPoints }} 点
          </el-tag>
        </div>

        <div class="validation-controls">
          <el-switch
            v-model="validationFailedOnly"
            active-text="仅看失败项"
            inactive-text="全部类型"
          />
          <el-select v-model="validationFamilyFilter" style="width: 180px">
            <el-option label="全部家族" value="all" />
            <el-option
              v-for="family in validationFamilies"
              :key="family"
              :label="family"
              :value="family"
            />
          </el-select>
          <el-input
            v-model.trim="validationKeyword"
            clearable
            placeholder="搜索类型编码/原因"
            style="max-width: 280px"
          />
          <span class="validation-generated-at">
            更新时间：{{ validation.generatedAt ? formatValidationTime(validation.generatedAt) : '--' }}
          </span>
        </div>

        <el-table
          :data="filteredValidationRows"
          stripe
          border
          size="small"
          max-height="320"
          class="validation-table"
          :empty-text="validationLoading ? '加载中...' : '暂无匹配的验证记录'"
        >
          <el-table-column prop="type" label="图表类型" min-width="180" show-overflow-tooltip />
          <el-table-column prop="family" label="家族" width="110" />
          <el-table-column prop="variant" label="变体" width="110" />
          <el-table-column label="结果" width="96">
            <template #default="{ row }">
              <el-tag :type="row.valid ? 'success' : 'danger'" size="small" effect="plain">
                {{ row.valid ? '通过' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sampleRows" label="样本行数" width="100" />
          <el-table-column prop="dataPoints" label="数据点" width="90" />
          <el-table-column prop="reason" label="说明" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-row :gutter="16" class="catalog-filter-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-form-item label="图表家族">
            <el-select v-model="selectedFamily" style="width: 100%">
              <el-option label="全部家族" value="all" />
              <el-option v-for="family in chartFamilies" :key="family" :label="family" :value="family" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-form-item label="主题变体">
            <el-select v-model="selectedVariant" style="width: 100%">
              <el-option label="全部变体" value="all" />
              <el-option v-for="variant in chartVariants" :key="variant" :label="variant" :value="variant" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="16" :md="8">
          <el-form-item label="快速搜索">
            <el-input
              v-model.trim="searchKeyword"
              clearable
              placeholder="输入类型编码或名称，例如 line.enterprise"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8" :md="4" class="catalog-stat-col">
          <div class="catalog-stat">
            <strong>{{ filteredChartCatalog.length }}</strong>
            <span>/ {{ chartCatalog.length }} 图表可选</span>
          </div>
        </el-col>
      </el-row>

      <div class="family-summary-grid">
        <button
          v-for="summary in familySummaries"
          :key="summary.family"
          type="button"
          class="family-summary-card"
          :class="{ active: selectedFamily === summary.family }"
          @click="selectedFamily = summary.family"
        >
          <div class="family-summary-card__head">
            <strong>{{ summary.family }}</strong>
            <el-tag size="small" effect="plain">{{ summary.total }} 种</el-tag>
          </div>
          <div class="family-summary-card__meta">
            <span>已验证 {{ summary.validated }}</span>
            <span class="danger">失败 {{ summary.failed }}</span>
            <span>待验证 {{ summary.pending }}</span>
          </div>
          <div class="family-summary-card__coverage">
            覆盖率 {{ summary.coverageRate.toFixed(0) }}%
          </div>
        </button>
      </div>

      <div class="catalog-switches">
        <el-switch
          v-model="validatedOnly"
          active-text="仅看已验证"
          inactive-text="显示全部"
        />
        <el-switch
          v-model="failedOnly"
          active-text="仅看失败待修复"
          inactive-text="含通过项"
        />
        <el-button text @click="resetCatalogFilters">
          重置筛选
        </el-button>
      </div>

      <el-card shadow="never" class="market-card">
        <template #header>
          <div class="market-header">
            <span>图表目录</span>
            <span class="market-header__hint">点击卡片可切换预览图表（当前：{{ selectedChartType }})</span>
          </div>
        </template>
        <div class="market-grid">
          <button
            v-for="chart in pagedChartCatalog"
            :key="chart.type"
            type="button"
            class="market-item"
            :class="{ active: chart.type === selectedChartType }"
            @click="selectChartType(chart.type)"
          >
            <div class="market-item__meta">
              <el-tag size="small" effect="plain">{{ chart.family }}</el-tag>
              <el-tag size="small" type="info" effect="plain">{{ chart.variant }}</el-tag>
              <el-tooltip
                v-if="validationByType[chart.type] && !validationByType[chart.type].valid && validationByType[chart.type].reason"
                effect="dark"
                placement="top"
                :content="validationByType[chart.type].reason"
              >
                <el-tag size="small" type="danger" effect="dark">待修复</el-tag>
              </el-tooltip>
              <el-tag
                v-else-if="validationByType[chart.type]?.valid"
                size="small"
                type="success"
                effect="dark"
              >
                已验证
              </el-tag>
              <el-tag v-else size="small" type="warning" effect="plain">待验证</el-tag>
            </div>
            <strong>{{ chart.name }}</strong>
            <code>{{ chart.type }}</code>
          </button>
        </div>
        <div class="market-pagination">
          <span class="market-pagination__summary">
            第 {{ catalogPage }} / {{ catalogTotalPages }} 页，共 {{ filteredChartCatalog.length }} 种
          </span>
          <el-pagination
            v-model:current-page="catalogPage"
            v-model:page-size="catalogPageSize"
            layout="prev, pager, next, sizes"
            :total="filteredChartCatalog.length"
            :page-sizes="[12, 24, 36, 48]"
            :pager-count="7"
            small
          />
        </div>
        <div class="market-actions">
          <el-button type="primary" @click="openConversationWithChart()">
            用当前图表类型开始 AI 对话
          </el-button>
          <el-button plain @click="router.push({ path: '/chatbi/query', query: { chartType: selectedChartType } })">
            去智能查询并带入图表
          </el-button>
        </div>
        <div class="trial-prompts">
          <span class="trial-prompts__label">场景化 AI 试用</span>
          <el-tag
            v-for="prompt in chartTrialPrompts"
            :key="prompt"
            class="trial-prompts__tag"
            effect="plain"
            @click="openConversationWithPrompt(prompt)"
          >
            {{ prompt }}
          </el-tag>
        </div>
      </el-card>

      <el-row :gutter="16" class="toolbar-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-form-item label="图表类型">
            <el-select v-model="selectedChartType" filterable style="width: 100%" @change="refreshChart">
              <el-option
                v-for="chart in filteredChartCatalog"
                :key="chart.type"
                :label="chart.name"
                :value="chart.type"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-form-item label="数据源">
            <el-select v-model="selectedDatasource" style="width: 100%" @change="loadTables">
              <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-form-item label="数据表">
            <el-select v-model="selectedTable" style="width: 100%" @change="loadColumns">
              <el-option v-for="table in tables" :key="table" :label="table" :value="table" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-form-item label="度量字段">
            <el-select v-model="selectedMetric" style="width: 100%" @change="refreshChart">
              <el-option v-for="field in numericColumns" :key="field" :label="field" :value="field" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item label="维度字段">
            <el-select v-model="selectedDimension" style="width: 100%" @change="refreshChart">
              <el-option v-for="field in columns" :key="field" :label="field" :value="field" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item label="聚合方式">
            <el-select v-model="aggregation" style="width: 100%" @change="refreshChart">
              <el-option label="SUM" value="SUM" />
              <el-option label="AVG" value="AVG" />
              <el-option label="COUNT" value="COUNT" />
              <el-option label="MAX" value="MAX" />
              <el-option label="MIN" value="MIN" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8" class="action-col">
          <el-button type="primary" :loading="loading" @click="refreshChart">刷新图表</el-button>
        </el-col>
      </el-row>

      <div class="chart-container">
        <ChatChart
          :title="currentTitle"
          :type="selectedChartType"
          :x-data="chartXData"
          :series-data="chartSeries"
          :data="chartData"
          height="520px"
          :show-data-zoom="true"
        />
      </div>
      </el-card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Search } from '@element-plus/icons-vue'
import { ChatChart, ENTERPRISE_CHART_CATALOG, getChartFamily } from '@/components/Chart'
import { adminService } from '@/adapters'
import type { DataSource } from '@/types'
import { request } from '@/utils/http'
import PageHeader from '@/components/PageHeader.vue'

interface ChartCatalogItem {
  type: string
  family: string
  variant: string
  name: string
}

interface FamilyValidationItem {
  family: string
  validated: boolean
  sampleRows: number
  dataPoints: number
  reason: string
}

interface TypeValidationItem {
  type: string
  family: string
  variant: string
  valid: boolean
  sampleRows: number
  dataPoints: number
  reason: string
}

interface ValidationSummary {
  validatedTypes: number
  totalTypes: number
  validatedFamilies: number
  totalFamilies: number
  coverageRate: number
  familyValidation: FamilyValidationItem[]
  typeValidation: TypeValidationItem[]
  generatedAt: string
}

const chartCatalog = ref<ChartCatalogItem[]>(
  ENTERPRISE_CHART_CATALOG.map(item => ({
    type: item.type,
    family: item.family,
    variant: item.variant,
    name: item.name
  }))
)
const router = useRouter()
const route = useRoute()
const selectedChartType = ref('bar.enterprise')
const selectedFamily = ref('all')
const selectedVariant = ref('all')
const searchKeyword = ref('')

const datasources = ref<DataSource[]>([])
const selectedDatasource = ref<number>()
const tables = ref<string[]>([])
const selectedTable = ref('')
const columns = ref<string[]>([])
const numericColumns = ref<string[]>([])
const selectedDimension = ref('')
const selectedMetric = ref('')
const aggregation = ref('SUM')

const loading = ref(false)
const records = ref<Record<string, unknown>[]>([])
const validationLoading = ref(false)
const validation = ref<ValidationSummary>({
  validatedTypes: 0,
  totalTypes: 0,
  validatedFamilies: 0,
  totalFamilies: 0,
  coverageRate: 0,
  familyValidation: [],
  typeValidation: [],
  generatedAt: ''
})
const validationFailedOnly = ref(false)
const validationFamilyFilter = ref('all')
const validationKeyword = ref('')
const validatedOnly = ref(false)
const failedOnly = ref(false)
const catalogPage = ref(1)
const catalogPageSize = ref(24)

const chartFamilies = computed(() =>
  Array.from(new Set(chartCatalog.value.map(item => item.family))).sort()
)

const chartVariants = computed(() =>
  Array.from(new Set(chartCatalog.value.map(item => item.variant))).sort()
)

const validationFamilies = computed(() =>
  Array.from(new Set(validation.value.typeValidation.map(item => item.family))).sort()
)

const filteredValidationRows = computed(() => {
  const keyword = validationKeyword.value.toLowerCase()
  return validation.value.typeValidation.filter(item => {
    if (validationFailedOnly.value && item.valid) {
      return false
    }
    if (validationFamilyFilter.value !== 'all' && item.family !== validationFamilyFilter.value) {
      return false
    }
    if (!keyword) {
      return true
    }
    return item.type.toLowerCase().includes(keyword)
      || item.family.toLowerCase().includes(keyword)
      || item.variant.toLowerCase().includes(keyword)
      || item.reason.toLowerCase().includes(keyword)
  })
})

const filteredChartCatalog = computed(() => {
  const keyword = searchKeyword.value.toLowerCase()
  return chartCatalog.value.filter(item => {
    if (selectedFamily.value !== 'all' && item.family !== selectedFamily.value) {
      return false
    }
    if (selectedVariant.value !== 'all' && item.variant !== selectedVariant.value) {
      return false
    }
    if (!keyword) {
      const validationItem = validationByType.value[item.type]
      if (failedOnly.value) {
        return Boolean(validationItem && !validationItem.valid)
      }
      if (validatedOnly.value) {
        return Boolean(validationItem?.valid)
      }
      return true
    }
    const matched = item.type.toLowerCase().includes(keyword) || item.name.toLowerCase().includes(keyword)
    if (!matched) {
      return false
    }
    const validationItem = validationByType.value[item.type]
    if (failedOnly.value) {
      return Boolean(validationItem && !validationItem.valid)
    }
    if (validatedOnly.value) {
      return Boolean(validationItem?.valid)
    }
    return true
  })
})

const catalogTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredChartCatalog.value.length / catalogPageSize.value))
)

const pagedChartCatalog = computed(() => {
  const start = (catalogPage.value - 1) * catalogPageSize.value
  return filteredChartCatalog.value.slice(start, start + catalogPageSize.value)
})

const validationByType = computed<Record<string, TypeValidationItem>>(() =>
  validation.value.typeValidation.reduce((acc, item) => {
    acc[item.type] = item
    return acc
  }, {} as Record<string, TypeValidationItem>)
)

const familySummaries = computed(() =>
  chartFamilies.value.map(family => {
    const familyItems = chartCatalog.value.filter(item => item.family === family)
    let validated = 0
    let failed = 0
    let pending = 0

    familyItems.forEach(item => {
      const status = validationByType.value[item.type]
      if (!status) {
        pending += 1
      } else if (status.valid) {
        validated += 1
      } else {
        failed += 1
      }
    })

    const total = familyItems.length
    const coverageRate = total ? (validated / total) * 100 : 0

    return {
      family,
      total,
      validated,
      failed,
      pending,
      coverageRate
    }
  })
)

const currentTitle = computed(() => {
  const chart = chartCatalog.value.find(item => item.type === selectedChartType.value)
  return `${chart?.name || '图表'} - ${selectedTable.value || '未选择数据表'}`
})

const chartXData = computed(() => records.value.map(row => String(row[selectedDimension.value] ?? '')))

const chartSeries = computed(() => {
  const family = getChartFamily(selectedChartType.value)
  if (['pie', 'treemap', 'sunburst', 'sankey', 'graph', 'tree', 'gauge', 'heatmap', 'candlestick', 'boxplot', 'funnel'].includes(family)) {
    return []
  }
  return [{ name: selectedMetric.value, data: records.value.map(row => Number(row[selectedMetric.value] ?? 0)) }]
})

const chartData = computed(() => {
  const family = getChartFamily(selectedChartType.value)
  if (family === 'pie' || family === 'treemap' || family === 'sunburst' || family === 'funnel') {
    return records.value.map(row => ({
      name: String(row[selectedDimension.value] ?? '未知'),
      value: Number(row[selectedMetric.value] ?? 0)
    }))
  }

  if (family === 'gauge') {
    if (!records.value.length) return [{ value: 0, name: selectedMetric.value }]
    const avg = records.value.reduce((sum, row) => sum + Number(row[selectedMetric.value] ?? 0), 0) / records.value.length
    return [{ value: Number(avg.toFixed(2)), name: selectedMetric.value }]
  }

  if (family === 'heatmap') {
    return records.value.map((row, index) => [index, 0, Number(row[selectedMetric.value] ?? 0)])
  }

  if (family === 'candlestick') {
    return records.value.map(row => {
      const v = Number(row[selectedMetric.value] ?? 0)
      return [v * 0.95, v * 1.03, v * 0.92, v * 1.08]
    })
  }

  if (family === 'boxplot') {
    return records.value.slice(0, 8).map(row => {
      const v = Number(row[selectedMetric.value] ?? 0)
      return [v * 0.7, v * 0.85, v, v * 1.2, v * 1.35]
    })
  }

  return records.value
})

const chartTrialPrompts = computed(() => {
  const family = getChartFamily(selectedChartType.value)
  switch (family) {
    case 'line':
    case 'area':
      return ['本月销售额趋势', '近30天活跃用户趋势', '上季度回款额趋势']
    case 'bar':
      return ['本月销售额按区域对比', '本月毛利率按部门对比', '上月投诉量按产品对比']
    case 'pie':
    case 'treemap':
    case 'sunburst':
      return ['本月销售额占比分析', '本月客户类型占比', '库存金额结构占比']
    case 'scatter':
      return ['销售额与毛利率相关性分析', '客户数与回款额相关性', '工时与交付及时率关联']
    case 'funnel':
      return ['本月销售漏斗转化分析', '线索到签约转化漏斗', '审批流程阶段漏斗']
    case 'gauge':
      return ['本月毛利率达成情况', '订单履约率达成情况', '项目交付及时率达成情况']
    case 'radar':
      return ['区域经营能力雷达对比', '团队绩效雷达分析', '产品综合表现雷达图']
    default:
      return ['先给我一个经营总览', '本月销售额按区域对比', '本月毛利率趋势分析']
  }
})

function normalizeTableNames(data: unknown): string[] {
  if (!Array.isArray(data)) return []
  return data
    .map(item => {
      if (typeof item === 'string') return item
      if (item && typeof item === 'object' && 'name' in item) return String((item as { name: string }).name)
      return ''
    })
    .filter(Boolean)
}

function buildSql() {
  if (!selectedTable.value || !selectedDimension.value || !selectedMetric.value) return ''
  if (aggregation.value === 'COUNT') {
    return `SELECT ${selectedDimension.value}, COUNT(*) AS ${selectedMetric.value} FROM ${selectedTable.value} GROUP BY ${selectedDimension.value} ORDER BY ${selectedMetric.value} DESC LIMIT 24`
  }
  return `SELECT ${selectedDimension.value}, ${aggregation.value}(${selectedMetric.value}) AS ${selectedMetric.value} FROM ${selectedTable.value} GROUP BY ${selectedDimension.value} ORDER BY ${selectedMetric.value} DESC LIMIT 24`
}

async function loadDatasources() {
  datasources.value = await adminService.getDataSources()
  if (!datasources.value.length) {
    ElMessage.error('未发现可用数据源')
    return
  }
  selectedDatasource.value = datasources.value[0].id
  await loadTables()
}

async function loadChartCatalog() {
  const response = await request<Array<{ type: string; family: string; variant: string; displayName?: string }>>('/chart-catalog/types')
  if (!response.success || !Array.isArray(response.data) || !response.data.length) {
    return
  }
  chartCatalog.value = response.data.map(item => ({
    type: item.type,
    family: item.family,
    variant: item.variant,
    name: item.displayName || item.type
  }))
  if (!chartCatalog.value.some(item => item.type === selectedChartType.value)) {
    selectedChartType.value = chartCatalog.value[0].type
  }
  tryApplyRouteChartType()
}

function tryApplyRouteChartType() {
  const typeFromRoute = String(route.query.chartType || '').trim()
  if (!typeFromRoute) {
    return
  }
  if (chartCatalog.value.some(item => item.type === typeFromRoute)) {
    selectedChartType.value = typeFromRoute
  }
}

async function loadValidation() {
  validationLoading.value = true
  const response = await request<{
    validatedTypes: number
    totalTypes: number
    validatedFamilies: number
    totalFamilies: number
    coverageRate: number
    familyValidation: FamilyValidationItem[]
    typeValidation: TypeValidationItem[]
    generatedAt: string
  }>('/chart-catalog/validation?limit=0')
  validationLoading.value = false

  if (!response.success || !response.data) {
    ElMessage.warning(response.error || '图表数据验证摘要加载失败')
    return
  }
  validation.value = {
    validatedTypes: response.data.validatedTypes ?? 0,
    totalTypes: response.data.totalTypes ?? 0,
    validatedFamilies: response.data.validatedFamilies ?? 0,
    totalFamilies: response.data.totalFamilies ?? 0,
    coverageRate: response.data.coverageRate ?? 0,
    familyValidation: response.data.familyValidation || [],
    typeValidation: response.data.typeValidation || [],
    generatedAt: response.data.generatedAt || ''
  }
}

function formatValidationTime(timestamp: string) {
  if (!timestamp) {
    return '--'
  }
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return timestamp
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

async function loadTables() {
  if (!selectedDatasource.value) return
  const response = await request<Array<{ name: string }>>(`/datasources/${selectedDatasource.value}/tables`)
  if (!response.success) {
    ElMessage.error(response.error || '加载数据表失败')
    return
  }
  tables.value = normalizeTableNames(response.data)
  selectedTable.value = tables.value[0] || ''
  await loadColumns()
}

async function loadColumns() {
  if (!selectedDatasource.value || !selectedTable.value) return
  const response = await request<Array<{ name: string }>>(`/datasources/${selectedDatasource.value}/columns?table=${encodeURIComponent(selectedTable.value)}`)
  if (!response.success) {
    ElMessage.error(response.error || '加载字段失败')
    return
  }

  columns.value = (response.data || []).map(item => item.name)
  numericColumns.value = columns.value.filter(col => /amount|price|qty|quantity|count|rate|score|cost|profit|value|days|num|id/i.test(col))
  selectedDimension.value = columns.value.find(col => !numericColumns.value.includes(col)) || columns.value[0] || ''
  selectedMetric.value = numericColumns.value[0] || columns.value[1] || columns.value[0] || ''
  await refreshChart()
}

async function refreshChart() {
  if (!selectedDatasource.value) return
  const sql = buildSql()
  if (!sql) return

  loading.value = true
  const response = await request<{ records?: Record<string, unknown>[] }>('/query/execute', {
    method: 'POST',
    body: JSON.stringify({ datasourceId: selectedDatasource.value, sql })
  })
  loading.value = false

  if (!response.success) {
    records.value = []
    ElMessage.error(response.error || '图表查询失败')
    return
  }
  records.value = response.data?.records || []
}

function selectChartType(type: string) {
  if (type === selectedChartType.value) {
    return
  }
  selectedChartType.value = type
  refreshChart()
}

function resetCatalogFilters() {
  selectedFamily.value = 'all'
  selectedVariant.value = 'all'
  searchKeyword.value = ''
  validatedOnly.value = false
  failedOnly.value = false
  catalogPage.value = 1
}

function openConversationWithChart() {
  router.push({
    path: '/chatbi/conversation',
    query: {
      chartType: selectedChartType.value,
      q: encodeURIComponent('先给我一个经营总览')
    }
  })
}

function openConversationWithPrompt(prompt: string) {
  router.push({
    path: '/chatbi/conversation',
    query: {
      chartType: selectedChartType.value,
      q: encodeURIComponent(prompt)
    }
  })
}

onMounted(async () => {
  await loadChartCatalog()
  await loadValidation()
  await loadDatasources()
})

watch(filteredChartCatalog, (catalog) => {
  const pages = Math.max(1, Math.ceil(catalog.length / catalogPageSize.value))
  if (catalogPage.value > pages) {
    catalogPage.value = pages
  }
  if (!catalog.length) {
    return
  }
  if (!catalog.some(item => item.type === selectedChartType.value)) {
    selectedChartType.value = catalog[0].type
    refreshChart()
  }
})

watch([selectedFamily, selectedVariant, searchKeyword, validatedOnly, failedOnly], () => {
  catalogPage.value = 1
})

watch(failedOnly, value => {
  if (value) {
    validatedOnly.value = false
  }
})

watch(validatedOnly, value => {
  if (value) {
    failedOnly.value = false
  }
})

watch(selectedChartType, (type) => {
  if (route.query.chartType === type) {
    return
  }
  router.replace({
    query: {
      ...route.query,
      chartType: type
    }
  })
})
</script>

<style scoped>
.advanced-charts-page {
  min-height: 100vh;
}

.advanced-main {
  padding-top: 16px;
}

.page-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.header-row h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1f2d3d;
}

.header-row p {
  margin: 6px 0 0;
  color: #5c6b7a;
}

.toolbar-row {
  margin-bottom: 4px;
}

.catalog-filter-row {
  margin-bottom: 2px;
}

.catalog-switches {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin: 4px 0 12px;
}

.family-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 4px 0 12px;
}

.family-summary-card {
  border: 1px solid #e6ebf5;
  border-radius: 10px;
  background: #fff;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  display: grid;
  gap: 8px;
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.family-summary-card:hover {
  border-color: #9eb8ef;
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.08);
  transform: translateY(-1px);
}

.family-summary-card.active {
  border-color: #2f6bff;
  box-shadow: 0 10px 26px rgba(47, 107, 255, 0.16);
  background: linear-gradient(180deg, #fbfdff 0%, #f3f7ff 100%);
}

.family-summary-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.family-summary-card__head strong {
  color: #1f2937;
  font-size: 14px;
}

.family-summary-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #475467;
  font-size: 12px;
}

.family-summary-card__meta .danger {
  color: #b42318;
}

.family-summary-card__coverage {
  color: #667085;
  font-size: 12px;
}

.catalog-stat-col {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
}

.catalog-stat {
  min-height: 32px;
  color: #475467;
}

.catalog-stat strong {
  font-size: 24px;
  color: #111827;
  margin-right: 4px;
}

.catalog-stat span {
  font-size: 13px;
}

.market-card {
  margin-bottom: 16px;
  border-color: #e8eef8;
}

.market-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}

.market-header__hint {
  color: #667085;
  font-size: 12px;
  font-weight: 500;
}

.market-grid {
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.market-item {
  border: 1px solid #e6ebf5;
  border-radius: 10px;
  background: #fff;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.market-item:hover {
  border-color: #9eb8ef;
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.08);
  transform: translateY(-1px);
}

.market-item.active {
  border-color: #2f6bff;
  box-shadow: 0 10px 26px rgba(47, 107, 255, 0.16);
  background: linear-gradient(180deg, #fbfdff 0%, #f3f7ff 100%);
}

.market-item__meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.market-item strong {
  color: #1f2937;
  font-size: 14px;
  line-height: 1.4;
}

.market-item code {
  color: #475467;
  font-size: 12px;
  background: #f4f7fd;
  border-radius: 6px;
  padding: 4px 6px;
}

.market-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.trial-prompts {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e6ebf5;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.trial-prompts__label {
  color: #667085;
  font-size: 12px;
}

.trial-prompts__tag {
  cursor: pointer;
}

.market-pagination {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.market-pagination__summary {
  color: #667085;
  font-size: 12px;
}

.validation-card {
  margin-bottom: 16px;
  border-color: #e8eef8;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
}

.validation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}

.validation-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.validation-item {
  border: 1px solid #e6ebf5;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
}

.validation-item span {
  display: block;
  color: #667085;
  font-size: 12px;
}

.validation-item strong {
  display: block;
  margin-top: 4px;
  color: #1f2937;
  font-size: 18px;
}

.family-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.validation-controls {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.validation-generated-at {
  margin-left: auto;
  color: #667085;
  font-size: 12px;
}

.validation-table {
  margin-top: 12px;
}

.action-col {
  display: flex;
  align-items: end;
  justify-content: flex-end;
}

.chart-container {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #e6ebf5;
  border-radius: 10px;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f8fc 100%);
}

@media (max-width: 992px) {
  .family-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .market-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .validation-summary {
    grid-template-columns: 1fr;
  }

  .validation-generated-at {
    margin-left: 0;
    width: 100%;
  }

  .catalog-stat-col {
    justify-content: flex-start;
  }

  .catalog-switches {
    margin-top: 8px;
  }
}

@media (max-width: 640px) {
  .family-summary-grid {
    grid-template-columns: 1fr;
  }

  .market-grid {
    grid-template-columns: 1fr;
    max-height: 260px;
  }

  .market-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .btn-text {
    display: none;
  }
}
</style>
