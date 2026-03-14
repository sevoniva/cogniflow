<template>
  <div class="visual-query-page cb-page">
    <PageHeader subtitle="可视化查询工作台" max-width="1320px" show-back back-path="/chatbi/query">
      <template #actions>
        <el-button link @click="router.push('/chatbi/dashboard')">
          <el-icon><DataBoard /></el-icon>
          <span class="btn-text">仪表板</span>
        </el-button>
        <el-button type="primary" :loading="executing" @click="executeQuery">
          <el-icon><Search /></el-icon>
          执行查询
        </el-button>
      </template>
    </PageHeader>

    <main class="cb-page-container cb-page-container--wide cb-content visual-main">
      <section class="hero-grid">
        <Card padding="lg" shadow="md" class="hero-card">
          <div class="hero-copy">
            <div class="hero-copy__eyebrow">Visual SQL Builder</div>
            <h2>拖拽思维构建查询，用真实数据校验字段与 SQL</h2>
            <p>选择数据源、表、字段、过滤和聚合条件，系统将生成安全 SQL 并直接查询当前后端真实数据源，结果可继续沉淀到仪表板中。</p>
          </div>
          <div class="hero-metrics">
            <div class="hero-metric">
              <span>可用数据源</span>
              <strong>{{ datasources.length }}</strong>
            </div>
            <div class="hero-metric">
              <span>当前数据表</span>
              <strong>{{ tables.length }}</strong>
            </div>
            <div class="hero-metric">
              <span>查询结果行数</span>
              <strong>{{ queryResult.length }}</strong>
            </div>
          </div>
        </Card>

        <Card padding="lg" class="guide-card">
          <div class="guide-card__header">
            <span>使用建议</span>
            <el-tag size="small" type="primary" effect="plain">真实链路</el-tag>
          </div>
          <div class="guide-list">
            <div class="guide-item">
              <strong>优先选维度 + 度量</strong>
              <span>有维度时系统会自动补 `GROUP BY`，更适合做经营分析。</span>
            </div>
            <div class="guide-item">
              <strong>执行前先看 SQL</strong>
              <span>SQL 预览区会即时展示拼接结果，便于审查安全性和可读性。</span>
            </div>
            <div class="guide-item">
              <strong>结果可继续沉淀</strong>
              <span>确认 SQL 合理后，可将相同逻辑复制到仪表板编辑器中做持续展示。</span>
            </div>
          </div>
        </Card>
      </section>

      <el-alert
        v-if="error"
        :title="error"
        type="error"
        closable
        @close="error = ''"
      />

      <section class="builder-layout">
        <div class="builder-panel">
          <Card padding="lg" class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><Coin /></el-icon>
                数据源与表
              </span>
            </template>
            <div class="panel-stack">
              <el-select v-model="selectedDatasource" placeholder="选择数据源" @change="onDatasourceChange">
                <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
              <el-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables.length" @change="onTableChange">
                <el-option v-for="table in tables" :key="table" :label="table" :value="table" />
              </el-select>
              <div class="field-legend">
                <span>字段总数 {{ availableFields.length }}</span>
                <span>数值字段 {{ numericFields.length }}</span>
              </div>
            </div>
          </Card>

          <Card padding="lg" class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><Checked /></el-icon>
                字段选择
              </span>
            </template>
            <div v-if="availableFields.length" class="field-list">
              <el-checkbox-group v-model="selectedFields">
                <label v-for="field in availableFields" :key="field.name" class="field-item">
                  <el-checkbox :label="field.name">
                    <span class="field-item__name">{{ field.name }}</span>
                  </el-checkbox>
                  <el-tag size="small" :type="field.type === 'NUMBER' ? 'warning' : 'info'" effect="plain">
                    {{ field.type }}
                  </el-tag>
                </label>
              </el-checkbox-group>
            </div>
            <EmptyState
              v-else
              type="data"
              title="暂无字段"
              description="请先选择数据源与数据表"
            />
          </Card>

          <Card padding="lg" class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><Filter /></el-icon>
                过滤条件
              </span>
              <el-button link type="primary" @click="addFilter">
                <el-icon><Plus /></el-icon>
                添加
              </el-button>
            </template>
            <div class="stack-list">
              <div v-for="(filter, index) in filters" :key="index" class="stack-item">
                <el-select v-model="filter.field" placeholder="字段">
                  <el-option v-for="field in availableFields" :key="field.name" :label="field.name" :value="field.name" />
                </el-select>
                <el-select v-model="filter.operator" placeholder="操作符" class="operator-select">
                  <el-option label="=" value="=" />
                  <el-option label="!=" value="!=" />
                  <el-option label=">" value=">" />
                  <el-option label=">=" value=">=" />
                  <el-option label="<" value="<" />
                  <el-option label="<=" value="<=" />
                  <el-option label="LIKE" value="LIKE" />
                  <el-option label="IN" value="IN" />
                  <el-option label="IS NULL" value="IS NULL" />
                  <el-option label="IS NOT NULL" value="IS NOT NULL" />
                </el-select>
                <el-input
                  v-model="filter.value"
                  placeholder="值"
                  :disabled="filter.operator === 'IS NULL' || filter.operator === 'IS NOT NULL'"
                />
                <el-button link type="danger" @click="removeFilter(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <EmptyState v-if="!filters.length" type="document" title="暂无过滤条件" description="如需限定时间、区域或状态，可添加条件。" />
            </div>
          </Card>

          <Card padding="lg" class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><DataAnalysis /></el-icon>
                分组与聚合
              </span>
            </template>
            <div class="panel-stack">
              <el-select v-model="groupByFields" multiple collapse-tags collapse-tags-tooltip placeholder="选择分组字段">
                <el-option v-for="field in availableFields" :key="field.name" :label="field.name" :value="field.name" />
              </el-select>
              <div class="stack-list compact">
                <div v-for="(agg, index) in aggregations" :key="index" class="stack-item">
                  <el-select v-model="agg.field" placeholder="字段">
                    <el-option v-for="field in numericFields" :key="field.name" :label="field.name" :value="field.name" />
                  </el-select>
                  <el-select v-model="agg.function" placeholder="函数" class="operator-select">
                    <el-option label="SUM" value="SUM" />
                    <el-option label="AVG" value="AVG" />
                    <el-option label="COUNT" value="COUNT" />
                    <el-option label="MAX" value="MAX" />
                    <el-option label="MIN" value="MIN" />
                  </el-select>
                  <el-button link type="danger" @click="removeAggregation(index)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <el-button plain @click="addAggregation">
                <el-icon><Plus /></el-icon>
                添加聚合
              </el-button>
            </div>
          </Card>

          <Card padding="lg" class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><Sort /></el-icon>
                排序与限制
              </span>
            </template>
            <div class="panel-stack">
              <el-select v-model="sortField" clearable placeholder="排序字段">
                <el-option v-for="field in availableFields" :key="field.name" :label="field.name" :value="field.name" />
              </el-select>
              <el-select v-model="sortOrder" placeholder="排序方式">
                <el-option label="升序 ASC" value="ASC" />
                <el-option label="降序 DESC" value="DESC" />
              </el-select>
              <el-input-number v-model="limit" :min="1" :max="1000" :step="10" style="width: 100%" />
            </div>
          </Card>
        </div>

        <div class="preview-panel">
          <Card padding="lg" class="panel-card">
            <template #header>
              <span class="card-title">
                <el-icon><Document /></el-icon>
                SQL 预览
              </span>
              <el-button link type="primary" @click="generateSql">
                <el-icon><Refresh /></el-icon>
                生成 SQL
              </el-button>
            </template>
            <div class="sql-preview"><pre><code>{{ sqlPreview || '-- 请先完成字段配置，再生成 SQL' }}</code></pre></div>
          </Card>

          <Card padding="none" class="panel-card result-card">
            <template #header>
              <span class="card-title">
                <el-icon><Grid /></el-icon>
                查询结果
              </span>
              <el-tag size="small" effect="plain">{{ queryResult.length }} 行</el-tag>
            </template>
            <el-table :data="queryResult" max-height="520" v-loading="executing" style="width: 100%">
              <el-table-column v-for="col in resultColumns" :key="col" :prop="col" :label="col" min-width="120" show-overflow-tooltip />
            </el-table>
            <EmptyState
              v-if="!queryResult.length && !executing"
              type="chart"
              title="暂无查询结果"
              description="点击右上角执行查询，结果会展示在这里。"
            />
          </Card>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Checked,
  Coin,
  DataAnalysis,
  DataBoard,
  Delete,
  Document,
  Filter,
  Grid,
  Plus,
  Refresh,
  Search,
  Sort
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adminService } from '@/adapters'
import { request } from '@/utils/http'
import type { DataSource } from '@/types'

