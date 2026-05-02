<template>
  <div class="data-permission">
    <div class="header">
      <h2>数据权限管理</h2>
      <el-button type="primary" @click="handleCreate">新建权限规则</el-button>
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

    <!-- 权限规则列表 -->
    <el-card class="table-card">
      <el-table :data="permissionList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="tableName" label="表名" width="120" />
        <el-table-column prop="fieldName" label="字段名" width="120" />
        <el-table-column prop="operator" label="操作符" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.operator }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="值" min-width="150" />
        <el-table-column prop="valueType" label="值类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.valueType === 'CONSTANT' ? '' : 'warning'">
              {{ valueTypeMap[row.valueType] || row.valueType }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-form-item label="操作符" prop="operator">
          <el-select v-model="formData.operator" placeholder="请选择操作符">
            <el-option label="等于" value="=" />
            <el-option label="不等于" value="!=" />
            <el-option label="大于" value=">" />
            <el-option label="大于等于" value=">=" />
            <el-option label="小于" value="<" />
            <el-option label="小于等于" value="<=" />
            <el-option label="LIKE" value="LIKE" />
            <el-option label="IN" value="IN" />
          </el-select>
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="formData.value" placeholder="请输入值" />
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="formData.valueType" placeholder="请选择值类型">
            <el-option label="常量" value="CONSTANT" />
            <el-option label="字段" value="FIELD" />
            <el-option label="用户属性" value="USER_ATTR" />
          </el-select>
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
import type { FormInstance, FormRules } from 'element-plus'
import { extractRecords, request } from '@/utils/http'

interface DataPermissionRule {
  id?: number
  ruleName: string
  tableName: string
  fieldName: string
  operator: string
  value: string
  valueType: string
  priority: number
  status: number
  roleId?: number
  userId?: number
  createdAt?: string
  updatedAt?: string
}

const valueTypeMap: Record<string, string> = {
  CONSTANT: '常量',
  FIELD: '字段',
  USER_ATTR: '用户属性'
}

const searchForm = reactive({
  ruleName: '',
  tableName: '',
  status: null as number | null
})

const sourceList = ref<DataPermissionRule[]>([])
const permissionList = ref<DataPermissionRule[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<DataPermissionRule>({
  ruleName: '',
  tableName: '',
  fieldName: '',
  operator: '=',
  value: '',
  valueType: 'CONSTANT',
  priority: 0,
  status: 1
})

const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  fieldName: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  operator: [{ required: true, message: '请选择操作符', trigger: 'change' }],
  value: [{ required: true, message: '请输入值', trigger: 'blur' }],
  valueType: [{ required: true, message: '请选择值类型', trigger: 'change' }]
}

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 加载权限规则列表
const loadPermissions = async () => {
  loading.value = true
  try {
    const data = await request<DataPermissionRule[]>('/data-permissions')
    if (data.success) {
      sourceList.value = extractRecords<DataPermissionRule>(data.data)
      applyFilters()
    }
  } catch (error) {
    console.error('加载权限规则失败:', error)
    ElMessage.error('加载权限规则失败')
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
  if (searchForm.status !== null) {
    result = result.filter(item => item.status === searchForm.status)
  }

  pagination.total = result.length
  const start = (pagination.current - 1) * pagination.size
  permissionList.value = result.slice(start, start + pagination.size)
}

const handleSearch = () => {
  pagination.current = 1
  applyFilters()
}

const handleReset = () => {
  searchForm.ruleName = ''
  searchForm.tableName = ''
  searchForm.status = null
  pagination.current = 1
  applyFilters()
}

const handleCreate = () => {
  dialogTitle.value = '新建权限规则'
  dialogVisible.value = true
}

const handleEdit = (rule: DataPermissionRule) => {
  dialogTitle.value = '编辑权限规则'
  Object.assign(formData, rule)
  dialogVisible.value = true
}

const handleDelete = async (rule: DataPermissionRule) => {
  try {
    await ElMessageBox.confirm('确定要删除该权限规则吗？', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const data = await request(`/data-permissions/${rule.id}`, {
      method: 'DELETE'
    })
    if (data.success) {
      ElMessage.success('删除成功')
      loadPermissions()
    } else {
      ElMessage.error(data.error || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    ruleName: '', tableName: '', fieldName: '',
    operator: '=', value: '', valueType: 'CONSTANT',
    priority: 0, status: 1
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const url = formData.id ? `/api/data-permissions/${formData.id}` : '/api/data-permissions'
      const method = formData.id ? 'PUT' : 'POST'

      const data = await request(url, {
        method,
        body: JSON.stringify(formData)
      })
      if (data.success) {
        ElMessage.success(formData.id ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadPermissions()
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
  loadPermissions()
})
</script>

<style scoped lang="scss">
.data-permission {
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
