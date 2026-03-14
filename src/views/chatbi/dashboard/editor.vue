<template>
  <div class="dashboard-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <span class="dashboard-title">{{ dashboardName || '未命名仪表板' }}</span>
        <el-button link @click="editName">
          <el-icon><Edit /></el-icon>
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button @click="togglePreview" :type="previewMode ? 'success' : ''">
          <el-icon><View /></el-icon>
          {{ previewMode ? '退出预览' : '预览' }}
        </el-button>
        <el-button type="primary" @click="saveDashboard" :loading="saving">
          <el-icon><Upload /></el-icon>
          保存
        </el-button>
        <el-button type="success" @click="publishDashboard">
          <el-icon><Promotion /></el-icon>
          发布
        </el-button>
      </div>
    </div>

    <div class="editor-container">
      <!-- 左侧组件面板 -->
      <div class="component-panel" v-if="!previewMode">
        <div class="panel-section">
          <h4 class="section-title">图表类型</h4>
          <div class="chart-types">
            <div
              v-for="chart in chartTypes"
              :key="chart.type"
              class="chart-item"
              draggable="true"
              @dragstart="onDragStart($event, chart)"
            >
              <div class="chart-icon">
                <el-icon :size="24"><component :is="chart.icon" /></el-icon>
              </div>
              <span class="chart-name">{{ chart.name }}</span>
            </div>
          </div>
        </div>

        <div class="panel-section">
          <h4 class="section-title">数据源</h4>
          <el-select v-model="selectedDatasource" placeholder="选择数据源" style="width: 100%" @change="loadTables">
            <el-option
              v-for="ds in datasources"
              :key="ds.id"
              :label="ds.name"
              :value="ds.id"
            />
          </el-select>
        </div>

        <div class="panel-section" v-if="selectedDatasource">
          <h4 class="section-title">数据表</h4>
          <el-tree
            :data="tableFields"
            :props="{ children: 'fields', label: 'name' }"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.fields"><Document /></el-icon>
                <el-icon v-else><DataLine /></el-icon>
                <span>{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 中间画布区域 -->
      <div class="canvas-area">
        <div class="canvas-toolbar" v-if="!previewMode">
          <el-button-group>
            <el-button :icon="Plus" @click="addComponent" size="small">添加组件</el-button>
            <el-button :icon="Delete" @click="clearCanvas" size="small">清空画布</el-button>
          </el-button-group>
        </div>

        <div class="canvas" ref="canvasRef" @dragover="onDragOver" @drop="onDrop">
          <div
            v-for="component in components"
            :key="component.id"
            class="canvas-component"
            :class="{ 'selected': selectedComponent?.id === component.id, 'preview': previewMode }"
            :style="{
              left: component.x + 'px',
              top: component.y + 'px',
              width: component.width + 'px',
              height: component.height + 'px',
              zIndex: component.zIndex
            }"
            @click="selectComponent(component)"
            @mousedown="startMove($event, component)"
          >
            <!-- 组件内容 -->
            <div class="component-content" @mousedown.stop>
              <div class="component-header">
                <span class="component-title">{{ component.title || '未命名组件' }}</span>
                <div class="component-actions" v-if="!previewMode">
                  <el-button link @click.stop="editComponent(component)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button link @click.stop="removeComponent(component)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="component-body">
                <!-- 图表渲染区域 -->
                <div v-if="component.chartType" class="chart-placeholder">
                  <div class="chart-preview" :ref="el => initChart(el as Element | ComponentPublicInstance | null, component)">
                    <v-chart
                      v-if="component.chartOption"
                      :option="component.chartOption"
                      :autoresize="true"
                      style="width: 100%; height: 100%"
                    />
                  </div>
                </div>
                <div v-else class="empty-chart">
                  <el-icon :size="48"><DataLine /></el-icon>
                  <p>点击配置数据</p>
                </div>
              </div>
            </div>

            <!-- 调整大小手柄 -->
            <div
              v-if="!previewMode && selectedComponent?.id === component.id"
              class="resize-handle resize-se"
              @mousedown.stop="startResize($event, component)"
            />
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="property-panel" v-if="selectedComponent && !previewMode">
        <div class="panel-header">
          <h4>组件属性</h4>
          <el-button link @click="selectedComponent = null">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <el-form label-position="top" size="small">
          <el-form-item label="组件名称">
            <el-input v-model="selectedComponent.title" @change="updateComponent" />
          </el-form-item>

          <el-form-item label="图表类型">
            <el-select v-model="selectedComponent.chartType" @change="onChartTypeChange" style="width: 100%">
              <el-option
                v-for="chart in chartOptions"
                :key="chart.type"
                :label="chart.name"
                :value="chart.type"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="数据表">
            <el-select v-model="selectedComponent.table" @change="onTableChange" style="width: 100%">
              <el-option
                v-for="table in tables"
                :key="table"
                :label="table"
                :value="table"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="维度字段">
            <el-select v-model="selectedComponent.dimension" multiple collapse-tags collapse-tags-tooltip style="width: 100%">
              <el-option
                v-for="field in availableFields"
                :key="field"
                :label="field"
                :value="field"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="度量字段">
            <el-select v-model="selectedComponent.metrics" multiple collapse-tags collapse-tags-tooltip style="width: 100%">
              <el-option
                v-for="field in availableFields"
                :key="field"
                :label="field"
                :value="field"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="聚合方式">
            <el-select v-model="selectedComponent.aggregation" style="width: 100%">
              <el-option label="SUM" value="SUM" />
              <el-option label="AVG" value="AVG" />
              <el-option label="COUNT" value="COUNT" />
              <el-option label="MAX" value="MAX" />
              <el-option label="MIN" value="MIN" />
            </el-select>
          </el-form-item>

          <el-form-item label="过滤器">
            <div class="filter-builder">
              <el-button size="small" @click="addFilter" style="width: 100%">
                <el-icon><Plus /></el-icon>
                添加过滤条件
              </el-button>
              <div v-for="(filter, index) in selectedComponent.filters" :key="index" class="filter-item">
                <el-select v-model="filter.field" placeholder="字段" size="small">
                  <el-option
                    v-for="field in availableFields"
                    :key="field"
                    :label="field"
                    :value="field"
                  />
                </el-select>
                <el-select v-model="filter.operator" placeholder="操作符" size="small" style="width: 80px">
                  <el-option label="=" value="=" />
                  <el-option label="!=" value="!=" />
                  <el-option label=">" value=">" />
                  <el-option label="<" value="<" />
                  <el-option label="LIKE" value="LIKE" />
                  <el-option label="IN" value="IN" />
                </el-select>
                <el-input v-model="filter.value" placeholder="值" size="small" />
                <el-button link type="danger" @click="removeFilter(index)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </el-form-item>

          <el-divider />

          <el-form-item label="位置 X">
            <el-input-number v-model="selectedComponent.x" :min="0" :step="10" @change="updateComponent" />
          </el-form-item>

          <el-form-item label="位置 Y">
            <el-input-number v-model="selectedComponent.y" :min="0" :step="10" @change="updateComponent" />
          </el-form-item>

          <el-form-item label="宽度">
            <el-input-number v-model="selectedComponent.width" :min="100" :step="10" @change="updateComponent" />
          </el-form-item>

          <el-form-item label="高度">
            <el-input-number v-model="selectedComponent.height" :min="100" :step="10" @change="updateComponent" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 编辑名称对话框 -->
    <el-dialog v-model="nameDialogVisible" title="编辑仪表板名称" width="400px">
      <el-input v-model="dashboardName" placeholder="请输入仪表板名称" />
      <template #footer>
        <el-button @click="nameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEditName">确定</el-button>
      </template>
    </el-dialog>

    <!-- 数据配置对话框 -->
    <el-dialog v-model="configDialogVisible" title="配置图表数据" width="600px">
      <el-form label-width="100px">
        <el-form-item label="图表类型">
          <el-select v-model="tempComponent.chartType" style="width: 100%">
            <el-option
              v-for="chart in chartOptions"
              :key="chart.type"
              :label="chart.name"
              :value="chart.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据表">
          <el-select v-model="tempComponent.table" @change="loadTempFields" style="width: 100%">
            <el-option
              v-for="table in tables"
              :key="table"
              :label="table"
              :value="table"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="维度字段">
          <el-select v-model="tempComponent.dimension" multiple style="width: 100%">
            <el-option
              v-for="field in availableFields"
              :key="field"
              :label="field"
              :value="field"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="度量字段">
          <el-select v-model="tempComponent.metrics" multiple style="width: 100%">
            <el-option
              v-for="field in availableFields"
              :key="field"
              :label="field"
              :value="field"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type ComponentPublicInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  Upload,
  Promotion,
  View,
  Plus,
  Delete,
  Close,
  DataLine,
  Document,
  TrendCharts,
  PieChart,
  Histogram,
  Pointer,
  Grid,
  Filter
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart as PieChartType, ScatterChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { adminService } from '@/adapters'
import type { DataSource } from '@/types'
import { request } from '@/utils/http'
import { ENTERPRISE_CHART_CATALOG, getChartFamily } from '@/components/Chart'

