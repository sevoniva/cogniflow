/**
 * Chat BI 数据契约层
 * 
 * 本层定义前后端数据交换的标准格式
 * 前端页面只依赖本层，不依赖具体实现
 * 后续接后端时，只需在 services 层切换实现
 */

// ==================== 用户侧数据契约 ====================

/** 查询项（历史和收藏共用） */
export interface QueryItem {
  id: number
  name: string
  text: string
  createdAt: string
}

/** 热门查询 */
export interface HotQuery {
  id: number
  text: string
  count: string
}

/** 查询结果 */
export interface QueryResult {
  query: string
  metric: string
  timeRange: string
  dimension: string
  data: Record<string, string | number>[]
  total: number
  summary?: string
  source?: string
  suggestions?: string[]
  candidateMetrics?: string[]
  disambiguation?: boolean
  aiStatus?: Partial<AiRuntimeStatus>
  diagnosis?: {
    code: string
    reason: string
    recovered?: boolean
    actions?: string[]
    guidanceScenario?: string
    intentTags?: string[]
    candidateMetricsPreview?: string[]
    candidateMetricCount?: number
    slotConflict?: boolean
    slotEvidence?: {
      primaryMetric?: {
        value?: string
        source?: string
        confidence?: number
      }
      secondaryMetric?: {
        value?: string | null
        source?: string
        confidence?: number
        conflict?: boolean
        candidates?: string[]
        reason?: string | null
        rankedCandidates?: Array<{
          metric?: string
          score?: number
          position?: number
          reason?: string
        }>
      }
      timeContext?: {
        value?: string | null
        used?: boolean
      }
      timeExplicit?: {
        value?: string | null
        used?: boolean
      }
      timeComparison?: boolean
    }
  }
}

/** 查询请求参数 */
export interface QueryRequest {
  text: string
  userId?: string
}

/** AI 运行状态 */
export interface AiRuntimeStatus {
  mode: 'llm' | 'semantic'
  enabled: boolean
  runtimeEnabled: boolean
  reason: string
  defaultProvider: string
  providerName?: string | null
  model?: string | null
  providerEnabled?: boolean
  apiKeyConfigured?: boolean
}

// ==================== 管理侧数据契约 ====================

/** 指标 */
export interface Metric {
  id: number
  code: string
  name: string
  definition: string
  status: 'active' | 'inactive'
  updatedAt: string
}

/** 同义词 */
export interface Synonym {
  id: number
  standard: string
  aliases: string[]
}

/** 规则模板 */
export interface RuleTemplate {
  id: number
  name: string
  pattern: string
  priority: number
  status: 'active' | 'inactive'
}

/** 指标请求（新增/编辑） */
export interface MetricRequest {
  code: string
  name: string
  definition: string
  status?: 'active' | 'inactive'
}

/** 同义词请求 */
export interface SynonymRequest {
  standard: string
  aliases: string[]
}

/** 数据源 */
export interface DataSource {
  id: number
  name: string
  type: string
  host?: string
  port?: number
  username?: string
  database?: string
  status: 'active' | 'inactive'
  updatedAt: string
}

/** 数据源请求 */
export interface DataSourceRequest {
  name: string
  type: string
  host?: string
  port?: number
  username?: string
  password?: string
  database?: string
  status?: 'active' | 'inactive'
}

/** 订阅 */
export interface Subscription {
  id: number
  title: string
  type: string
  resourceId: number
  subscriberId: number
  subscriberName: string
  pushMethod: string
  receiver: string
  frequency: string
  pushTime: string
  pushDay: string
  status: number
  createdAt: string
}

/** 订阅请求 */
export interface SubscriptionRequest {
  title: string
  type: string
  resourceId: number
  pushMethod: string
  receiver: string
  frequency: string
  pushTime: string
  pushDay: string
  status?: number
}

/** 分享 */
export interface Share {
  id: number
  title: string
  type: string
  resourceId: number
  shareCode: string
  status: number
  expireTime?: string
  createdAt: string
}

/** 分享请求 */
export interface ShareRequest {
  title: string
  type: string
  resourceId: number
  status?: number
  expireTime?: string
}

// ==================== API 响应契约 ====================

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  message?: string
}

export type MetricListResponse = ApiResponse<Metric[]>
export type SynonymListResponse = ApiResponse<Synonym[]>
export type QueryResultResponse = ApiResponse<QueryResult>
