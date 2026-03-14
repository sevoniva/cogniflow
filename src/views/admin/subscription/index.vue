<template>
  <div class="subscription-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-primary-light); color: var(--cb-primary);">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">订阅总数</div>
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
            placeholder="搜索订阅标题..."
            clearable
            :prefix-icon="Search"
            style="width: 280px"
          />
          <el-select v-model="typeFilter" placeholder="订阅类型" clearable style="width: 140px">
            <el-option label="仪表盘" value="DASHBOARD" />
            <el-option label="报表" value="REPORT" />
            <el-option label="指标" value="METRIC" />
          </el-select>
        </div>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增订阅
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
        <el-table-column prop="title" label="订阅标题" min-width="180" />
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'DASHBOARD' ? 'primary' : row.type === 'REPORT' ? 'success' : 'info'" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subscriberName" label="订阅人" width="120" />
        <el-table-column prop="pushMethod" label="推送方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pushMethod === 'EMAIL' ? 'warning' : row.pushMethod === 'DINGTALK' ? 'danger' : 'success'" size="small">
              {{ pushMethodLabel(row.pushMethod) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="接收人" width="180" show-overflow-tooltip />
        <el-table-column prop="frequency" label="频率" width="100" align="center">
          <template #default="{ row }">
            <span>{{ frequencyLabel(row.frequency) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pushTime" label="推送时间" width="100" align="center" />
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
        <el-table-column label="操作" width="200" fixed="right" align="center">
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
          :type="keyword || typeFilter ? 'search' : 'data'"
          :title="keyword || typeFilter ? '未找到匹配结果' : '暂无订阅'"
          :description="keyword || typeFilter ? '请调整筛选条件后重试' : '点击右上角「新增订阅」创建第一个订阅'"
        >
          <el-button v-if="keyword || typeFilter" @click="clearFilter">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
          <el-button v-else type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            新增订阅
          </el-button>
        </EmptyState>
      </div>
    </Card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showAdd"
      :title="isEdit ? '编辑订阅' : '新增订阅'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px" class="subscription-form">
        <el-form-item label="订阅标题" required>
          <el-input
            v-model="form.title"
            placeholder="如：销售日报订阅"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="订阅类型" required>
          <el-select v-model="form.type" placeholder="请选择订阅类型" style="width: 100%">
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
          <div class="form-hint">要订阅的仪表盘/报表/指标的 ID</div>
        </el-form-item>
        <el-form-item label="推送方式" required>
          <el-select v-model="form.pushMethod" placeholder="请选择推送方式" style="width: 100%">
            <el-option label="邮件" value="EMAIL" />
            <el-option label="钉钉" value="DINGTALK" />
            <el-option label="企业微信" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人" required>
          <el-input
            v-model="form.receiver"
            :placeholder="receiverPlaceholder"
            maxlength="200"
          />
          <div class="form-hint">{{ receiverHint }}</div>
        </el-form-item>
        <el-form-item label="推送频率" required>
          <el-select v-model="form.frequency" placeholder="请选择推送频率" style="width: 100%">
            <el-option label="每天" value="DAILY" />
            <el-option label="每周" value="WEEKLY" />
            <el-option label="每月" value="MONTHLY" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="推送时间" required>
          <el-time-picker
            v-model="pushTimeValue"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择推送时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="推送日期">
          <el-input
            v-model="form.pushDay"
            :placeholder="pushDayPlaceholder"
            maxlength="50"
          />
          <div class="form-hint">{{ pushDayHint }}</div>
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Bell,
  CircleCheck,
  CircleClose,
  Edit,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import type { Subscription, SubscriptionRequest } from '@/types'

const keyword = ref('')
const typeFilter = ref('')
const list = ref<Subscription[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showAdd = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<SubscriptionRequest>({
  title: '',
  type: 'DASHBOARD',
  resourceId: 1,
  pushMethod: 'EMAIL',
  receiver: '',
  frequency: 'DAILY',
  pushTime: '09:00',
  pushDay: ''
})
const pushTimeValue = ref('09:00')

const activeCount = computed(() => list.value.filter(s => s.status === 1).length)
const inactiveCount = computed(() => list.value.filter(s => s.status === 0).length)

const typeLabel = (type: string) => {
  const map: Record<string, string> = { DASHBOARD: '仪表盘', REPORT: '报表', METRIC: '指标' }
  return map[type] || type
}

const pushMethodLabel = (method: string) => {
  const map: Record<string, string> = { EMAIL: '邮件', DINGTALK: '钉钉', WECHAT: '企业微信' }
  return map[method] || method
}

const frequencyLabel = (freq: string) => {
  const map: Record<string, string> = { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', CUSTOM: '自定义' }
  return map[freq] || freq
}

const receiverPlaceholder = computed(() => {
  const map: Record<string, string> = {
    EMAIL: '请输入邮箱地址',
    DINGTALK: '请输入钉钉 webhook 地址',
    WECHAT: '请输入企业微信 webhook 地址'
  }
  return map[form.value.pushMethod] || '请输入接收人'
})

const receiverHint = computed(() => {
  const map: Record<string, string> = {
    EMAIL: '例如：user@example.com',
    DINGTALK: '例如：https://oapi.dingtalk.com/robot/send?access_token=xxx',
    WECHAT: '例如：https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx'
  }
  return map[form.value.pushMethod] || ''
})

const pushDayPlaceholder = computed(() => {
  const map: Record<string, string> = {
    WEEKLY: '如：周一 或 星期一',
    MONTHLY: '如：1 号 或 每月 15 日',
    CUSTOM: '自定义cron 表达式或描述'
  }
  return map[form.value.frequency] || ''
})

const pushDayHint = computed(() => {
  if (form.value.frequency === 'DAILY') return '每日推送无需设置日期'
  if (form.value.frequency === 'WEEKLY') return '如：周一、周二等'
  if (form.value.frequency === 'MONTHLY') return '如：1 号、15 号等'
  return ''
})

watch(() => form.value.frequency, () => {
  if (form.value.frequency === 'DAILY') {
    form.value.pushDay = ''
  }
})

onMounted(() => {
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminService.getSubscriptions()
  } catch (e: any) {
    error.value = e?.message || '加载数据失败，请检查后端服务是否启动'
    console.error('加载订阅列表失败:', e)
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
  isEdit.value = false
  editId.value = null
  form.value = {
    title: '',
    type: 'DASHBOARD',
    resourceId: 1,
    pushMethod: 'EMAIL',
    receiver: '',
    frequency: 'DAILY',
    pushTime: '09:00',
    pushDay: ''
  }
  pushTimeValue.value = '09:00'
  showAdd.value = true
}

function edit(row: Subscription) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    title: row.title,
    type: row.type,
    resourceId: row.resourceId,
    pushMethod: row.pushMethod,
    receiver: row.receiver,
    frequency: row.frequency,
    pushTime: row.pushTime,
    pushDay: row.pushDay
  }
  pushTimeValue.value = row.pushTime
  showAdd.value = true
}

function clearFilter() {
  keyword.value = ''
  typeFilter.value = ''
}

async function toggleStatus(row: Subscription) {
  const res = await adminService.toggleSubscriptionStatus(row.id)
  if (res.success) {
    await load()
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } else {
    ElMessage.error(res.error || '操作失败')
  }
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该订阅？删除后将无法恢复', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await adminService.deleteSubscription(id)
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
    ElMessage.warning('请输入订阅标题')
    return
  }
  if (!form.value.receiver.trim()) {
    ElMessage.warning('请输入接收人')
    return
  }

  form.value.pushTime = pushTimeValue.value

  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      const res = await adminService.updateSubscription(editId.value, form.value)
      if (res.success) {
        ElMessage.success('已更新')
        showAdd.value = false
        await load()
      } else {
        ElMessage.error(res.error || '更新失败')
      }
    } else {
      const res = await adminService.addSubscription(form.value)
      if (res.success) {
        ElMessage.success('已创建')
        showAdd.value = false
        await load()
      } else {
        ElMessage.error(res.error || '创建失败')
      }
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.subscription-page {
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
.subscription-form {
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
