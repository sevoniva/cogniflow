<template>
  <div class="dashboard-page cb-page">
    <PageHeader subtitle="仪表板工作台" max-width="1200px" show-back back-path="/chatbi/query">
      <template #actions>
        <el-button link @click="router.push('/chatbi/visual-query')">
          <el-icon><Grid /></el-icon>
          <span class="btn-text">可视化查询</span>
        </el-button>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建仪表板
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-page-container--wide cb-content dashboard-main">
      <section class="hero-grid">
        <Card padding="lg" shadow="md" class="hero-card">
          <div class="hero-copy">
            <div class="hero-copy__eyebrow">Dashboard Studio</div>
            <h2>汇总分析视图、组件编排与嵌入分享入口</h2>
            <p>这里展示已保存的真实业务仪表板。支持进入编辑器调整图表组件、发布到嵌入页，结果与前台查询使用同一批数据源。</p>
          </div>
          <div class="hero-metrics">
            <div class="hero-metric">
              <span>仪表板总数</span>
              <strong>{{ dashboardList.length }}</strong>
            </div>
            <div class="hero-metric">
              <span>已发布</span>
              <strong>{{ publishedCount }}</strong>
            </div>
            <div class="hero-metric">
              <span>草稿</span>
              <strong>{{ draftCount }}</strong>
            </div>
          </div>
        </Card>

        <Card padding="lg" class="hero-side-card">
          <div class="hero-side-card__header">
            <span>操作建议</span>
            <el-tag type="primary" effect="plain" size="small">企业版规范</el-tag>
          </div>
          <div class="suggestion-list">
            <div class="suggestion-item">
              <strong>先做维度梳理</strong>
              <span>建议先在可视化查询页验证 SQL，再进入仪表板编排组件。</span>
            </div>
            <div class="suggestion-item">
              <strong>发布后再嵌入</strong>
              <span>已发布仪表板才能稳定用于分享与嵌入访问。</span>
            </div>
            <div class="suggestion-item">
              <strong>保持指标同名</strong>
              <span>统一命名有助于 AI 与同义词链路准确理解组件含义。</span>
            </div>
          </div>
        </Card>
      </section>

      <Card padding="md" class="toolbar-card">
        <div class="cb-toolbar">
          <div class="cb-filter-bar">
            <el-input
              v-model="searchForm.name"
              placeholder="搜索仪表板名称"
              clearable
              :prefix-icon="Search"
              style="width: 280px"
            />
            <el-select v-model="statusFilter" clearable placeholder="发布状态" style="width: 140px">
              <el-option label="已发布" :value="1" />
              <el-option label="草稿" :value="0" />
            </el-select>
          </div>
          <el-button @click="resetFilters">
            <el-icon><Refresh /></el-icon>
            重置筛选
          </el-button>
        </div>
      </Card>

      <Card padding="none">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          closable
          style="margin: 16px"
          @close="error = ''"
        />

        <div v-loading="loading" class="dashboard-grid">
          <article
            v-for="dashboard in filteredDashboards"
            :key="dashboard.id"
            class="dashboard-card"
          >
            <div class="dashboard-card__cover">
              <img
                v-if="dashboard.coverImage"
                :src="dashboard.coverImage"
                alt="dashboard cover"
                class="cover-image"
              />
              <div v-else class="cover-placeholder">
                <el-icon><DataLine /></el-icon>
                <span>{{ dashboard.name }}</span>
              </div>
              <div class="cover-mask">
                <el-button type="primary" size="small" @click="handleView(dashboard)">进入编辑</el-button>
              </div>
            </div>
            <div class="dashboard-card__body">
              <div class="dashboard-card__head">
                <strong>{{ dashboard.name }}</strong>
                <el-tag :type="dashboard.status === 1 ? 'success' : 'info'" effect="plain" size="small">
                  {{ dashboard.status === 1 ? '已发布' : '草稿' }}
                </el-tag>
              </div>
              <p>{{ dashboard.description || '暂无描述，建议补充看板适用场景与目标人群。' }}</p>
              <div class="dashboard-card__meta">
                <span>{{ dashboard.createdByName || '系统创建' }}</span>
                <span>{{ formatDateTime(dashboard.updatedAt || dashboard.createdAt) }}</span>
              </div>
              <div class="dashboard-card__actions">
                <el-button link type="primary" @click="handleView(dashboard)">编辑</el-button>
                <el-button link @click="handlePreview(dashboard)">打开</el-button>
              </div>
            </div>
          </article>
        </div>

        <EmptyState
          v-if="!loading && !filteredDashboards.length"
          :type="dashboardList.length ? 'search' : 'chart'"
          :title="dashboardList.length ? '没有匹配的仪表板' : '暂无仪表板'"
          :description="dashboardList.length ? '试试更换关键词或状态筛选。' : '点击右上角创建第一个仪表板。'"
        >
          <el-button type="primary" @click="handleCreate">新建仪表板</el-button>
        </EmptyState>
      </Card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DataLine, Grid, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatDateTime, request } from '@/utils/http'

