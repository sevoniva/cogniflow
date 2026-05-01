import { ref, onMounted, onUnmounted } from 'vue'

export interface QueryProgress {
  stage: string
  message: string
  percent: number
  timestamp: number
}

const STAGE_ORDER = ['ANALYZING', 'GENERATING_SQL', 'EXECUTING', 'BUILDING_RESULT', 'DONE']

export function useQueryProgress() {
  const progress = ref<QueryProgress | null>(null)
  const connected = ref(false)
  let ws: WebSocket | null = null

  const connect = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/query-progress`

    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      connected.value = true
      console.log('[WebSocket] 查询进度连接已建立')
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as QueryProgress
        progress.value = data
      } catch (e) {
        console.warn('[WebSocket] 消息解析失败', e)
      }
    }

    ws.onclose = () => {
      connected.value = false
      console.log('[WebSocket] 查询进度连接已关闭')
    }

    ws.onerror = (err) => {
      console.error('[WebSocket] 连接错误', err)
      connected.value = false
    }
  }

  const disconnect = () => {
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
  }

  const reset = () => {
    progress.value = null
  }

  const isActive = (stage: string) => {
    if (!progress.value) return false
    const currentIdx = STAGE_ORDER.indexOf(progress.value.stage)
    const targetIdx = STAGE_ORDER.indexOf(stage)
    return targetIdx >= 0 && targetIdx <= currentIdx
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return {
    progress,
    connected,
    connect,
    disconnect,
    reset,
    isActive
  }
}
