import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 用户侧页面 - 懒加载
const QueryHome = () => import('@/views/chatbi/query/index.vue')
const QueryResult = () => import('@/views/chatbi/result/index.vue')
const QueryHistory = () => import('@/views/chatbi/history/index.vue')
const QueryFavorite = () => import('@/views/chatbi/favorite/index.vue')
const VisualQuery = () => import('@/views/chatbi/visual-query/index.vue')

// 管理侧页面 - 懒加载
const AdminLayout = () => import('@/views/admin/layout/index.vue')
const AdminMetric = () => import('@/views/admin/metric/index.vue')
const AdminSemantic = () => import('@/views/admin/semantic/index.vue')
const AdminPermission = () => import('@/views/admin/permission/index.vue')
const AdminAi = () => import('@/views/admin/ai/index.vue')
const AdminDatasource = () => import('@/views/admin/datasource/index.vue')
const AdminSynonym = () => import('@/views/admin/synonym/index.vue')
const AdminSubscription = () => import('@/views/admin/subscription/index.vue')
const AdminShare = () => import('@/views/admin/share/index.vue')
const DashboardList = () => import('@/views/chatbi/dashboard/index.vue')
const DashboardEditor = () => import('@/views/chatbi/dashboard/editor.vue')
const DataPermission = () => import('@/views/chatbi/data-permission/index.vue')
const DataMasking = () => import('@/views/chatbi/data-masking/index.vue')
const AlertRule = () => import('@/views/chatbi/alert-rule/index.vue')
const AdvancedCharts = () => import('@/views/chatbi/advanced-charts/index.vue')
const ChartMarket = () => import('@/views/chatbi/advanced-charts/index.vue')
const EmbedDashboard = () => import('@/views/chatbi/embed/index.vue')
const AlertDashboard = () => import('@/views/chatbi/alert-dashboard/index.vue')
const AgileDashboard = () => import('@/views/AgileDashboard.vue')
const SalesDashboard = () => import('@/views/SalesDashboard.vue')
const OperationDashboard = () => import('@/views/OperationDashboard.vue')
const HomePage = () => import('@/views/HomePage.vue')
const LoginPage = () => import('@/views/LoginPage.vue')
const ConversationQuery = () => import('@/views/ConversationQuery.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'HomePage',
      component: HomePage,
      meta: { title: '首页' }
    },
    {
      path: '/login',
      name: 'Login',
      component: LoginPage,
      meta: { title: '登录', public: true }
    },
    // 用户侧
    {
      path: '/chatbi/query',
      name: 'QueryHome',
      component: QueryHome,
      meta: { title: '智能查询' }
    },
    {
      path: '/chatbi/conversation',
      name: 'ConversationQuery',
      component: ConversationQuery,
      meta: { title: 'AI对话查询' }
    },
    {
      path: '/chatbi/result',
      name: 'QueryResult',
      component: QueryResult,
      meta: { title: '查询结果' }
    },
    {
      path: '/chatbi/history',
      name: 'QueryHistory',
      component: QueryHistory,
      meta: { title: '查询历史' }
    },
    {
      path: '/chatbi/favorite',
      name: 'QueryFavorite',
      component: QueryFavorite,
      meta: { title: '我的收藏' }
    },
    {
      path: '/chatbi/visual-query',
      name: 'VisualQuery',
      component: VisualQuery,
      meta: { title: '可视化查询' }
    },
    {
      path: '/chatbi/advanced-charts',
      name: 'AdvancedCharts',
      component: AdvancedCharts,
      meta: { title: '高级图表' }
    },
    {
      path: '/chatbi/chart-market',
      name: 'ChartMarket',
      component: ChartMarket,
      meta: { title: '图表应用市场' }
    },
    {
      path: '/embed/:token',
      name: 'EmbedDashboard',
      component: EmbedDashboard,
      meta: { title: '嵌入仪表板' }
    },
    {
      path: '/chatbi/alert-dashboard',
      name: 'AlertDashboard',
      component: AlertDashboard,
      meta: { title: '异常告警' }
    },
    {
      path: '/agile-dashboard',
      name: 'AgileDashboard',
      component: AgileDashboard,
      meta: { title: '敏捷研发管理' }
    },
    {
      path: '/sales-dashboard',
      name: 'SalesDashboard',
      component: SalesDashboard,
      meta: { title: '销售分析' }
    },
    {
      path: '/operation-dashboard',
      name: 'OperationDashboard',
      component: OperationDashboard,
      meta: { title: '运营分析' }
    },
    // 仪表板管理
    {
      path: '/chatbi/dashboard',
      name: 'DashboardList',
      component: DashboardList,
      meta: { title: '仪表板管理' }
    },
    {
      path: '/chatbi/dashboard/:id/edit',
      name: 'DashboardEditor',
      component: DashboardEditor,
      meta: { title: '仪表板编辑' }
    },
    // 管理侧
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/metric',
      children: [
        {
          path: 'metric',
          component: AdminMetric,
          meta: { title: '指标管理' }
        },
        {
          path: 'semantic',
          component: AdminSemantic,
          meta: { title: '语义配置' }
        },
        {
          path: 'synonym',
          component: AdminSynonym,
          meta: { title: '同义词管理' }
        },
        {
          path: 'ai',
          component: AdminAi,
          meta: { title: 'AI 设置' }
        },
        {
          path: 'datasource',
          component: AdminDatasource,
          meta: { title: '数据源管理' }
        },
        {
          path: 'subscription',
          component: AdminSubscription,
          meta: { title: '订阅管理' }
        },
        {
          path: 'share',
          component: AdminShare,
          meta: { title: '分享管理' }
        },
        {
          path: 'permission',
          component: AdminPermission,
          meta: { title: '权限配置' }
        },
        {
          path: 'data-permission',
          component: DataPermission,
          meta: { title: '数据权限' }
        },
        {
          path: 'data-masking',
          component: DataMasking,
          meta: { title: '数据脱敏' }
        },
        {
          path: 'alert-rule',
          component: AlertRule,
          meta: { title: '告警规则' }
        }
      ]
    }
  ]
})

// 路由守卫 - 认证 + 页面标题
router.beforeEach((to, _from, next) => {
  const title = to.meta.title as string
  document.title = title ? `${title} - Chat BI` : 'Chat BI'

  const userStore = useUserStore()
  const isPublic = to.meta.public === true

  if (!isPublic && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && userStore.isLoggedIn) {
    next({ name: 'HomePage' })
  } else {
    next()
  }
})

export default router
