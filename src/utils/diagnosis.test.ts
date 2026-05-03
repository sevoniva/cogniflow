import { describe, expect, it } from 'vitest'
import {
  buildRankedCandidateLabel,
  filterRankedCandidatesByReason,
  hiddenRankedCandidateCount,
  rankedCandidateDisplayIndex,
  rankedCandidateReasonGroups,
  rankedCandidateReasonOptions,
  rankedCandidateReasonSummary,
  visibleRankedCandidates
} from './diagnosis'

describe('diagnosis ranked candidate helpers', () => {
  it('formats ranked candidate label with reason', () => {
    const text = buildRankedCandidateLabel(
      { metric: '指标A', score: 65, reason: '位于比较连接词后' },
      0
    )
    expect(text).toContain('候选排序1')
    expect(text).toContain('指标A（65）')
    expect(text).toContain('位于比较连接词后')
  })

  it('supports empty fields fallback', () => {
    const text = buildRankedCandidateLabel({}, 1)
    expect(text).toContain('候选排序2')
    expect(text).toContain('未知指标（0）')
  })

  it('returns compact list and hidden count when collapsed', () => {
    const items = [
      { metric: '指标A' },
      { metric: '指标B' },
      { metric: '指标D' }
    ]
    expect(visibleRankedCandidates(items, false, 2)).toHaveLength(2)
    expect(hiddenRankedCandidateCount(items, false, 2)).toBe(1)
  })

  it('returns full list and no hidden count when expanded', () => {
    const items = [
      { metric: '指标A' },
      { metric: '指标B' },
      { metric: '指标D' }
    ]
    expect(visibleRankedCandidates(items, true, 2)).toHaveLength(3)
    expect(hiddenRankedCandidateCount(items, true, 2)).toBe(0)
  })

  it('classifies ranked candidate reasons into multiple groups', () => {
    const groups = rankedCandidateReasonGroups({
      reason: '命中指标词 + 位于比较连接词后 + 与时间词距离近'
    })

    expect(groups).toEqual(['semantic', 'time', 'connector'])
    expect(
      rankedCandidateReasonSummary({
        reason: '命中指标词 + 位于比较连接词后 + 与时间词距离近'
      })
    ).toBe('语义 / 时间 / 连接词')
  })

  it('builds reason filter options with counts', () => {
    const items = [
      { metric: '指标A', reason: '命中指标词 + 位于比较连接词后' },
      { metric: '指标B', reason: '与时间词距离近' },
      { metric: '指标C', reason: '候选基础命中' }
    ]

    expect(rankedCandidateReasonOptions(items)).toEqual([
      { key: 'all', label: '全部', count: 3 },
      { key: 'semantic', label: '语义', count: 2 },
      { key: 'time', label: '时间', count: 1 },
      { key: 'connector', label: '连接词', count: 1 }
    ])
  })

  it('filters ranked candidates by selected reason', () => {
    const items = [
      { metric: '指标A', reason: '命中指标词 + 位于比较连接词后' },
      { metric: '指标B', reason: '与时间词距离近' },
      { metric: '指标C', reason: '候选基础命中' }
    ]

    expect(filterRankedCandidatesByReason(items, 'connector').map(item => item.metric)).toEqual([
      '指标A'
    ])
    expect(filterRankedCandidatesByReason(items, 'time').map(item => item.metric)).toEqual([
      '指标B'
    ])
  })

  it('falls back to other group when reason is empty', () => {
    const items = [{ metric: '指标A' }]

    expect(rankedCandidateReasonGroups(items[0])).toEqual(['other'])
    expect(rankedCandidateReasonOptions(items)).toEqual([
      { key: 'all', label: '全部', count: 1 },
      { key: 'other', label: '其他', count: 1 }
    ])
  })

  it('keeps original ranking index after filtering', () => {
    const items = [
      { metric: '指标A', reason: '命中指标词' },
      { metric: '指标B', reason: '与时间词距离近' },
      { metric: '指标D', reason: '位于比较连接词后' }
    ]
    const filtered = filterRankedCandidatesByReason(items, 'time')

    expect(rankedCandidateDisplayIndex(items, filtered[0], 0)).toBe(1)
  })
})
