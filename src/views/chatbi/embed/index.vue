<template>
  <div class="embed-dashboard">
    <div v-if="loading" class="state-panel">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>正在加载嵌入仪表板...</p>
    </div>

    <div v-else-if="error" class="state-panel state-panel--error">
      <el-icon><WarningFilled /></el-icon>
      <p>{{ error }}</p>
    </div>

    <div v-else class="embed-shell">
      <header class="embed-header">
        <div>
          <div class="embed-header__eyebrow">Embedded Dashboard</div>
          <h1>{{ dashboardName }}</h1>
          <p>当前页渲染的是已发布仪表板配置，可直接用于分享和系统嵌入。</p>
        </div>
        <div class="embed-header__tags">
          <el-tag type="success" effect="plain">真实数据配置</el-tag>
          <el-tag type="info" effect="plain">ECharts 自适应</el-tag>
        </div>
      </header>

      <section class="dashboard-grid">
        <article
          v-for="chart in charts"
          :key="chart.id"
          class="chart-card"
          :style="{ minHeight: `${Math.max(chart.height, 300)}px` }"
        >
          <div class="chart-card__header">
            <strong>{{ chart.title }}</strong>
            <el-tag size="small" effect="plain">{{ chart.typeLabel }}</el-tag>
          </div>
          <div class="chart-card__body">
            <v-chart v-if="chart.option" :option="chart.option" autoresize class="chart-view" />
            <el-empty v-else description="该图表暂无可展示配置" :image-size="80" />
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { request } from '@/utils/http'

use([CanvasRenderer, BarChart, LineChart, PieChart, ScatterChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

interface EmbeddedChart {
  id: number
  title: string
  height: number
  option: Record<string, unknown> | null
  typeLabel: string
}

const route = useRoute()
const loading = ref(true)
const error = ref('')
const dashboardName = ref('')
const charts = ref<EmbeddedChart[]>([])

function humanizeType(chartType?: string) {
  const map: Record<string, string> = {
    bar: '柱状图',
    line: '折线图',
    pie: '饼图',
    scatter: '散点图',
    table: '表格视图',
    filter: '过滤器'
  }
  return map[chartType || ''] || '图表'
}

function buildFallbackOption(chart: any) {
  if (chart.chartOption) {
    return chart.chartOption
  }

  if (chart.chartType === 'pie') {
    return {
      title: { text: chart.title, left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '58%', data: [] }]
    }
  }

  return {
    title: { text: chart.title, left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value' },
    series: []
  }
}

async function loadDashboard() {
  const embedToken = String(route.params.token || '')
  const response = await request<any>(`/embedded/${embedToken}`)
  loading.value = false

  if (!response.success || !response.data) {
    error.value = response.error || '加载嵌入仪表板失败'
    return
  }

  dashboardName.value = response.data.dashboardName || response.data.title || '嵌入仪表板'

  if (!response.data.chartsConfig) {
    charts.value = []
    return
  }

  const config = JSON.parse(response.data.chartsConfig)
  charts.value = (Array.isArray(config) ? config : []).map((chart: any, index: number) => ({
    id: chart.id || index,
    title: chart.title || `图表 ${index + 1}`,
    height: Number(chart.height || 320),
    option: buildFallbackOption(chart),
    typeLabel: humanizeType(chart.chartType)
  }))
}

onMounted(loadDashboard)
</script>

<style scoped>
.embed-dashboard {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(47, 107, 255, 0.12), transparent 24%),
    linear-gradient(180deg, #f4f8fd 0%, #eef4fb 100%);
}

.state-panel {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--cb-text-secondary);
}

.state-panel .el-icon {
  font-size: 48px;
}

.state-panel--error {
  color: var(--cb-danger);
}

.embed-shell {
  max-width: 1360px;
  margin: 0 auto;
  padding: 32px 20px 40px;
}

.embed-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(129, 157, 219, 0.14);
  box-shadow: var(--cb-shadow-sm);
}

.embed-header__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--cb-primary);
}

.embed-header h1 {
  margin: 12px 0 10px;
  font-size: 32px;
  color: var(--cb-indigo);
}

.embed-header p {
  margin: 0;
  color: var(--cb-text-regular);
  line-height: 1.8;
}

.embed-header__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.chart-card {
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(129, 157, 219, 0.14);
  box-shadow: var(--cb-shadow-sm);
}

.chart-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--cb-border-lighter);
}

.chart-card__header strong {
  color: var(--cb-text-primary);
  font-size: 16px;
}

.chart-card__body {
  flex: 1;
  min-height: 280px;
  padding: 16px;
}

.chart-view {
  width: 100%;
  min-height: 280px;
}

@media (max-width: 720px) {
  .embed-header {
    flex-direction: column;
  }

  .embed-header h1 {
    font-size: 24px;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