use([CanvasRenderer, BarChart, LineChart, PieChartType, ScatterChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

interface DashboardComponent {
  id: number
  title: string
  chartType: string
  x: number
  y: number
  width: number
  height: number
  zIndex: number
  datasourceId?: number
  table?: string
  dimension?: string[]
  metrics?: string[]
  aggregation?: string
  filters?: Array<{ field: string; operator: string; value: string }>
  chartOption?: any
  chartData?: Record<string, unknown>[]
}

interface ChartType {
  type: string
  name: string
  icon: any
}

interface TableInfo {
  name: string
}

const route = useRoute()
const router = useRouter()

const dashboardId = ref<number>()
const dashboardName = ref('')
const nameDialogVisible = ref(false)
const previewMode = ref(false)
const saving = ref(false)

const selectedDatasource = ref<number>()
const datasources = ref<DataSource[]>([])
const tables = ref<string[]>([])
const tableFields = ref<Array<{ id: string; name: string; fields: unknown[] }>>([])
const availableFields = ref<string[]>([])

const chartTypes: ChartType[] = [
  { type: 'bar', name: '柱状图', icon: Histogram },
  { type: 'line', name: '折线图', icon: TrendCharts },
  { type: 'pie', name: '饼图', icon: PieChart },
  { type: 'scatter', name: '散点图', icon: Pointer },
  { type: 'radar', name: '雷达图', icon: TrendCharts },
  { type: 'funnel', name: '漏斗图', icon: Filter },
  { type: 'heatmap', name: '热力图', icon: Grid },
  { type: 'treemap', name: '矩形树图', icon: Grid },
  { type: 'sunburst', name: '旭日图', icon: PieChart },
  { type: 'sankey', name: '桑基图', icon: TrendCharts },
  { type: 'waterfall', name: '瀑布图', icon: Histogram },
  { type: 'gauge', name: '仪表盘', icon: Pointer },
  { type: 'candlestick', name: 'K线图', icon: TrendCharts },
  { type: 'boxplot', name: '箱线图', icon: Histogram },
  { type: 'graph', name: '关系图', icon: Grid },
  { type: 'tree', name: '树图', icon: Document },
  { type: 'table', name: '表格', icon: Grid },
  { type: 'filter', name: '过滤器', icon: Filter }
]
const chartOptions = ENTERPRISE_CHART_CATALOG

const components = ref<DashboardComponent[]>([])
const selectedComponent = ref<DashboardComponent | null>(null)
const configDialogVisible = ref(false)
const tempComponent = ref<Partial<DashboardComponent>>({})

const canvasRef = ref<HTMLElement>()

let draggedChart: ChartType | null = null
let isMoving = false
let moveOffset = { x: 0, y: 0 }
let isResizing = false
let resizeStart = { x: 0, y: 0, width: 0, height: 0 }

function normalizeTableNames(data: unknown): string[] {
  if (!Array.isArray(data)) return []
  return data.map(item => {
    if (typeof item === 'string') return item
    if (item && typeof item === 'object' && 'name' in item) {
      return String((item as TableInfo).name)
    }
    return ''
  }).filter(Boolean)
}

function pickPreferredTable(list: string[]) {
  const priorities = ['sales_order', 'sales_summary', 'customer', 'inventory', 'service_ticket', 'agile_project']
  return priorities.find(item => list.includes(item)) || list[0]
}

function quoteValue(value: string) {
  return `'${String(value).replace(/'/g, "''")}'`
}

function buildWhereClause(filters: DashboardComponent['filters']) {
  const conditions = (filters || [])
    .filter(filter => filter.field && filter.operator)
    .map(filter => {
      if (filter.operator === 'IS NULL' || filter.operator === 'IS NOT NULL') {
        return `${filter.field} ${filter.operator}`
      }
      if (filter.operator === 'LIKE') {
        return `${filter.field} LIKE ${quoteValue(`%${filter.value}%`)}`
      }
      if (filter.operator === 'IN') {
        return `${filter.field} IN (${filter.value})`
      }
      return `${filter.field} ${filter.operator} ${quoteValue(filter.value)}`
    })

  return conditions.length ? `WHERE ${conditions.join(' AND ')}` : ''
}

function buildComponentSql(component: DashboardComponent) {
  if (!component.table) return ''

  const dimensions = component.dimension || []
  const metrics = component.metrics || []
  const aggregation = component.aggregation || 'SUM'
  const selectParts: string[] = []

  const chartFamily = getChartFamily(component.chartType)
  if (chartFamily === 'table' || metrics.length === 0) {
    if (dimensions.length > 0) {
      selectParts.push(...dimensions)
    } else {
      selectParts.push('*')
    }
  } else {
    if (dimensions.length > 0) {
      selectParts.push(...dimensions)
    }
    metrics.forEach(metric => {
      selectParts.push(`${aggregation}(${metric}) AS ${metric}`)
    })
  }

  const clauses = [
    `SELECT ${selectParts.join(', ')}`,
    `FROM ${component.table}`,
    buildWhereClause(component.filters)
  ].filter(Boolean)

  if (dimensions.length > 0 && metrics.length > 0 && chartFamily !== 'table' && chartFamily !== 'filter') {
    clauses.push(`GROUP BY ${dimensions.join(', ')}`)
  }

  clauses.push('LIMIT 20')
  return clauses.join('\n')
}

function buildEmptyOption(component: DashboardComponent) {
  const chartFamily = getChartFamily(component.chartType)
  if (chartFamily === 'pie' || chartFamily === 'sunburst' || chartFamily === 'treemap') {
    return {
      title: { text: component.title, left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '58%', data: [] }]
    }
  }

  return {
    title: { text: component.title, left: 'center' },
    tooltip: { trigger: chartFamily === 'scatter' ? 'item' : 'axis' },
    legend: { data: component.metrics || [], bottom: 0 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value' },
    series: []
  }
}

function buildChartOptionFromRecords(component: DashboardComponent, records: Record<string, unknown>[]) {
  if (!records.length) {
    return buildEmptyOption(component)
  }

  const chartFamily = getChartFamily(component.chartType)
  const dimensions = component.dimension || []
  const metrics = component.metrics || []
  const firstRowKeys = Object.keys(records[0])
  const categoryField = dimensions[0] || firstRowKeys[0]
  const metricFields = metrics.length ? metrics : firstRowKeys.filter(key => key !== categoryField)

  if (chartFamily === 'pie' || chartFamily === 'sunburst' || chartFamily === 'treemap') {
    const metricField = metricFields[0] || firstRowKeys[1]
    return {
      title: { text: component.title, left: 'center' },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: chartFamily === 'treemap' ? 'treemap' : chartFamily === 'sunburst' ? 'sunburst' : 'pie',
        radius: ['38%', '68%'],
        data: records.map(row => ({
          name: String(row[categoryField] ?? '未知'),
          value: Number(row[metricField] ?? 0)
        }))
      }]
    }
  }

  if (chartFamily === 'scatter') {
    const xField = metricFields[0] || firstRowKeys[0]
    const yField = metricFields[1] || firstRowKeys[1] || xField
    return {
      title: { text: component.title, left: 'center' },
      tooltip: { trigger: 'item' },
      xAxis: { type: 'value', name: xField },
      yAxis: { type: 'value', name: yField },
      series: [{
        type: 'scatter',
        symbolSize: 14,
        data: records.map(row => [Number(row[xField] ?? 0), Number(row[yField] ?? 0), row[categoryField]])
      }]
    }
  }

  if (chartFamily === 'funnel') {
    const metricField = metricFields[0] || firstRowKeys[1]
    return {
      title: { text: component.title, left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'funnel',
        top: 40,
        bottom: 20,
        left: '10%',
        width: '80%',
        data: records.map(row => ({
          name: String(row[categoryField] ?? '未知'),
          value: Number(row[metricField] ?? 0)
        }))
      }]
    }
  }

  if (chartFamily === 'gauge') {
    const metricField = metricFields[0] || firstRowKeys[1]
    const avgValue = records.reduce((sum, row) => sum + Number(row[metricField] ?? 0), 0) / records.length
    return {
      title: { text: component.title, left: 'center' },
      series: [{
        type: 'gauge',
        progress: { show: true, width: 14 },
        axisLine: { lineStyle: { width: 14 } },
        detail: { valueAnimation: true, formatter: '{value}' },
        data: [{ value: Number(avgValue.toFixed(2)), name: metricField }]
      }]
    }
  }

  if (chartFamily === 'heatmap') {
    const metricField = metricFields[0] || firstRowKeys[1]
    return {
      title: { text: component.title, left: 'center' },
      tooltip: { position: 'top' },
      xAxis: { type: 'category', data: records.map(row => String(row[categoryField] ?? '')) },
      yAxis: { type: 'category', data: [metricField] },
      visualMap: { min: 0, max: 100, calculable: true, orient: 'horizontal', left: 'center', bottom: 0 },
      series: [{
        type: 'heatmap',
        data: records.map((row, index) => [index, 0, Number(row[metricField] ?? 0)])
      }]
    }
  }

  return {
    title: { text: component.title, left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: metricFields, bottom: 0 },
    xAxis: { type: 'category', data: records.map(row => String(row[categoryField] ?? '')) },
    yAxis: { type: 'value' },
    series: metricFields.map(field => ({
      type: chartFamily === 'line' || chartFamily === 'area' ? 'line' : 'bar',
      areaStyle: chartFamily === 'area' ? { opacity: 0.25 } : undefined,
      smooth: chartFamily === 'line' || chartFamily === 'area',
      name: field,
      data: records.map(row => Number(row[field] ?? 0))
    }))
  }
}

