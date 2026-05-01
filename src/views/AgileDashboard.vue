<template>
  <div class="agile-dashboard">
    <el-row :gutter="20">
      <!-- 项目概览卡片 -->
      <el-col :span="6" v-for="item in overviewCards" :key="item.key">
        <el-card class="overview-card">
          <div class="card-content">
            <div class="card-icon" :style="{ backgroundColor: item.color }">
              <el-icon :size="32"><component :is="item.icon" /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-value">{{ item.value }}</div>
              <div class="card-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 迭代速率趋势 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>迭代速率趋势</span>
          </template>
          <div ref="velocityChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 缺陷统计 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>缺陷严重程度分布</span>
          </template>
          <div ref="defectChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 测试通过率 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>测试通过率趋势</span>
          </template>
          <div ref="testPassRateChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 代码提交统计 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>代码提交趋势</span>
          </template>
          <div ref="commitChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 部署成功率 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>部署成功率（按环境）</span>
          </template>
          <div ref="deploymentChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 质量指标 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>质量指标趋势</span>
          </template>
          <div ref="qualityChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 需求完成情况 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>需求状态分布</span>
          </template>
          <div ref="storyChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 团队效能 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>团队成员故事点统计</span>
          </template>
          <div ref="teamChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 需求类型分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>需求类型分布</span>
          </template>
          <div ref="storyTypeChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 缺陷状态分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>缺陷状态分布</span>
          </template>
          <div ref="defectStatusChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 工时利用率 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>工时利用率（Top 5）</span>
          </template>
          <div ref="utilizationChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 开发者提交排行 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>开发者提交排行（最近30天）</span>
          </template>
          <div ref="developerChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 技术债务趋势 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>技术债务比率趋势</span>
          </template>
          <div ref="techDebtChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 缺陷密度趋势 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>缺陷密度趋势</span>
          </template>
          <div ref="defectDensityChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 平均修复时间趋势 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>平均修复时间趋势（小时）</span>
          </template>
          <div ref="mttrChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { markRaw, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Folder, User, TrendCharts, Document } from '@element-plus/icons-vue'
import { request } from '@/utils/http'

const chartInstances: echarts.ECharts[] = []
onBeforeUnmount(() => {
  chartInstances.forEach(c => c.dispose())
  chartInstances.length = 0
})

const projectId = ref(1) // 默认项目ID

const overviewCards = ref([
  { key: 'totalProjects', label: '项目总数', value: 0, icon: markRaw(Folder), color: '#409EFF' },
  { key: 'activeProjects', label: '活跃项目', value: 0, icon: markRaw(TrendCharts), color: '#67C23A' },
  { key: 'totalMembers', label: '团队人数', value: 0, icon: markRaw(User), color: '#E6A23C' },
  { key: 'activeSprints', label: '进行中迭代', value: 0, icon: markRaw(Document), color: '#F56C6C' }
])

const velocityChart = ref<HTMLElement>()
const defectChart = ref<HTMLElement>()
const testPassRateChart = ref<HTMLElement>()
const commitChart = ref<HTMLElement>()
const deploymentChart = ref<HTMLElement>()
const qualityChart = ref<HTMLElement>()
const storyChart = ref<HTMLElement>()
const teamChart = ref<HTMLElement>()
const storyTypeChart = ref<HTMLElement>()
const defectStatusChart = ref<HTMLElement>()
const utilizationChart = ref<HTMLElement>()
const developerChart = ref<HTMLElement>()
const techDebtChart = ref<HTMLElement>()
const defectDensityChart = ref<HTMLElement>()
const mttrChart = ref<HTMLElement>()

const getAgile = (path: string, params: Record<string, string | number> = {}) => {
  const search = new URLSearchParams(
    Object.entries(params).reduce((acc, [key, value]) => {
      acc[key] = String(value)
      return acc
    }, {} as Record<string, string>)
  ).toString()

  return request<any>(search ? `${path}?${search}` : path)
}

