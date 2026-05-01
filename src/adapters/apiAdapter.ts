import type {
  ApiResponse,
  DataSource,
  DataSourceRequest,
  HotQuery,
  Metric,
  MetricRequest,
  QueryItem,
  QueryRequest,
  QueryResult,
  RuleTemplate,
  Share,
  ShareRequest,
  Subscription,
  SubscriptionRequest,
  Synonym,
  SynonymRequest
} from '@/types'
import type { IAdminService, IChatbiService } from '@/services'
import {
  extractRecords,
  formatDateTime,
  normalizeBoolean,
  request,
  slugifyCode
} from '@/utils/http'
import { useUserStore } from '@/stores/user'

function getCurrentUserId(): number {
  try {
    const store = useUserStore()
    return store.userInfo?.id ?? 1
  } catch {
    return 1
  }
}

function getCurrentUsername(): string {
  try {
    const store = useUserStore()
    return store.userInfo?.username ?? 'admin'
  } catch {
    return 'admin'
  }
}
const MAX_RECENT = 10
const PAGE_SIZE = 100

interface QueryHistoryEntity {
  id: number
  userId?: number
  username?: string
  queryName?: string
  queryType?: string
  queryContent?: string
  datasourceId?: number
  resultData?: string
  duration?: number
  status?: string
  errorMsg?: string
  isFavorite?: boolean
  createdAt?: string
  updatedAt?: string
}

function shortenText(text: string, max = 30): string {
  return text.length > max ? `${text.slice(0, max)}...` : text
}

function mapQueryHistory(item: QueryHistoryEntity): QueryItem {
  const text = item.queryContent || ''
  return {
    id: Number(item.id),
    name: item.queryName || shortenText(text),
    text,
    createdAt: formatDateTime(item.createdAt)
  }
}

function mapMetric(item: any): Metric {
  const rawStatus = String(item?.status ?? 'inactive').toLowerCase()
  return {
    id: Number(item.id),
    code: item.code || '',
    name: item.name || '',
    definition: item.definition || '',
    status: rawStatus === 'active' || rawStatus === '1' ? 'active' : 'inactive',
    updatedAt: formatDateTime(item.updatedAt || item.createdAt)
  }
}

function mapSynonym(item: any): Synonym {
  return {
    id: Number(item.id),
    standard: item.standardWord ?? item.standard ?? '',
    aliases: Array.isArray(item.aliases) ? item.aliases : []
  }
}

function mapDataSource(item: any): DataSource {
  const rawStatus = item?.status
  return {
    id: Number(item.id),
    name: item.name || '',
    type: item.type || '',
    host: item.host || '',
    port: item.port,
    username: item.username || '',
    database: item.database || '',
    status: rawStatus === 1 || rawStatus === '1' || rawStatus === 'active' ? 'active' : 'inactive',
    updatedAt: formatDateTime(item.updatedAt || item.createdAt)
  }
}

function mapSubscription(item: any): Subscription {
  return {
    id: Number(item.id),
    title: item.title || '',
    type: item.type || 'DASHBOARD',
    resourceId: Number(item.resourceId || 0),
    subscriberId: Number(item.subscriberId || getCurrentUserId()),
    subscriberName: item.subscriberName || getCurrentUsername(),
    pushMethod: item.pushMethod || 'EMAIL',
    receiver: item.receiver || '',
    frequency: item.frequency || 'DAILY',
    pushTime: item.pushTime || '09:00',
    pushDay: item.pushDay || '',
    status: Number(item.status ?? 1),
    createdAt: formatDateTime(item.createdAt)
  }
}

function mapShare(item: any): Share {
  return {
    id: Number(item.id),
    title: item.title || '',
    type: item.type || 'DASHBOARD',
    resourceId: Number(item.resourceId || 0),
    shareCode: item.shareCode || item.shareToken || '',
    status: Number(item.status ?? 1),
    expireTime: item.expireTime ? formatDateTime(item.expireTime) : '',
    createdAt: formatDateTime(item.createdAt)
  }
}

function toSuccessResponse<T>(source: ApiResponse<any>, data?: T): ApiResponse<T> {
  return {
    success: source.success,
    data,
    error: source.error,
    message: source.message
  }
}

