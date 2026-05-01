import { ref, onMounted, onUnmounted } from 'vue'

export interface QueryProgress {
  stage: string
  message: string
  percent: number
  timestamp: number
}

const STAGE_ORDER = ['ANALYZING', 'GENERATING_SQL', 'EXECUTING', 'BUILDING_RESULT', 'DONE']

const MAX_RECONNECT_DELAY = 30000
const INITIAL_RECONNECT_DELAY = 1000

export function useQueryProgress() {
  const progress = ref<QueryProgress | null>(null)
  const connected = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectDelay = INITIAL_RECONNECT_DELAY
  let intentionalClose = false

  const connect = () => {
    if (ws?.readyState === WebSocket.OPEN || ws?.readyState === WebSocket.CONNECTING) return

    intentionalClose = false
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/query-progress`

    try {
      ws = new WebSocket(wsUrl)
    } catch {
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      connected.value = true
      reconnectDelay = INITIAL_RECONNECT_DELAY
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
      ws = null
      if (!intentionalClose) {
        console.log('[WebSocket] 连接断开，准备重连...')
        scheduleReconnect()
      }
    }

    ws.onerror = (err) => {
      console.error('[WebSocket] 连接错误', err)
      connected.value = false
    }
  }

  const scheduleReconnect = () => {
    if (reconnectTimer || intentionalClose) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelay)
    reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY)
  }

  const disconnect = () => {
    intentionalClose = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
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
