<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <button class="brand" type="button" @click="router.push('/admin')">
        <div class="brand-mark">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="brand-copy">
          <strong>Chat BI</strong>
          <span>企业数据管理中枢</span>
        </div>
      </button>

      <section class="workspace-card">
        <div class="workspace-card__meta">
          <span class="workspace-card__eyebrow">Workspace</span>
          <el-tag size="small" type="success" effect="plain">真实 API</el-tag>
        </div>
        <h3>后台配置与 AI 运行已联通</h3>
        <p>指标、同义词、数据源和 AI 设置共用统一持久化数据，修改后可直接影响前台查询与对话链路。</p>
        <div class="workspace-card__tags">
          <el-tag size="small" effect="plain">MySQL 持久化</el-tag>
          <el-tag size="small" effect="plain">Element Plus</el-tag>
          <el-tag size="small" effect="plain">Kimi / 语义双链路</el-tag>
        </div>
      </section>

      <div class="menu-groups">
        <section v-for="group in menuGroups" :key="group.title" class="menu-group">
          <div class="menu-group__title">{{ group.title }}</div>
          <el-menu
            :default-active="activeMenu"
            class="admin-menu"
            background-color="transparent"
            text-color="var(--cb-text-regular)"
            active-text-color="var(--cb-primary)"
            router
          >
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </el-menu>
        </section>
      </div>

      <div class="sidebar-footer">
        <el-button plain class="footer-btn" @click="router.push('/chatbi/query')">
          <el-icon><HomeFilled /></el-icon>
          前台查询
        </el-button>
        <el-button type="primary" class="footer-btn" @click="router.push('/chatbi/conversation')">
          <el-icon><ChatDotRound /></el-icon>
          AI 对话
        </el-button>
      </div>
    </aside>

    <div class="main-container">
      <header class="admin-header">
        <div class="header-copy">
          <div class="header-copy__icon">
            <el-icon><component :is="currentIcon" /></el-icon>
          </div>
          <div>
            <div class="header-copy__eyebrow">Admin Console</div>
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageDesc }}</p>
          </div>
        </div>
        <div class="header-actions">
          <el-tag type="success" effect="plain">配置实时生效</el-tag>
          <el-tag type="info" effect="plain">统一后台框架</el-tag>
        </div>
      </header>

      <main class="admin-content">
        <section class="page-intro">
          <div>
            <strong>{{ pageTitle }}</strong>
            <span>{{ pageDesc }}</span>
          </div>
          <div class="page-intro__actions">
            <el-button link type="primary" @click="router.push('/chatbi/query')">查看前台效果</el-button>
            <el-button link @click="router.push('/chatbi/conversation')">去 AI 对话验证</el-button>
          </div>
        </section>
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  Coin,
  Collection,
  Connection,
  DataAnalysis,
  DataLine,
  HomeFilled,
  Key,
  Lock,
  Share,
  Warning
} from '@element-plus/icons-vue'
import { adminService } from '@/adapters'

interface MenuItem {
  path: string
  title: string
  desc: string
  icon: any
}

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

const menuGroups = [
  {
    title: '语义与指标',
    items: [
      { path: '/admin/metric', title: '指标管理', desc: '配置查询指标，管理业务指标体系', icon: DataLine },
      { path: '/admin/semantic', title: '语义配置', desc: '配置同义词和规则模板，优化查询理解', icon: Connection },
      { path: '/admin/synonym', title: '同义词管理', desc: '配置业务术语同义词，提升查询识别率', icon: Collection },
      { path: '/admin/ai', title: 'AI 设置', desc: '查看外部模型运行状态，核对 Kimi / OpenAI 是否已启用', icon: Connection }
    ]
  },
  {
    title: '平台治理',
    items: [
      { path: '/admin/datasource', title: '数据源管理', desc: '管理数据库连接，支持多种数据库类型', icon: Coin },
      { path: '/admin/permission', title: '权限配置', desc: '管理指标可见范围和数据访问权限', icon: Lock },
      { path: '/admin/data-permission', title: '数据权限', desc: '配置行级数据权限，控制数据访问范围', icon: Lock },
      { path: '/admin/data-masking', title: '数据脱敏', desc: '配置敏感数据脱敏规则，保护数据安全', icon: Key },
      { path: '/admin/alert-rule', title: '告警规则', desc: '配置阈值告警和异常检测规则', icon: Warning }
    ]
  },
  {
    title: '协同分发',
    items: [
      { path: '/admin/subscription', title: '订阅管理', desc: '配置数据订阅，支持邮件、钉钉、企微推送', icon: Bell },
      { path: '/admin/share', title: '分享管理', desc: '管理分享链接，生成分享码供他人访问', icon: Share }
    ]
  }
] as const satisfies ReadonlyArray<{ title: string; items: ReadonlyArray<MenuItem> }>