interface Dashboard {
  id?: number
  name: string
  description?: string
  layoutConfig?: string
  chartsConfig?: string
  coverImage?: string
  createdBy?: number
  createdByName?: string
  isPublic?: boolean
  status: number
  createdAt?: string
  updatedAt?: string
}

const router = useRouter()
const searchForm = reactive({ name: '' })
const statusFilter = ref<number | null>(null)
const dashboardList = ref<Dashboard[]>([])
const loading = ref(false)
const error = ref('')

const publishedCount = computed(() => dashboardList.value.filter(item => item.status === 1).length)
const draftCount = computed(() => dashboardList.value.filter(item => item.status !== 1).length)
const filteredDashboards = computed(() => {
  const keyword = searchForm.name.trim().toLowerCase()
  return dashboardList.value.filter(item => {
    const matchesKeyword = !keyword || item.name.toLowerCase().includes(keyword) || (item.description || '').toLowerCase().includes(keyword)
    const matchesStatus = statusFilter.value === null || item.status === statusFilter.value
    return matchesKeyword && matchesStatus
  })
})

async function loadDashboards() {
  loading.value = true
  error.value = ''
  const response = await request<Dashboard[]>('/dashboards')
  if (response.success) {
    dashboardList.value = Array.isArray(response.data) ? response.data : []
  } else {
    error.value = response.error || '加载仪表板失败'
  }
  loading.value = false
}

function resetFilters() {
  searchForm.name = ''
  statusFilter.value = null
}

function handleCreate() {
  router.push('/chatbi/dashboard/new/edit')
}

function handleView(dashboard: Dashboard) {
  router.push(`/chatbi/dashboard/${dashboard.id}/edit`)
}

function handlePreview(dashboard: Dashboard) {
  router.push(`/chatbi/dashboard/${dashboard.id}/edit`)
}

onMounted(loadDashboards)
</script>

<style scoped>
.dashboard-main {
  gap: 20px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.8fr);
  gap: 20px;
}

.hero-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(237, 245, 255, 0.94));
}

.hero-copy__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--cb-primary);
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 12px 0 14px;
  font-size: 30px;
  color: var(--cb-indigo);
}

.hero-copy p {
  margin: 0;
  color: var(--cb-text-regular);
  line-height: 1.8;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.hero-metric {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(129, 157, 219, 0.12);
}

.hero-metric span {
  display: block;
  margin-bottom: 8px;
  color: var(--cb-text-secondary);
  font-size: 13px;
}

.hero-metric strong {
  font-size: 26px;
  color: var(--cb-indigo);
}

.hero-side-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  color: var(--cb-text-primary);
  font-weight: 600;
}

.suggestion-list {
  display: grid;
  gap: 12px;
}

.suggestion-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--cb-bg-hover);
}

.suggestion-item strong {
  display: block;
  margin-bottom: 6px;
  color: var(--cb-indigo);
}

.suggestion-item span {
  color: var(--cb-text-regular);
  line-height: 1.7;
  font-size: 13px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
  padding: 18px;
  min-height: 180px;
}

.dashboard-card {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(129, 157, 219, 0.14);
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--cb-shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dashboard-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--cb-shadow-md);
}

.dashboard-card__cover {
  position: relative;
  min-height: 170px;
  background: linear-gradient(135deg, #234a9f, #3f7bff 58%, #1cb5a7);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 14px;
  padding: 18px;
  color: rgba(255, 255, 255, 0.96);
}

.cover-placeholder .el-icon {
  font-size: 34px;
}

.cover-placeholder span {
  font-size: 18px;
  font-weight: 600;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.36);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.dashboard-card:hover .cover-mask {
  opacity: 1;
}

.dashboard-card__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

.dashboard-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.dashboard-card__head strong {
  color: var(--cb-text-primary);
  font-size: 18px;
}

.dashboard-card__body p {
  min-height: 44px;
  color: var(--cb-text-regular);
  line-height: 1.7;
  font-size: 13px;
}

.dashboard-card__meta,
.dashboard-card__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.dashboard-card__meta {
  color: var(--cb-text-secondary);
  font-size: 12px;
}

@media (max-width: 960px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .hero-copy h2 {
    font-size: 24px;
  }
}

@media (max-width: 640px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
    padding: 14px;
  }
}
</style>