interface Field {
  name: string
  type: string
}

interface FilterItem {
  field: string
  operator: string
  value: string
}

interface Aggregation {
  field: string
  function: string
}

interface TableInfo {
  name: string
  remarks?: string
}

const router = useRouter()
const datasources = ref<DataSource[]>([])
const tables = ref<string[]>([])
const availableFields = ref<Field[]>([])

const selectedDatasource = ref<number>()
const selectedTable = ref('')
const selectedFields = ref<string[]>([])
const filters = ref<FilterItem[]>([])
const groupByFields = ref<string[]>([])
const aggregations = ref<Aggregation[]>([])
const sortField = ref('')
const sortOrder = ref<'ASC' | 'DESC'>('ASC')
const limit = ref(100)
const sqlPreview = ref('')
const queryResult = ref<Record<string, unknown>[]>([])
const resultColumns = ref<string[]>([])
const executing = ref(false)
const error = ref('')

const numericFields = computed(() => availableFields.value.filter(field => field.type === 'NUMBER'))

function normalizeFieldType(type?: string) {
  const upper = (type || '').toUpperCase()
  return /INT|DECIMAL|DOUBLE|FLOAT|NUMBER|NUMERIC|BIGINT/.test(upper) ? 'NUMBER' : 'STRING'
}

