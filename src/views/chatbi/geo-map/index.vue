<template>
  <div class="geo-map">
    <div class="map-header">
      <h2>地理地图</h2>
      <div class="header-actions">
        <el-select v-model="mapType" placeholder="选择地图类型" @change="onMapTypeChange" style="width: 150px">
          <el-option label="中国地图" value="china" />
          <el-option label="世界地图" value="world" />
          <el-option label="省份地图" value="province" />
        </el-select>
        <el-select v-model="chartType" placeholder="选择图表类型" @change="onChartTypeChange" style="width: 120px">
          <el-option label="区域填充" value="map" />
          <el-option label="散点图" value="scatter" />
          <el-option label="热力图" value="heatmap" />
          <el-option label="飞线图" value="lines" />
        </el-select>
        <el-button type="primary" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="map-container">
      <!-- 地图区域 -->
      <div ref="mapRef" class="map-wrapper" v-loading="loading">
        <v-chart
          v-if="option"
          :option="option"
          :autoresize="true"
          :manual-update="false"
          style="width: 100%; height: 100%"
        />
      </div>

      <!-- 侧边数据面板 -->
      <div class="data-panel">
        <el-card title="数据筛选" class="filter-card">
          <el-form label-width="80px" size="small">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="数据指标">
              <el-select v-model="selectedMetric" multiple style="width: 100%">
                <el-option label="销售额" value="sales" />
                <el-option label="用户数" value="users" />
                <el-option label="订单量" value="orders" />
                <el-option label="访问量" value="visits" />
              </el-select>
            </el-form-item>
            <el-form-item label="区域筛选">
              <el-select v-model="selectedRegions" multiple style="width: 100%">
                <el-option label="华北" value="north" />
                <el-option label="华东" value="east" />
                <el-option label="华南" value="south" />
                <el-option label="西南" value="west" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card title="数据排行" class="rank-card">
          <el-table :data="rankData" size="small" :show-header="false">
            <el-table-column prop="name" label="区域" />
            <el-table-column prop="value" label="数值" align="right">
              <template #default="{ row }">
                <el-tag :type="getRankTag(row.rank)">{{ row.value }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card title="图例说明" class="legend-card">
          <div class="legend-item" v-for="item in legendItems" :key="item.label">
            <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
            <span class="legend-label">{{ item.label }}</span>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="区域详情"
      width="500px"
    >
      <div v-if="selectedRegion" class="region-detail">
        <h3>{{ selectedRegion.name }}</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="销售额">{{ selectedRegion.sales }}</el-descriptions-item>
          <el-descriptions-item label="用户数">{{ selectedRegion.users }}</el-descriptions-item>
          <el-descriptions-item label="订单量">{{ selectedRegion.orders }}</el-descriptions-item>
          <el-descriptions-item label="访问量">{{ selectedRegion.visits }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewDetail">查看详情</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { MapChart, ScatterChart, LinesChart, HeatmapChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
// 注册 ECharts 组件
use([
  CanvasRenderer,
  MapChart,
  ScatterChart,
  LinesChart,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  LegendComponent,
  GridComponent
])

// 注册中国地图
// 实际使用时需要注册地图数据
// echarts.registerMap('china', chinaMapGeoJson)

const loading = ref(false)
const detailVisible = ref(false)
const selectedRegion = ref<any>(null)

const mapType = ref('china')
const chartType = ref('map')
const dateRange = ref<[Date, Date] | null>(null)
const selectedMetric = ref<string[]>(['sales'])
const selectedRegions = ref<string[]>([])

// 地图配置
const option = computed(() => {
  if (chartType.value === 'map') {
    return getMapOption()
  } else if (chartType.value === 'scatter') {
    return getScatterOption()
  } else if (chartType.value === 'lines') {
    return getLinesOption()
  } else if (chartType.value === 'heatmap') {
    return getHeatmapOption()
  }
  return {}
})

// 排行数据
const rankData = ref([
  { name: '广东', value: 123456, rank: 1 },
  { name: '江苏', value: 98765, rank: 2 },
  { name: '浙江', value: 87654, rank: 3 },
  { name: '山东', value: 76543, rank: 4 },
  { name: '四川', value: 65432, rank: 5 }
])

// 图例
const legendItems = ref([
  { label: '0-1000', color: '#e0f3f8' },
  { label: '1000-5000', color: '#abd9e9' },
  { label: '5000-10000', color: '#74add1' },
  { label: '10000-50000', color: '#4575b4' },
  { label: '50000+', color: '#313695' }
])

// 获取地图配置
function getMapOption() {
  return {
    title: {
      text: '全国销售分布图',
      left: 'center',
      top: 20
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>销售额：{c}万'
    },
    visualMap: {
      min: 0,
      max: 100000,
      left: 'left',
      top: 'bottom',
      text: ['高', '低'],
      calculable: true,
      inRange: {
        color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695']
      }
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.2,
      label: {
        show: true,
        color: '#333',
        fontSize: 10
      },
      itemStyle: {
        areaColor: '#f3f3f3',
        borderColor: '#666',
        borderWidth: 1
      },
      emphasis: {
        itemStyle: {
          areaColor: '#2a333d'
        },
        label: {
          color: '#fff'
        }
      }
    },
    series: [{
      type: 'map',
      map: 'china',
      geoIndex: 0,
      data: getMapData()
    }]
  }
}

// 获取散点图配置
function getScatterOption() {
  return {
    title: {
      text: '城市销售散点图',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}'
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.2,
      itemStyle: {
        areaColor: '#f3f3f3',
        borderColor: '#666'
      }
    },
    series: [{
      type: 'scatter',
      coordinateSystem: 'geo',
      data: getScatterData(),
      symbolSize: function (val: any) {
        return Math.min(val[2] / 1000, 50)
      },
      itemStyle: {
        color: '#ee6666'
      }
    }]
  }
}

// 获取飞线图配置
function getLinesOption() {
  return {
    title: {
      text: '物流路线飞线图',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.2,
      itemStyle: {
        areaColor: '#f3f3f3',
        borderColor: '#666'
      }
    },
    series: [
      {
        type: 'lines',
        coordinateSystem: 'geo',
        effect: {
          show: true,
          period: 6,
          trailLength: 0.7,
          color: '#fff',
          symbolSize: 3
        },
        lineStyle: {
          color: '#ee6666',
          width: 0,
          curveness: 0.2
        },
        data: getLinesData()
      }
    ]
  }
}

// 获取热力图配置
function getHeatmapOption() {
  return {
    title: {
      text: '用户密度热力图',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.2,
      itemStyle: {
        areaColor: '#f3f3f3',
        borderColor: '#666'
      }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'geo',
      data: getHeatmapData(),
      pointSize: 10,
      blurSize: 20
    }]
  }
}

