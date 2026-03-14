<template>
  <div class="chat-boxplot-chart">
    <v-chart :option="chartOption" :autoresize="true" style="height: 100%; min-height: 400px;" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BoxplotChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  BoxplotChart,
  TitleComponent,
  TooltipComponent,
  GridComponent
])

interface Props {
  data: Array<[number, number, number, number, number]> // [最小值, Q1, 中位数, Q3, 最大值]
  xAxisData: string[]
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '箱线图'
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
    trigger: 'item',
    axisPointer: {
      type: 'shadow'
    },
    formatter: (params: any) => {
      const data = params.data
      return `${params.name}<br/>
        最大值: ${data[5]}<br/>
        Q3: ${data[4]}<br/>
        中位数: ${data[3]}<br/>
        Q1: ${data[2]}<br/>
        最小值: ${data[1]}`
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
    boundaryGap: true,
    nameGap: 30,
    splitArea: {
      show: false
    },
    axisLabel: {
      color: '#666'
    },
    splitLine: {
      show: false
    }
  },
  yAxis: {
    type: 'value',
    splitArea: {
      show: true
    },
    axisLabel: {
      color: '#666'
    }
  },
  series: [
    {
      name: 'boxplot',
      type: 'boxplot',
      data: props.data,
      itemStyle: {
        color: '#409EFF',
        borderColor: '#1a5fb4'
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))
</script>

<style scoped>
.chat-boxplot-chart {
  width: 100%;
  height: 100%;
}
</style>
