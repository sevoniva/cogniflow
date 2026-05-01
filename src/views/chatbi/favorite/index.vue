<template>
  <div class="favorite-page cb-page">
    <PageHeader 
      subtitle="我的收藏" 
      show-back
      back-path="/chatbi/query"
      max-width="720px"
    >
      <template #actions>
        <el-button link @click="router.push('/chatbi/history')">
          <el-icon><Clock /></el-icon>
          <span class="btn-text">历史</span>
        </el-button>
        <el-button link type="primary" @click="goHome">
          <el-icon><Search /></el-icon>
          <span class="btn-text">去查询</span>
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-content" style="max-width: 720px;">
      <!-- 收藏列表 -->
      <Card v-if="list.length" padding="none" hoverable>
        <template #header>
          <span class="card-title">
            <el-icon><Star /></el-icon>
            收藏列表
            <el-tag size="small" type="info" effect="plain">{{ list.length }} 条</el-tag>
          </span>
        </template>
        
        <ListItem
          v-for="item in list"
          :key="item.id"
          :title="item.name"
          :description="item.text"
          :meta="item.createdAt"
          clickable
          @click="doQuery(item.text)"
        >
          <template #icon>
            <div class="favorite-icon">
              <el-icon><Star /></el-icon>
            </div>
          </template>
          <template #actions>
            <el-button link type="primary" @click.stop="doQuery(item.text)">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button link @click.stop="rename(item)">
              <el-icon><Edit /></el-icon>
              重命名
            </el-button>
            <el-button link type="danger" @click.stop="remove(item.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </ListItem>
      </Card>

      <!-- 空状态 -->
      <Card v-else padding="lg">
        <EmptyState
          type="favorite"
          title="暂无收藏查询"
          description="在查询结果页点击「加入收藏」按钮，将常用查询保存到这里"
        >
          <el-button type="primary" @click="goHome">
            <el-icon><Search /></el-icon>
            去查询
          </el-button>
        </EmptyState>
      </Card>

      <!-- 清空按钮 -->
      <div v-if="list.length" class="footer-actions">
        <el-button type="danger" plain @click="clearAll">
          <el-icon><Delete /></el-icon>
          清空全部收藏
        </el-button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Clock, 
  Search, 
  Star, 
  Delete,
  Edit
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import ListItem from '@/components/ListItem.vue'
import EmptyState from '@/components/EmptyState.vue'
import { chatbiService } from '@/adapters'
import type { QueryItem } from '@/types'

const router = useRouter()
const list = ref<QueryItem[]>([])

async function load() {
  list.value = await chatbiService.getFavorites()
}

function goHome() {
  router.push('/chatbi/query')
}

function doQuery(text: string) {
  router.push({ path: '/chatbi/result', query: { q: encodeURIComponent(text) } })
}

function rename(item: QueryItem) {
  ElMessageBox.prompt('请输入新的收藏名称', '重命名', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: item.name,
    inputValidator: (val) => {
      if (!val.trim()) return '名称不能为空'
      if (val.trim().length > 30) return '名称不能超过30个字符'
      return true
    }
  }).then(async ({ value }) => {
    await chatbiService.renameFavorite(item.id, value.trim())
    await load()
    ElMessage.success('已重命名')
  }).catch(() => {})
}

function remove(id: number) {
  ElMessageBox.confirm('确定删除该收藏？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    await chatbiService.removeFavorite(id)
    await load()
    ElMessage.success('已删除')
  }).catch(() => {})
}

function clearAll() {
  ElMessageBox.confirm('确定清空所有收藏？此操作不可恢复', '清空确认', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    await chatbiService.clearFavorites()
    await load()
    ElMessage.success('已清空所有收藏')
  }).catch(() => {})
}

load()
</script>

<style scoped>
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

/* 收藏图标 */
.favorite-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--cb-radius-sm);
  background: var(--cb-warning-light);
  color: var(--cb-warning);
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
  .btn-text {
    display: none;
  }
}
</style>
