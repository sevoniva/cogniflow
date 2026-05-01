import type { ApiResponse } from '@/types'
import { useUserStore } from '@/stores/user'
import { ofetch, type FetchOptions } from 'ofetch'
import { z } from 'zod'

const API_BASE = ((import.meta as any).env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const TIMEOUT = 10000

const ApiEnvelopeSchema = z.object({
  success: z.boolean().optional(),
  data: z.unknown().optional(),
  error: z.string().optional(),
  message: z.string().optional()
})

const ResultEnvelopeSchema = z.object({
  code: z.number().optional(),
  message: z.string().optional(),
  data: z.unknown().optional()
})

function isApiEnvelope(payload: unknown): payload is { success?: boolean; data?: unknown; error?: string; message?: string } {
  return ApiEnvelopeSchema.safeParse(payload).success
}

function isResultEnvelope(payload: unknown): payload is { code?: number; message?: string; data?: unknown } {
  return ResultEnvelopeSchema.safeParse(payload).success
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

async function fetchWithTimeout(url: string, options: RequestInit = {}): Promise<Response> {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), TIMEOUT)

  try {
    return await fetch(url, {
      credentials: 'include',
      ...options,
      signal: controller.signal
    })
  } finally {
    window.clearTimeout(timeoutId)
  }
}

/**
 * HTTP 请求（兼容原有接口）
 *
 * 保留原生 fetch 实现以确保测试兼容性。
 * 新代码推荐使用下方导出的 $http（ofetch 实例）。
 */
export async function request<T>(path: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = path.startsWith('http://') || path.startsWith('https://')
    ? path
    : (normalizedPath === API_BASE || normalizedPath.startsWith(`${API_BASE}/`))
      ? normalizedPath
      : `${API_BASE}${normalizedPath}`

  const headers = new Headers(options.headers || {})
  const isJsonBody = options.body !== undefined && !(options.body instanceof FormData)
  if (isJsonBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  // 注入 JWT token
  try {
    const userStore = useUserStore()
    if (userStore.token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${userStore.token}`)
    }
  } catch {
    // store 未初始化时忽略
  }

  try {
    const response = await fetchWithTimeout(url, {
      ...options,
      headers
    })

    const rawText = await response.text()
    const payload = rawText ? JSON.parse(rawText) : null
    const normalized = normalizeResponse<T>(payload, response)

    if (!response.ok && normalized.success) {
      return {
        success: false,
        error: `HTTP ${response.status}: ${response.statusText}`,
        message: normalized.message,
        data: normalized.data
      }
    }

    return normalized
  } catch (error: any) {
    if (error?.name === 'AbortError') {
      return { success: false, error: '请求超时，请检查后端服务是否启动' }
    }

    if (error instanceof SyntaxError) {
      return { success: false, error: '接口返回数据格式异常' }
    }

    return { success: false, error: error?.message || '网络请求失败，请检查网络连接' }
  }
}

/**
 * 基于 ofetch 的高级 HTTP 实例
 *
 * 特性：拦截器、自动重试、请求取消、基础 URL、去重请求。
 * 新代码推荐使用 $http 替代 request()。
 */
export const $http = ofetch.create({
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
    try {
      const userStore = useUserStore()
      if (userStore.token && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${userStore.token}`)
      }
    } catch {
      // store 未初始化时忽略
    }
    options.headers = headers
  },

  onResponseError({ response, request }) {
    console.warn(`[HTTP] ${request} -> ${response.status} ${response.statusText}`)
  }
})

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

export interface StreamChunk {
  event: string
  data: string
}

/**
 * SSE 流式请求
 *
 * 使用 fetch + ReadableStream 解析 SSE 事件流。
 * 支持 POST JSON body（比 EventSource 更灵活）。
 */
export async function streamRequest(
  path: string,
  options: RequestInit & { body: string },
  onChunk: (chunk: StreamChunk) => void
): Promise<void> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = path.startsWith('http://') || path.startsWith('https://')
    ? path
    : (normalizedPath === API_BASE || normalizedPath.startsWith(`${API_BASE}/`))
      ? normalizedPath
      : `${API_BASE}${normalizedPath}`

  const extraHeaders: Record<string, string> = {
    'Accept': 'text/event-stream',
    'Content-Type': 'application/json'
  }
  try {
    const userStore = useUserStore()
    if (userStore.token) {
      extraHeaders['Authorization'] = `Bearer ${userStore.token}`
    }
  } catch {
    // store 未初始化时忽略
  }

  const response = await fetch(url, {
    ...options,
    headers: {
      ...((options.headers as Record<string, string>) || {}),
      ...extraHeaders
    },
    signal: options.signal
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('Response body is not readable')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // SSE 格式解析：event: xxx\ndata: yyy\n\n
    let eventName = ''
    const lines = buffer.split('\n')
    buffer = ''

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        const data = line.slice(5).trim()
        if (eventName) {
          onChunk({ event: eventName, data })
          eventName = ''
        }
      } else if (line.trim() === '') {
        // 空行分隔事件
        eventName = ''
      } else {
        // 不完整的行，放回 buffer
        buffer = line
      }
    }
  }
}
