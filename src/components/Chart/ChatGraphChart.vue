<template>
  <div class="chat-graph-chart">
    <v-chart :option="chartOption" :autoresize="true" style="height: 100%; min-height: 500px;" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  GraphChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent
])

interface GraphNode {
  id: string
  name: string
  value?: number
  category?: number
  symbolSize?: number
}

interface GraphLink {
  source: string
  target: string
  value?: number
}

interface Props {
  nodes: GraphNode[]
  links: GraphLink[]
  categories?: Array<{ name: string }>
  title?: string
  layout?: 'force' | 'circular' | 'none'
}

const props = withDefaults(defineProps<Props>(), {
  title: '关系图',
  layout: 'force',
  categories: () => []
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
    formatter: (params: any) => {
      if (params.dataType === 'edge') {
        return `${params.data.source} → ${params.data.target}`
      }
      return `${params.name}<br/>连接数: ${params.value || 0}`
    }
  },
  legend: props.categories.length > 0 ? {
    data: props.categories.map(c => c.name),
    bottom: 10
  } : undefined,
  series: [
    {
      type: 'graph',
      layout: props.layout,
      data: props.nodes,
      links: props.links,
      categories: props.categories,
      roam: true,
      label: {
        show: true,
        position: 'right',
        formatter: '{b}'
      },
      labelLayout: {
        hideOverlap: true
      },
      scaleLimit: {
        min: 0.4,
        max: 2
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 10
        }
      },
      force: props.layout === 'force' ? {
        repulsion: 100,
        edgeLength: [50, 200]
      } : undefined
    }
  ]
}))
</script>

<style scoped>
.chat-graph-chart {
  width: 100%;
  height: 100%;
}
</style>
