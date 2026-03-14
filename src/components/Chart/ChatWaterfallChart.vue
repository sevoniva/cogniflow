<template>
  <div class="chat-waterfall-chart">
    <v-chart :option="chartOption" :autoresize="true" style="height: 100%; min-height: 400px;" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  BarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
])

interface WaterfallData {
  name: string
  value: number
  isTotal?: boolean
}

interface Props {
  data: WaterfallData[]
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '瀑布图'
})

const chartOption = computed(() => {
  // 计算累计值和辅助数据
  let cumulative = 0
  const assistData: (number | '-')[] = []
  const valueData: number[] = []

  props.data.forEach((item) => {
    if (item.isTotal) {
      assistData.push('-')
      valueData.push(cumulative + item.value)
      cumulative += item.value
    } else {
      assistData.push(cumulative)
      valueData.push(item.value)
      cumulative += item.value
    }
  })

  return {
    title: {
      text: props.title,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: '#1a2332'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const tar = params[1]
        return `${tar.name}<br/>${tar.seriesName}: ${tar.value}`
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      top: '15%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: props.data.map(d => d.name),
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666'
      }
    },
    series: [
      {
        name: '辅助',
        type: 'bar',
        stack: 'total',
        itemStyle: {
          borderColor: 'transparent',
          color: 'transparent'
        },
        emphasis: {
          itemStyle: {
            borderColor: 'transparent',
            color: 'transparent'
          }
        },
        data: assistData
      },
      {
        name: '数据',
        type: 'bar',
        stack: 'total',
        label: {
          show: true,
          position: 'inside',
          formatter: (params: any) => {
            return params.value > 0 ? `+${params.value}` : params.value
          }
        },
        data: valueData,
        itemStyle: {
          color: (params: any) => {
            return props.data[params.dataIndex].isTotal
              ? '#67C23A'
              : params.value > 0
                ? '#409EFF'
                : '#F56C6C'
          }
        }
      }
    ]
  }
})
</script>

<style scoped>
.chat-waterfall-chart {
  width: 100%;
  height: 100%;
}
</style>
