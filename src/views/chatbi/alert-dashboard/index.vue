<template>
  <div class="alert-dashboard">
    <div class="header">
      <div>
        <h2>异常告警总览</h2>
        <p>实时汇总当前告警规则配置状态，支持直接新增、启停和维护规则。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建告警
      </el-button>
    </div>

    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #ecf5ff; color: #409eff">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">规则总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #fef0f0; color: #f56c6c">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.threshold }}</div>
            <div class="stat-label">阈值告警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.fluctuation }}</div>
            <div class="stat-label">波动告警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f0f9eb; color: #67c23a">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.active }}</div>
            <div class="stat-label">已启用</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <template #header>
        <div class="table-header">
          <span>规则列表</span>
          <el-input v-model="keyword" placeholder="搜索规则名称" clearable style="width: 260px" />
        </div>
      </template>

      <el-table :data="filteredList" style="width: 100%" v-loading="loading">
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="alertType" label="告警类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getAlertTypeTag(row.alertType)">{{ alertTypeText(row.alertType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值/波动值" width="120">
          <template #default="{ row }">
            <span>{{ row.alertType === 'FLUCTUATION' ? `${row.fluctuationRate || 0}%` : row.thresholdValue ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pushMethod" label="通知方式" width="120" />
        <el-table-column prop="receiver" label="接收人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editRule(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleRule(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="deleteRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑告警规则' : '新建告警规则'" width="620px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="form.alertType" style="width: 100%">
            <el-option label="阈值告警" value="THRESHOLD" />
            <el-option label="波动告警" value="FLUCTUATION" />
            <el-option label="异常检测" value="ANOMALY" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值类型" v-if="form.alertType === 'THRESHOLD'">
          <el-select v-model="form.thresholdType" style="width: 100%">
            <el-option label="大于" value="GT" />
            <el-option label="小于" value="LT" />
            <el-option label="大于等于" value="GTE" />
            <el-option label="小于等于" value="LTE" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" v-if="form.alertType === 'THRESHOLD'">
          <el-input-number v-model="form.thresholdValue" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="波动率" v-if="form.alertType === 'FLUCTUATION'">
          <el-input-number v-model="form.fluctuationRate" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="通知方式">
          <el-select v-model="form.pushMethod" style="width: 100%">
            <el-option label="邮件" value="EMAIL" />
            <el-option label="钉钉" value="DINGTALK" />
            <el-option label="企业微信" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人">
          <el-input v-model="form.receiver" placeholder="请输入邮箱或 webhook 地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, CircleCheck, DataAnalysis, Plus, Warning } from '@element-plus/icons-vue'
import { extractRecords, formatDateTime, request } from '@/utils/http'

interface AlertRule {
  id?: number
  ruleName: string
  alertType: string
  thresholdType?: string
  thresholdValue?: number
  fluctuationRate?: number
  pushMethod: string
  receiver: string
  status: number
  updatedAt?: string
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const keyword = ref('')
const sourceList = ref<AlertRule[]>([])

const form = reactive<AlertRule>({
  ruleName: '',
  alertType: 'THRESHOLD',
  thresholdType: 'GT',
  thresholdValue: 0,
  fluctuationRate: 10,
  pushMethod: 'EMAIL',
  receiver: '',
  status: 1
})

const filteredList = computed(() => {
  if (!keyword.value.trim()) {
    return sourceList.value
  }
  return sourceList.value.filter(item => item.ruleName.includes(keyword.value.trim()))
})

const stats = computed(() => ({
  total: sourceList.value.length,
  threshold: sourceList.value.filter(item => item.alertType === 'THRESHOLD').length,
  fluctuation: sourceList.value.filter(item => item.alertType === 'FLUCTUATION').length,
  active: sourceList.value.filter(item => item.status === 1).length
}))

function alertTypeText(type: string) {
  const map: Record<string, string> = {
    THRESHOLD: '阈值告警',
    FLUCTUATION: '波动告警',
    ANOMALY: '异常检测'
  }
  return map[type] || type
}

function getAlertTypeTag(type: string) {
  const map: Record<string, string> = {
    THRESHOLD: 'danger',
    FLUCTUATION: 'warning',
    ANOMALY: 'info'
  }
  return map[type] || ''
}

function resetForm() {
  editingId.value = null
  form.ruleName = ''
  form.alertType = 'THRESHOLD'
  form.thresholdType = 'GT'
  form.thresholdValue = 0
  form.fluctuationRate = 10
  form.pushMethod = 'EMAIL'
  form.receiver = ''
  form.status = 1
}

async function loadRules() {
  loading.value = true
  try {
    const response = await request<AlertRule[]>('/alert-rules')
    if (response.success) {
      sourceList.value = extractRecords<any>(response.data).map(item => ({
        ...item,
        updatedAt: formatDateTime(item.updatedAt || item.createdAt)
      }))
    } else {
      ElMessage.error(response.error || '加载告警规则失败')
    }
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function editRule(rule: AlertRule) {
  editingId.value = rule.id || null
  Object.assign(form, {
    ...rule,
    thresholdType: rule.thresholdType || 'GT',
    thresholdValue: rule.thresholdValue ?? 0,
    fluctuationRate: rule.fluctuationRate ?? 10
  })
  dialogVisible.value = true
}

async function saveRule() {
  if (!form.ruleName.trim()) {
    ElMessage.warning('请输入规则名称')
    return
  }
  if (!form.receiver.trim()) {
    ElMessage.warning('请输入接收人')
    return
  }

  saving.value = true
  try {
    const url = editingId.value ? `/alert-rules/${editingId.value}` : '/alert-rules'
    const method = editingId.value ? 'PUT' : 'POST'
    const response = await request(url, {
      method,
      body: JSON.stringify(form)
    })

    if (response.success) {
      ElMessage.success(editingId.value ? '规则已更新' : '规则已创建')
      dialogVisible.value = false
      resetForm()
      await loadRules()
    } else {
      ElMessage.error(response.error || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

async function toggleRule(rule: AlertRule) {
  const nextStatus = rule.status === 1 ? 0 : 1
  const response = await request(`/alert-rules/${rule.id}/status?status=${nextStatus}`, {
    method: 'PATCH'
  })

  if (response.success) {
    ElMessage.success(nextStatus === 1 ? '规则已启用' : '规则已停用')
    await loadRules()
  } else {
    ElMessage.error(response.error || '状态更新失败')
  }
}

async function deleteRule(rule: AlertRule) {
  try {
    await ElMessageBox.confirm(`确定删除规则「${rule.ruleName}」吗？`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }

  const response = await request(`/alert-rules/${rule.id}`, { method: 'DELETE' })
  if (response.success) {
    ElMessage.success('规则已删除')
    await loadRules()
  } else {
    ElMessage.error(response.error || '删除失败')
  }
}

onMounted(loadRules)
</script>

<style scoped lang="scss">
.alert-dashboard {
  padding: 20px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;

    h2 {
      margin: 0 0 8px;
      font-size: 24px;
      font-weight: 600;
    }

    p {
      margin: 0;
      color: #909399;
    }
  }

  .overview-cards {
    margin-bottom: 20px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.1;
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 4px;
  }

  .table-card {
    border-radius: 16px;
  }

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