async function getQueryHistoryById(id: number): Promise<QueryHistoryEntity | undefined> {
  const response = await request<QueryHistoryEntity>(`/query-history/${id}`)
  return response.success ? response.data : undefined
}

async function getRecentQueryHistory(limit = MAX_RECENT): Promise<QueryHistoryEntity[]> {
  const response = await request<QueryHistoryEntity[]>(`/query-history/recent?userId=${getCurrentUserId()}&limit=${limit}`)
  return response.success ? extractRecords<QueryHistoryEntity>(response.data) : []
}

async function getFavoriteQueryHistory(): Promise<QueryHistoryEntity[]> {
  const response = await request<QueryHistoryEntity[]>(`/query-history/favorites?userId=${getCurrentUserId()}`)
  return response.success ? extractRecords<QueryHistoryEntity>(response.data) : []
}

async function createHistoryRecord(text: string, overrides: Partial<QueryHistoryEntity> = {}): Promise<ApiResponse<QueryHistoryEntity>> {
  return request<QueryHistoryEntity>('/query-history', {
    method: 'POST',
    body: JSON.stringify({
      userId: getCurrentUserId(),
      username: getCurrentUsername(),
      queryName: overrides.queryName || shortenText(text),
      queryType: 'NATURAL_LANGUAGE',
      queryContent: text,
      status: 'SUCCESS',
      isFavorite: false,
      ...overrides
    })
  })
}

async function updateHistoryRecord(id: number, updates: Partial<QueryHistoryEntity>): Promise<ApiResponse<QueryHistoryEntity>> {
  const current = await getQueryHistoryById(id)
  if (!current) {
    return { success: false, error: '查询记录不存在' }
  }

  return request<QueryHistoryEntity>(`/query-history/${id}`, {
    method: 'PUT',
    body: JSON.stringify({
      ...current,
      ...updates
    })
  })
}

function buildDataSourcePayload(requestData: Partial<DataSourceRequest> & { name: string; type: string }, current?: any) {
  const code = current?.code || slugifyCode(requestData.name, 'DATASOURCE')
  const payload: Record<string, unknown> = {
    ...current,
    name: requestData.name,
    code,
    type: requestData.type,
    host: requestData.host || '',
    port: requestData.port || undefined,
    username: requestData.username || '',
    database: requestData.database || '',
    status: requestData.status === 'inactive' ? 0 : 1
  }

  if (requestData.password) {
    payload.passwordEncrypted = requestData.password
  }

  if (requestData.host && requestData.port && requestData.database) {
    payload.url = current?.url || ''
  }

  return payload
}