async function hydrateComponentData(component: DashboardComponent, showMessage = false) {
  const sql = buildComponentSql(component)
  const datasourceId = component.datasourceId || selectedDatasource.value
  if (!sql || !datasourceId) {
    component.chartOption = buildEmptyOption(component)
    return
  }

  const response = await request<{ records?: Record<string, unknown>[] }>('/query/execute', {
    method: 'POST',
    body: JSON.stringify({
      datasourceId,
      sql
    })
  })

  if (!response.success) {
    component.chartOption = buildEmptyOption(component)
    if (showMessage) {
      ElMessage.error(response.error || '图表预览数据加载失败')
    }
    return
  }

  component.datasourceId = datasourceId
  component.chartData = response.data?.records || []
  component.chartOption = buildChartOptionFromRecords(component, component.chartData)
}

async function loadDashboard() {
  const id = route.params.id as string
  if (!id || id === 'new') {
    return
  }

  dashboardId.value = Number(id)
  const response = await request<any>(`/dashboards/${id}`)
  if (!response.success || !response.data) {
    ElMessage.error(response.error || '加载仪表板失败')
    return
  }

  dashboardName.value = response.data.name || ''
  if (response.data.chartsConfig) {
    components.value = JSON.parse(response.data.chartsConfig).map((item: DashboardComponent) => ({
      aggregation: 'SUM',
      filters: [],
      ...item
    }))
  }
}

