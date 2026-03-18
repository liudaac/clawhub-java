import type { ApiResponse } from '~/types'

export function useApi() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  const api = $fetch.create({
    baseURL: config.public.apiBase,
    headers: {
      'Content-Type': 'application/json',
    },
    onRequest({ options }) {
      const token = authStore.token
      if (token) {
        options.headers = {
          ...options.headers,
          Authorization: `Bearer ${token}`,
        }
      }
    },
    onResponseError({ response }) {
      if (response.status === 401) {
        authStore.logout()
        navigateTo('/')
      }
    },
  })

  return {
    // Auth
    whoami: () => api<ApiResponse<User>>('/auth/whoami'),
    logout: () => api<ApiResponse<void>>('/auth/logout', { method: 'POST' }),

    // Skills
    getSkills: (params?: { page?: number; size?: number; sort?: string }) => 
      api<ApiResponse<Skill[]>>('/skills', { query: params }),
    getSkill: (slug: string) => 
      api<ApiResponse<Skill>>(`/skills/${slug}`),
    createSkill: (data: { slug: string; displayName: string; summary?: string }) => 
      api<ApiResponse<Skill>>('/skills', { method: 'POST', body: data }),

    // Comments
    getComments: (slug: string) => 
      api<ApiResponse<Comment[]>>(`/skills/${slug}/comments`),
    createComment: (slug: string, body: string) => 
      api<ApiResponse<Comment>>(`/skills/${slug}/comments`, { method: 'POST', body: { body } }),

    // Stars
    starSkill: (slug: string) => 
      api<ApiResponse<void>>(`/skills/${slug}/stars`, { method: 'POST' }),
    unstarSkill: (slug: string) => 
      api<ApiResponse<void>>(`/skills/${slug}/stars`, { method: 'DELETE' }),
    checkStar: (slug: string) => 
      api<ApiResponse<{ hasStarred: boolean; count: number }>>(`/skills/${slug}/stars/check`),

    // Search
    search: (q: string, type?: 'skills' | 'souls' | 'all') => 
      api<ApiResponse<unknown>>('/search', { query: { q, type } }),
  }
}
