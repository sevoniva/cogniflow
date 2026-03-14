<template>
  <div class="permission-page">
    <Card padding="lg" shadow="md" class="hero-card">
      <div class="hero-content">
        <div>
          <div class="hero-eyebrow">Security Governance Center</div>
          <h2>统一管理数据访问、脱敏与告警策略</h2>
          <p>
            基于真实后端配置汇总当前平台的数据安全状态，支持快速进入权限、脱敏、告警和数据源配置页面。
          </p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="router.push('/admin/data-permission')">数据权限</el-button>
          <el-button plain @click="router.push('/admin/data-masking')">数据脱敏</el-button>
          <el-button plain @click="router.push('/admin/alert-rule')">告警规则</el-button>
        </div>
      </div>
    </Card>

    <div class="metric-grid">
      <Card v-for="item in overviewCards" :key="item.title" padding="lg" hoverable class="metric-card">
        <div class="metric-icon" :style="{ background: item.bgColor, color: item.color }">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="metric-info">
          <span>{{ item.title }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.desc }}</p>
        </div>
      </Card>
    </div>

    <div class="content-grid">
      <Card padding="none" class="panel-card">
        <template #header>
          <span class="card-title">
            <el-icon><Lock /></el-icon>
            治理入口
          </span>
        </template>

        <div class="action-list">
          <button class="action-item" type="button" @click="router.push('/admin/data-permission')">
            <div>
              <strong>数据权限规则</strong>
              <span>维护行级过滤规则，控制不同角色和用户可见的数据范围。</span>
            </div>
            <el-tag size="small" type="primary" effect="plain">{{ dataPermissionCount }} 条</el-tag>
          </button>
          <button class="action-item" type="button" @click="router.push('/admin/data-masking')">
            <div>
              <strong>数据脱敏规则</strong>
              <span>保护手机号、邮箱、证件等敏感字段，支持隐藏、部分脱敏与加密。</span>
            </div>
            <el-tag size="small" type="warning" effect="plain">{{ dataMaskingCount }} 条</el-tag>
          </button>
          <button class="action-item" type="button" @click="router.push('/admin/alert-rule')">
            <div>
              <strong>告警规则配置</strong>
              <span>围绕阈值、波动和异常检测建立主动预警机制。</span>
            </div>
            <el-tag size="small" type="danger" effect="plain">{{ alertRuleCount }} 条</el-tag>
          </button>
          <button class="action-item" type="button" @click="router.push('/admin/datasource')">
            <div>
              <strong>数据源巡检</strong>
              <span>检查数据源连通性、启用状态和接入规模。</span>
            </div>
            <el-tag size="small" type="success" effect="plain">{{ datasourceCount }} 个</el-tag>
          </button>
        </div>
      </Card>

      <Card padding="none" class="panel-card">
        <template #header>
          <span class="card-title">
            <el-icon><DocumentChecked /></el-icon>
            平台建议
          </span>
        </template>

        <div class="insight-list">
          <div class="insight-item">
            <strong>{{ dataPermissionCount > 0 ? '权限规则已建立' : '缺少权限规则' }}</strong>
            <p>
              {{ dataPermissionCount > 0
                ? `当前已配置 ${dataPermissionCount} 条数据权限规则，建议检查是否覆盖核心业务表。`
                : '建议优先为关键业务表配置最小权限访问策略。' }}
            </p>
          </div>
          <div class="insight-item">
            <strong>{{ dataMaskingCount > 0 ? '敏感字段已有保护' : '敏感字段未配置脱敏' }}</strong>
            <p>
              {{ dataMaskingCount > 0
                ? `当前已配置 ${dataMaskingCount} 条脱敏规则，可继续覆盖手机号、邮箱、证件号等字段。`
                : '建议为客户资料、员工信息等敏感字段补齐脱敏策略。' }}
            </p>
          </div>
          <div class="insight-item">
            <strong>{{ alertRuleCount > 0 ? '告警链路已接入' : '告警规则数量不足' }}</strong>
            <p>
              {{ alertRuleCount > 0
                ? `当前已配置 ${alertRuleCount} 条告警规则，建议覆盖核心经营指标和关键数据源。`
                : '建议为销售额、毛利率和数据源健康状态添加基础告警。' }}
            </p>
          </div>
        </div>
      </Card>
    </div>

    <Card padding="none" class="panel-card">
      <template #header>
        <span class="card-title">
          <el-icon><DocumentChecked /></el-icon>
          策略审计检索
        </span>
      </template>

      <div class="audit-toolbar">
        <el-input
          v-model.trim="auditFilters.keyword"
          clearable
          placeholder="检索操作人/动作/资源类型/异常信息"
          style="width: 320px"
        />
        <el-select v-model="auditFilters.resourceType" clearable placeholder="资源类型" style="width: 220px">
          <el-option v-for="item in resourceTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="auditFilters.result" clearable placeholder="结果" style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button @click="loadAuditLogs">刷新</el-button>
      </div>

      <el-table :data="auditRows" stripe border size="small" class="audit-table">
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" min-width="110" />
        <el-table-column prop="action" label="动作" min-width="220" show-overflow-tooltip />
        <el-table-column prop="resourceType" label="资源类型" min-width="180" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="摘要" min-width="320" show-overflow-tooltip />
      </el-table>
      <div class="audit-pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="auditTotal"
          :page-size="auditFilters.size"
          :current-page="auditFilters.current"
          @current-change="handleAuditPageChange"
        />
      </div>
    </Card>

    <Card padding="none" class="panel-card">
      <template #header>
        <span class="card-title">
          <el-icon><DocumentChecked /></el-icon>
          策略版本历史
        </span>
      </template>

      <div class="audit-toolbar">
        <el-select v-model="policyVersionFilters.scope" clearable placeholder="策略范围" style="width: 220px">
          <el-option v-for="item in policyScopeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="policyVersionFilters.limit" style="width: 120px">
          <el-option label="20 条" :value="20" />
          <el-option label="50 条" :value="50" />
          <el-option label="100 条" :value="100" />
        </el-select>
        <el-button @click="loadPolicyVersions">刷新版本</el-button>
      </div>

      <el-table :data="policyVersionRows" stripe border size="small" class="audit-table">
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.timestamp ? new Date(row.timestamp).toISOString() : '') }}
          </template>
        </el-table-column>
        <el-table-column prop="scope" label="范围" min-width="160" />
        <el-table-column prop="version" label="版本" min-width="90" />
        <el-table-column prop="operation" label="操作" min-width="110" />
        <el-table-column prop="operator" label="操作人" min-width="110" />
      </el-table>
    </Card>

    <Card padding="none" class="panel-card">
      <template #header>
        <span class="card-title">
          <el-icon><Warning /></el-icon>
          越权告警闭环
        </span>
      </template>

      <div class="audit-toolbar">
        <el-select v-model="accessAlertFilters.status" clearable placeholder="状态" style="width: 140px">
          <el-option v-for="item in accessAlertStatusOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="accessAlertFilters.severity" clearable placeholder="级别" style="width: 140px">
          <el-option v-for="item in accessAlertSeverityOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="accessAlertFilters.reason" clearable filterable placeholder="拦截原因" style="width: 280px">
          <el-option v-for="item in accessAlertReasonOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="accessAlertFilters.limit" style="width: 120px">
          <el-option label="20 条" :value="20" />
          <el-option label="50 条" :value="50" />
          <el-option label="100 条" :value="100" />
        </el-select>
        <el-button @click="loadAccessAlerts">刷新告警</el-button>
      </div>

      <el-table :data="accessAlertRows" stripe border size="small" class="audit-table">
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.timestamp ? new Date(row.timestamp).toISOString() : '') }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="触发人" min-width="110" />
        <el-table-column prop="scene" label="场景" min-width="120" />
        <el-table-column prop="severity" label="级别" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'CRITICAL' ? 'danger' : row.severity === 'HIGH' ? 'warning' : 'info'" effect="plain">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACKED' ? 'success' : 'danger'" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="拦截原因" min-width="320" show-overflow-tooltip />
        <el-table-column prop="queryText" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'ACKED'"
              link
              type="primary"
              @click="acknowledgeAccessAlert(row.id)"
            >
              确认
            </el-button>
            <span v-else class="text-secondary">已确认</span>
          </template>
        </el-table-column>
      </el-table>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Coin,
  Connection,
  DataLine,
  DocumentChecked,
  Lock,
  Warning
} from '@element-plus/icons-vue'
import Card from '@/components/Card.vue'
import { adminService } from '@/adapters'
import { extractRecords, formatDateTime, request } from '@/utils/http'