function pickPreferredTable(list: string[]) {
  const priorities = ['sales_order', 'sales_summary', 'customer', 'inventory', 'service_ticket', 'agile_project']
  return priorities.find(item => list.includes(item)) || list[0]
}

async function loadDatasources() {
  error.value = ''
  datasources.value = await adminService.getDataSources()
  if (datasources.value.length > 0) {
    selectedDatasource.value = datasources.value[0].id
    await onDatasourceChange()
  }
}

async function onDatasourceChange() {
  if (!selectedDatasource.value) return
  resetQueryState()
  const response = await request<TableInfo[]>(`/datasources/${selectedDatasource.value}/tables`)
  if (!response.success) {
    error.value = response.error || '加载数据表失败'
    tables.value = []
    return
  }
  tables.value = (response.data || []).map(item => item.name)
  if (tables.value.length > 0) {
    selectedTable.value = pickPreferredTable(tables.value)
    await onTableChange()
  }
}

async function onTableChange() {
  if (!selectedDatasource.value || !selectedTable.value) return
  resetQueryState(false)
  const response = await request<Array<{ name: string; type: string }>>(`/datasources/${selectedDatasource.value}/columns?table=${encodeURIComponent(selectedTable.value)}`)
  if (!response.success) {
    error.value = response.error || '加载字段失败'
    availableFields.value = []
    return
  }
  availableFields.value = (response.data || []).map(field => ({
    name: field.name,
    type: normalizeFieldType(field.type)
  }))
}

function resetQueryState(resetTable = true) {
  if (resetTable) selectedTable.value = ''
  selectedFields.value = []
  filters.value = []
  groupByFields.value = []
  aggregations.value = []
  sortField.value = ''
  sqlPreview.value = ''
  queryResult.value = []
  resultColumns.value = []
}

function addFilter() {
  filters.value.push({ field: '', operator: '=', value: '' })
}

function removeFilter(index: number) {
  filters.value.splice(index, 1)
}

function addAggregation() {
  aggregations.value.push({ field: '', function: 'SUM' })
}

function removeAggregation(index: number) {
  aggregations.value.splice(index, 1)
}

function quoteValue(value: string) {
  return `'${String(value).replace(/'/g, "''")}'`
}

async function ensureQueryContextReady() {
  if (!selectedDatasource.value && datasources.value.length > 0) {
    selectedDatasource.value = datasources.value[0].id
  }

  if (!selectedDatasource.value) {
    await loadDatasources()
  }

  if (!selectedDatasource.value) {
    ElMessage.warning('请先选择数据源')
    return false
  }

  if (!selectedTable.value) {
    if (!tables.value.length) {
      await onDatasourceChange()
    }
    if (tables.value.length > 0) {
      selectedTable.value = pickPreferredTable(tables.value)
      await onTableChange()
    }
  }

  if (!selectedTable.value) {
    ElMessage.warning('请先选择数据表')
    return false
  }

  return true
}

