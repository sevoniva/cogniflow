import { configure } from '@testing-library/vue'
import { beforeEach, vi } from 'vitest'

// 全局配置
configure({ testIdAttribute: 'data-testid' })

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = MockResizeObserver as typeof ResizeObserver
}

// 全局 mock
beforeEach(() => {
  // 清空所有 mock
  vi.clearAllMocks()
})

// 全局工具函数
globalThis.mockFetch = (data: unknown) => {
  globalThis.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve(data),
      text: () => Promise.resolve(JSON.stringify(data)),
      headers: new Headers(),
      redirected: false,
      status: 200,
      statusText: 'OK',
      type: 'basic',
      url: '',
      clone: vi.fn(),
      body: null,
      bodyUsed: false,
      arrayBuffer: vi.fn(),
      blob: vi.fn(),
      formData: vi.fn(),
    } as unknown as Response)
  )
}

globalThis.mockFetchError = (status = 500, statusText = 'Internal Server Error') => {
  globalThis.fetch = vi.fn(() =>
    Promise.resolve({
      ok: false,
      status,
      statusText,
      headers: new Headers(),
      redirected: false,
      type: 'basic',
      url: '',
      clone: vi.fn(),
      body: null,
      bodyUsed: false,
      json: () => Promise.reject(new Error(statusText)),
      text: () => Promise.reject(new Error(statusText)),
      arrayBuffer: vi.fn(),
      blob: vi.fn(),
      formData: vi.fn(),
    } as unknown as Response)
  )
}
