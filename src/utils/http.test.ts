import { afterEach, describe, expect, it, vi } from 'vitest'

import { extractRecords, extractTotal, formatDateTime, normalizeBoolean, request, slugifyCode } from './http'

function mockResponse(payload: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    text: vi.fn().mockResolvedValue(JSON.stringify(payload))
  } as unknown as Response
}

describe('http request helper', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('prefixes relative paths with the API base', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ code: 200, data: { ok: true } })
    )

    await request('/metrics')

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/metrics',
      expect.objectContaining({ credentials: 'include' })
    )
  })

  it('does not duplicate the API prefix for API-relative paths', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ code: 200, data: { ok: true } })
    )

    await request('/api/analytics/sales/overview')

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/analytics/sales/overview',
      expect.objectContaining({ credentials: 'include' })
    )
  })

  it('returns normalized api envelope errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ success: false, error: 'boom' }, 400)
    )

    const result = await request('/metrics')
    expect(result.success).toBe(false)
    expect(result.error).toContain('boom')
  })

  it('handles absolute urls and keeps existing content-type header', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ success: true, data: { ok: true } })
    )
    await request('https://example.com/api/custom', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain'
      },
      body: 'payload'
    })
    expect(fetchSpy).toHaveBeenCalledWith(
      'https://example.com/api/custom',
      expect.objectContaining({
        credentials: 'include',
        headers: expect.any(Headers)
      })
    )
    const headers = fetchSpy.mock.calls[0][1]?.headers as Headers
    expect(headers.get('Content-Type')).toBe('text/plain')
  })

  it('adds content-type for json body and supports non-slash paths', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ success: true, data: { ok: true } })
    )
    await request('metrics', {
      method: 'POST',
      body: JSON.stringify({ a: 1 })
    })
    expect(fetchSpy).toHaveBeenCalledWith('/api/metrics', expect.any(Object))
    const headers = fetchSpy.mock.calls[0][1]?.headers as Headers
    expect(headers.get('Content-Type')).toBe('application/json')
  })

  it('returns normalized result envelope errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ code: 500, message: 'server error' }, 500)
    )

    const result = await request('/metrics')
    expect(result.success).toBe(false)
    expect(result.error).toContain('server error')
  })

  it('returns timeout error on abort', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new DOMException('timeout', 'AbortError'))
    const result = await request('/metrics')
    expect(result.success).toBe(false)
    expect(result.error).toContain('请求超时')
  })

  it('returns parse error when payload is invalid json', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      text: vi.fn().mockResolvedValue('not-json')
    } as unknown as Response)

    const result = await request('/metrics')
    expect(result.success).toBe(false)
    expect(result.error).toContain('格式异常')
  })

  it('returns fallback network error for unknown failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue({})
    const result = await request('/metrics')
    expect(result.success).toBe(false)
    expect(result.error).toContain('网络请求失败')
  })

  it('returns http envelope when backend response ok=false but payload marked success', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse({ success: true, data: { ok: true }, message: 'bad gateway' }, 502)
    )
    const result = await request('/metrics')
    expect(result.success).toBe(false)
    expect(result.error).toContain('HTTP 502')
    expect(result.message).toBe('bad gateway')
  })

  it('supports empty response text payload', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      text: vi.fn().mockResolvedValue('')
    } as unknown as Response)
    const result = await request('/metrics')
    expect(result.success).toBe(true)
    expect(result.data).toBeNull()
  })

  it('extracts records and total from paged payload', () => {
    const data = { records: [{ id: 1 }, { id: 2 }], total: 2 }
    expect(extractRecords(data)).toHaveLength(2)
    expect(extractTotal(data)).toBe(2)
  })

  it('returns defaults when records/total missing', () => {
    expect(extractRecords(null)).toEqual([])
    expect(extractTotal({ total: '2' }, 9)).toBe(9)
  })

  it('formats date and fallback values', () => {
    expect(formatDateTime('2026-03-12T12:00:00.000Z')).toContain('2026')
    expect(formatDateTime('')).toBe('-')
    expect(formatDateTime('invalid')).toBe('invalid')
  })

  it('normalizes boolean-like values', () => {
    expect(normalizeBoolean(true)).toBe(true)
    expect(normalizeBoolean(1)).toBe(true)
    expect(normalizeBoolean('1')).toBe(true)
    expect(normalizeBoolean(0)).toBe(false)
  })

  it('slugifies code and falls back for empty values', () => {
    expect(slugifyCode('销售 额-增长', 'METRIC')).toMatch(/销售_额_增长/i)
    expect(slugifyCode('   ', 'METRIC')).toContain('METRIC_')
  })
})
