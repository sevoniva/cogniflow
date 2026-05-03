<template>
  <div class="home-page">
    <div class="hero-section">
      <h1 class="hero-title">ChatBI 智能数据分析平台</h1>
      <p class="hero-subtitle">用自然语言提问，自动生成结构化结果、多视角图表与管理洞察</p>

      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="例如：本月华东大区销售额与毛利率趋势"
          size="large"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </template>
        </el-input>
      </div>

      <div class="hero-actions">
        <el-button type="primary" size="large" @click="handleSearch">
          <el-icon><Search /></el-icon>
          立即查询
        </el-button>
        <el-button plain size="large" @click="router.push('/chatbi/conversation')">
          <el-icon><Service /></el-icon>
          AI 对话分析
        </el-button>
        <el-button plain size="large" @click="router.push('/chatbi/chart-market')">
          <el-icon><DataAnalysis /></el-icon>
          图表应用市场
        </el-button>
      </div>

      <AiRuntimeBanner
        class="hero-ai-banner"
        :status="aiStatus"
        compact
        description="首页、查询页和对话页都使用同一套 AI 运行状态提示。若未启用外部模型，系统仍会基于真实业务数据、指标和同义词提供可用分析。"
      >
        <template #actions>
          <el-button type="primary" plain @click="router.push('/chatbi/conversation')">
            进入对话
          </el-button>
          <el-button plain @click="router.push('/admin/ai')">
            AI 设置
          </el-button>
          <el-button plain @click="router.push('/admin/semantic')">
            管理同义词
          </el-button>
        </template>
      </AiRuntimeBanner>

      <div class="hero-stats">
        <div class="hero-stat">
          <span>活跃指标</span>
          <strong>{{ platformStats.metricCount }}</strong>
        </div>
        <div class="hero-stat">
          <span>在线数据源</span>
          <strong>{{ platformStats.datasourceCount }}</strong>
        </div>
        <div class="hero-stat">
          <span>查询历史</span>
          <strong>{{ platformStats.historyCount }}</strong>
        </div>
        <div class="hero-stat">
          <span>个人收藏</span>
          <strong>{{ platformStats.favoriteCount }}</strong>
        </div>
      </div>

      <div class="hero-guide">
        <div class="guide-card">
          <span class="guide-card__step">01</span>
          <strong>先输入业务问题</strong>
          <p>直接问“本月核心指标”或“核心指标按维度对比”，系统会先匹配真实业务指标和同义词。</p>
        </div>
        <div class="guide-card">
          <span class="guide-card__step">02</span>
          <strong>查看结果和图表</strong>
          <p>结果页会自动输出趋势、对比、占比和健康度视图，图表和表格都基于后端真实数据。</p>
        </div>
        <div class="guide-card">
          <span class="guide-card__step">03</span>
          <strong>继续用 AI 追问</strong>
          <p>进入 AI 对话后，可以继续问“增长趋势如何”“哪个地区贡献最大”。</p>
        </div>
      </div>

      <div class="example-queries">
        <span class="example-queries__label">可直接点击体验</span>
        <div class="example-queries__list">
          <button type="button" class="example-query" @click="fillExample('本月核心指标')">本月核心指标</button>
          <button type="button" class="example-query" @click="fillExample('核心指标按维度对比')">核心指标按维度对比</button>
          <button type="button" class="example-query" @click="fillExample('核心指标按团队对比')">核心指标按团队对比</button>
          <button type="button" class="example-query" @click="router.push('/chatbi/conversation?q=' + encodeURIComponent('先给我一个数据概览'))">数据概览 AI 分析</button>
        </div>
      </div>
    </div>

    <div class="scenarios-section">
      <h2 class="section-title">业务场景</h2>
      <el-row :gutter="20">
        <el-col :span="8" v-for="scenario in scenarios" :key="scenario.id">
          <el-card shadow="hover" class="scenario-card" @click="goToScenario(scenario)">
            <div class="scenario-content">
              <div class="scenario-icon" :style="{ background: scenario.color }">
                <el-icon :size="40">
                  <component :is="scenario.icon" />
                </el-icon>
              </div>
              <div class="scenario-info">
                <h3>{{ scenario.name }}</h3>
                <p>{{ scenario.description }}</p>
                <div class="scenario-stats">
                  <span>{{ scenario.tag }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="reports-section">
      <h2 class="section-title">推荐指标</h2>
      <el-row :gutter="20">
        <el-col :span="6" v-for="report in recommendedReports" :key="report.id">
          <el-card shadow="hover" class="report-card" @click="goToReport(report)">
            <div class="report-icon" :style="{ background: report.color }">
              <el-icon :size="32">
                <component :is="report.icon" />
              </el-icon>
            </div>
            <h4>{{ report.name }}</h4>
            <p class="report-desc">{{ report.definition }}</p>
            <div class="report-meta">
              <span><el-icon><DataLine /></el-icon> {{ report.statusText }}</span>
              <span><el-icon><Clock /></el-icon> {{ report.updatedAt }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!recommendedReports.length" description="暂无可推荐指标，请先在管理后台配置指标" />
    </div>

    <div class="quick-actions">
      <h2 class="section-title">快速入口</h2>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :xl="4">
          <el-card shadow="hover" class="action-card" @click="$router.push('/chatbi/conversation')">
            <el-icon :size="48" color="var(--el-color-primary)"><Service /></el-icon>
            <h4>AI 对话分析</h4>
            <p>连续追问，保留上下文</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :xl="4">
          <el-card shadow="hover" class="action-card" @click="$router.push('/chatbi/query')">
            <el-icon :size="48" color="var(--el-color-primary)"><ChatDotRound /></el-icon>
            <h4>智能查询</h4>
            <p>使用自然语言提问</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :xl="4">
          <el-card shadow="hover" class="action-card" @click="$router.push('/chatbi/history')">
            <el-icon :size="48" color="var(--el-color-success)"><Clock /></el-icon>
            <h4>查询历史</h4>
            <p>查看最近执行记录</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :xl="4">
          <el-card shadow="hover" class="action-card" @click="$router.push('/chatbi/favorite')">
            <el-icon :size="48" color="var(--el-color-warning)"><StarFilled /></el-icon>
            <h4>我的收藏</h4>
            <p>管理收藏的查询与指标</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :xl="4">
          <el-card shadow="hover" class="action-card" @click="$router.push('/admin/datasource')">
            <el-icon :size="48" color="var(--el-color-danger)"><Setting /></el-icon>
            <h4>数据源管理</h4>
            <p>配置数据源连接</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :xl="4">
          <el-card shadow="hover" class="action-card" @click="$router.push('/chatbi/chart-market')">
            <el-icon :size="48" color="var(--el-color-primary)"><DataAnalysis /></el-icon>
            <h4>图表应用市场</h4>
            <p>浏览并应用全部图表类型</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { markRaw, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Avatar,
  Box,
  ChatDotRound,
  Clock,
  Coin,
  DataAnalysis,
  DataLine,
  Search,
  Service,
  Setting,
  StarFilled,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import AiRuntimeBanner from '@/components/AiRuntimeBanner.vue'
import { adminService, chatbiService } from '@/adapters'
import type { AiRuntimeStatus, Metric } from '@/types'
import { request } from '@/utils/http'

const router = useRouter()
const searchQuery = ref('')
const activeMetrics = ref<Metric[]>([])
const platformStats = reactive({
  metricCount: 0,
  datasourceCount: 0,
  historyCount: 0,
  favoriteCount: 0
})
const aiStatus = ref<AiRuntimeStatus>({
  mode: 'semantic',
  enabled: false,
  runtimeEnabled: false,
  reason: '正在检测 AI 运行状态',
  defaultProvider: 'kimi',
  providerName: 'Kimi',
  model: null
})

const iconPool = [markRaw(TrendCharts), markRaw(User), markRaw(DataAnalysis), markRaw(Coin)]
const colorPool = [
  'linear-gradient(135deg, #1f5eff 0%, #51a6ff 100%)',
  'linear-gradient(135deg, #0ea5a4 0%, #4ade80 100%)',
  'linear-gradient(135deg, #f59e0b 0%, #f97316 100%)',
  'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)'
]

const scenarios = [
  {
    id: 1,
    name: '销售分析',
    description: '核心指标、维度、趋势等数据分析',
    icon: TrendCharts,
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    tag: '经营增长',
    route: '/sales-dashboard'
  },
  {
    id: 2,
    name: '运营分析',
    description: '用户活跃、留存、转化等运营数据分析',
    icon: User,
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    tag: '用户经营',
    route: '/operation-dashboard'
  },
  {
    id: 3,
    name: '敏捷研发',
    description: '迭代进度、需求、缺陷等研发数据分析',
    icon: DataAnalysis,
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    tag: '研发效能',
    route: '/agile-dashboard'
  },
  {
    id: 4,
    name: '财务分析',
    description: '收入、成本、利润等财务数据分析',
    icon: Coin,
    color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    tag: '财务管控',
    route: '/chatbi/query'
  },
  {
    id: 5,
    name: '人力资源',
    description: '员工、招聘、绩效等人力资源数据分析',
    icon: Avatar,
    color: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    tag: '组织洞察',
    route: '/chatbi/query'
  },
  {
    id: 6,
    name: '供应链',
    description: '库存、采购、物流等供应链数据分析',
    icon: Box,
    color: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
    tag: '供应协同',
    route: '/chatbi/query'
  }
]

const recommendedReports = ref<Array<Metric & { icon: any; color: string; statusText: string }>>([])

async function loadHomeData() {
  const [metrics, datasources, histories, favorites, aiStatusResponse] = await Promise.all([
    adminService.getMetrics(),
    adminService.getDataSources(),
    chatbiService.getRecentQueries(),
    chatbiService.getFavorites(),
    request<AiRuntimeStatus>('/ai-model/status')
  ])

  activeMetrics.value = metrics.filter(item => item.status === 'active')
  platformStats.metricCount = activeMetrics.value.length
  platformStats.datasourceCount = datasources.length
  platformStats.historyCount = histories.length
  platformStats.favoriteCount = favorites.length

  recommendedReports.value = activeMetrics.value.slice(0, 4).map((metric, index) => ({
    ...metric,
    icon: iconPool[index % iconPool.length],
    color: colorPool[index % colorPool.length],
    statusText: metric.status === 'active' ? '实时可查' : '未启用'
  }))

  if (aiStatusResponse.success && aiStatusResponse.data) {
    aiStatus.value = aiStatusResponse.data
  }
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({
      path: '/chatbi/result',
      query: { q: encodeURIComponent(searchQuery.value.trim()) }
    })
  }
}

const fillExample = (text: string) => {
  searchQuery.value = text
}

const goToScenario = (scenario: { route: string }) => {
  router.push(scenario.route)
}

const goToReport = (report: Metric) => {
  router.push({
    path: '/chatbi/result',
    query: { q: `本月${report.name}` }
  })
}

onMounted(loadHomeData)
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4fb 0%, #ffffff 100%);
  padding: 40px 80px;
}

.hero-section {
  text-align: center;
  padding: 60px 0;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 20px;
  color: var(--el-text-color-regular);
  margin-bottom: 40px;
}

.search-box {
  max-width: 800px;
  margin: 0 auto 28px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 28px;
}

.hero-ai-banner {
  max-width: 1080px;
  margin: 0 auto 28px;
  text-align: left;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  max-width: 960px;
  margin: 0 auto;
}

.hero-guide {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  max-width: 1080px;
  margin: 28px auto 0;
}

.guide-card {
  text-align: left;
  padding: 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(129, 157, 219, 0.16);
  box-shadow: var(--cb-shadow-sm);
}

.guide-card__step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.12), rgba(20, 184, 166, 0.18));
  color: var(--cb-primary);
  font-size: 13px;
  font-weight: 700;
}

