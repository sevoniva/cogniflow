<template>
  <div class="metric-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-primary-light); color: var(--cb-primary);">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">指标总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-success-light); color: var(--cb-success);">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ activeCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-warning-light); color: var(--cb-warning);">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ inactiveCount }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <Card class="toolbar-card" padding="md">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input 
            v-model="keyword" 
            placeholder="搜索指标名称或编码..." 
            clearable
            :prefix-icon="Search"
            style="width: 280px"
          />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="已启用" value="active" />
            <el-option label="已禁用" value="inactive" />
          </el-select>
        </div>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增指标
        </el-button>
      </div>
    </Card>

    <!-- 数据表格 -->
    <Card padding="none">
      <!-- 错误提示 -->
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        closable
        @close="error = ''"
        style="margin: 16px;"
      />
      
      <el-table 
        :data="filteredList" 
        border 
        stripe
        size="small" 
        v-loading="loading"
        :header-cell-style="{ background: '#fafbfc', fontWeight: 600 }"
      >
        <el-table-column prop="code" label="指标编码" width="140">
          <template #default="{ row }">
            <code class="metric-code">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="指标名称" min-width="150" />
        <el-table-column prop="definition" label="业务定义" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="active"
              inactive-value="inactive"
              @change="() => toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="120" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.updatedAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="danger" @click="remove(row.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <div v-if="!filteredList.length && !loading" class="table-empty">
        <EmptyState
          :type="keyword || statusFilter ? 'search' : 'data'"
          :title="keyword || statusFilter ? '未找到匹配结果' : '暂无指标'"
          :description="keyword || statusFilter ? '请调整筛选条件后重试' : '点击右上角「新增指标」创建第一个指标'"
        >
          <el-button v-if="keyword || statusFilter" @click="clearFilter">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
          <el-button v-else type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            新增指标
          </el-button>
        </EmptyState>
      </div>
    </Card>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="showAdd" 
      :title="isEdit ? '编辑指标' : '新增指标'" 
      width="560px" 
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-width="90px" class="metric-form">
        <el-form-item label="指标编码" required>
          <el-input 
            v-model="form.code" 
            placeholder="如：EXPENSE_DEPT"
            :disabled="isEdit"
            maxlength="50"
            show-word-limit
          />
          <div class="form-hint">唯一标识，创建后不可修改</div>
        </el-form-item>
        <el-form-item label="指标名称" required>
          <el-input 
            v-model="form.name" 
            placeholder="如：部门费用支出"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="业务定义">
          <el-input 
            v-model="form.definition" 
            type="textarea" 
            :rows="4" 
            placeholder="描述指标的业务含义和计算逻辑，帮助用户理解该指标"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  Search, 
  DataLine, 
  CircleCheck, 
  CircleClose,
  Edit,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import type { Metric, MetricRequest } from '@/types'

const keyword = ref('')
const statusFilter = ref('')
const list = ref<Metric[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showAdd = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<MetricRequest>({ code: '', name: '', definition: '' })

const activeCount = computed(() => list.value.filter(m => m.status === 'active').length)
const inactiveCount = computed(() => list.value.filter(m => m.status === 'inactive').length)

onMounted(() => {
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminService.getMetrics()
  } catch (e: any) {
    error.value = e?.message || '加载数据失败，请检查后端服务是否启动'
    console.error('加载指标列表失败:', e)
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => {
  let result = list.value
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    result = result.filter(i => 
      i.name.toLowerCase().includes(k) || 
      i.code.toLowerCase().includes(k)
    )
  }
  if (statusFilter.value) {
    result = result.filter(i => i.status === statusFilter.value)
  }
  return result
})

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  form.value = { code: '', name: '', definition: '' }
  showAdd.value = true
}

function edit(row: Metric) {
  isEdit.value = true
  editId.value = row.id
  form.value = { code: row.code, name: row.name, definition: row.definition }
  showAdd.value = true
}

function clearFilter() {
  keyword.value = ''
  statusFilter.value = ''
}

async function toggleStatus(row: Metric) {
  const res = await adminService.toggleMetricStatus(row.id)
  if (res.success) {
    await load()
    ElMessage.success(row.status === 'active' ? '已启用' : '已禁用')
  } else {
    ElMessage.error(res.error || '操作失败')
  }
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该指标？删除后将无法恢复', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await adminService.deleteMetric(id)
    if (res.success) {
      await load()
      ElMessage.success('已删除')
    } else {
      ElMessage.error(res.error || '删除失败')
    }
  } catch {
    // 取消
  }
}

async function save() {
  if (!form.value.code.trim()) {
    ElMessage.warning('请输入指标编码')
    return
  }
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入指标名称')
    return
  }
  
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      const res = await adminService.updateMetric(editId.value, { 
        name: form.value.name, 
        definition: form.value.definition 
      })
      if (res.success) {
        ElMessage.success('已更新')
        showAdd.value = false
        await load()
      } else {
        ElMessage.error(res.error || '更新失败')
      }
    } else {
      const res = await adminService.addMetric(form.value)
      if (res.success) {
        ElMessage.success('已创建')
        showAdd.value = false
        await load()
      } else {
        ElMessage.warning(res.error || '创建失败')
      }
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.metric-page {
  max-width: 1200px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: var(--cb-radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--cb-shadow-sm);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--cb-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--cb-text-secondary);
  margin-top: 4px;
}

/* 工具栏 */
.toolbar-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

/* 表格 */
.metric-code {
  background: var(--cb-bg-hover);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--cb-text-regular);
}

.time-text {
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.table-empty {
  padding: 40px 20px;
}

/* 表单 */
.metric-form {
  padding: 20px 10px 0;
}

.form-hint {
  font-size: 12px;
  color: var(--cb-text-secondary);
  margin-top: 6px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
  
  .stat-card {
    padding: 16px;
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
  
  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .toolbar-left {
    flex-direction: column;
  }
  
  .toolbar-left .el-input,
  .toolbar-left .el-select {
    width: 100% !important;
  }
}
</style>