const router = useRouter()

const metricCount = ref(0)
const synonymCount = ref(0)
const datasourceCount = ref(0)
const dataPermissionCount = ref(0)
const dataMaskingCount = ref(0)
const alertRuleCount = ref(0)
const auditRows = ref<Array<{
  id: number
  createdAt: string
  username: string
  action: string
  resourceType: string
  result: string
  errorMessage: string
}>>([])
const auditTotal = ref(0)
const resourceTypeOptions = ref<string[]>([])
const policyScopeOptions = ref<string[]>([])
const accessAlertStatusOptions = ref<string[]>([])
const accessAlertSeverityOptions = ref<string[]>([])
const accessAlertReasonOptions = ref<string[]>([])
const policyVersionRows = ref<Array<{
  id: number
  timestamp: number
  scope: string
  version: number
  operation: string
  operator: string
}>>([])
const accessAlertRows = ref<Array<{
  id: number
  timestamp: number
  username: string
  status: string
  severity: string
  scene: string
  reason: string
  queryText: string
}>>([])
const auditFilters = reactive({
  current: 1,
  size: 8,
  resourceType: '',
  result: '',
  keyword: ''
})
const policyVersionFilters = reactive({
  scope: '',
  limit: 20
})
const accessAlertFilters = reactive({
  status: '',
  severity: '',
  reason: '',
  limit: 20
})