export const apiChatbiService: IChatbiService = {
  async getRecentQueries(): Promise<QueryItem[]> {
    const records = await getRecentQueryHistory(MAX_RECENT)
    return records.map(mapQueryHistory)
  },

  async saveQuery(text: string): Promise<void> {
    const trimmed = text.trim()
    if (!trimmed) return

    const recent = await getRecentQueryHistory(MAX_RECENT)
    if (recent.some(item => item.queryContent === trimmed)) {
      return
    }

    await createHistoryRecord(trimmed)
  },

  async deleteQuery(id: number): Promise<void> {
    await request<void>(`/query-history/${id}`, { method: 'DELETE' })
  },

  async clearRecentQueries(): Promise<void> {
    const recent = await getRecentQueryHistory(100)
    await Promise.all(recent.map(item => request<void>(`/query-history/${item.id}`, { method: 'DELETE' })))
  },

  async getFavorites(): Promise<QueryItem[]> {
    const records = await getFavoriteQueryHistory()
    return records.map(mapQueryHistory)
  },

  async addFavorite(text: string, name?: string): Promise<boolean> {
    const trimmed = text.trim()
    if (!trimmed) return false

    const favorites = await getFavoriteQueryHistory()
    if (favorites.some(item => item.queryContent === trimmed)) {
      return false
    }

    const recent = await getRecentQueryHistory(100)
    const matched = recent.find(item => item.queryContent === trimmed)

    if (matched) {
      const updated = await updateHistoryRecord(matched.id, {
        queryName: name?.trim() || matched.queryName || shortenText(trimmed),
        isFavorite: true
      })
      return updated.success
    }

    const created = await createHistoryRecord(trimmed, {
      queryName: name?.trim() || shortenText(trimmed),
      isFavorite: true
    })
    return created.success
  },

  async removeFavorite(id: number): Promise<void> {
    await request<void>(`/query-history/${id}/favorite?isFavorite=false`, { method: 'PATCH' })
  },

  async renameFavorite(id: number, newName: string): Promise<void> {
    await updateHistoryRecord(id, { queryName: newName.trim() })
  },

  async clearFavorites(): Promise<void> {
    const favorites = await getFavoriteQueryHistory()
    await Promise.all(
      favorites.map(item => request<void>(`/query-history/${item.id}/favorite?isFavorite=false`, { method: 'PATCH' }))
    )
  },

  async getHotQueries(): Promise<HotQuery[]> {
    const response = await request<HotQuery[]>('/query/hot')
    return response.success ? extractRecords<HotQuery>(response.data) : []
  },

  async getActiveMetrics(): Promise<Metric[]> {
    const response = await request<Metric[]>('/metrics/active')
    if (response.success) {
      return extractRecords<any>(response.data).map(mapMetric)
    }

    const fallback = await request<Metric[]>('/metrics')
    return fallback.success
      ? extractRecords<any>(fallback.data).map(mapMetric).filter(item => item.status === 'active')
      : []
  },

  async executeQuery(requestData: QueryRequest): Promise<QueryResult> {
    const response = await request<QueryResult>('/query', {
      method: 'POST',
      body: JSON.stringify(requestData)
    })

    if (!response.success || !response.data) {
      throw new Error(response.error || '查询失败')
    }

    return response.data
  }
}

