import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface ConversationMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  sql?: string
  chartType?: string
  diagnosis?: unknown
  timestamp: number
}

/**
 * 对话状态管理
 *
 * 改造说明：
 * - 替代原 ConversationQuery.vue 中本地 ref 管理的 messages 数组
 * - 支持多轮对话、流式输出缓冲区、消息分支（预留）
 * - 为后续 SSE 流式改造做准备
 */
export const useConversationStore = defineStore('conversation', () => {
  // State
  const messages = ref<ConversationMessage[]>([])
  const conversationId = ref<string>('')
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const currentStreamMessageId = ref<string>('')

  // Getters
  const messageCount = computed(() => messages.value.length)
  const lastMessage = computed(() => messages.value[messages.value.length - 1] || null)

  // Actions
  function startConversation(id?: string) {
    conversationId.value = id || `conv_${Date.now()}`
    messages.value = []
    isStreaming.value = false
    streamingContent.value = ''
    currentStreamMessageId.value = ''
  }

  function addMessage(message: ConversationMessage) {
    messages.value.push(message)
  }

  function updateMessage(id: string, updates: Partial<ConversationMessage>) {
    const idx = messages.value.findIndex(m => m.id === id)
    if (idx >= 0) {
      messages.value[idx] = { ...messages.value[idx], ...updates }
    }
  }

  function startStreaming(messageId: string) {
    isStreaming.value = true
    streamingContent.value = ''
    currentStreamMessageId.value = messageId
  }

  function appendStreamChunk(chunk: string) {
    streamingContent.value += chunk
  }

  function finishStreaming() {
    isStreaming.value = false
    // 将流式内容固化到消息中
    const idx = messages.value.findIndex(m => m.id === currentStreamMessageId.value)
    if (idx >= 0) {
      messages.value[idx].content = streamingContent.value
    }
    streamingContent.value = ''
    currentStreamMessageId.value = ''
  }

  function clearConversation() {
    messages.value = []
    conversationId.value = ''
    isStreaming.value = false
    streamingContent.value = ''
    currentStreamMessageId.value = ''
  }

  return {
    messages,
    conversationId,
    isStreaming,
    streamingContent,
    currentStreamMessageId,
    messageCount,
    lastMessage,
    startConversation,
    addMessage,
    updateMessage,
    startStreaming,
    appendStreamChunk,
    finishStreaming,
    clearConversation
  }
})
