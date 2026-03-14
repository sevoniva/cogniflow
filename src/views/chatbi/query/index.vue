<template>
  <div class="query-page cb-page">
    <PageHeader subtitle="智能数据查询" max-width="1120px">
      <template #actions>
        <el-button link @click="router.push('/chatbi/favorite')">
          <el-icon><Star /></el-icon>
          <span class="btn-text">收藏</span>
        </el-button>
        <el-button link @click="router.push('/chatbi/history')">
          <el-icon><Clock /></el-icon>
          <span class="btn-text">历史</span>
        </el-button>
        <el-button link type="primary" @click="router.push('/chatbi/conversation')">
          <el-icon><ChatDotRound /></el-icon>
          <span class="btn-text">AI对话</span>
        </el-button>
        <el-button link @click="router.push('/chatbi/chart-market')">
          <el-icon><Grid /></el-icon>
          <span class="btn-text">图表市场</span>
        </el-button>
        <el-divider direction="vertical" />
        <el-button link type="primary" @click="router.push('/admin')">
          <el-icon><Setting /></el-icon>
          <span class="btn-text">管理</span>
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-content query-main" style="max-width: 1120px; padding-top: 40px;">
      <section class="hero-grid">
        <Card padding="lg" shadow="md" class="hero-card">
          <div class="search-core">
            <div class="eyebrow">Enterprise Insight Workspace</div>
            <div class="search-heading">
              <h2>经营分析、趋势洞察、报表问答一次完成</h2>
              <p>输入自然语言，系统自动匹配指标、识别时间维度并生成结构化结果与多视角图表，支持 AI 对话追问。</p>
            </div>

            <AiRuntimeBanner
              compact
              :status="aiStatus"
              description="推荐问法优先覆盖已配置业务指标；若外部模型未启用，系统会继续基于真实 MySQL 业务数据和同义词执行查询。"
            />

            <div class="search-input-row">
              <el-input
                v-model="queryText"
                class="search-input"
                placeholder="例如：本月华东大区销售额与毛利率趋势"
                maxlength="100"
                clearable
                size="large"
                :prefix-icon="Search"
                @keydown.enter="handleSubmit"
              />
              <el-button
                type="primary"
                size="large"
                :disabled="!queryText.trim()"
                :loading="isLoading"
                @click="handleSubmit"
              >
                <el-icon class="btn-icon"><Search /></el-icon>
                查询
              </el-button>
              <el-button plain size="large" @click="router.push('/chatbi/conversation')">
                <el-icon class="btn-icon"><ChatDotRound /></el-icon>
                AI 对话
              </el-button>
              <el-button plain size="large" @click="router.push('/chatbi/chart-market')">
                <el-icon class="btn-icon"><Grid /></el-icon>
                图表市场
              </el-button>
            </div>

            <div class="hero-metrics">
              <div class="hero-stat">
                <span class="hero-stat__label">活跃指标</span>
                <strong>{{ activeMetrics.length }}</strong>
              </div>
              <div class="hero-stat">
                <span class="hero-stat__label">热门问题</span>
                <strong>{{ hotQueries.length }}</strong>
              </div>
              <div class="hero-stat">
                <span class="hero-stat__label">查询历史</span>
                <strong>{{ recentList.length }}</strong>
              </div>
            </div>

            <div class="quick-tags">
              <span class="tags-label">推荐问法</span>
              <div class="tags-list">
                <el-tag
                  v-for="ex in examples"
                  :key="ex"
                  class="quick-tag"
                  effect="plain"
                  size="large"
                  @click="queryText = ex"
                >
                  {{ ex }}
                </el-tag>
              </div>
            </div>
          </div>
        </Card>

        <Card padding="lg" class="ops-card">
          <div class="ops-card__header">
            <span>分析场景模板</span>
            <el-tag size="small" type="primary" effect="plain">4 套</el-tag>
          </div>
          <div class="scenario-list">
            <button class="scenario-item" type="button" @click="queryText = '本月销售额、毛利率、回款额综合分析'">
              <strong>经营日报</strong>
              <span>适合总览销售、利润、回款进度与异常波动。</span>
            </button>
            <button class="scenario-item" type="button" @click="queryText = '本季度客户投诉量按区域分布及环比变化'">
              <strong>客户体验</strong>
              <span>聚焦投诉热点、区域波动与主要问题归因。</span>
            </button>
            <button class="scenario-item" type="button" @click="queryText = '研发工时利用率按团队对比并输出效率排名'">
              <strong>研发效能</strong>
              <span>适合观察团队产能与资源利用率。</span>
            </button>
            <button class="scenario-item" type="button" @click="queryText = '审批平均时长按部门拆解并标记风险流程'">
              <strong>流程治理</strong>
              <span>用于识别流程堵点、审批时效风险和责任部门。</span>
            </button>
          </div>
        </Card>
      </section>

      <section class="content-grid">
        <Card padding="none" hoverable class="panel-card">
          <template #header>
            <span class="card-title">
              <el-icon><TrendCharts /></el-icon>
              指标看板
            </span>
            <el-tag size="small" effect="plain">{{ activeMetrics.length }} 个活跃指标</el-tag>
          </template>

          <div class="metric-board" v-if="activeMetrics.length">
            <div
              v-for="metric in activeMetrics.slice(0, 6)"
              :key="metric.id"
              class="metric-board__item"
              @click="queryText = `本月${metric.name}`"
            >
              <div class="metric-board__icon">
                <el-icon><DataLine /></el-icon>
              </div>
              <strong>{{ metric.name }}</strong>
              <span>{{ metric.definition }}</span>
            </div>
          </div>

          <EmptyState
            v-else
            type="chart"
            title="暂无可用指标"
            description="请在管理后台配置指标后，即可发起查询"
          >
            <el-button type="primary" @click="router.push('/admin/metric')">
              前往配置
            </el-button>
          </EmptyState>
        </Card>

        <div class="side-stack">
          <Card v-if="recentList.length" padding="none" hoverable class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><Clock /></el-icon>
                最近查询
              </span>
              <el-button v-if="hasRealHistory" link type="danger" size="small" @click="clearRecent">
                <el-icon><Delete /></el-icon>
                清空
              </el-button>
            </template>

            <ListItem
              v-for="item in recentList.slice(0, 5)"
              :key="item.id"
              :title="item.name"
              :meta="item.createdAt"
              clickable
              @click="useRecent(item.text)"
            >
              <template #icon>
                <el-icon><Clock /></el-icon>
              </template>
            </ListItem>
          </Card>

          <Card padding="none" hoverable class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><TrendCharts /></el-icon>
                热门分析
              </span>
            </template>

            <template v-if="hotQueries.length">
              <ListItem
                v-for="item in hotQueries"
                :key="item.id"
                :title="item.text"
                :meta="item.count"
                clickable
                @click="queryText = item.text"
              >
                <template #icon>
                  <div class="metric-icon">
                    <el-icon><DataLine /></el-icon>
                  </div>
                </template>
              </ListItem>
            </template>

            <EmptyState
              v-else
              type="chart"
              title="暂无可用指标"
              description="请在管理后台配置指标后，即可发起查询"
            >
              <el-button type="primary" @click="router.push('/admin/metric')">
                前往配置
              </el-button>
            </EmptyState>
          </Card>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Star,
  ChatDotRound,
  Grid,
  Clock,
  Setting,
  Search,
  Delete,
  DataLine,
  TrendCharts
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import AiRuntimeBanner from '@/components/AiRuntimeBanner.vue'
import Card from '@/components/Card.vue'
import ListItem from '@/components/ListItem.vue'
import EmptyState from '@/components/EmptyState.vue'
import { chatbiService } from '@/adapters'
import type { AiRuntimeStatus, QueryItem, Metric } from '@/types'
import { request } from '@/utils/http'