const overviewCards = computed(() => [
  {
    title: '活跃指标',
    value: metricCount.value,
    desc: '参与查询和治理配置的核心业务指标数量。',
    icon: DataLine,
    bgColor: 'var(--cb-primary-light)',
    color: 'var(--cb-primary)'
  },
  {
    title: '语义词库',
    value: synonymCount.value,
    desc: '已建立的标准词与业务别名映射关系。',
    icon: Connection,
    bgColor: 'var(--cb-success-light)',
    color: 'var(--cb-success)'
  },
  {
    title: '在线数据源',
    value: datasourceCount.value,
    desc: '当前纳入治理视图的数据源数量。',
    icon: Coin,
    bgColor: 'var(--cb-info-light)',
    color: 'var(--cb-info)'
  },
  {
    title: '告警规则',
    value: alertRuleCount.value,
    desc: '用于主动发现指标波动与异常的规则。',
    icon: Warning,
    bgColor: 'var(--cb-danger-light)',
    color: 'var(--cb-danger)'
  }
])

async function loadSummary() {
  const [metrics, synonyms, datasources, permissionRules, maskingRules, alertRules] = await Promise.all([
    adminService.getMetrics(),
    adminService.getSynonyms(),
    adminService.getDataSources(),
    request('/data-permissions'),
    request('/data-masking'),
    request('/alert-rules')
  ])

  metricCount.value = metrics.filter(item => item.status === 'active').length
  synonymCount.value = synonyms.length
  datasourceCount.value = datasources.length
  dataPermissionCount.value = permissionRules.success ? extractRecords(permissionRules.data).length : 0
  dataMaskingCount.value = maskingRules.success ? extractRecords(maskingRules.data).length : 0
  alertRuleCount.value = alertRules.success ? extractRecords(alertRules.data).length : 0
}

async function loadAuditResourceTypes() {
  const response = await request<string[]>('/audit/logs/options?limit=100')
  if (!response.success || !response.data) {
    return
  }
  resourceTypeOptions.value = response.data
}

async function loadPolicyScopeOptions() {
  const response = await request<string[]>('/audit/policy-versions/options?limit=100')
  if (!response.success || !response.data) {
    return
  }
  policyScopeOptions.value = response.data
}

async function loadAuditLogs() {
  const params = new URLSearchParams()
  params.set('current', String(auditFilters.current))
  params.set('size', String(auditFilters.size))
  if (auditFilters.resourceType) {
    params.set('resourceType', auditFilters.resourceType)
  }
  if (auditFilters.result) {
    params.set('result', auditFilters.result)
  }
  if (auditFilters.keyword) {
    params.set('keyword', auditFilters.keyword)
  }
  const response = await request<{ total: number; records: any[] }>(`/audit/logs?${params.toString()}`)
  if (!response.success || !response.data) {
    ElMessage.error(response.error || '加载审计日志失败')
    return
  }
  auditRows.value = Array.isArray(response.data.records) ? response.data.records : []
  auditTotal.value = typeof response.data.total === 'number' ? response.data.total : 0
}

