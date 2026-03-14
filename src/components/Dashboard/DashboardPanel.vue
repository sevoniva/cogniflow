<template>
  <div class="dashboard-container">
    <div class="dashboard-header" v-if="showHeader">
      <div class="dashboard-title">{{ title }}</div>
      <div class="dashboard-actions">
        <el-button :icon="Download" @click="handleExport" title="导出">导出</el-button>
        <el-button :icon="Refresh" @click="handleRefresh" title="刷新">刷新</el-button>
        <el-button :icon="Edit" @click="handleEdit" v-if="editable" title="编辑">编辑</el-button>
      </div>
    </div>
    <div class="dashboard-content">
      <grid-layout
        v-model:layout="layout"
        :col-num="12"
        :row-height="rowHeight"
        :is-draggable="editable"
        :is-resizable="editable"
        :vertical-compact="true"
        :use-css-transforms="true"
      >
        <grid-item
          v-for="item in layout"
          :key="item.i"
          :x="item.x"
          :y="item.y"
          :w="item.w"
          :h="item.h"
          :i="item.i"
          :min-w="item.minW || 2"
          :min-h="item.minH || 2"
          class="dashboard-item"
        >
          <div class="item-header">
            <span class="item-title">{{ item.title || '图表' }}</span>
            <div class="item-actions" v-if="editable">
              <el-button link type="danger" :icon="Delete" @click="handleRemove(item.i)" />
            </div>
          </div>
          <div class="item-content">
            <component
              :is="getChartComponent(item.chartType)"
              v-bind="item.chartProps"
              :loading="item.loading"
            />
          </div>
        </grid-item>
      </grid-layout>
      <el-empty v-if="layout.length === 0" description="暂无图表，请点击编辑添加" />
    </div>
    <!-- 添加图表对话框 -->
    <el-dialog v-model="dialogVisible" title="添加图表" width="600px">
      <el-form :model="chartForm" label-width="100px">
        <el-form-item label="图表类型">
          <el-select v-model="chartForm.chartType" placeholder="请选择">
            <el-option label="柱状图" value="bar" />
            <el-option label="折线图" value="line" />
            <el-option label="面积图" value="area" />
            <el-option label="饼图" value="pie" />
            <el-option label="散点图" value="scatter" />
            <el-option label="雷达图" value="radar" />
            <el-option label="仪表盘" value="gauge" />
            <el-option label="漏斗图" value="funnel" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="chartForm.title" placeholder="请输入图表标题" />
        </el-form-item>
        <el-form-item label="宽度">
          <el-slider v-model="chartForm.w" :min="2" :max="12" :step="1" />
        </el-form-item>
        <el-form-item label="高度">
          <el-slider v-model="chartForm.h" :min="2" :max="12" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddChart">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Download, Refresh, Edit, Delete } from '@element-plus/icons-vue';
import { GridLayout, GridItem } from 'vue-grid-layout';
import ChatBarChart from '../Chart/ChatBarChart.vue';
import ChatLineChart from '../Chart/ChatLineChart.vue';
import ChatAreaChart from '../Chart/ChatAreaChart.vue';
import ChatPieChart from '../Chart/ChatPieChart.vue';
import ChatScatterChart from '../Chart/ChatScatterChart.vue';
import ChatRadarChart from '../Chart/ChatRadarChart.vue';
import ChatGaugeChart from '../Chart/ChatGaugeChart.vue';
import ChatFunnelChart from '../Chart/ChatFunnelChart.vue';

interface DashboardItem {
  i: string;
  x: number;
  y: number;
  w: number;
  h: number;
  minW?: number;
  minH?: number;
  title?: string;
  chartType: string;
  chartProps?: any;
  loading?: boolean;
}

interface Props {
  title?: string;
  showHeader?: boolean;
  editable?: boolean;
  rowHeight?: number;
  data?: DashboardItem[];
}

const props = withDefaults(defineProps<Props>(), {
  title: '仪表板',
  showHeader: true,
  editable: false,
  rowHeight: 80,
  data: () => []
});

const emit = defineEmits<{
  (e: 'update', data: DashboardItem[]): void;
  (e: 'refresh'): void;
  (e: 'export'): void;
}>();

// 布局数据
const layout = ref<DashboardItem[]>([...(props.data || [])]);

// 对话框
const dialogVisible = ref(false);
const chartForm = ref({
  chartType: 'line',
  title: '',
  w: 4,
  h: 4
});

// 获取图表组件
const chartComponents: Record<string, any> = {
  bar: ChatBarChart,
  line: ChatLineChart,
  area: ChatAreaChart,
  pie: ChatPieChart,
  scatter: ChatScatterChart,
  radar: ChatRadarChart,
  gauge: ChatGaugeChart,
  funnel: ChatFunnelChart
};

const getChartComponent = (type: string) => {
  return chartComponents[type] || ChatLineChart;
};

// 事件处理
const handleExport = () => {
  emit('export');
};

const handleRefresh = () => {
  emit('refresh');
};

const handleEdit = () => {
  dialogVisible.value = true;
};

const handleRemove = (id: string) => {
  layout.value = layout.value.filter(item => item.i !== id);
  emit('update', layout.value);
};

const handleAddChart = () => {
  const id = `chart-${Date.now()}`;
  const newItem: DashboardItem = {
    i: id,
    x: 0,
    y: 0,
    w: chartForm.value.w,
    h: chartForm.value.h,
    title: chartForm.value.title || '新图表',
    chartType: chartForm.value.chartType,
    chartProps: {
      title: chartForm.value.title,
      xData: [],
      seriesData: []
    }
  };
  layout.value.push(newItem);
  emit('update', layout.value);
  dialogVisible.value = false;

  // 重置表单
  chartForm.value = {
    chartType: 'line',
    title: '',
    w: 4,
    h: 4
  };
};

// 监听布局变化
watch(layout, (newVal) => {
  emit('update', newVal);
}, { deep: true });

watch(() => props.data, (newVal) => {
  layout.value = [...(newVal || [])];
}, { deep: true });
</script>

<style scoped>
.dashboard-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.dashboard-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.dashboard-actions {
  display: flex;
  gap: 8px;
}

.dashboard-content {
  min-height: 400px;
}

.dashboard-item {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.item-content {
  padding: 12px;
  height: calc(100% - 45px);
}
</style>