// 加载项目概览
const loadProjectOverview = async () => {
  try {
    const data = await getAgile('/agile/project-overview')
    if (data.success) {
      overviewCards.value.forEach(card => {
        card.value = data.data[card.key] || 0
      })
    }
  } catch (error) {
    console.error('加载项目概览失败', error)
  }
}

// 渲染迭代速率图表
const renderVelocityChart = async () => {
  try {
    const data = await getAgile('/agile/sprint-velocity', { projectId: projectId.value })

    if (data.success && velocityChart.value) {
      const chart = echarts.init(velocityChart.value); chartInstances.push(chart)
      const chartData = data.data.reverse()

      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['计划故事点', '完成故事点', '速率'] },
        xAxis: {
          type: 'category',
          data: chartData.map((item: any) => item.sprint_name)
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '计划故事点',
            type: 'bar',
            data: chartData.map((item: any) => item.planned_story_points)
          },
          {
            name: '完成故事点',
            type: 'bar',
            data: chartData.map((item: any) => item.completed_story_points)
          },
          {
            name: '速率',
            type: 'line',
            data: chartData.map((item: any) => item.velocity)
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染迭代速率图表失败', error)
  }
}

// 渲染缺陷统计图表
const renderDefectChart = async () => {
  try {
    const data = await getAgile('/agile/defect-stats', { projectId: projectId.value })

    if (data.success && defectChart.value) {
      const chart = echarts.init(defectChart.value); chartInstances.push(chart)
      const severityData = data.data.bySeverity

      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            name: '缺陷数量',
            type: 'pie',
            radius: '50%',
            data: severityData.map((item: any) => ({
              name: item.severity,
              value: item.count
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染缺陷统计图表失败', error)
  }
}

// 渲染测试通过率图表
const renderTestPassRateChart = async () => {
  try {
    const data = await getAgile('/agile/test-pass-rate', { projectId: projectId.value })

    if (data.success && testPassRateChart.value) {
      const chart = echarts.init(testPassRateChart.value); chartInstances.push(chart)
      const chartData = data.data.reverse()

      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: chartData.map((item: any) => item.date)
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [
          {
            name: '通过率',
            type: 'line',
            data: chartData.map((item: any) => item.pass_rate.toFixed(2)),
            smooth: true,
            areaStyle: {}
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染测试通过率图表失败', error)
  }
}

// 渲染代码提交图表
const renderCommitChart = async () => {
  try {
    const data = await getAgile('/agile/commit-stats', { projectId: projectId.value })

    if (data.success && commitChart.value) {
      const chart = echarts.init(commitChart.value); chartInstances.push(chart)
      const trendData = data.data.trend

      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['提交次数', '新增行数', '删除行数'] },
        xAxis: {
          type: 'category',
          data: trendData.map((item: any) => item.date)
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '提交次数',
            type: 'bar',
            data: trendData.map((item: any) => item.count)
          },
          {
            name: '新增行数',
            type: 'line',
            data: trendData.map((item: any) => item.added)
          },
          {
            name: '删除行数',
            type: 'line',
            data: trendData.map((item: any) => item.deleted)
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染代码提交图表失败', error)
  }
}

// 渲染部署成功率图表
const renderDeploymentChart = async () => {
  try {
    const data = await getAgile('/agile/deployment-success-rate', { projectId: projectId.value })

    if (data.success && deploymentChart.value) {
      const chart = echarts.init(deploymentChart.value); chartInstances.push(chart)
      const envData = data.data.byEnvironment

      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: {
          type: 'category',
          data: envData.map((item: any) => item.environment)
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [
          {
            name: '成功率',
            type: 'bar',
            data: envData.map((item: any) => item.success_rate.toFixed(2)),
            itemStyle: {
              color: (params: any) => {
                const rate = params.value
                if (rate >= 90) return '#67C23A'
                if (rate >= 70) return '#E6A23C'
                return '#F56C6C'
              }
            }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染部署成功率图表失败', error)
  }
}

// 渲染质量指标图表
const renderQualityChart = async () => {
  try {
    const data = await getAgile('/agile/quality-metrics', { projectId: projectId.value })

    if (data.success && qualityChart.value) {
      const chart = echarts.init(qualityChart.value); chartInstances.push(chart)
      const chartData = data.data.reverse()

      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['代码覆盖率', '单元测试通过率', '集成测试通过率'] },
        xAxis: {
          type: 'category',
          data: chartData.map((item: any) => item.metric_date)
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [
          {
            name: '代码覆盖率',
            type: 'line',
            data: chartData.map((item: any) => item.code_coverage)
          },
          {
            name: '单元测试通过率',
            type: 'line',
            data: chartData.map((item: any) => item.unit_test_pass_rate)
          },
          {
            name: '集成测试通过率',
            type: 'line',
            data: chartData.map((item: any) => item.integration_test_pass_rate)
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染质量指标图表失败', error)
  }
}

// 渲染需求完成情况图表
const renderStoryChart = async () => {
  try {
    const data = await getAgile('/agile/story-completion', { projectId: projectId.value })

    if (data.success && storyChart.value) {
      const chart = echarts.init(storyChart.value); chartInstances.push(chart)
      const statusData = data.data.byStatus

      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            name: '需求数量',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            data: statusData.map((item: any) => ({
              name: item.status,
              value: item.count
            }))
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染需求完成情况图表失败', error)
  }
}

// 渲染团队效能图表
const renderTeamChart = async () => {
  try {
    const data = await getAgile('/agile/team-efficiency', { projectId: projectId.value })

    if (data.success && teamChart.value) {
      const chart = echarts.init(teamChart.value); chartInstances.push(chart)
      const storyPointsData = data.data.storyPointsByMember

      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: {
          type: 'category',
          data: storyPointsData.map((item: any) => item.real_name)
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '故事点',
            type: 'bar',
            data: storyPointsData.map((item: any) => item.total_points),
            itemStyle: { color: '#409EFF' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染团队效能图表失败', error)
  }
}

// 渲染需求类型分布图表
const renderStoryTypeChart = async () => {
  try {
    const data = await getAgile('/agile/story-completion', { projectId: projectId.value })

    if (data.success && storyTypeChart.value) {
      const chart = echarts.init(storyTypeChart.value); chartInstances.push(chart)
      const typeData = data.data.byType

      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: 10, top: 'center' },
        series: [
          {
            name: '需求类型',
            type: 'pie',
            radius: '60%',
            data: typeData.map((item: any) => ({
              name: item.story_type,
              value: item.count
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染需求类型分布图表失败', error)
  }
}

// 渲染缺陷状态分布图表
const renderDefectStatusChart = async () => {
  try {
    const data = await getAgile('/agile/defect-stats', { projectId: projectId.value })

    if (data.success && defectStatusChart.value) {
      const chart = echarts.init(defectStatusChart.value); chartInstances.push(chart)
      const statusData = data.data.byStatus

      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: 10, top: 'center' },
        series: [
          {
            name: '缺陷状态',
            type: 'pie',
            radius: '60%',
            data: statusData.map((item: any) => ({
              name: item.status,
              value: item.count
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染缺陷状态分布图表失败', error)
  }
}

// 渲染工时利用率图表
const renderUtilizationChart = async () => {
  try {
    const data = await getAgile('/agile/team-efficiency', { projectId: projectId.value })

    if (data.success && utilizationChart.value) {
      const chart = echarts.init(utilizationChart.value); chartInstances.push(chart)
      const utilizationData = data.data.utilizationByMember.slice(0, 5)

      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: {
          type: 'value',
          min: 0,
          max: 150,
          axisLabel: { formatter: '{value}%' }
        },
        yAxis: {
          type: 'category',
          data: utilizationData.map((item: any) => item.real_name)
        },
        series: [
          {
            name: '利用率',
            type: 'bar',
            data: utilizationData.map((item: any) => item.utilization?.toFixed(2) || 0),
            itemStyle: {
              color: (params: any) => {
                const rate = params.value
                if (rate >= 80 && rate <= 110) return '#67C23A'
                if (rate > 110) return '#E6A23C'
                return '#F56C6C'
              }
            }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染工时利用率图表失败', error)
  }
}

// 渲染开发者提交排行图表
const renderDeveloperChart = async () => {
  try {
    const data = await getAgile('/agile/commit-stats', { projectId: projectId.value })

    if (data.success && developerChart.value) {
      const chart = echarts.init(developerChart.value); chartInstances.push(chart)
      const authorData = data.data.byAuthor

      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['提交次数', '新增行数'] },
        xAxis: {
          type: 'category',
          data: authorData.map((item: any) => item.real_name),
          axisLabel: { rotate: 45 }
        },
        yAxis: [
          { type: 'value', name: '提交次数' },
          { type: 'value', name: '代码行数' }
        ],
        series: [
          {
            name: '提交次数',
            type: 'bar',
            data: authorData.map((item: any) => item.commits),
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '新增行数',
            type: 'line',
            yAxisIndex: 1,
            data: authorData.map((item: any) => item.added),
            itemStyle: { color: '#67C23A' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染开发者提交排行图表失败', error)
  }
}

// 渲染技术债务趋势图表
const renderTechDebtChart = async () => {
  try {
    const data = await getAgile('/agile/quality-metrics', { projectId: projectId.value })

    if (data.success && techDebtChart.value) {
      const chart = echarts.init(techDebtChart.value); chartInstances.push(chart)
      const chartData = data.data.reverse()

      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: chartData.map((item: any) => item.metric_date)
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '{value}%' }
        },
        series: [
          {
            name: '技术债务比率',
            type: 'line',
            data: chartData.map((item: any) => item.technical_debt_ratio),
            smooth: true,
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(244, 67, 54, 0.3)' },
                  { offset: 1, color: 'rgba(244, 67, 54, 0.1)' }
                ]
              }
            },
            itemStyle: { color: '#F56C6C' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染技术债务趋势图表失败', error)
  }
}

// 渲染缺陷密度趋势图表
const renderDefectDensityChart = async () => {
  try {
    const data = await getAgile('/agile/quality-metrics', { projectId: projectId.value })

    if (data.success && defectDensityChart.value) {
      const chart = echarts.init(defectDensityChart.value); chartInstances.push(chart)
      const chartData = data.data.reverse()

      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: chartData.map((item: any) => item.metric_date)
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '{value}' }
        },
        series: [
          {
            name: '缺陷密度',
            type: 'line',
            data: chartData.map((item: any) => item.defect_density),
            smooth: true,
            areaStyle: {},
            itemStyle: { color: '#E6A23C' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染缺陷密度趋势图表失败', error)
  }
}

// 渲染平均修复时间趋势图表
const renderMttrChart = async () => {
  try {
    const data = await getAgile('/agile/quality-metrics', { projectId: projectId.value })

    if (data.success && mttrChart.value) {
      const chart = echarts.init(mttrChart.value); chartInstances.push(chart)
      const chartData = data.data.reverse()

      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: chartData.map((item: any) => item.metric_date)
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '{value}h' }
        },
        series: [
          {
            name: '平均修复时间',
            type: 'line',
            data: chartData.map((item: any) => item.mean_time_to_repair),
            smooth: true,
            areaStyle: {},
            itemStyle: { color: '#909399' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('渲染平均修复时间趋势图表失败', error)
  }
}

onMounted(async () => {
  await loadProjectOverview()
  await nextTick()

  renderVelocityChart()
  renderDefectChart()
  renderTestPassRateChart()
  renderCommitChart()
  renderDeploymentChart()
  renderQualityChart()
  renderStoryChart()
  renderTeamChart()
  renderStoryTypeChart()
  renderDefectStatusChart()
  renderUtilizationChart()
  renderDeveloperChart()
  renderTechDebtChart()
  renderDefectDensityChart()
  renderMttrChart()
})
</script>

<style scoped>
.agile-dashboard {
  padding: 20px;
}

.overview-card {
  margin-bottom: 20px;
}

.card-content {
  display: flex;
  align-items: center;
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 16px;
}

.card-info {
  flex: 1;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.card-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
</style>
