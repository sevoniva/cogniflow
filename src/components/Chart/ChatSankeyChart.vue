<template>
  <div class="chat-sankey-chart">
    <v-chart :option="chartOption" :autoresize="true" style="height: 100%; min-height: 400px;" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { SankeyChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  SankeyChart,
  TitleComponent,
  TooltipComponent
])

interface SankeyNode {
  name: string
}

interface SankeyLink {
  source: string
  target: string
  value: number
}

interface Props {
  nodes: SankeyNode[]
  links: SankeyLink[]
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '桑基图'
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
    triggerOn: 'mousemove',
    formatter: (params: any) => {
      if (params.dataType === 'edge') {
        return `${params.data.source} → ${params.data.target}<br/>流量: ${params.data.value}`
      }
      return `${params.name}<br/>总量: ${params.value}`
    }
  },
  series: [
    {
      type: 'sankey',
      layout: 'none',
      emphasis: {
        focus: 'adjacency'
      },
      data: props.nodes,
      links: props.links,
      lineStyle: {
        color: 'gradient',
        curveness: 0.5
      },
      label: {
        color: '#333',
        fontSize: 12
      },
      itemStyle: {
        borderWidth: 1,
        borderColor: '#aaa'
      }
    }
  ]
}))
</script>

<style scoped>
.chat-sankey-chart {
  width: 100%;
  height: 100%;
}
</style>
