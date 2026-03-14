export interface EnterpriseChartType {
  type: string
  family: string
  name: string
  variant: string
  description: string
}

const CHART_FAMILIES: Array<{ family: string; name: string; description: string }> = [
  { family: 'bar', name: '柱状图', description: '适合类别对比分析' },
  { family: 'line', name: '折线图', description: '适合趋势分析' },
  { family: 'area', name: '面积图', description: '适合趋势与占比联动分析' },
  { family: 'pie', name: '饼图', description: '适合结构占比分析' },
  { family: 'scatter', name: '散点图', description: '适合相关性分析' },
  { family: 'radar', name: '雷达图', description: '适合多维能力对比' },
  { family: 'gauge', name: '仪表盘', description: '适合目标达成监控' },
  { family: 'funnel', name: '漏斗图', description: '适合转化链路分析' },
  { family: 'treemap', name: '矩形树图', description: '适合层级占比分析' },
  { family: 'sunburst', name: '旭日图', description: '适合层级环形分析' },
  { family: 'sankey', name: '桑基图', description: '适合流向分析' },
  { family: 'heatmap', name: '热力图', description: '适合密度分布分析' },
  { family: 'candlestick', name: 'K线图', description: '适合价格区间分析' },
  { family: 'boxplot', name: '箱线图', description: '适合分布异常分析' },
  { family: 'waterfall', name: '瀑布图', description: '适合增减贡献分析' },
  { family: 'graph', name: '关系图', description: '适合关系网络分析' },
  { family: 'tree', name: '树图', description: '适合层级结构分析' }
]

const STYLE_VARIANTS = [
  { id: 'classic', name: '经典' },
  { id: 'enterprise', name: '企业' },
  { id: 'minimal', name: '极简' },
  { id: 'contrast', name: '高对比' },
  { id: 'soft', name: '柔和' },
  { id: 'dark-grid', name: '深网格' },
  { id: 'light-grid', name: '浅网格' }
]

export const ENTERPRISE_CHART_CATALOG: EnterpriseChartType[] = CHART_FAMILIES.flatMap(family =>
  STYLE_VARIANTS.map(variant => ({
    type: `${family.family}.${variant.id}`,
    family: family.family,
    name: `${family.name}-${variant.name}`,
    variant: variant.id,
    description: family.description
  }))
)

const SIMPLE_ALIAS: Record<string, string> = {
  bar: 'bar.classic',
  line: 'line.classic',
  area: 'area.classic',
  pie: 'pie.classic',
  scatter: 'scatter.classic',
  radar: 'radar.classic',
  gauge: 'gauge.classic',
  funnel: 'funnel.classic',
  treemap: 'treemap.classic',
  sunburst: 'sunburst.classic',
  sankey: 'sankey.classic',
  heatmap: 'heatmap.classic',
  candlestick: 'candlestick.classic',
  boxplot: 'boxplot.classic',
  waterfall: 'waterfall.classic',
  graph: 'graph.classic',
  tree: 'tree.classic'
}

export function normalizeEnterpriseChartType(rawType?: string): string {
  const safeType = (rawType || '').trim()
  if (!safeType) return 'line.classic'
  if (safeType === 'table' || safeType === 'filter') return safeType
  if (SIMPLE_ALIAS[safeType]) return SIMPLE_ALIAS[safeType]
  const found = ENTERPRISE_CHART_CATALOG.find(item => item.type === safeType)
  return found ? found.type : 'line.classic'
}

export function getChartFamily(rawType?: string): string {
  const normalized = normalizeEnterpriseChartType(rawType)
  if (normalized === 'table' || normalized === 'filter') return normalized
  return normalized.split('.')[0]
}

export function getEnterpriseChartType(rawType?: string): EnterpriseChartType {
  const normalized = normalizeEnterpriseChartType(rawType)
  return ENTERPRISE_CHART_CATALOG.find(item => item.type === normalized) || ENTERPRISE_CHART_CATALOG[0]
}

export function getFeaturedChartTypes(limit = 12): EnterpriseChartType[] {
  return ENTERPRISE_CHART_CATALOG.slice(0, limit)
}