const router = useRouter()
const route = useRoute()

const queryText = ref('')
const isLoading = ref(false)
const recentList = ref<QueryItem[]>([])
const hasRealHistory = ref(false)
const hotQueries = ref<{ id: number; text: string; count: string }[]>([])
const aiStatus = ref<AiRuntimeStatus>({
  mode: 'semantic',
  enabled: false,
  runtimeEnabled: false,
  reason: '正在检测 AI 运行状态',
  defaultProvider: 'kimi',
  providerName: 'Kimi',
  model: null
})

const examples = [
  '先给我一个经营总览',
  '本月销售额是多少？',
  '库存周转天数按仓库对比',
  '本季度客户投诉量按区域分布'
]
const activeMetrics = ref<Metric[]>([])

async function loadActiveMetrics() {
  activeMetrics.value = await chatbiService.getActiveMetrics()
}

async function loadRecent() {
  recentList.value = await chatbiService.getRecentQueries()
  hasRealHistory.value = recentList.value.length > 0
}

async function loadHotQueries() {
  hotQueries.value = await chatbiService.getHotQueries()
}

async function loadAiStatus() {
  const response = await request<AiRuntimeStatus>('/ai-model/status')
  if (response.success && response.data) {
    aiStatus.value = response.data
  }
}

async function handleSubmit() {
  const text = queryText.value.trim()
  if (!text) {
    ElMessage.warning('请输入查询内容')
    return
  }
  if (text.length > 100) {
    ElMessage.error('查询语句过长，请简化后重试')
    return
  }

  if (activeMetrics.value.length === 0) {
    await loadActiveMetrics()
    if (activeMetrics.value.length === 0) {
      ElMessage.warning('当前无可用指标，请前往管理后台配置')
      return
    }
  }

  try {
    isLoading.value = true
    router.push({
      path: '/chatbi/result',
      query: { q: encodeURIComponent(text) }
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    isLoading.value = false
  }
}

function useRecent(text: string) {
  queryText.value = text
  handleSubmit()
}

function clearRecent() {
  ElMessageBox.confirm('确定清空最近查询记录？', '清空确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(() => {
    chatbiService.clearRecentQueries()
    loadRecent()
    ElMessage.success('已清空最近查询')
  }).catch(() => {})
}

onMounted(async () => {
  await Promise.all([loadRecent(), loadHotQueries(), loadActiveMetrics(), loadAiStatus()])
  const back = route.query.backQuery as string
  if (back) {
    queryText.value = decodeURIComponent(back)
    router.replace({ path: '/chatbi/query' })
  }
})
</script>

<style scoped>
.query-main {
  gap: 24px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.8fr);
  gap: 24px;
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-card {
  position: relative;
  overflow: hidden;
}

.hero-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.08), rgba(20, 184, 166, 0.08));
  pointer-events: none;
}

