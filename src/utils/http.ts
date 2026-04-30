import type { ApiResponse } from '@/types'
import { ofetch, type FetchOptions } from 'ofetch'

const API_BASE = ((import.meta as any).env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const TIMEOUT = 10000

/**
 * 底层 HTTP 客户端（ofetch 实例）
 *
 * 改造说明：
 * - 替换原生 fetch，获得拦截器、自动重试、请求取消、基础 URL 配置
 * - 保留原有 request() 接口兼容性
 */
const $http = ofetch.create({
  baseURL: API_BASE,
  credentials: 'include',
  timeout: TIMEOUT,
  retry: 1,
  retryStatusCodes: [408, 429, 500, 502, 503, 504],

  onRequest({ options }) {
    const headers = new Headers(options.headers || {})
    const isJsonBody = options.body !== undefined && !(options.body instanceof FormData)
    if (isJsonBody && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    options.headers = headers
  },

  onResponseError({ response, request }) {
    console.warn(`[HTTP] ${request} -> ${response.status} ${response.statusText}`)
  }
})

function isApiEnvelope(payload: unknown): payload is { success?: boolean; data?: unknown; error?: string; message?: string } {
  return !!payload && typeof payload === 'object' && ('success' in payload || 'error' in payload)
}

function isResultEnvelope(payload: unknown): payload is { code?: number; message?: string; data?: unknown } {
  return !!payload && typeof payload === 'object' && 'code' in payload
}

function normalizeResponse<T>(payload: unknown, response: Response): ApiResponse<T> {
  if (isApiEnvelope(payload)) {
    const success = Boolean(payload.success)
    return {
      success,
      data: payload.data as T,
      error: success ? undefined : (payload.error || payload.message || '请求失败'),
      message: payload.message
    }
  }

  if (isResultEnvelope(payload)) {
    const success = payload.code === 200 || payload.code === 0
    return {
      success,
      data: payload.data as T,
      error: success ? undefined : (payload.message || `请求失败(${payload.code})`),
      message: payload.message
    }
  }

  return {
    success: response.ok,
    data: payload as T,
    error: response.ok ? undefined : `HTTP ${response.status}: ${response.statusText}`
  }
}

/**
 * HTTP 请求（兼容原有接口）
 */
export async function request<T>(path: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const isAbsoluteUrl = path.startsWith('http://') || path.startsWith('https://')

  try {
    const response = await $http.raw(normalizedPath, {
      ...options as FetchOptions,
      baseURL: isAbsoluteUrl ? undefined : API_BASE
    })

    const payload = response._data
    const normalized = normalizeResponse<T>(payload, response.response)

    if (!response.response.ok && normalized.success) {
      return {
        success: false,
        error: `HTTP ${response.response.status}: ${response.response.statusText}`,
        message: normalized.message,
        data: normalized.data
      }
    }

    return normalized
  } catch (error: any) {
    if (error?.name === 'AbortError' || error?.message?.includes('timeout')) {
      return { success: false, error: '请求超时，请检查后端服务是否启动' }
    }

    if (error instanceof SyntaxError) {
      return { success: false, error: '接口返回数据格式异常' }
    }

    // ofetch 错误信息
    const statusCode = error?.response?.status
    if (statusCode) {
      return { success: false, error: `HTTP ${statusCode}: ${error.message || '请求失败'}` }
    }

    return { success: false, error: error?.message || '网络请求失败，请检查网络连接' }
  }
}

/**
 * 原生 ofetch 实例（供高级场景直接使用）
 */
export { $http }

export function extractRecords<T>(data: unknown): T[] {
  if (Array.isArray(data)) {
    return data as T[]
  }

  if (data && typeof data === 'object') {
    const candidate = data as Record<string, unknown>
    if (Array.isArray(candidate.records)) {
      return candidate.records as T[]
    }
  }

  return []
}

export function extractTotal(data: unknown, fallbackLength = 0): number {
  if (data && typeof data === 'object') {
    const candidate = data as Record<string, unknown>
    if (typeof candidate.total === 'number') {
      return candidate.total
    }
  }
  return fallbackLength
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(/\//g, '-')
}

export function normalizeBoolean(value: unknown): boolean {
  return value === true || value === 1 || value === '1'
}

export function slugifyCode(value: string, fallbackPrefix: string): string {
  const normalized = value
    .trim()
    .replace(/[^a-zA-Z0-9\u4e00-\u9fa5]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toUpperCase()

  if (normalized) {
    return normalized.slice(0, 40)
  }

  return `${fallbackPrefix}_${Date.now()}`
}
