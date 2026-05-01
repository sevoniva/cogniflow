<template>
  <div class="history-page cb-page">
    <PageHeader 
      subtitle="查询历史" 
      show-back
      back-path="/chatbi/query"
      max-width="720px"
    >
      <template #actions>
        <el-button link @click="router.push('/chatbi/favorite')">
          <el-icon><Star /></el-icon>
          <span class="btn-text">收藏</span>
        </el-button>
        <el-button link type="primary" @click="goHome">
          <el-icon><Search /></el-icon>
          <span class="btn-text">去查询</span>
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-content" style="max-width: 720px;">
      <!-- 筛选区 -->
      <Card v-if="list.length" padding="md">
        <div class="filter-section">
          <el-input 
            v-model="keyword" 
            placeholder="搜索历史查询..." 
            clearable
            :prefix-icon="Search"
            style="width: 240px"
          />
          <el-radio-group v-model="timeFilter" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="recent">最近3条</el-radio-button>
          </el-radio-group>
        </div>
      </Card>

      <!-- 历史列表 -->
      <Card v-if="filteredList.length" padding="none" hoverable>
        <template #header>
          <span class="card-title">
            <el-icon><Clock /></el-icon>
            历史记录
            <el-tag size="small" type="info" effect="plain">{{ filteredList.length }} 条</el-tag>
          </span>
        </template>
        
        <ListItem
          v-for="item in filteredList"
          :key="item.id"
          :title="item.name"
          :meta="item.createdAt"
          clickable
          @click="reQuery(item.text)"
        >
          <template #icon>
            <div class="history-icon">
              <el-icon><Clock /></el-icon>
            </div>
          </template>
          <template #actions>
            <el-button link type="primary" @click.stop="reQuery(item.text)">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button link type="danger" @click.stop="remove(item.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </ListItem>
      </Card>

      <!-- 筛选无结果 -->
      <Card v-else-if="list.length && !filteredList.length" padding="lg">
        <EmptyState
          type="search"
          title="未找到匹配的记录"
          description="请尝试其他关键词或清除筛选条件"
        >
          <el-button @click="clearFilter">
            <el-icon><Refresh /></el-icon>
            清除筛选
          </el-button>
        </EmptyState>
      </Card>

      <!-- 空状态 -->
      <Card v-else padding="lg">
        <EmptyState
          type="document"
          title="暂无查询历史"
          description="去首页发起查询，记录将自动保存到这里"
        >
          <el-button type="primary" @click="goHome">
            <el-icon><Search /></el-icon>
            去查询
          </el-button>
        </EmptyState>
      </Card>

      <!-- 清空按钮 -->
      <div v-if="filteredList.length" class="footer-actions">
        <el-button type="danger" plain @click="clearAll">
          <el-icon><Delete /></el-icon>
          清空全部历史
        </el-button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Star, 
  Search, 
  Clock, 
  Delete,
  Refresh
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import ListItem from '@/components/ListItem.vue'
import EmptyState from '@/components/EmptyState.vue'
import { chatbiService } from '@/adapters'
import type { QueryItem } from '@/types'

const router = useRouter()
const list = ref<QueryItem[]>([])
const keyword = ref('')
const timeFilter = ref<'all' | 'recent'>('all')

async function load() {
  list.value = await chatbiService.getRecentQueries()
}

const filteredList = computed(() => {
  let result = list.value
  if (keyword.value.trim()) {
    const k = keyword.value.toLowerCase()
    result = result.filter(i => i.text.toLowerCase().includes(k))
  }
  if (timeFilter.value === 'recent') {
    result = result.slice(0, 3)
  }
  return result
})

function goHome() {
  router.push('/chatbi/query')
}

function reQuery(text: string) {
  router.push({ path: '/chatbi/result', query: { q: encodeURIComponent(text) } })
}

function clearFilter() {
  keyword.value = ''
  timeFilter.value = 'all'
}

function remove(id: number) {
  ElMessageBox.confirm('确定删除该历史记录？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    await chatbiService.deleteQuery(id)
    await load()
    ElMessage.success('已删除')
  }).catch(() => {})
}

function clearAll() {
  ElMessageBox.confirm('确定清空所有历史记录？此操作不可恢复', '清空确认', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    await chatbiService.clearRecentQueries()
    await load()
    ElMessage.success('已清空所有历史记录')
  }).catch(() => {})
}

load()
</script>

<style scoped>
/* 筛选区 */
.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cb-space-md);
}

/* 卡片标题 */
.card-title {
  display: flex;
  align-items: center;
  gap: var(--cb-space-sm);
  font-size: var(--cb-font-size-md);
  font-weight: var(--cb-font-weight-bold);
  color: var(--cb-text-primary);
}

.card-title .el-tag {
  font-weight: var(--cb-font-weight-normal);
}

/* 历史图标 */
.history-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--cb-radius-sm);
  background: var(--cb-border-lighter);
  color: var(--cb-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 底部操作 */
.footer-actions {
  display: flex;
  justify-content: center;
  padding: var(--cb-space-lg) 0;
}

/* 响应式 */
@media (max-width: 640px) {
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-section .el-input {
    width: 100% !important;
  }
  
  .btn-text {
    display: none;
  }
}
</style>