// 获取地图数据
function getMapData() {
  return [
    { name: '广东', value: 98765 },
    { name: '江苏', value: 87654 },
    { name: '浙江', value: 76543 },
    { name: '山东', value: 65432 },
    { name: '四川', value: 54321 },
    { name: '湖北', value: 43210 },
    { name: '河南', value: 32109 },
    { name: '福建', value: 21098 },
    { name: '湖南', value: 10987 },
    { name: '安徽', value: 9876 }
  ]
}

// 获取散点数据
function getScatterData() {
  return [
    { name: '北京', value: [116.407526, 39.90403, 50000] },
    { name: '上海', value: [121.473701, 31.230416, 60000] },
    { name: '广州', value: [113.264385, 23.129112, 45000] },
    { name: '深圳', value: [114.057868, 22.543099, 55000] },
    { name: '成都', value: [104.066541, 30.572269, 35000] },
    { name: '杭州', value: [120.15507, 30.274084, 40000] },
    { name: '武汉', value: [114.305393, 30.593099, 30000] }
  ]
}

// 获取飞线数据
function getLinesData() {
  const geoCoordMap: Record<string, [number, number]> = {
    '北京': [116.407526, 39.90403],
    '上海': [121.473701, 31.230416],
    '广州': [113.264385, 23.129112],
    '深圳': [114.057868, 22.543099],
    '成都': [104.066541, 30.572269]
  }

  return [
    { fromName: '北京', toName: '上海', coords: [geoCoordMap['北京'], geoCoordMap['上海']] },
    { fromName: '北京', toName: '广州', coords: [geoCoordMap['北京'], geoCoordMap['广州']] },
    { fromName: '上海', toName: '深圳', coords: [geoCoordMap['上海'], geoCoordMap['深圳']] },
    { fromName: '广州', toName: '成都', coords: [geoCoordMap['广州'], geoCoordMap['成都']] },
    { fromName: '深圳', toName: '北京', coords: [geoCoordMap['深圳'], geoCoordMap['北京']] }
  ]
}

// 获取热力数据
function getHeatmapData() {
  return [
    { name: '北京', value: [116.407526, 39.90403, 80] },
    { name: '上海', value: [121.473701, 31.230416, 100] },
    { name: '广州', value: [113.264385, 23.129112, 70] },
    { name: '深圳', value: [114.057868, 22.543099, 90] },
    { name: '成都', value: [104.066541, 30.572269, 60] }
  ]
}

// 地图类型切换
const onMapTypeChange = async () => {
  loading.value = true
  // 实际使用时需要加载对应地图数据
  // if (mapType.value === 'world') {
  //   const worldMap = await import('echarts/map/world.json')
  //   echarts.registerMap('world', worldMap.default)
  // }
  setTimeout(() => {
    loading.value = false
  }, 500)
}

// 图表类型切换
const onChartTypeChange = () => {
  // 重新渲染
}

// 加载数据
const loadData = () => {
  loading.value = true
  ElMessage.success('数据刷新成功')
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

// 获取排行标签
const getRankTag = (rank: number) => {
  if (rank === 1) return ''
  if (rank === 2) return 'success'
  if (rank === 3) return 'warning'
  return 'info'
}

// 查看详情
const viewDetail = () => {
  ElMessage.info('跳转到详情页面')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.geo-map {
  padding: 20px;
  height: 100vh;
  display: flex;
  flex-direction: column;

  .map-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .map-container {
    flex: 1;
    display: grid;
    grid-template-columns: 1fr 350px;
    gap: 20px;
    min-height: 0;

    .map-wrapper {
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .data-panel {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .filter-card,
      .rank-card,
      .legend-card {
        background: #fff;
      }

      .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .legend-color {
          width: 20px;
          height: 20px;
          border-radius: 2px;
        }

        .legend-label {
          font-size: 12px;
          color: #666;
        }
      }
    }
  }

  .region-detail {
    h3 {
      margin: 0 0 16px;
      font-size: 18px;
      color: #333;
    }
  }
}
</style>
