<template>
  <div class="sales-dashboard">
    <el-page-header @back="$router.back()" content="销售分析" />

    <!-- 概览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.total_sales) }}</div>
              <div class="stat-label">总销售额（元）</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
              <el-icon :size="32"><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.total_profit) }}</div>
              <div class="stat-label">总利润（元）</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.total_orders) }}</div>
              <div class="stat-label">订单总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(overview.total_customers) }}</div>
              <div class="stat-label">客户总数</div>
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
            <span>销售额趋势</span>
          </template>
          <div ref="trendChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>地区销售分布</span>
          </template>
          <div ref="regionChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>产品销售排行 TOP10</span>
          </template>
          <div ref="productChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>产品类别分析</span>
          </template>
          <div ref="categoryChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>销售人员业绩排行 TOP10</span>
          </template>
          <div ref="salespersonChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>订单状态分布</span>
          </template>
          <div ref="statusChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { TrendCharts, Money, Document, User } from '@element-plus/icons-vue'
import { request } from '@/utils/http'

const API_BASE = '/analytics/sales'

const overview = ref({
  total_sales: 0,
  total_profit: 0,
  total_orders: 0,
  total_customers: 0
})

const trendChart = ref()
const regionChart = ref()
const productChart = ref()
const categoryChart = ref()
const salespersonChart = ref()
const statusChart = ref()

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

const loadSalesTrend = async () => {
  try {
    const data = await getAnalytics('/trend')
    if (data.success && data.data) {
      const chartData = data.data.reverse()
      const chart = echarts.init(trendChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['销售额', '利润'] },
        xAxis: { type: 'category', data: chartData.map((item: any) => item.month) },
        yAxis: { type: 'value' },
        series: [
          {
            name: '销售额',
            type: 'line',
            data: chartData.map((item: any) => item.sales_amount),
            smooth: true,
            itemStyle: { color: '#409eff' }
          },
          {
            name: '利润',
            type: 'line',
            data: chartData.map((item: any) => item.profit_amount),
            smooth: true,
            itemStyle: { color: '#67c23a' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载销售趋势失败', error)
  }
}

const loadRegionDistribution = async () => {
  try {
    const data = await getAnalytics('/region-distribution')
    if (data.success && data.data) {
      const chart = echarts.init(regionChart.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            type: 'pie',
            radius: '60%',
            data: data.data.map((item: any) => ({
              name: item.region,
              value: item.total_sales
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
    console.error('加载地区分布失败', error)
  }
}

const loadProductRanking = async () => {
  try {
    const data = await getAnalytics('/product-ranking?limit=10')
    if (data.success && data.data) {
      const chart = echarts.init(productChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: { type: 'value' },
        yAxis: {
          type: 'category',
          data: data.data.map((item: any) => item.product_name).reverse()
        },
        series: [
          {
            type: 'bar',
            data: data.data.map((item: any) => item.total_sales).reverse(),
            itemStyle: { color: '#409eff' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载产品排行失败', error)
  }
}

const loadCategoryAnalysis = async () => {
  try {
    const data = await getAnalytics('/category-analysis')
    if (data.success && data.data) {
      const chart = echarts.init(categoryChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['销售额', '利润'] },
        xAxis: {
          type: 'category',
          data: data.data.map((item: any) => item.product_category)
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '销售额',
            type: 'bar',
            data: data.data.map((item: any) => item.total_sales),
            itemStyle: { color: '#409eff' }
          },
          {
            name: '利润',
            type: 'bar',
            data: data.data.map((item: any) => item.total_profit),
            itemStyle: { color: '#67c23a' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载类别分析失败', error)
  }
}

const loadSalespersonRanking = async () => {
  try {
    const data = await getAnalytics('/salesperson-ranking?limit=10')
    if (data.success && data.data) {
      const chart = echarts.init(salespersonChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: { type: 'value' },
        yAxis: {
          type: 'category',
          data: data.data.map((item: any) => item.sales_person_name).reverse()
        },
        series: [
          {
            type: 'bar',
            data: data.data.map((item: any) => item.total_sales).reverse(),
            itemStyle: { color: '#e6a23c' }
          }
        ]
      })
    }
  } catch (error) {
    console.error('加载销售人员排行失败', error)
  }
}

const loadOrderStatus = async () => {
  try {
    const data = await getAnalytics('/order-status')
    if (data.success && data.data) {
      const chart = echarts.init(statusChart.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            data: data.data.map((item: any) => ({
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
    console.error('加载订单状态失败', error)
  }
}

onMounted(() => {
  loadOverview()
  loadSalesTrend()
  loadRegionDistribution()
  loadProductRanking()
  loadCategoryAnalysis()
  loadSalespersonRanking()
  loadOrderStatus()
})
</script>

<style scoped>
.sales-dashboard {
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
