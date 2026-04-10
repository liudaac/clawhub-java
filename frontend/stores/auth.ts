import { defineStore } from 'pinia'
import type { User } from '~/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isAuthenticated = computed(() => !!token.value)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  function setAuth(userData: User, tokenValue: string) {
    user.value = userData
    token.value = tokenValue
    try {
      localStorage.setItem('token', tokenValue)
    } catch (e) {
      // Safari private mode may throw
      console.warn('Failed to save token to localStorage')
    }
  }

  function logout() {
    user.value = null
    token.value = null
    try {
      localStorage.removeItem('token')
    } catch (e) {
      // Ignore errors in private mode
    }
  }

  async function init() {
    // Check for token from OAuth callback
    if (process.client) {
      const urlParams = new URLSearchParams(window.location.search)
      const oauthToken = urlParams.get('token')
      
      if (oauthToken) {
        token.value = oauthToken
        try {
          localStorage.setItem('token', oauthToken)
        } catch (e) {
          console.warn('Failed to save token to localStorage')
        }
        // Clean up URL
        window.history.replaceState({}, document.title, window.location.pathname)
        // Fetch user info
        await fetchUser()
        return
      }
    }
    
    // Try to restore from localStorage
    try {
      const savedToken = localStorage.getItem('token')
      if (savedToken) {
        token.value = savedToken
        await fetchUser()
      }
    } catch (e) {
      // Safari private mode may throw
      console.warn('Failed to access localStorage')
    }
  }

  async function fetchUser() {
    if (!token.value) return
    
    isLoading.value = true
    error.value = null
    
    try {
      const api = useApi()
      const response = await api.whoami()
      if (response.success && response.data) {
        user.value = response.data
      } else {
        // Token invalid, clear it
        logout()
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to fetch user'
      logout()
    } finally {
      isLoading.value = false
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    isLoading,
    error,
    setAuth,
    logout,
    init,
    fetchUser,
  }
})
