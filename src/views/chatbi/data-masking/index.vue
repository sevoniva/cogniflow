<template>
  <div class="data-masking">
    <div class="header">
      <h2>数据脱敏管理</h2>
      <el-button type="primary" @click="handleCreate">新建脱敏规则</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.ruleName" placeholder="请输入规则名称" clearable />
        </el-form-item>
        <el-form-item label="表名">
          <el-input v-model="searchForm.tableName" placeholder="请输入表名" clearable />
        </el-form-item>
        <el-form-item label="脱敏类型">
          <el-select v-model="searchForm.maskType" placeholder="请选择脱敏类型" clearable>
            <el-option label="完全隐藏" value="HIDE" />
            <el-option label="部分隐藏" value="PARTIAL" />
            <el-option label="哈希" value="HASH" />
            <el-option label="加密" value="ENCRYPT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 脱敏规则列表 -->
    <el-card class="table-card">
      <el-table :data="maskingList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="tableName" label="表名" width="120" />
        <el-table-column prop="fieldName" label="字段名" width="120" />
        <el-table-column prop="maskType" label="脱敏类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getMaskTypeTag(row.maskType)">
              {{ maskTypeMap[row.maskType] || row.maskType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maskPattern" label="脱敏模式" min-width="150" />
        <el-table-column prop="priority" label="优先级" width="80" />
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
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="formData.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="字段名" prop="fieldName">
          <el-input v-model="formData.fieldName" placeholder="请输入字段名" />
        </el-form-item>
        <el-form-item label="脱敏类型" prop="maskType">
          <el-select v-model="formData.maskType" placeholder="请选择脱敏类型" @change="handleMaskTypeChange">
            <el-option label="完全隐藏" value="HIDE" />
            <el-option label="部分隐藏" value="PARTIAL" />
            <el-option label="哈希" value="HASH" />
            <el-option label="加密" value="ENCRYPT" />
          </el-select>
        </el-form-item>
        <el-form-item label="脱敏模式" prop="maskPattern">
          <el-input
            v-model="formData.maskPattern"
            placeholder="例如：前 3 后 4（部分隐藏时使用）"
            :disabled="formData.maskType === 'HIDE' || formData.maskType === 'HASH' || formData.maskType === 'ENCRYPT'"
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            <span v-if="formData.maskType === 'PARTIAL'">格式：前 N 后 M，例如"前 3 后 4"表示显示前 3 位和后 4 位</span>
            <span v-else-if="formData.maskType === 'HIDE'">完全隐藏，不显示任何内容</span>
            <span v-else-if="formData.maskType === 'HASH'">使用 SHA-256 哈希</span>
            <span v-else-if="formData.maskType === 'ENCRYPT'">使用 AES 加密</span>
          </div>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="formData.priority" :min="0" :max="100" />
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
import { InfoFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { extractRecords, request } from '@/utils/http'

interface DataMaskingRule {
  id?: number
  ruleName: string
  tableName: string
  fieldName: string
  maskType: string
  maskPattern?: string
  priority: number
  status: number
  roleId?: number
  userId?: number
  createdAt?: string
  updatedAt?: string
}

const maskTypeMap: Record<string, string> = {
  HIDE: '完全隐藏',
  PARTIAL: '部分隐藏',
  HASH: '哈希',
  ENCRYPT: '加密'
}

const getMaskTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    HIDE: 'danger',
    PARTIAL: 'warning',
    HASH: 'info',
    ENCRYPT: 'success'
  }
  return tagMap[type] || ''
}

const searchForm = reactive({
  ruleName: '',
  tableName: '',
  maskType: ''
})

const sourceList = ref<DataMaskingRule[]>([])
const maskingList = ref<DataMaskingRule[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<DataMaskingRule>({
  ruleName: '',
  tableName: '',
  fieldName: '',
  maskType: 'PARTIAL',
  maskPattern: '前 3 后 4',
  priority: 0,
  status: 1
})

const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  fieldName: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  maskType: [{ required: true, message: '请选择脱敏类型', trigger: 'change' }],
  maskPattern: [{ required: true, message: '请输入脱敏模式', trigger: 'blur' }]
}

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 加载脱敏规则列表
const loadMaskingRules = async () => {
  loading.value = true
  try {
    const data = await request<DataMaskingRule[]>('/data-masking')
    if (data.success) {
      sourceList.value = extractRecords<DataMaskingRule>(data.data)
      applyFilters()
    }
  } catch (error) {
    console.error('加载脱敏规则失败:', error)
    ElMessage.error('加载脱敏规则失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  let result = [...sourceList.value]

  if (searchForm.ruleName) {
    result = result.filter(item => item.ruleName?.includes(searchForm.ruleName))
  }
  if (searchForm.tableName) {
    result = result.filter(item => item.tableName?.includes(searchForm.tableName))
  }
  if (searchForm.maskType) {
    result = result.filter(item => item.maskType === searchForm.maskType)
  }

  pagination.total = result.length
  const start = (pagination.current - 1) * pagination.size
  maskingList.value = result.slice(start, start + pagination.size)
}

const handleSearch = () => {
  pagination.current = 1
  applyFilters()
}

const handleReset = () => {
  searchForm.ruleName = ''
  searchForm.tableName = ''
  searchForm.maskType = ''
  pagination.current = 1
  applyFilters()
}

const handleCreate = () => {
  dialogTitle.value = '新建脱敏规则'
  dialogVisible.value = true
}

const handleEdit = (rule: DataMaskingRule) => {
  dialogTitle.value = '编辑脱敏规则'
  Object.assign(formData, rule)
  dialogVisible.value = true
}

const handleDelete = async (rule: DataMaskingRule) => {
  try {
    await ElMessageBox.confirm('确定要删除该脱敏规则吗？', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const data = await request(`/data-masking/${rule.id}`, {
      method: 'DELETE'
    })
    if (data.success) {
      ElMessage.success('删除成功')
      loadMaskingRules()
    } else {
      ElMessage.error(data.error || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleMaskTypeChange = (type: string) => {
  if (type === 'HIDE') {
    formData.maskPattern = ''
  } else if (type === 'HASH' || type === 'ENCRYPT') {
    formData.maskPattern = ''
  } else if (type === 'PARTIAL') {
    formData.maskPattern = '前 3 后 4'
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.keys(formData).forEach(key => {
    delete (formData as any)[key]
  })
  formData.maskType = 'PARTIAL'
  formData.maskPattern = '前 3 后 4'
  formData.priority = 0
  formData.status = 1
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const url = formData.id ? `/api/data-masking/${formData.id}` : '/api/data-masking'
      const method = formData.id ? 'PUT' : 'POST'

      const data = await request(url, {
        method,
        body: JSON.stringify(formData)
      })
      if (data.success) {
        ElMessage.success(formData.id ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadMaskingRules()
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
  loadMaskingRules()
})
</script>

<style scoped lang="scss">
.data-masking {
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

  .form-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }
}
</style>