async function loadPolicyVersions() {
  const params = new URLSearchParams()
  params.set('limit', String(policyVersionFilters.limit))
  if (policyVersionFilters.scope) {
    params.set('scope', policyVersionFilters.scope)
  }
  const response = await request<any[]>(`/audit/policy-versions?${params.toString()}`)
  if (!response.success || !response.data) {
    ElMessage.error(response.error || '加载策略版本失败')
    return
  }
  policyVersionRows.value = Array.isArray(response.data) ? response.data : []
}

async function loadAccessAlertOptions() {
  const response = await request<{ statuses: string[]; severities: string[]; reasons: string[] }>('/audit/access-alerts/options?limit=100')
  if (!response.success || !response.data) {
    return
  }
  accessAlertStatusOptions.value = Array.isArray(response.data.statuses) ? response.data.statuses : []
  accessAlertSeverityOptions.value = Array.isArray(response.data.severities) ? response.data.severities : []
  accessAlertReasonOptions.value = Array.isArray(response.data.reasons) ? response.data.reasons : []
}

async function loadAccessAlerts() {
  const params = new URLSearchParams()
  params.set('limit', String(accessAlertFilters.limit))
  if (accessAlertFilters.status) {
    params.set('status', accessAlertFilters.status)
  }
  if (accessAlertFilters.severity) {
    params.set('severity', accessAlertFilters.severity)
  }
  if (accessAlertFilters.reason) {
    params.set('keyword', accessAlertFilters.reason)
  }
  const response = await request<any[]>(`/audit/access-alerts?${params.toString()}`)
  if (!response.success || !response.data) {
    ElMessage.error(response.error || '加载越权告警失败')
    return
  }
  accessAlertRows.value = Array.isArray(response.data) ? response.data : []
}

async function acknowledgeAccessAlert(id: number) {
  const response = await request(`/audit/access-alerts/${id}/ack?operator=admin`, {
    method: 'PUT'
  })
  if (!response.success) {
    ElMessage.error(response.error || '确认告警失败')
    return
  }
  ElMessage.success('告警已确认')
  await loadAccessAlerts()
}

function handleAuditPageChange(page: number) {
  auditFilters.current = page
  loadAuditLogs()
}

onMounted(async () => {
  await Promise.all([
    loadSummary(),
    loadAuditResourceTypes(),
    loadAuditLogs(),
    loadPolicyScopeOptions(),
    loadPolicyVersions(),
    loadAccessAlertOptions(),
    loadAccessAlerts()
  ])
})
</script>

<style scoped>
.permission-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(238, 245, 255, 0.94));
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.hero-eyebrow {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--cb-primary);
}

.hero-content h2 {
  margin: 10px 0 12px;
  color: var(--cb-indigo);
  font-size: 30px;
}

.hero-content p {
  margin: 0;
  max-width: 720px;
  color: var(--cb-text-regular);
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.metric-info span,
.metric-info strong,
.metric-info p {
  display: block;
}

.metric-info span {
  color: var(--cb-text-secondary);
  font-size: 13px;
}

.metric-info strong {
  margin: 6px 0;
  font-size: 28px;
  color: var(--cb-indigo);
}

.metric-info p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--cb-text-regular);
}

.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 24px;
}

.audit-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px 20px 0;
}

.audit-table {
  margin-top: 12px;
}

.audit-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 20px 18px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--cb-indigo);
}

.action-list,
.insight-list {
  display: flex;
  flex-direction: column;
}

.action-item,
.insight-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(129, 157, 219, 0.12);
  background: transparent;
  text-align: left;
}

.action-item {
  border: 0;
  cursor: pointer;
  transition: background 0.2s ease;
}

.action-item + .action-item,
.insight-item + .insight-item {
  border-top: 1px solid rgba(129, 157, 219, 0.12);
}

.action-item:hover {
  background: rgba(47, 107, 255, 0.04);
}

.action-item strong,
.insight-item strong {
  color: var(--cb-text-primary);
  font-size: 15px;
}

.action-item span,
.insight-item p {
  display: block;
  margin-top: 8px;
  color: var(--cb-text-regular);
  line-height: 1.7;
  font-size: 13px;
}

.insight-item {
  display: block;
}

.insight-item p {
  margin-bottom: 0;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .action-item {
    flex-direction: column;
  }
}
</style>