.guide-card strong {
  display: block;
  margin-top: 16px;
  font-size: 18px;
  color: var(--cb-indigo);
}

.guide-card p {
  margin: 10px 0 0;
  color: var(--el-text-color-regular);
  line-height: 1.7;
}

.example-queries {
  max-width: 1080px;
  margin: 20px auto 0;
  text-align: left;
}

.example-queries__label {
  display: block;
  margin-bottom: 12px;
  color: var(--cb-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.example-queries__list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.example-query {
  padding: 11px 16px;
  border: 1px solid rgba(47, 107, 255, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--cb-text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--cb-shadow-sm);
}

.example-query:hover {
  color: var(--cb-primary);
  border-color: rgba(47, 107, 255, 0.34);
  transform: translateY(-1px);
}

.hero-stat {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(129, 157, 219, 0.16);
  box-shadow: var(--cb-shadow-sm);
}

.hero-stat span,
.hero-stat strong {
  display: block;
}

.hero-stat span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.hero-stat strong {
  margin-top: 8px;
  font-size: 28px;
  color: var(--cb-indigo);
}

.scenarios-section,
.reports-section,
.quick-actions {
  margin-top: 60px;
}

.section-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 24px;
}

.scenario-card,
.report-card,
.action-card {
  cursor: pointer;
  transition: all 0.3s;
  height: 100%;
  border: none;
  box-shadow: var(--cb-shadow-sm);
}

.scenario-card:hover,
.report-card:hover,
.action-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--cb-shadow-md);
}

.scenario-content {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.scenario-icon,
.report-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.scenario-info h3 {
  font-size: 20px;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.scenario-info p,
.report-desc,
.action-card p {
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.scenario-stats {
  margin-top: 12px;
  color: var(--cb-primary);
  font-size: 13px;
  font-weight: 600;
}

.report-card {
  text-align: center;
}

.report-icon {
  margin: 0 auto 20px;
}

.report-card h4,
.action-card h4 {
  font-size: 18px;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}

.report-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.report-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.action-card {
  text-align: center;
  padding: 20px;
}

.action-card .el-icon {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .home-page {
    padding: 20px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-guide {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-ai-banner {
    margin-bottom: 24px;
  }

  .el-col {
    width: 100%;
    margin-bottom: 20px;
  }

  .scenario-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}
</style>