.search-core {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--cb-primary);
  font-weight: 700;
}

.search-heading h2 {
  margin: 0;
  font-size: 36px;
  line-height: 1.15;
  color: var(--cb-indigo);
}

.search-heading p {
  margin: 12px 0 0;
  color: var(--cb-text-regular);
  font-size: 15px;
  line-height: 1.8;
  max-width: 760px;
}

.search-input-row {
  display: flex;
  gap: 16px;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  min-height: 58px;
  padding: 6px 12px 6px 16px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-stat {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(129, 157, 219, 0.16);
}

.hero-stat__label {
  display: block;
  font-size: 12px;
  color: var(--cb-text-secondary);
  margin-bottom: 8px;
}

.hero-stat strong {
  font-size: 24px;
  color: var(--cb-indigo);
}

.quick-tags {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tags-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--cb-text-secondary);
}

.tags-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-tag {
  cursor: pointer;
  border-radius: 999px;
}

.ops-card {
  background: var(--cb-bg-dark-panel);
  color: #fff;
}

.ops-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: 600;
}

.scenario-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.scenario-item {
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 18px;
  padding: 16px 18px;
  text-align: left;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.scenario-item:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
}

.scenario-item strong,
.scenario-item span {
  display: block;
}

.scenario-item span {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.72);
}

.panel-card {
  min-height: 100%;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--cb-indigo);
  font-weight: 700;
}

.metric-board {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 20px;
}

.metric-board__item {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  border: 1px solid rgba(129, 157, 219, 0.14);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.metric-board__item:hover {
  transform: translateY(-3px);
  box-shadow: var(--cb-shadow-sm);
}

.metric-board__item strong,
.metric-board__item span {
  display: block;
}

.metric-board__item strong {
  margin: 10px 0 8px;
  color: var(--cb-indigo);
}

.metric-board__item span {
  color: var(--cb-text-secondary);
  line-height: 1.6;
  font-size: 13px;
}

.metric-board__icon,
.metric-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--cb-primary-light);
  color: var(--cb-primary);
}

.btn-icon {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .hero-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .search-heading h2 {
    font-size: 28px;
  }

  .search-input-row,
  .hero-metrics,
  .metric-board {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .search-input-row .el-button {
    width: 100%;
    height: 48px;
  }

  .btn-text {
    display: none;
  }
}
</style>