const menuConfig = Object.fromEntries(menuGroups.flatMap(group => group.items.map(item => [item.path, item]))) as Record<string, MenuItem>

const currentMenu = computed(() => menuConfig[route.path] || {
  path: '/admin',
  title: '管理后台',
  desc: '统一维护指标、语义、数据源和 AI 运行配置。',
  icon: DataAnalysis
})
const pageTitle = computed(() => currentMenu.value.title)
const pageDesc = computed(() => currentMenu.value.desc)
const currentIcon = computed(() => currentMenu.value.icon)

onMounted(async () => {
  await adminService.initData()
})
</script>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(47, 107, 255, 0.14), transparent 26%),
    linear-gradient(180deg, #f5f8fd 0%, #eef3fb 100%);
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100vh;
  padding: 22px 18px 18px;
  background: rgba(255, 255, 255, 0.8);
  border-right: 1px solid rgba(129, 157, 219, 0.14);
  backdrop-filter: blur(18px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.12), rgba(20, 184, 166, 0.08));
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--cb-primary), #4e7dff);
  color: #fff;
  font-size: 22px;
  box-shadow: var(--cb-shadow-sm);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 18px;
  color: var(--cb-indigo);
}

.brand-copy span {
  font-size: 12px;
  color: var(--cb-text-secondary);
}

.workspace-card {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(242, 247, 255, 0.92));
  border: 1px solid rgba(129, 157, 219, 0.16);
  box-shadow: var(--cb-shadow-sm);
}

.workspace-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.workspace-card__eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--cb-primary);
}

.workspace-card h3 {
  margin: 14px 0 10px;
  color: var(--cb-indigo);
  font-size: 18px;
}

.workspace-card p {
  margin: 0;
  color: var(--cb-text-regular);
  line-height: 1.7;
  font-size: 13px;
}

.workspace-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.menu-groups {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-right: 4px;
}

.menu-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-group__title {
  padding: 0 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--cb-text-secondary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-menu {
  border-right: none;
}

.admin-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 3px 0;
  border-radius: 14px;
  color: var(--cb-text-regular);
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(47, 107, 255, 0.08);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: rgba(47, 107, 255, 0.12);
  color: var(--cb-primary);
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
}

.sidebar-footer {
  display: grid;
  gap: 10px;
}

.footer-btn {
  width: 100%;
  justify-content: flex-start;
}

.main-container {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 24px 32px 20px;
  background: rgba(243, 246, 251, 0.88);
  backdrop-filter: blur(16px);
}

.header-copy {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-copy__icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(47, 107, 255, 0.14), rgba(20, 184, 166, 0.1));
  color: var(--cb-primary);
  font-size: 22px;
}

.header-copy__eyebrow {
  font-size: 11px;
  font-weight: 700;
  color: var(--cb-primary);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.header-copy h1 {
  margin: 6px 0 8px;
  font-size: 28px;
  color: var(--cb-indigo);
}

.header-copy p {
  margin: 0;
  color: var(--cb-text-regular);
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.admin-content {
  padding: 0 32px 32px;
}

.page-intro {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 18px 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(129, 157, 219, 0.14);
  border-radius: 18px;
  box-shadow: var(--cb-shadow-sm);
}

.page-intro strong {
  display: block;
  margin-bottom: 6px;
  color: var(--cb-text-primary);
}

.page-intro span {
  color: var(--cb-text-secondary);
  font-size: 13px;
}

.page-intro__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .admin-layout {
    grid-template-columns: 260px 1fr;
  }
}

@media (max-width: 960px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: relative;
    height: auto;
  }

  .admin-header,
  .admin-content {
    padding-left: 18px;
    padding-right: 18px;
  }
}

@media (max-width: 640px) {
  .admin-header,
  .page-intro {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-copy h1 {
    font-size: 22px;
  }
}
</style>
