<template>
  <div class="share-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-primary-light); color: var(--cb-primary);">
          <el-icon><ShareIcon /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">分享总数</div>
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
        <div class="stat-icon" style="background: var(--cb-info-light); color: var(--cb-info);">
          <el-icon><CopyDocument /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ copiedCount }}</div>
          <div class="stat-label">已复制</div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <Card class="toolbar-card" padding="md">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索分享标题..."
            clearable
            :prefix-icon="Search"
            style="width: 280px"
          />
          <el-select v-model="typeFilter" placeholder="分享类型" clearable style="width: 140px">
            <el-option label="仪表盘" value="DASHBOARD" />
            <el-option label="报表" value="REPORT" />
            <el-option label="指标" value="METRIC" />
          </el-select>
        </div>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增分享
        </el-button>
      </div>
    </Card>

    <!-- 数据表格 -->
    <Card padding="none">
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
        <el-table-column prop="title" label="分享标题" min-width="180" />
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'DASHBOARD' ? 'primary' : row.type === 'REPORT' ? 'success' : 'info'" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shareCode" label="分享码" width="180" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.shareCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.expireTime || '永不过期' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="() => toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyShareCode(row.shareCode)">
              <el-icon><CopyDocument /></el-icon>
              复制
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
          :type="keyword || typeFilter ? 'search' : 'data'"
          :title="keyword || typeFilter ? '未找到匹配结果' : '暂无分享'"
          :description="keyword || typeFilter ? '请调整筛选条件后重试' : '点击右上角「新增分享」创建第一个分享'"
        >
          <el-button v-if="keyword || typeFilter" @click="clearFilter">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
          <el-button v-else type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            新增分享
          </el-button>
        </EmptyState>
      </div>
    </Card>

    <!-- 新增对话框 -->
    <el-dialog
      v-model="showAdd"
      :title="'新增分享'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-width="90px" class="share-form">
        <el-form-item label="分享标题" required>
          <el-input
            v-model="form.title"
            placeholder="如：销售仪表盘分享"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分享类型" required>
          <el-select v-model="form.type" placeholder="请选择分享类型" style="width: 100%">
            <el-option label="仪表盘" value="DASHBOARD" />
            <el-option label="报表" value="REPORT" />
            <el-option label="指标" value="METRIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源 ID" required>
          <el-input-number
            v-model="form.resourceId"
            :min="1"
            placeholder="请输入资源 ID"
            style="width: 100%"
          />
          <div class="form-hint">要分享的仪表盘/报表/指标的 ID</div>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选择过期时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <div class="form-hint">不设置则永不过期</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">
          创建
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
  Share as ShareIcon,
  CircleCheck,
  CopyDocument,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import type { Share as ShareItem, ShareRequest } from '@/types'

const keyword = ref('')
const typeFilter = ref('')
const list = ref<ShareItem[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showAdd = ref(false)
const form = ref<ShareRequest>({
  title: '',
  type: 'DASHBOARD',
  resourceId: 1,
  status: 1
})

const activeCount = computed(() => list.value.filter(s => s.status === 1).length)
const copiedCount = computed(() => list.value.filter(s => s.status === 1).length)

const typeLabel = (type: string) => {
  const map: Record<string, string> = { DASHBOARD: '仪表盘', REPORT: '报表', METRIC: '指标' }
  return map[type] || type
}

onMounted(() => {
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminService.getShares()
  } catch (e: any) {
    error.value = e?.message || '加载数据失败，请检查后端服务是否启动'
    console.error('加载分享列表失败:', e)
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => {
  let result = list.value
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    result = result.filter(i => i.title.toLowerCase().includes(k))
  }
  if (typeFilter.value) {
    result = result.filter(i => i.type === typeFilter.value)
  }
  return result
})

function openAddDialog() {
  form.value = {
    title: '',
    type: 'DASHBOARD',
    resourceId: 1,
    status: 1
  }
  showAdd.value = true
}

function clearFilter() {
  keyword.value = ''
  typeFilter.value = ''
}

async function toggleStatus(row: ShareItem) {
  const res = await adminService.toggleShareStatus(row.id)
  if (res.success) {
    await load()
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } else {
    ElMessage.error(res.error || '操作失败')
  }
}

function copyShareCode(code: string) {
  navigator.clipboard.writeText(code)
  ElMessage.success('分享码已复制到剪贴板')
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该分享？删除后将无法恢复', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await adminService.deleteShare(id)
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
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入分享标题')
    return
  }

  saving.value = true
  try {
    const res = await adminService.addShare(form.value)
    if (res.success) {
      ElMessage.success('已创建')
      showAdd.value = false
      await load()
    } else {
      ElMessage.error(res.error || '创建失败')
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.share-page {
  max-width: 1400px;
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
.time-text {
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.table-empty {
  padding: 40px 20px;
}

/* 表单 */
.share-form {
  padding: 20px 10px 0;
}

.form-hint {
  font-size: 12px;
  color: var(--cb-text-secondary);
  margin-top: 6px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
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
