import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  id: number
  username: string
  avatar?: string
  roles?: string[]
}

/**
 * 用户状态管理
 *
 * 改造说明：
 * - 替代原 localStorage / URL query 传参的用户状态管理
 * - 支持持久化（localStorage 自动同步）
 */
export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('chatbi_token') || '')
  const refreshToken = ref<string>(localStorage.getItem('chatbi_refresh_token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => !!token.value)

  // Actions
  function setToken(newToken: string, newRefreshToken?: string) {
    token.value = newToken
    localStorage.setItem('chatbi_token', newToken)
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('chatbi_refresh_token', newRefreshToken)
    }
  }

  function setUserInfo(info: UserInfo | null) {
    userInfo.value = info
    if (info) {
      localStorage.setItem('chatbi_user', JSON.stringify(info))
    } else {
      localStorage.removeItem('chatbi_user')
    }
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('chatbi_token')
    localStorage.removeItem('chatbi_refresh_token')
    localStorage.removeItem('chatbi_user')
  }

  function restoreFromStorage() {
    const storedUser = localStorage.getItem('chatbi_user')
    if (storedUser) {
      try {
        userInfo.value = JSON.parse(storedUser)
      } catch {
        localStorage.removeItem('chatbi_user')
      }
    }
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout,
    restoreFromStorage
  }
})
