<template>
  <div class="datasource-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-primary-light); color: var(--cb-primary);">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">数据源总数</div>
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
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ dbTypeCount }}</div>
          <div class="stat-label">数据库类型</div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <Card class="toolbar-card" padding="md">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索数据源名称..."
            clearable
            :prefix-icon="Search"
            style="width: 280px"
          />
          <el-select v-model="typeFilter" placeholder="数据库类型" clearable style="width: 160px">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="Oracle" value="ORACLE" />
            <el-option label="OB-Oracle" value="OB_ORACLE" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="ClickHouse" value="CLICKHOUSE" />
            <el-option label="SQL Server" value="SQLSERVER" />
          </el-select>
        </div>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增数据源
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
        <el-table-column prop="name" label="数据源名称" min-width="180" />
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="dbTypeTag(row.type)" size="small">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机" width="180" show-overflow-tooltip />
        <el-table-column prop="port" label="端口" width="80" align="center" />
        <el-table-column prop="database" label="数据库" width="140" show-overflow-tooltip />
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
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="success" @click="testConnection(row.id)">
              <el-icon><Connection /></el-icon>
              测试
            </el-button>
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
          :title="keyword || typeFilter ? '未找到匹配结果' : '暂无数据源'"
          :description="keyword || typeFilter ? '请调整筛选条件后重试' : '点击右上角「新增数据源」创建第一个数据源'"
        >
          <el-button v-if="keyword || typeFilter" @click="clearFilter">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
          <el-button v-else type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            新增数据源
          </el-button>
        </EmptyState>
      </div>
    </Card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showAdd"
      :title="isEdit ? '编辑数据源' : '新增数据源'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-width="90px" class="datasource-form">
        <el-form-item label="数据源名称" required>
          <el-input
            v-model="form.name"
            placeholder="如：生产数据库"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="数据库类型" required>
          <el-select v-model="form.type" placeholder="请选择数据库类型" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="Oracle" value="ORACLE" />
            <el-option label="OB-Oracle" value="OB_ORACLE" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="ClickHouse" value="CLICKHOUSE" />
            <el-option label="SQL Server" value="SQLSERVER" />
            <el-option label="Hive" value="HIVE" />
            <el-option label="MariaDB" value="MARIADB" />
            <el-option label="达梦" value="DM" />
            <el-option label="人大金仓" value="KINGBASE" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机地址" required>
          <el-input
            v-model="form.host"
            placeholder="如：192.168.1.100 或 localhost"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="端口" required>
          <el-input-number
            v-model="form.port"
            :min="1"
            :max="65535"
            placeholder="如：3306"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="数据库名" required>
          <el-input
            v-model="form.database"
            placeholder="如：chatbi_db"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input
            v-model="form.username"
            placeholder="如：root"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            maxlength="100"
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
  Coin,
  CircleCheck,
  DataLine,
  Connection,
  Edit,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import type { DataSource, DataSourceRequest } from '@/types'

const keyword = ref('')
const typeFilter = ref('')
const list = ref<DataSource[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showAdd = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<DataSourceRequest>({
  name: '',
  type: 'MYSQL',
  host: '',
  port: 3306,
  username: '',
  password: '',
  database: ''
})

const activeCount = computed(() => list.value.filter(d => d.status === 'active').length)
const dbTypeCount = computed(() => {
  const types = new Set(list.value.map(d => d.type))
  return types.size
})

const dbTypeTag = (type: string) => {
  const map: Record<string, any> = {
    MYSQL: '',
    ORACLE: 'danger',
    POSTGRESQL: 'success',
    CLICKHOUSE: 'warning'
  }
  return map[type] || 'info'
}

onMounted(() => {
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminService.getDataSources()
  } catch (e: any) {
    error.value = e?.message || '加载数据失败，请检查后端服务是否启动'
    console.error('加载数据源列表失败:', e)
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => {
  let result = list.value
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    result = result.filter(i => i.name.toLowerCase().includes(k))
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
    name: '',
    type: 'MYSQL',
    host: '',
    port: 3306,
    username: '',
    password: '',
    database: ''
  }
  showAdd.value = true
}

function edit(row: DataSource) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    name: row.name,
    type: row.type,
    host: row.host || '',
    port: row.port || 3306,
    username: row.username || '',
    password: '',
    database: row.database || ''
  }
  showAdd.value = true
}

function clearFilter() {
  keyword.value = ''
  typeFilter.value = ''
}

async function toggleStatus(row: DataSource) {
  const res = await adminService.updateDataSource(row.id, { status: row.status })
  if (res.success) {
    await load()
    ElMessage.success(row.status === 'active' ? '已启用' : '已禁用')
  } else {
    ElMessage.error(res.error || '操作失败')
    row.status = row.status === 'active' ? 'inactive' : 'active'
  }
}

async function testConnection(id: number) {
  const res = await adminService.testDataSource(id)
  if (res.success) {
    ElMessage.success('连接测试成功')
  } else {
    ElMessage.error(res.error || '连接测试失败')
  }
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该数据源？删除后将无法恢复', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await adminService.deleteDataSource(id)
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
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入数据源名称')
    return
  }
  if (!form.value.host?.trim()) {
    ElMessage.warning('请输入主机地址')
    return
  }
  if (!form.value.database?.trim()) {
    ElMessage.warning('请输入数据库名')
    return
  }
  if (!form.value.username?.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }

  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      const res = await adminService.updateDataSource(editId.value, form.value)
      if (res.success) {
        ElMessage.success('已更新')
        showAdd.value = false
        await load()
      } else {
        ElMessage.error(res.error || '更新失败')
      }
    } else {
      const res = await adminService.addDataSource(form.value)
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
.datasource-page {
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
.datasource-form {
  padding: 20px 10px 0;
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
