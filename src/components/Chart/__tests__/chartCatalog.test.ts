import { describe, it, expect } from 'vitest'
import { ENTERPRISE_CHART_CATALOG, getChartFamily, normalizeEnterpriseChartType } from '@/components/Chart'

describe('Enterprise chart catalog', () => {
  it('supports 100+ chart styles', () => {
    expect(ENTERPRISE_CHART_CATALOG.length).toBeGreaterThanOrEqual(100)
  })

  it('normalizes simple chart type aliases', () => {
    expect(normalizeEnterpriseChartType('bar')).toBe('bar.classic')
    expect(normalizeEnterpriseChartType('line')).toBe('line.classic')
    expect(normalizeEnterpriseChartType('pie')).toBe('pie.classic')
  })

  it('extracts chart family correctly', () => {
    expect(getChartFamily('sankey.enterprise')).toBe('sankey')
    expect(getChartFamily('table')).toBe('table')
  })
})

