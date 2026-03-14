export interface RankedCandidateEvidence {
  metric?: string
  score?: number
  position?: number
  reason?: string
}

export type RankedCandidateReasonGroup = 'semantic' | 'time' | 'connector' | 'other'
export type RankedCandidateReasonFilter = 'all' | RankedCandidateReasonGroup

export interface RankedCandidateReasonOption {
  key: RankedCandidateReasonFilter
  label: string
  count: number
}

const RANKED_CANDIDATE_REASON_GROUPS: Array<{
  key: RankedCandidateReasonGroup
  label: string
  patterns: RegExp[]
}> = [
  {
    key: 'semantic',
    label: '语义',
    patterns: [/命中指标词/, /候选基础命中/, /语义/, /同义词/, /模糊/]
  },
  {
    key: 'time',
    label: '时间',
    patterns: [/时间槽位/, /时间词/, /本月|上月|本周|时间/]
  },
  {
    key: 'connector',
    label: '连接词',
    patterns: [/连接词/, /比较/, /分别/, /同时/, /以及/, /还有/, /并列/]
  },
  {
    key: 'other',
    label: '其他',
    patterns: []
  }
]

export function buildRankedCandidateLabel(candidate: RankedCandidateEvidence, index: number): string {
  const metric = candidate.metric || '未知指标'
  const score = candidate.score ?? 0
  const reason = candidate.reason ? ` - ${candidate.reason}` : ''
  return `候选排序${index + 1}：${metric}（${score}）${reason}`
}

export function rankedCandidateReasonGroups(
  candidate: RankedCandidateEvidence
): RankedCandidateReasonGroup[] {
  const reason = candidate.reason || ''
  const matched = RANKED_CANDIDATE_REASON_GROUPS
    .filter(group => group.key !== 'other')
    .filter(group => group.patterns.some(pattern => pattern.test(reason)))
    .map(group => group.key)

  return matched.length ? matched : ['other']
}

export function rankedCandidateReasonSummary(candidate: RankedCandidateEvidence): string {
  return rankedCandidateReasonGroups(candidate)
    .map(group => RANKED_CANDIDATE_REASON_GROUPS.find(item => item.key === group)?.label || '其他')
    .join(' / ')
}

export function rankedCandidateReasonOptions(
  candidates: RankedCandidateEvidence[] | undefined
): RankedCandidateReasonOption[] {
  const list = candidates || []
  const counts = new Map<RankedCandidateReasonGroup, number>()

  list.forEach(candidate => {
    rankedCandidateReasonGroups(candidate).forEach(group => {
      counts.set(group, (counts.get(group) || 0) + 1)
    })
  })

  const options: RankedCandidateReasonOption[] = [
    { key: 'all', label: '全部', count: list.length }
  ]

  RANKED_CANDIDATE_REASON_GROUPS.forEach(group => {
    const count = counts.get(group.key) || 0
    if (count > 0) {
      options.push({
        key: group.key,
        label: group.label,
        count
      })
    }
  })

  return options
}

export function filterRankedCandidatesByReason(
  candidates: RankedCandidateEvidence[] | undefined,
  filter: RankedCandidateReasonFilter
): RankedCandidateEvidence[] {
  const list = candidates || []
  if (filter === 'all') {
    return list
  }
  return list.filter(candidate => rankedCandidateReasonGroups(candidate).includes(filter))
}

export function rankedCandidateDisplayIndex(
  candidates: RankedCandidateEvidence[] | undefined,
  candidate: RankedCandidateEvidence,
  fallbackIndex: number
): number {
  const index = (candidates || []).indexOf(candidate)
  return index >= 0 ? index : fallbackIndex
}

export function visibleRankedCandidates(
  candidates: RankedCandidateEvidence[] | undefined,
  expanded: boolean,
  compactLimit = 2
): RankedCandidateEvidence[] {
  const list = candidates || []
  if (expanded) {
    return list
  }
  return list.slice(0, compactLimit)
}

export function hiddenRankedCandidateCount(
  candidates: RankedCandidateEvidence[] | undefined,
  expanded: boolean,
  compactLimit = 2
): number {
  const list = candidates || []
  if (expanded || list.length <= compactLimit) {
    return 0
  }
  return list.length - compactLimit
}
