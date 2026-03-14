<template>
  <div class="semantic-page">
    <!-- 同义词配置 -->
    <Card class="section-card" padding="none">
      <template #header>
        <div class="section-header">
          <div class="section-title">
            <div class="title-icon" style="background: var(--cb-primary-light); color: var(--cb-primary);">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="title-text">
              <h3>同义词配置</h3>
              <p>配置业务术语的同义词，提升查询理解的准确性</p>
            </div>
          </div>
          <el-button type="primary" @click="showAddSynonym = true">
            <el-icon><Plus /></el-icon>
            添加同义词
          </el-button>
        </div>
      </template>

      <!-- 错误提示 -->
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        closable
        @close="error = ''"
        style="margin: 16px;"
      />

      <!-- 筛选栏 -->
      <div class="section-filter">
        <el-input 
          v-model="synonymKeyword" 
          placeholder="搜索标准词..." 
          clearable
          :prefix-icon="Search"
          style="width: 260px"
        />
        <el-tag v-if="synonyms.length" type="info" effect="plain" size="small">
          共 {{ synonyms.length }} 条
        </el-tag>
      </div>

      <!-- 同义词列表 -->
      <div v-if="filteredSynonyms.length" class="synonym-list">
        <div 
          v-for="item in filteredSynonyms" 
          :key="item.id" 
          class="synonym-item"
        >
          <div class="synonym-main">
            <div class="standard-word">
              <el-tag type="primary" effect="dark" size="small">标准词</el-tag>
              <span class="word-text">{{ item.standard }}</span>
            </div>
            <div class="alias-list">
              <el-tag 
                v-for="alias in item.aliases" 
                :key="alias" 
                type="info"
                effect="plain"
                size="small"
                class="alias-tag"
              >
                {{ alias }}
              </el-tag>
            </div>
          </div>
          <el-button link type="danger" @click="removeSynonym(item.id)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="section-empty">
        <EmptyState
          :type="synonymKeyword ? 'search' : 'data'"
          :title="synonymKeyword ? '未找到匹配结果' : '暂无同义词'"
          :description="synonymKeyword ? '请调整关键词后重试' : '点击右上角「添加同义词」创建第一条配置'"
        >
          <el-button v-if="synonymKeyword" @click="synonymKeyword = ''">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
        </EmptyState>
      </div>
    </Card>

    <!-- 规则模板 -->
    <Card class="section-card" padding="none">
      <template #header>
        <div class="section-header">
          <div class="section-title">
            <div class="title-icon" style="background: var(--cb-success-light); color: var(--cb-success);">
              <el-icon><Document /></el-icon>
            </div>
            <div class="title-text">
              <h3>规则模板</h3>
              <p>预定义的查询模板，用于匹配和解析用户输入</p>
            </div>
          </div>
          <el-button type="primary" plain @click="loadRuleTemplates">
            <el-icon><Refresh /></el-icon>
            刷新模板
          </el-button>
        </div>
      </template>

      <el-table 
        :data="ruleTemplates" 
        border 
        stripe
        size="small"
        :header-cell-style="{ background: '#fafbfc', fontWeight: 600 }"
      >
        <el-table-column prop="name" label="模板名称" width="150" />
        <el-table-column prop="pattern" label="匹配模式" min-width="200">
          <template #default="{ row }">
            <code class="pattern-code">{{ row.pattern }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority <= 2 ? 'danger' : 'info'" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="active"
              inactive-value="inactive"
              disabled
            />
          </template>
        </el-table-column>
        <el-table-column label="状态说明" width="120" fixed="right" align="center">
          <template #default>
            <el-tag type="success" effect="plain" size="small">实时生效</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </Card>

    <!-- 添加同义词对话框 -->
    <el-dialog 
      v-model="showAddSynonym" 
      title="添加同义词" 
      width="520px" 
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="synonymForm" label-width="90px" class="synonym-form">
        <el-form-item label="标准词" required>
          <el-input 
            v-model="synonymForm.standard" 
            placeholder="如：费用支出"
            maxlength="50"
            show-word-limit
          />
          <div class="form-hint">业务术语的标准表述</div>
        </el-form-item>
        <el-form-item label="同义词" required>
          <el-input 
            v-model="synonymForm.aliasesInput" 
            type="textarea" 
            :rows="4" 
            placeholder="多个同义词用逗号或换行分隔，如：&#10;花费, 支出, 成本&#10;开销"
            maxlength="200"
            show-word-limit
          />
          <div class="form-hint">用户可能使用的其他表述方式</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddSynonym = false">取消</el-button>
        <el-button type="primary" @click="saveSynonym" :loading="saving">
          添加
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
  Connection, 
  Document,
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import type { Synonym } from '@/types'

const synonymKeyword = ref('')
const synonyms = ref<Synonym[]>([])
const ruleTemplates = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showAddSynonym = ref(false)
const synonymForm = ref({ standard: '', aliasesInput: '' })
const error = ref('')

onMounted(() => {
  loadSynonyms()
  loadRuleTemplates()
})

async function loadSynonyms() {
  loading.value = true
  error.value = ''
  try {
    synonyms.value = await adminService.getSynonyms()
  } catch (e: any) {
    error.value = e?.message || '加载同义词失败，请检查后端服务'
    console.error('加载同义词失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadRuleTemplates() {
  ruleTemplates.value = await adminService.getRuleTemplates()
}

const filteredSynonyms = computed(() => {
  if (!synonymKeyword.value) return synonyms.value
  const k = synonymKeyword.value.toLowerCase()
  return synonyms.value.filter(i => 
    i.standard.toLowerCase().includes(k) ||
    i.aliases.some(a => a.toLowerCase().includes(k))
  )
})

async function saveSynonym() {
  const standard = synonymForm.value.standard?.trim()
  const aliasesInput = synonymForm.value.aliasesInput?.trim()
  
  if (!standard) {
    ElMessage.warning('请输入标准词')
    return
  }
  if (!aliasesInput) {
    ElMessage.warning('请输入同义词')
    return
  }
  
  const aliases = aliasesInput
    .split(/[,，\n]/)
    .map((s: string) => s.trim())
    .filter((s: string) => s)
  
  if (aliases.length === 0) {
    ElMessage.warning('请输入有效的同义词')
    return
  }
  
  saving.value = true
  try {
    const res = await adminService.addSynonym({ 
      standard: standard, 
      aliases 
    })
    if (res.success) {
      showAddSynonym.value = false
      synonymForm.value = { standard: '', aliasesInput: '' }
      await loadSynonyms()
      ElMessage.success('已添加')
    } else {
      ElMessage.warning(res.error || '添加失败')
    }
  } finally {
    saving.value = false
  }
}

async function removeSynonym(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该同义词配置？', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    const res = await adminService.deleteSynonym(id)
    if (res.success) {
      await loadSynonyms()
      ElMessage.success('已删除')
    } else {
      ElMessage.error(res.error || '删除失败')
    }
  } catch {
    // 取消
  }
}
</script>

<style scoped>
.semantic-page {
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.title-text h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--cb-text-primary);
  margin: 0 0 4px;
}

.title-text p {
  font-size: 13px;
  color: var(--cb-text-secondary);
  margin: 0;
}

/* 筛选栏 */
.section-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--cb-border-lighter);
}

/* 同义词列表 */
.synonym-list {
  padding: 8px;
}

.synonym-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--cb-border-lighter);
  transition: background 0.15s;
}

.synonym-item:last-child {
  border-bottom: none;
}

.synonym-item:hover {
  background: var(--cb-bg-hover);
}

.synonym-main {
  flex: 1;
  min-width: 0;
}

.standard-word {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.word-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--cb-text-primary);
}

.alias-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-left: 64px;
}

.alias-tag {
  font-size: 12px;
}

/* 空状态 */
.section-empty {
  padding: 40px 20px;
}

/* 表单 */
.synonym-form {
  padding: 20px 10px 0;
}

.form-hint {
  font-size: 12px;
  color: var(--cb-text-secondary);
  margin-top: 6px;
}

/* 规则模板 */
.pattern-code {
  background: var(--cb-bg-hover);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--cb-text-regular);
}

/* 响应式 */
@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .section-filter {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .section-filter .el-input {
    width: 100% !important;
  }
  
  .synonym-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .alias-list {
    padding-left: 0;
  }
}
</style>
