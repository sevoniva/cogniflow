<template>
  <div class="operation-dashboard">
    <el-page-header @back="$router.back()" content="运营分析" />

    <!-- 概览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.total_users) }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
              <el-icon :size="32"><UserFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.dau) }}</div>
              <div class="stat-label">今日活跃用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
              <el-icon :size="32"><Plus /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.month_new_users) }}</div>
              <div class="stat-label">本月新增用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
              <el-icon :size="32"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.total_events) }}</div>
              <div class="stat-label">总事件数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>用户活跃度趋势（近30天）</span>
          </template>
          <div ref="activityChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>渠道分析</span>
          </template>
          <div ref="channelChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>设备类型分布</span>
          </template>
          <div ref="deviceChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>事件类型分析</span>
          </template>
          <div ref="eventChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>地域分布 TOP20</span>
          </template>
          <div ref="regionChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>用户注册趋势</span>
          </template>
          <div ref="registrationChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { User, UserFilled, Plus, DataAnalysis } from '@element-plus/icons-vue'
import { request } from '@/utils/http'

const chartInstances: echarts.ECharts[] = []
onBeforeUnmount(() => {
  chartInstances.forEach(c => c.dispose())
  chartInstances.length = 0
})

const API_BASE = '/analytics/operation'

const overview = ref({
  total_users: 0,
  dau: 0,
  month_new_users: 0,
  total_events: 0
})

const activityChart = ref()
const channelChart = ref()
const deviceChart = ref()
const eventChart = ref()
const regionChart = ref()
const registrationChart = ref()

const formatNumber = (num: number) => {
  if (!num) return '0'
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

const getAnalytics = (path: string) => request<any>(`${API_BASE}${path}`)

const loadOverview = async () => {
  try {
    const data = await getAnalytics('/overview')
    if (data.success && data.data) {
      overview.value = data.data
    }
  } catch (error) {
    console.error('加载概览数据失败', error)
  }
}

const loadUserActivity = async () => {
  try {
    const data = await getAnalytics('/user-activity')
    if (data.success && data.data) {
      const chartData = data.data.reverse()
      const chart = echarts.init(activityChart.value); chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['日活用户', '事件数'] },
        xAxis: { type: 'category', data: chartData.map((item: any) => item.date) },
        yAxis: [
          { type: 'value', name: '日活用户' },
          { type: 'value', name: '事件数' }
        ],
        series: [
          {
            name: '日活用户',
            type: 'line',
            data: chartData.map((item: any) => item.dau),
            smooth: true,
            itemStyle: { color: '#409eff' }
          },
          {
            name: '事件数',
            type: 'line',
            yAxisIndex: 1,
            data: chartData.map((item: any) => item.event_count),
            smooth: true,
            itemStyle: { color: '#67c23a' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载用户活跃度失败', error)
  }
}

const loadChannelAnalysis = async () => {
  try {
    const data = await getAnalytics('/channel-analysis')
    if (data.success && data.data) {
      const chart = echarts.init(channelChart.value); chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['用户数', '事件数', '转化数'] },
        xAxis: { type: 'category', data: data.data.map((item: any) => item.channel) },
        yAxis: { type: 'value' },
        series: [
          {
            name: '用户数',
            type: 'bar',
            data: data.data.map((item: any) => item.user_count),
            itemStyle: { color: '#409eff' }
          },
          {
            name: '事件数',
            type: 'bar',
            data: data.data.map((item: any) => item.event_count),
            itemStyle: { color: '#67c23a' }
          },
          {
            name: '转化数',
            type: 'bar',
            data: data.data.map((item: any) => item.purchase_count),
            itemStyle: { color: '#e6a23c' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载渠道分析失败', error)
  }
}

const loadDeviceAnalysis = async () => {
  try {
    const data = await getAnalytics('/device-analysis')
    if (data.success && data.data) {
      const deviceData = data.data.reduce((acc: any, item: any) => {
        const existing = acc.find((d: any) => d.name === item.device_type)
        if (existing) {
          existing.value += item.user_count
        } else {
          acc.push({ name: item.device_type, value: item.user_count })
        }
        return acc
      }, [])

      const chart = echarts.init(deviceChart.value); chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            type: 'pie',
            radius: '60%',
            data: deviceData,
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
    console.error('加载设备分析失败', error)
  }
}

const loadEventAnalysis = async () => {
  try {
    const data = await getAnalytics('/event-analysis')
    if (data.success && data.data) {
      const chart = echarts.init(eventChart.value); chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            data: data.data.map((item: any) => ({
              name: item.event_type,
              value: item.event_count
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
    console.error('加载事件分析失败', error)
  }
}

const loadRegionAnalysis = async () => {
  try {
    const data = await getAnalytics('/region-analysis')
    if (data.success && data.data) {
      const chart = echarts.init(regionChart.value); chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: { type: 'value' },
        yAxis: {
          type: 'category',
          data: data.data.map((item: any) => `${item.province}-${item.city}`).reverse()
        },
        series: [
          {
            type: 'bar',
            data: data.data.map((item: any) => item.user_count).reverse(),
            itemStyle: { color: '#409eff' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载地域分析失败', error)
  }
}

const loadUserRegistration = async () => {
  try {
    const data = await getAnalytics('/user-registration')
    if (data.success && data.data) {
      // 按月份聚合数据
      const monthData = data.data.reduce((acc: any, item: any) => {
        const existing = acc.find((d: any) => d.month === item.month)
        if (existing) {
          existing.new_users += item.new_users
        } else {
          acc.push({ month: item.month, new_users: item.new_users })
        }
        return acc
      }, [])

      monthData.sort((a: any, b: any) => a.month.localeCompare(b.month))

      const chart = echarts.init(registrationChart.value); chartInstances.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: monthData.map((item: any) => item.month) },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            data: monthData.map((item: any) => item.new_users),
            smooth: true,
            areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
            itemStyle: { color: '#409eff' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载用户注册趋势失败', error)
  }
}

onMounted(() => {
  loadOverview()
  loadUserActivity()
  loadChannelAnalysis()
  loadDeviceAnalysis()
  loadEventAnalysis()
  loadRegionAnalysis()
  loadUserRegistration()
})
</script>

<style scoped>
.operation-dashboard {
  padding: 20px;
}

.overview-cards {
  margin: 20px 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.chart-row {
  margin-top: 20px;
}
</style>