function buildWhereClause() {
  const conditions = filters.value
    .filter(filter => filter.field && filter.operator)
    .map(filter => {
      if (filter.operator === 'IS NULL' || filter.operator === 'IS NOT NULL') {
        return `${filter.field} ${filter.operator}`
      }
      if (filter.operator === 'LIKE') {
        return `${filter.field} LIKE ${quoteValue(`%${filter.value}%`)}`
      }
      if (filter.operator === 'IN') {
        return `${filter.field} IN (${filter.value})`
      }
      return `${filter.field} ${filter.operator} ${quoteValue(filter.value)}`
    })

  return conditions.length ? `WHERE ${conditions.join(' AND ')}` : ''
}

async function generateSql() {
  const contextReady = await ensureQueryContextReady()
  if (!contextReady) {
    return ''
  }

  const selectParts: string[] = []
  if (aggregations.value.length > 0) {
    selectParts.push(...groupByFields.value)
    aggregations.value.forEach(agg => {
      if (agg.field) {
        selectParts.push(`${agg.function}(${agg.field}) AS ${agg.function}_${agg.field}`)
      }
    })
  } else if (selectedFields.value.length > 0) {
    selectParts.push(...selectedFields.value)
  } else {
    selectParts.push('*')
  }

  const clauses = [
    `SELECT ${selectParts.join(', ')}`,
    `FROM ${selectedTable.value}`,
    buildWhereClause()
  ].filter(Boolean)

  if (groupByFields.value.length > 0 && aggregations.value.length > 0) {
    clauses.push(`GROUP BY ${groupByFields.value.join(', ')}`)
  }

  if (sortField.value) {
    clauses.push(`ORDER BY ${sortField.value} ${sortOrder.value}`)
  }

  clauses.push(`LIMIT ${limit.value}`)
  sqlPreview.value = clauses.join('\n')
  return sqlPreview.value
}

async function executeQuery() {
  error.value = ''
  const sql = await generateSql()
  if (!sql || !selectedDatasource.value) {
    return
  }

  executing.value = true
  const response = await request<{ records?: Record<string, unknown>[]; total?: number }>('/query/execute', {
    method: 'POST',
    body: JSON.stringify({
      datasourceId: selectedDatasource.value,
      sql
    })
  })
  executing.value = false

  if (!response.success) {
    error.value = response.error || '查询失败'
    ElMessage.error(error.value)
    return
  }

  queryResult.value = response.data?.records || []
  resultColumns.value = queryResult.value.length ? Object.keys(queryResult.value[0]) : []
  ElMessage.success(`查询成功，共 ${queryResult.value.length} 条结果`)
}

onMounted(loadDatasources)
</script>

<style scoped>
.visual-main {
  gap: 20px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.8fr);
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
  color: var(--cb-indigo);
  font-size: 30px;
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
  border: 1px solid rgba(129, 157, 219, 0.12);
  background: rgba(255, 255, 255, 0.84);
}

.hero-metric span {
  display: block;
  margin-bottom: 8px;
  color: var(--cb-text-secondary);
  font-size: 13px;
}

.hero-metric strong {
  color: var(--cb-indigo);
  font-size: 26px;
}

.guide-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  color: var(--cb-text-primary);
  font-weight: 600;
}

.guide-list {
  display: grid;
  gap: 12px;
}

.guide-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--cb-bg-hover);
}

.guide-item strong {
  display: block;
  margin-bottom: 6px;
  color: var(--cb-indigo);
}

.guide-item span {
  color: var(--cb-text-regular);
  font-size: 13px;
  line-height: 1.7;
}

.builder-layout {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 20px;
}

.builder-panel,
.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-card :deep(.cb-card-header) {
  gap: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--cb-text-primary);
  font-weight: 600;
}

.panel-stack,
.stack-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stack-list.compact {
  gap: 10px;
}

.stack-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.operator-select {
  width: 120px;
}

.field-list {
  display: grid;
  gap: 10px;
}

.field-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--cb-bg-hover);
}

.field-item__name {
  color: var(--cb-text-primary);
}

.field-legend {
  display: flex;
  justify-content: space-between;
  color: var(--cb-text-secondary);
  font-size: 12px;
}

.sql-preview {
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #182848, #243a63);
  color: #ecf3ff;
  min-height: 240px;
  overflow: auto;
}

.sql-preview pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.8;
}

.result-card :deep(.cb-card-body) {
  padding: 0;
}

.result-card :deep(.el-empty) {
  padding: 48px 0;
}

@media (max-width: 1100px) {
  .hero-grid,
  .builder-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-copy h2 {
    font-size: 24px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .stack-item {
    grid-template-columns: 1fr;
  }

  .operator-select {
    width: 100%;
  }
}
</style>
