<template>
  <div class="alert-rule">
    <div class="header">
      <h2>告警规则管理</h2>
      <el-button type="primary" @click="handleCreate">新建告警规则</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.ruleName" placeholder="请输入规则名称" clearable />
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="searchForm.alertType" placeholder="请选择告警类型" clearable>
            <el-option label="阈值告警" value="THRESHOLD" />
            <el-option label="波动告警" value="FLUCTUATION" />
            <el-option label="异常检测" value="ANOMALY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 告警规则列表 -->
    <el-card class="table-card">
      <el-table :data="alertList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="alertType" label="告警类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getAlertTypeTag(row.alertType)">
              {{ alertTypeMap[row.alertType] || row.alertType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thresholdType" label="阈值类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" v-if="row.thresholdType">
              {{ row.thresholdType === 'GT' ? '大于' : row.thresholdType === 'LT' ? '小于' : row.thresholdType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值" width="100" />
        <el-table-column prop="pushMethod" label="通知方式" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.pushMethod === 'EMAIL' ? 'warning' : 'success'">
              {{ pushMethodMap[row.pushMethod] || row.pushMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="接收人" min-width="150" show-overflow-tooltip />
        <el-table-column prop="lastAlertTime" label="最后告警时间" width="180" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="warning"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="applyFilters"
        @size-change="applyFilters"
        class="pagination"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="650px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="告警类型" prop="alertType">
          <el-select v-model="formData.alertType" placeholder="请选择告警类型" @change="handleAlertTypeChange">
            <el-option label="阈值告警" value="THRESHOLD" />
            <el-option label="波动告警" value="FLUCTUATION" />
            <el-option label="异常检测" value="ANOMALY" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值类型" prop="thresholdType" v-if="formData.alertType === 'THRESHOLD'">
          <el-select v-model="formData.thresholdType" placeholder="请选择阈值类型">
            <el-option label="大于" value="GT" />
            <el-option label="大于等于" value="GTE" />
            <el-option label="小于" value="LT" />
            <el-option label="小于等于" value="LTE" />
            <el-option label="等于" value="EQ" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue" v-if="formData.alertType === 'THRESHOLD'">
          <el-input-number v-model="formData.thresholdValue" :precision="2" placeholder="请输入阈值" style="width: 100%" />
        </el-form-item>
        <el-form-item label="波动率%" prop="fluctuationRate" v-if="formData.alertType === 'FLUCTUATION'">
          <el-input-number v-model="formData.fluctuationRate" :precision="2" :step="0.1" placeholder="请输入波动率" style="width: 100%" />
        </el-form-item>
        <el-form-item label="通知方式" prop="pushMethod">
          <el-select v-model="formData.pushMethod" placeholder="请选择通知方式">
            <el-option label="邮件" value="EMAIL" />
            <el-option label="短信" value="SMS" />
            <el-option label="钉钉" value="DINGTALK" />
            <el-option label="企业微信" value="WECHAT" />
            <el-option label="飞书" value="FEISHU" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人" prop="receiver">
          <el-input
            v-model="formData.receiver"
            type="textarea"
            :rows="3"
            placeholder="请输入接收人，多个请用逗号分隔"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { extractRecords, request } from '@/utils/http'

interface AlertRule {
  id?: number
  ruleName: string
  metricId?: number
  datasourceId?: number
  alertType: string
  thresholdType?: string
  thresholdValue?: number
  fluctuationRate?: number
  pushMethod: string
  receiver: string
  status: number
  lastAlertTime?: string
  createdAt?: string
  updatedAt?: string
}

const alertTypeMap: Record<string, string> = {
  THRESHOLD: '阈值告警',
  FLUCTUATION: '波动告警',
  ANOMALY: '异常检测'
}

const pushMethodMap: Record<string, string> = {
  EMAIL: '邮件',
  SMS: '短信',
  DINGTALK: '钉钉',
  WECHAT: '企业微信',
  FEISHU: '飞书'
}

const getAlertTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    THRESHOLD: 'warning',
    FLUCTUATION: 'info',
    ANOMALY: 'danger'
  }
  return tagMap[type] || ''
}

const searchForm = reactive({
  ruleName: '',
  alertType: '',
  status: null as number | null
})

const sourceList = ref<AlertRule[]>([])
const alertList = ref<AlertRule[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<AlertRule>({
  ruleName: '',
  alertType: 'THRESHOLD',
  thresholdType: 'GT',
  thresholdValue: 0,
  fluctuationRate: 10,
  pushMethod: 'EMAIL',
  receiver: '',
  status: 1
})

const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  alertType: [{ required: true, message: '请选择告警类型', trigger: 'change' }],
  thresholdType: [{ required: true, message: '请选择阈值类型', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  fluctuationRate: [{ required: true, message: '请输入波动率', trigger: 'blur' }],
  pushMethod: [{ required: true, message: '请选择通知方式', trigger: 'change' }],
  receiver: [{ required: true, message: '请输入接收人', trigger: 'blur' }]
}

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 加载告警规则列表
const loadAlertRules = async () => {
  loading.value = true
  try {
    const data = await request<AlertRule[]>('/alert-rules')
    if (data.success) {
      sourceList.value = extractRecords<AlertRule>(data.data)
      applyFilters()
    }
  } catch (error) {
    console.error('加载告警规则失败:', error)
    ElMessage.error('加载告警规则失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  let result = [...sourceList.value]

  if (searchForm.ruleName) {
    result = result.filter(item => item.ruleName?.includes(searchForm.ruleName))
  }
  if (searchForm.alertType) {
    result = result.filter(item => item.alertType === searchForm.alertType)
  }
  if (searchForm.status !== null) {
    result = result.filter(item => item.status === searchForm.status)
  }

  pagination.total = result.length
  const start = (pagination.current - 1) * pagination.size
  alertList.value = result.slice(start, start + pagination.size)
}

const handleSearch = () => {
  pagination.current = 1
  applyFilters()
}

const handleReset = () => {
  searchForm.ruleName = ''
  searchForm.alertType = ''
  searchForm.status = null
  pagination.current = 1
  applyFilters()
}

const handleCreate = () => {
  dialogTitle.value = '新建告警规则'
  dialogVisible.value = true
}

const handleEdit = (rule: AlertRule) => {
  dialogTitle.value = '编辑告警规则'
  Object.assign(formData, rule)
  dialogVisible.value = true
}

const handleToggleStatus = async (rule: AlertRule) => {
  const action = rule.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该告警规则吗？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const data = await request(`/alert-rules/${rule.id}/status?status=${rule.status === 1 ? 0 : 1}`, {
      method: 'PATCH'
    })
    if (data.success) {
      ElMessage.success(`${action}成功`)
      loadAlertRules()
    } else {
      ElMessage.error(data.error || `${action}失败`)
    }
  } catch (error) {
    console.error(`${action}失败:`, error)
    ElMessage.error(`${action}失败`)
  }
}

const handleDelete = async (rule: AlertRule) => {
  try {
    await ElMessageBox.confirm('确定要删除该告警规则吗？', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const data = await request(`/alert-rules/${rule.id}`, {
      method: 'DELETE'
    })
    if (data.success) {
      ElMessage.success('删除成功')
      loadAlertRules()
    } else {
      ElMessage.error(data.error || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleAlertTypeChange = (type: string) => {
  if (type === 'THRESHOLD') {
    formData.thresholdType = 'GT'
    formData.thresholdValue = 0
  } else if (type === 'FLUCTUATION') {
    formData.fluctuationRate = 10
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.keys(formData).forEach(key => {
    delete (formData as any)[key]
  })
  formData.alertType = 'THRESHOLD'
  formData.thresholdType = 'GT'
  formData.thresholdValue = 0
  formData.fluctuationRate = 10
  formData.pushMethod = 'EMAIL'
  formData.status = 1
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const url = formData.id ? `/api/alert-rules/${formData.id}` : '/api/alert-rules'
      const method = formData.id ? 'PUT' : 'POST'

      const data = await request(url, {
        method,
        body: JSON.stringify(formData)
      })
      if (data.success) {
        ElMessage.success(formData.id ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadAlertRules()
      } else {
        ElMessage.error(data.error || '操作失败')
      }
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadAlertRules()
})
</script>

<style scoped lang="scss">
.alert-rule {
  padding: 20px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }
  }

  .search-card {
    margin-bottom: 20px;
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      justify-content: flex-end;
    }
  }
}
</style>
