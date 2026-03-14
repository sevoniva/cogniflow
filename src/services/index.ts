/**
 * Chat BI 服务层
 * 
 * 本层定义数据访问的服务接口
 * 页面通过本层访问数据，不直接操作 storage 或后端
 * 
 * 接口统一为异步（Promise），当前默认实现为 REST API
 */

import type {
  QueryItem,
  HotQuery,
  QueryResult,
  QueryRequest,
  Metric,
  Synonym,
  RuleTemplate,
  MetricRequest,
  SynonymRequest,
  DataSource,
  DataSourceRequest,
  Subscription,
  SubscriptionRequest,
  Share,
  ShareRequest,
  ApiResponse
} from '@/types'

// ==================== 用户侧服务接口 ====================

export interface IChatbiService {
  // 查询历史
  getRecentQueries(): Promise<QueryItem[]>
  saveQuery(text: string): Promise<void>
  deleteQuery(id: number): Promise<void>
  clearRecentQueries(): Promise<void>
  
  // 我的收藏
  getFavorites(): Promise<QueryItem[]>
  addFavorite(text: string, name?: string): Promise<boolean>
  removeFavorite(id: number): Promise<void>
  renameFavorite(id: number, newName: string): Promise<void>
  clearFavorites(): Promise<void>
  
  // 热门查询
  getHotQueries(): Promise<HotQuery[]>
  getActiveMetrics(): Promise<Metric[]>
  
  // 执行查询
  executeQuery(request: QueryRequest): Promise<QueryResult>
}

// ==================== 管理侧服务接口 ====================

export interface IAdminService {
  // 指标管理
  getMetrics(): Promise<Metric[]>
  getMetricById(id: number): Promise<Metric | undefined>
  addMetric(request: MetricRequest): Promise<ApiResponse<Metric>>
  updateMetric(id: number, updates: Partial<Metric>): Promise<ApiResponse<Metric>>
  deleteMetric(id: number): Promise<ApiResponse<void>>
  toggleMetricStatus(id: number): Promise<ApiResponse<Metric>>
  
  // 同义词管理
  getSynonyms(): Promise<Synonym[]>
  addSynonym(request: SynonymRequest): Promise<ApiResponse<Synonym>>
  deleteSynonym(id: number): Promise<ApiResponse<void>>

  // 数据源管理
  getDataSources(): Promise<DataSource[]>
  getDataSourceById(id: number): Promise<DataSource | undefined>
  addDataSource(request: DataSourceRequest): Promise<ApiResponse<DataSource>>
  updateDataSource(id: number, updates: Partial<DataSourceRequest>): Promise<ApiResponse<DataSource>>
  deleteDataSource(id: number): Promise<ApiResponse<void>>
  testDataSource(id: number): Promise<ApiResponse<boolean>>

  // 订阅管理
  getSubscriptions(): Promise<Subscription[]>
  getSubscriptionById(id: number): Promise<Subscription | undefined>
  addSubscription(request: SubscriptionRequest): Promise<ApiResponse<Subscription>>
  updateSubscription(id: number, updates: Partial<SubscriptionRequest>): Promise<ApiResponse<Subscription>>
  deleteSubscription(id: number): Promise<ApiResponse<void>>
  toggleSubscriptionStatus(id: number): Promise<ApiResponse<Subscription>>

  // 分享管理
  getShares(): Promise<Share[]>
  getShareById(id: number): Promise<Share | undefined>
  addShare(request: ShareRequest): Promise<ApiResponse<Share>>
  updateShare(id: number, updates: Partial<ShareRequest>): Promise<ApiResponse<Share>>
  deleteShare(id: number): Promise<ApiResponse<void>>
  toggleShareStatus(id: number): Promise<ApiResponse<Share>>

  // 规则模板（当前只读）
  getRuleTemplates(): Promise<RuleTemplate[]>

  // 数据初始化
  initData(): Promise<void>
}
