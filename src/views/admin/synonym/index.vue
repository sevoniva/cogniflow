<template>
  <div class="synonym-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-primary-light); color: var(--cb-primary);">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ list.length }}</div>
          <div class="stat-label">同义词总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--cb-success-light); color: var(--cb-success);">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ activeCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <Card class="toolbar-card" padding="md">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索标准词或别名..."
            clearable
            :prefix-icon="Search"
            style="width: 280px"
          />
        </div>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增同义词
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
        <el-table-column prop="standard" label="标准词" width="180">
          <template #default="{ row }">
            <span class="standard-text">{{ row.standard }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="aliases" label="别名列表" min-width="300">
          <template #default="{ row }">
            <div class="aliases-wrapper">
              <el-tag
                v-for="(alias, index) in row.aliases"
                :key="index"
                size="small"
                type="info"
                style="margin-right: 8px; margin-bottom: 4px;"
              >
                {{ alias }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.updatedAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="remove(row.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!filteredList.length && !loading" class="table-empty">
        <EmptyState
          :type="keyword ? 'search' : 'data'"
          :title="keyword ? '未找到匹配结果' : '暂无同义词'"
          :description="keyword ? '请调整筛选条件后重试' : '点击右上角「新增同义词」创建第一个同义词'"
        >
          <el-button v-if="keyword" @click="clearFilter">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
          <el-button v-else type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            新增同义词
          </el-button>
        </EmptyState>
      </div>
    </Card>

    <!-- 新增对话框 -->
    <el-dialog
      v-model="showAdd"
      :title="'新增同义词'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px" class="synonym-form">
        <el-form-item label="标准词" required>
          <el-input
            v-model="form.standard"
            placeholder="如：销售额"
            maxlength="50"
            show-word-limit
          />
          <div class="form-hint">标准业务术语，用于统一识别</div>
        </el-form-item>
        <el-form-item label="别名列表" required>
          <el-tag
            v-for="(alias, index) in form.aliases"
            :key="index"
            closable
            @close="removeAlias(index)"
            style="margin-right: 8px; margin-bottom: 8px;"
          >
            {{ alias }}
          </el-tag>
          <div class="alias-input-wrapper">
            <el-input
              v-model="aliasInput"
              placeholder="输入别名后按 Enter 添加"
              @keydown.enter="addAlias"
              maxlength="50"
            >
              <template #append>
                <el-button @click="addAlias">
                  <el-icon><Plus /></el-icon>
                  添加
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="form-hint">按 Enter 键快速添加别名，支持多个别名</div>
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
  Collection,
  DataAnalysis,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import type { Synonym, SynonymRequest } from '@/types'

const keyword = ref('')
const list = ref<Synonym[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showAdd = ref(false)
const form = ref<SynonymRequest>({ standard: '', aliases: [] })
const aliasInput = ref('')

const activeCount = computed(() => list.value.length)

onMounted(() => {
  load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminService.getSynonyms()
  } catch (e: any) {
    error.value = e?.message || '加载数据失败，请检查后端服务是否启动'
    console.error('加载同义词列表失败:', e)
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => {
  let result = list.value
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    result = result.filter(i =>
      i.standard.toLowerCase().includes(k) ||
      i.aliases.some(a => a.toLowerCase().includes(k))
    )
  }
  return result
})

function openAddDialog() {
  form.value = { standard: '', aliases: [] }
  aliasInput.value = ''
  showAdd.value = true
}

function clearFilter() {
  keyword.value = ''
}

function addAlias() {
  const alias = aliasInput.value.trim()
  if (!alias) return
  if (form.value.aliases.includes(alias)) {
    ElMessage.warning('该别名已存在')
    return
  }
  form.value.aliases.push(alias)
  aliasInput.value = ''
}

function removeAlias(index: number) {
  form.value.aliases.splice(index, 1)
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该同义词？删除后将无法恢复', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await adminService.deleteSynonym(id)
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
  if (!form.value.standard.trim()) {
    ElMessage.warning('请输入标准词')
    return
  }
  if (form.value.aliases.length === 0) {
    ElMessage.warning('请至少添加一个别名')
    return
  }

  saving.value = true
  try {
    const res = await adminService.addSynonym(form.value)
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
.synonym-page {
  max-width: 1200px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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
.standard-text {
  font-weight: 600;
  color: var(--cb-primary);
  font-size: 14px;
}

.aliases-wrapper {
  display: flex;
  flex-wrap: wrap;
}

.time-text {
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.table-empty {
  padding: 40px 20px;
}

/* 表单 */
.synonym-form {
  padding: 20px 10px 0;
}

.alias-input-wrapper {
  margin-top: 8px;
}

.form-hint {
  font-size: 12px;
  color: var(--cb-text-secondary);
  margin-top: 6px;
}

/* 响应式 */
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

  .toolbar-left .el-input {
    width: 100% !important;
  }
}
</style>