async function loadDatasources() {
  datasources.value = await adminService.getDataSources()
  if (!datasources.value.length) {
    ElMessage.warning('当前没有可用数据源，请先在管理后台配置')
    return
  }

  const defaultDatasourceId = components.value[0]?.datasourceId || datasources.value[0].id
  selectedDatasource.value = defaultDatasourceId
  await loadTables()
}

async function loadTables() {
  if (!selectedDatasource.value) return

  const response = await request<TableInfo[]>(`/datasources/${selectedDatasource.value}/tables`)
  if (!response.success) {
    ElMessage.error(response.error || '加载数据表失败')
    return
  }

  tables.value = normalizeTableNames(response.data)
  tableFields.value = tables.value.map(table => ({
    id: table,
    name: table,
    fields: []
  }))
  if (!tempComponent.value.table && tables.value.length > 0) {
    tempComponent.value.table = pickPreferredTable(tables.value)
  }
}

async function loadTempFields() {
  if (!selectedDatasource.value || !tempComponent.value.table) return
  const response = await request<Array<{ name: string }>>(`/datasources/${selectedDatasource.value}/columns?table=${encodeURIComponent(tempComponent.value.table)}`)
  if (!response.success) {
    ElMessage.error(response.error || '加载字段失败')
    return
  }
  availableFields.value = (response.data || []).map(col => col.name)
}

