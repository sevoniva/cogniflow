<template>
  <div class="chat-heatmap-chart">
    <v-chart :option="chartOption" :autoresize="true" style="height: 100%; min-height: 400px;" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { HeatmapChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  VisualMapComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  VisualMapComponent
])

interface Props {
  data: Array<[number, number, number]>
  xAxisData?: string[]
  yAxisData?: string[]
  title?: string
  min?: number
  max?: number
}

const props = withDefaults(defineProps<Props>(), {
  xAxisData: () => ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  yAxisData: () => ['上午', '下午', '晚上'],
  title: '热力图',
  min: 0,
  max: 100
})

const chartOption = computed(() => ({
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
    position: 'top',
    formatter: (params: any) => {
      return `${props.yAxisData[params.data[1]]} - ${props.xAxisData[params.data[0]]}<br/>数值: ${params.data[2]}`
    }
  },
  grid: {
    left: '10%',
    right: '10%',
    top: '15%',
    bottom: '15%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: props.xAxisData,
    splitArea: {
      show: true
    },
    axisLabel: {
      color: '#666'
    }
  },
  yAxis: {
    type: 'category',
    data: props.yAxisData,
    splitArea: {
      show: true
    },
    axisLabel: {
      color: '#666'
    }
  },
  visualMap: {
    min: props.min,
    max: props.max,
    calculable: true,
    orient: 'horizontal',
    left: 'center',
    bottom: '0%',
    inRange: {
      color: ['#e0f3ff', '#409EFF', '#1a5fb4']
    }
  },
  series: [
    {
      name: '热力值',
      type: 'heatmap',
      data: props.data,
      label: {
        show: true,
        color: '#fff'
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))
</script>

<style scoped>
.chat-heatmap-chart {
  width: 100%;
  height: 100%;
}
</style>
