import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { QueryItem, QueryResult } from '@/types'

export interface QueryContext {
  question: string
  result?: QueryResult
  datasourceId?: number
  timestamp: number
}

/**
 * 查询状态管理
 *
 * 改造说明：
 * - 替代原 URL query 传参的跨页面状态管理
 * - 当前查询上下文、历史记录、收藏、偏好设置统一在此管理
 */
export const useQueryStore = defineStore('query', () => {
  // State
  const currentQuery = ref<QueryContext | null>(null)
  const recentQueries = ref<QueryItem[]>([])
  const favoriteQueries = ref<QueryItem[]>([])
  const preferredChartType = ref<string>(localStorage.getItem('chatbi_preferred_chart') || 'bar')
  const isLoading = ref(false)

  // Getters
  const hasCurrentQuery = computed(() => !!currentQuery.value?.question)
  const currentQuestion = computed(() => currentQuery.value?.question || '')

  // Actions
  function setCurrentQuery(context: QueryContext) {
    currentQuery.value = context
  }

  function clearCurrentQuery() {
    currentQuery.value = null
  }

  function setRecentQueries(queries: QueryItem[]) {
    recentQueries.value = queries
  }

  function addRecentQuery(query: QueryItem) {
    // 去重并限制数量
    const exists = recentQueries.value.findIndex(q => q.text === query.text)
    if (exists >= 0) {
      recentQueries.value.splice(exists, 1)
    }
    recentQueries.value.unshift(query)
    if (recentQueries.value.length > 50) {
      recentQueries.value = recentQueries.value.slice(0, 50)
    }
  }

  function setFavoriteQueries(queries: QueryItem[]) {
    favoriteQueries.value = queries
  }

  function addFavorite(query: QueryItem) {
    if (!favoriteQueries.value.find(q => q.id === query.id)) {
      favoriteQueries.value.unshift(query)
    }
  }

  function removeFavorite(id: number) {
    const idx = favoriteQueries.value.findIndex(q => q.id === id)
    if (idx >= 0) {
      favoriteQueries.value.splice(idx, 1)
    }
  }

  function setPreferredChartType(type: string) {
    preferredChartType.value = type
    localStorage.setItem('chatbi_preferred_chart', type)
  }

  function setLoading(loading: boolean) {
    isLoading.value = loading
  }

  return {
    currentQuery,
    recentQueries,
    favoriteQueries,
    preferredChartType,
    isLoading,
    hasCurrentQuery,
    currentQuestion,
    setCurrentQuery,
    clearCurrentQuery,
    setRecentQueries,
    addRecentQuery,
    setFavoriteQueries,
    addFavorite,
    removeFavorite,
    setPreferredChartType,
    setLoading
  }
})
