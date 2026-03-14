<template>
  <div class="chat-candlestick-chart">
    <v-chart :option="chartOption" :autoresize="true" style="height: 100%; min-height: 400px;" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { CandlestickChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent,
  LegendComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  CandlestickChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent,
  LegendComponent
])

interface Props {
  data: Array<[number, number, number, number]> // [开盘, 收盘, 最低, 最高]
  xAxisData: string[]
  title?: string
  showMA?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: 'K线图',
  showMA: true
})

// 计算移动平均线
const calculateMA = (dayCount: number) => {
  const result = []
  for (let i = 0; i < props.data.length; i++) {
    if (i < dayCount - 1) {
      result.push('-')
      continue
    }
    let sum = 0
    for (let j = 0; j < dayCount; j++) {
      sum += props.data[i - j][1] // 使用收盘价
    }
    result.push((sum / dayCount).toFixed(2))
  }
  return result
}

const chartOption = computed(() => {
  const series: any[] = [
    {
      name: 'K线',
      type: 'candlestick',
      data: props.data,
      itemStyle: {
        color: '#ef5350',
        color0: '#26a69a',
        borderColor: '#ef5350',
        borderColor0: '#26a69a'
      }
    }
  ]

  if (props.showMA) {
    series.push(
      {
        name: 'MA5',
        type: 'line',
        data: calculateMA(5),
        smooth: true,
        lineStyle: {
          width: 1
        },
        showSymbol: false
      },
      {
        name: 'MA10',
        type: 'line',
        data: calculateMA(10),
        smooth: true,
        lineStyle: {
          width: 1
        },
        showSymbol: false
      },
      {
        name: 'MA20',
        type: 'line',
        data: calculateMA(20),
        smooth: true,
        lineStyle: {
          width: 1
        },
        showSymbol: false
      }
    )
  }

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
    legend: {
      data: props.showMA ? ['K线', 'MA5', 'MA10', 'MA20'] : ['K线'],
      top: 30
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      formatter: (params: any) => {
        const data = params[0]
        if (!data) return ''
        const values = data.data
        return `${data.name}<br/>
          开盘: ${values[0]}<br/>
          收盘: ${values[1]}<br/>
          最低: ${values[2]}<br/>
          最高: ${values[3]}`
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      top: '20%',
      bottom: '15%'
    },
    xAxis: {
      type: 'category',
      data: props.xAxisData,
      scale: true,
      boundaryGap: false,
      axisLine: { onZero: false },
      splitLine: { show: false },
      min: 'dataMin',
      max: 'dataMax'
    },
    yAxis: {
      scale: true,
      splitArea: {
        show: true
      }
    },
    dataZoom: [
      {
        type: 'inside',
        start: 50,
        end: 100
      },
      {
        show: true,
        type: 'slider',
        top: '90%',
        start: 50,
        end: 100
      }
    ],
    series
  }
})
</script>

<style scoped>
.chat-candlestick-chart {
  width: 100%;
  height: 100%;
}
</style>