export const apiAdminService: IAdminService = {
  async getMetrics(): Promise<Metric[]> {
    const response = await request<Metric[]>('/metrics')
    return response.success ? extractRecords<any>(response.data).map(mapMetric) : []
  },

  async getMetricById(id: number): Promise<Metric | undefined> {
    const response = await request<any>(`/metrics/${id}`)
    return response.success && response.data ? mapMetric(response.data) : undefined
  },

  async addMetric(requestData: MetricRequest): Promise<ApiResponse<Metric>> {
    const response = await request<any>('/metrics', {
      method: 'POST',
      body: JSON.stringify({
        code: slugifyCode(requestData.code || requestData.name, 'METRIC'),
        name: requestData.name,
        definition: requestData.definition,
        status: requestData.status === 'inactive' ? 'inactive' : 'active',
        dataType: 'NUMERIC'
      })
    })
    return toSuccessResponse(response, response.data ? mapMetric(response.data) : undefined)
  },

  async updateMetric(id: number, updates: Partial<Metric>): Promise<ApiResponse<Metric>> {
    const response = await request<any>(`/metrics/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: updates.name,
        definition: updates.definition,
        status: updates.status
      })
    })
    return toSuccessResponse(response, response.data ? mapMetric(response.data) : undefined)
  },

  async deleteMetric(id: number): Promise<ApiResponse<void>> {
    return request<void>(`/metrics/${id}`, { method: 'DELETE' })
  },

  async toggleMetricStatus(id: number): Promise<ApiResponse<Metric>> {
    const response = await request<any>(`/metrics/${id}/toggle`, { method: 'PATCH' })
    return toSuccessResponse(response, response.data ? mapMetric(response.data) : undefined)
  },

  async getSynonyms(): Promise<Synonym[]> {
    const response = await request<Synonym[]>('/synonyms')
    return response.success ? extractRecords<any>(response.data).map(mapSynonym) : []
  },

  async addSynonym(requestData: SynonymRequest): Promise<ApiResponse<Synonym>> {
    const response = await request<any>('/synonyms', {
      method: 'POST',
      body: JSON.stringify({
        standardWord: requestData.standard,
        aliases: requestData.aliases,
        status: 1
      })
    })
    return toSuccessResponse(response, response.data ? mapSynonym(response.data) : undefined)
  },

  async deleteSynonym(id: number): Promise<ApiResponse<void>> {
    return request<void>(`/synonyms/${id}`, { method: 'DELETE' })
  },

  async getDataSources(): Promise<DataSource[]> {
    const response = await request<DataSource[]>('/datasources/all')
    return response.success ? extractRecords<any>(response.data).map(mapDataSource) : []
  },

  async getDataSourceById(id: number): Promise<DataSource | undefined> {
    const response = await request<any>(`/datasources/${id}`)
    return response.success && response.data ? mapDataSource(response.data) : undefined
  },

  async addDataSource(requestData: DataSourceRequest): Promise<ApiResponse<DataSource>> {
    const response = await request<any>('/datasources', {
      method: 'POST',
      body: JSON.stringify(buildDataSourcePayload(requestData as DataSourceRequest & { name: string; type: string }))
    })
    return toSuccessResponse(response, response.data ? mapDataSource(response.data) : undefined)
  },

  async updateDataSource(id: number, updates: Partial<DataSourceRequest>): Promise<ApiResponse<DataSource>> {
    const current = await request<any>(`/datasources/${id}`)
    if (!current.success || !current.data) {
      return { success: false, error: current.error || '数据源不存在' }
    }

    const payload = buildDataSourcePayload({
      name: updates.name || current.data.name,
      type: updates.type || current.data.type,
      host: updates.host ?? current.data.host,
      port: updates.port ?? current.data.port,
      username: updates.username ?? current.data.username,
      password: updates.password,
      database: updates.database ?? current.data.database,
      status: updates.status ?? (current.data.status === 1 ? 'active' : 'inactive')
    }, current.data)

    const response = await request<any>(`/datasources/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
    return toSuccessResponse(response, response.data ? mapDataSource(response.data) : undefined)
  },

  async deleteDataSource(id: number): Promise<ApiResponse<void>> {
    return request<void>(`/datasources/${id}`, { method: 'DELETE' })
  },

  async testDataSource(id: number): Promise<ApiResponse<boolean>> {
    const current = await request<any>(`/datasources/${id}`)
    if (!current.success || !current.data) {
      return { success: false, error: current.error || '数据源不存在' }
    }

    const response = await request<any>('/datasources/test', {
      method: 'POST',
      body: JSON.stringify(current.data)
    })

    return {
      success: response.success,
      data: normalizeBoolean(response.data?.success),
      error: response.error,
      message: response.message
    }
  },

  async getSubscriptions(): Promise<Subscription[]> {
    const response = await request<any>(`/subscriptions?current=1&size=${PAGE_SIZE}`)
    return response.success ? extractRecords<any>(response.data).map(mapSubscription) : []
  },

  async getSubscriptionById(id: number): Promise<Subscription | undefined> {
    const response = await request<any>(`/subscriptions/${id}`)
    return response.success && response.data ? mapSubscription(response.data) : undefined
  },

  async addSubscription(requestData: SubscriptionRequest): Promise<ApiResponse<Subscription>> {
    const response = await request<any>('/subscriptions', {
      method: 'POST',
      body: JSON.stringify({
        ...requestData,
        subscriberId: getCurrentUserId(),
        subscriberName: getCurrentUsername(),
        createdBy: getCurrentUserId(),
        status: requestData.status ?? 1
      })
    })
    return toSuccessResponse(response, response.data ? mapSubscription(response.data) : undefined)
  },

  async updateSubscription(id: number, updates: Partial<SubscriptionRequest>): Promise<ApiResponse<Subscription>> {
    const current = await request<any>(`/subscriptions/${id}`)
    if (!current.success || !current.data) {
      return { success: false, error: current.error || '订阅不存在' }
    }

    const response = await request<any>(`/subscriptions/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        ...current.data,
        ...updates,
        subscriberId: current.data.subscriberId || getCurrentUserId(),
        subscriberName: current.data.subscriberName || getCurrentUsername(),
        createdBy: current.data.createdBy || getCurrentUserId(),
        status: updates.status ?? current.data.status ?? 1
      })
    })
    return toSuccessResponse(response, response.data ? mapSubscription(response.data) : undefined)
  },

  async deleteSubscription(id: number): Promise<ApiResponse<void>> {
    return request<void>(`/subscriptions/${id}`, { method: 'DELETE' })
  },

  async toggleSubscriptionStatus(id: number): Promise<ApiResponse<Subscription>> {
    const current = await request<any>(`/subscriptions/${id}`)
    if (!current.success || !current.data) {
      return { success: false, error: current.error || '订阅不存在' }
    }

    const nextStatus = Number(current.data.status ?? 1) === 1 ? 0 : 1
    return this.updateSubscription(id, { status: nextStatus })
  },

  async getShares(): Promise<Share[]> {
    const response = await request<any>(`/shares?current=1&size=${PAGE_SIZE}`)
    return response.success ? extractRecords<any>(response.data).map(mapShare) : []
  },

  async getShareById(id: number): Promise<Share | undefined> {
    const response = await request<any>(`/shares/${id}`)
    return response.success && response.data ? mapShare(response.data) : undefined
  },

  async addShare(requestData: ShareRequest): Promise<ApiResponse<Share>> {
    const response = await request<any>('/shares', {
      method: 'POST',
      body: JSON.stringify({
        ...requestData,
        shareMethod: 'LINK',
        validityType: requestData.expireTime ? 'DATE_RANGE' : 'PERMANENT',
        createdBy: getCurrentUserId(),
        creatorName: getCurrentUsername(),
        status: requestData.status ?? 1
      })
    })
    return toSuccessResponse(response, response.data ? mapShare(response.data) : undefined)
  },

  async updateShare(id: number, updates: Partial<ShareRequest>): Promise<ApiResponse<Share>> {
    const current = await request<any>(`/shares/${id}`)
    if (!current.success || !current.data) {
      return { success: false, error: current.error || '分享不存在' }
    }

    const response = await request<any>(`/shares/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        ...current.data,
        ...updates,
        shareMethod: current.data.shareMethod || 'LINK',
        validityType: updates.expireTime ? 'DATE_RANGE' : (current.data.validityType || 'PERMANENT'),
        createdBy: current.data.createdBy || getCurrentUserId(),
        creatorName: current.data.creatorName || getCurrentUsername(),
        status: updates.status ?? current.data.status ?? 1
      })
    })
    return toSuccessResponse(response, response.data ? mapShare(response.data) : undefined)
  },

  async deleteShare(id: number): Promise<ApiResponse<void>> {
    return request<void>(`/shares/${id}`, { method: 'DELETE' })
  },

  async toggleShareStatus(id: number): Promise<ApiResponse<Share>> {
    const current = await request<any>(`/shares/${id}`)
    if (!current.success || !current.data) {
      return { success: false, error: current.error || '分享不存在' }
    }

    const nextStatus = Number(current.data.status ?? 1) === 1 ? 0 : 1
    return this.updateShare(id, { status: nextStatus })
  },

  async getRuleTemplates(): Promise<RuleTemplate[]> {
    const [examples, metrics, synonyms] = await Promise.all([
      request<string[]>('/query/examples'),
      request<any[]>('/metrics/active'),
      request<any[]>('/synonyms')
    ])

    const exampleList = examples.success ? extractRecords<string>(examples.data) : []
    const metricList = metrics.success ? extractRecords<any>(metrics.data) : []
    const synonymList = synonyms.success ? extractRecords<any>(synonyms.data) : []

    return [
      {
        id: 1,
        name: '指标直查',
        pattern: exampleList[0] || '时间 + 指标',
        priority: 1,
        status: metricList.length > 0 ? 'active' : 'inactive'
      },
      {
        id: 2,
        name: '趋势分析',
        pattern: exampleList[1] || '时间范围 + 指标 + 趋势',
        priority: 2,
        status: metricList.length > 1 ? 'active' : 'inactive'
      },
      {
        id: 3,
        name: '语义映射',
        pattern: `已配置 ${synonymList.length} 组业务同义词`,
        priority: 3,
        status: synonymList.length > 0 ? 'active' : 'inactive'
      }
    ]
  },

  async initData(): Promise<void> {
    return Promise.resolve()
  }
}