const onDragStart = (event: DragEvent, chart: ChartType) => {
  draggedChart = chart
  event.dataTransfer?.setData('chart-type', chart.type)
}

const onDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const onDrop = (event: DragEvent) => {
  event.preventDefault()
  if (!draggedChart || !canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left - 50
  const y = event.clientY - rect.top - 30

  const newComponent: DashboardComponent = {
    id: Date.now(),
    title: draggedChart.name,
    chartType: draggedChart.type,
    x: Math.max(0, x),
    y: Math.max(0, y),
    width: 300,
    height: 200,
    zIndex: components.value.length + 1,
    datasourceId: selectedDatasource.value,
    aggregation: 'SUM',
    filters: [],
    chartOption: buildEmptyOption({
      id: Date.now(),
      title: draggedChart.name,
      chartType: draggedChart.type,
      x: 0,
      y: 0,
      width: 300,
      height: 200,
      zIndex: 0
    } as DashboardComponent)
  }

  components.value.push(newComponent)
  selectComponent(newComponent)
  draggedChart = null
}

const selectComponent = (component: DashboardComponent) => {
  selectedComponent.value = component
  tempComponent.value = { ...component }
  if (component.datasourceId) {
    selectedDatasource.value = component.datasourceId
  }
}

const startMove = (event: MouseEvent, component: DashboardComponent) => {
  if (previewMode.value) return
  isMoving = true
  moveOffset.x = event.clientX - component.x
  moveOffset.y = event.clientY - component.y

  const onMouseMove = (e: MouseEvent) => {
    if (!isMoving) return
    component.x = Math.max(0, e.clientX - moveOffset.x)
    component.y = Math.max(0, e.clientY - moveOffset.y)
  }

  const onMouseUp = () => {
    isMoving = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const startResize = (event: MouseEvent, component: DashboardComponent) => {
  isResizing = true
  resizeStart.x = event.clientX
  resizeStart.y = event.clientY
  resizeStart.width = component.width
  resizeStart.height = component.height

  const onMouseMove = (e: MouseEvent) => {
    if (!isResizing) return
    const dx = e.clientX - resizeStart.x
    const dy = e.clientY - resizeStart.y
    component.width = Math.max(100, resizeStart.width + dx)
    component.height = Math.max(100, resizeStart.height + dy)
  }

  const onMouseUp = () => {
    isResizing = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const addComponent = () => {
  const newComponent: DashboardComponent = {
    id: Date.now(),
    title: '新组件',
    chartType: 'bar',
    x: 50 + components.value.length * 20,
    y: 50 + components.value.length * 20,
    width: 300,
    height: 200,
    zIndex: components.value.length + 1,
    datasourceId: selectedDatasource.value,
    aggregation: 'SUM',
    filters: [],
    chartOption: buildEmptyOption({
      id: Date.now(),
      title: '新组件',
      chartType: 'bar',
      x: 0,
      y: 0,
      width: 300,
      height: 200,
      zIndex: 0
    } as DashboardComponent)
  }
  components.value.push(newComponent)
  selectComponent(newComponent)
  configDialogVisible.value = true
}

const editComponent = (component: DashboardComponent) => {
  selectComponent(component)
  configDialogVisible.value = true
}

const removeComponent = (component: DashboardComponent) => {
  const index = components.value.findIndex(c => c.id === component.id)
  if (index > -1) {
    components.value.splice(index, 1)
    if (selectedComponent.value?.id === component.id) {
      selectedComponent.value = null
    }
  }
}

const clearCanvas = () => {
  components.value = []
  selectedComponent.value = null
}

const updateComponent = () => {
  if (!selectedComponent.value) return
  const index = components.value.findIndex(c => c.id === selectedComponent.value!.id)
  if (index > -1) {
    components.value[index] = { ...selectedComponent.value }
  }
}

const onChartTypeChange = () => {
  updateComponent()
  void updateChartOption()
}

const onTableChange = () => {
  void loadSelectedFields()
}

async function loadSelectedFields() {
  if (!selectedDatasource.value || !selectedComponent.value?.table) return
  const response = await request<Array<{ name: string }>>(`/datasources/${selectedDatasource.value}/columns?table=${encodeURIComponent(selectedComponent.value.table)}`)
  if (!response.success) {
    ElMessage.error(response.error || '加载字段失败')
    return
  }
  availableFields.value = (response.data || []).map(col => col.name)
}

const addFilter = () => {
  if (!selectedComponent.value) return
  if (!selectedComponent.value.filters) {
    selectedComponent.value.filters = []
  }
  selectedComponent.value.filters.push({ field: '', operator: '=', value: '' })
}

const removeFilter = (index: number) => {
  if (!selectedComponent.value?.filters) return
  selectedComponent.value.filters.splice(index, 1)
}

async function updateChartOption() {
  if (!selectedComponent.value) return
  await hydrateComponentData(selectedComponent.value)
  updateComponent()
}

const initChart = (el: Element | ComponentPublicInstance | null, component: DashboardComponent) => {
  if (!(el instanceof HTMLElement) || !component.chartOption) return
}

async function saveDashboard() {
  if (!dashboardName.value.trim()) {
    ElMessage.warning('请输入仪表板名称')
    nameDialogVisible.value = true
    return
  }

  saving.value = true
  const payload = {
    name: dashboardName.value.trim(),
    chartsConfig: JSON.stringify(components.value),
    status: dashboardId.value ? undefined : 0
  }

  const response = await request<any>(dashboardId.value ? `/dashboards/${dashboardId.value}` : '/dashboards', {
    method: dashboardId.value ? 'PUT' : 'POST',
    body: JSON.stringify(payload)
  })
  saving.value = false

  if (!response.success) {
    ElMessage.error(response.error || '保存失败')
    return
  }

  ElMessage.success('保存成功')
  if (!dashboardId.value && response.data?.id) {
    dashboardId.value = response.data.id
    router.replace(`/chatbi/dashboard/${dashboardId.value}/edit`)
  }
}

async function publishDashboard() {
  if (!dashboardId.value) {
    ElMessage.warning('请先保存仪表板')
    return
  }

  const response = await request<void>(`/dashboards/${dashboardId.value}/publish?status=1`, {
    method: 'PATCH'
  })
  if (!response.success) {
    ElMessage.error(response.error || '发布失败')
    return
  }

  ElMessage.success('发布成功')
}

const editName = () => {
  nameDialogVisible.value = true
}

const confirmEditName = () => {
  if (!dashboardName.value.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  dashboardName.value = dashboardName.value.trim()
  nameDialogVisible.value = false
}

const togglePreview = () => {
  previewMode.value = !previewMode.value
  selectedComponent.value = null
}

const goBack = () => {
  router.push('/chatbi/dashboard')
}

async function confirmConfig() {
  if (!selectedComponent.value) {
    configDialogVisible.value = false
    return
  }

  Object.assign(selectedComponent.value, tempComponent.value, {
    datasourceId: selectedDatasource.value || selectedComponent.value.datasourceId,
    aggregation: tempComponent.value.aggregation || selectedComponent.value.aggregation || 'SUM'
  })
  await hydrateComponentData(selectedComponent.value, true)
  updateComponent()
  configDialogVisible.value = false
}

onMounted(async () => {
  await loadDashboard()
  await loadDatasources()
})

onUnmounted(() => {
})
</script>

<style scoped lang="scss">
.dashboard-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;

  .editor-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .dashboard-title {
        font-size: 16px;
        font-weight: 600;
      }
    }

    .toolbar-right {
      display: flex;
      gap: 12px;
    }
  }

  .editor-container {
    display: flex;
    flex: 1;
    overflow: hidden;

    .component-panel {
      width: 240px;
      background: #fff;
      border-right: 1px solid #e4e7ed;
      overflow-y: auto;
      padding: 16px;

      .panel-section {
        margin-bottom: 24px;

        .section-title {
          margin: 0 0 12px;
          font-size: 14px;
          font-weight: 600;
          color: #303133;
        }
      }

      .chart-types {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 8px;

        .chart-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 12px 8px;
          background: #f5f7fa;
          border-radius: 4px;
          cursor: grab;
          transition: all 0.2s;

          &:hover {
            background: #ecf5ff;
            transform: translateY(-2px);
          }

          .chart-icon {
            margin-bottom: 8px;
            color: #409eff;
          }

          .chart-name {
            font-size: 12px;
            color: #606266;
          }
        }
      }

      .tree-node {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #606266;
      }
    }

    .canvas-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .canvas-toolbar {
        padding: 8px 16px;
        background: #fff;
        border-bottom: 1px solid #e4e7ed;
      }

      .canvas {
        flex: 1;
        position: relative;
        overflow: auto;
        background: #fff;
        margin: 16px;
        border-radius: 4px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
        min-height: 800px;
      }

      .canvas-component {
        position: absolute;
        border: 2px solid transparent;
        border-radius: 4px;
        background: #fff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        transition: box-shadow 0.2s, border-color 0.2s;

        &.selected {
          border-color: #409eff;
          box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
        }

        &.preview {
          cursor: default;
        }

        &:not(.preview):hover {
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
        }

        .component-content {
          width: 100%;
          height: 100%;
          display: flex;
          flex-direction: column;

          .component-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 8px 12px;
            background: #f5f7fa;
            border-radius: 4px 4px 0 0;
            cursor: move;

            .component-title {
              font-size: 13px;
              font-weight: 600;
              color: #303133;
            }

            .component-actions {
              display: flex;
              gap: 4px;
            }
          }

          .component-body {
            flex: 1;
            padding: 12px;
            overflow: hidden;

            .chart-placeholder {
              width: 100%;
              height: 100%;
            }

            .empty-chart {
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              height: 100%;
              color: #909399;

              p {
                margin: 8px 0 0;
                font-size: 12px;
              }
            }
          }
        }

        .resize-handle {
          position: absolute;
          right: 0;
          bottom: 0;
          width: 12px;
          height: 12px;
          cursor: se-resize;
          background: linear-gradient(135deg, transparent 50%, #409eff 50%);
          border-radius: 0 0 4px 0;
        }
      }
    }

    .property-panel {
      width: 280px;
      background: #fff;
      border-left: 1px solid #e4e7ed;
      overflow-y: auto;
      padding: 16px;

      .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        h4 {
          margin: 0;
          font-size: 14px;
          font-weight: 600;
          color: #303133;
        }
      }

      .filter-builder {
        width: 100%;

        .filter-item {
          display: flex;
          gap: 4px;
          margin-top: 8px;
          align-items: center;
        }
      }
    }
  }
}
</style>
  const chartFamily = getChartFamily(component.chartType)
