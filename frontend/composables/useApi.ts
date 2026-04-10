import type { ApiResponse } from '~/types'

export function useApi() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()
  const toast = inject('toast') as { warning: (message: string, title?: string) => void }

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
      if (response.status === 429) {
        const retryAfter = response._data?.retryAfter || 60
        const message = `Rate limit exceeded. Please try again in ${retryAfter} seconds.`
        if (toast) {
          toast.warning(message, 'Too Many Requests')
        }
        throw new Error(message)
      }
    },
  })

  return {
    // Auth
    whoami: () => api<ApiResponse<User>>('/auth/whoami'),
    logout: () => api<ApiResponse<void>>('/auth/logout', { method: 'POST' }),

    // Skills
    getSkills: (params?: { page?: number; size?: number; sort?: string; capabilityTags?: string[] }) => 
      api<ApiResponse<Skill[]>>('/skills', { query: params }),
    getSkill: (slug: string) => 
      api<ApiResponse<Skill>>(`/skills/${slug}`),
    createSkill: (data: { slug: string; displayName: string; summary?: string }) => 
      api<ApiResponse<Skill>>('/skills', { method: 'POST', body: data }),
    updateSkill: (slug: string, data: { displayName?: string; summary?: string; capabilityTags?: string[] }) =>
      api<ApiResponse<Skill>>(`/skills/${slug}`, { method: 'PATCH', body: data }),

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

    // Admin / Moderation
    getPendingReview: (page?: number, size?: number) =>
      api<ApiResponse<Skill[]>>(`/admin/moderation/pending`, { query: { page, size } }),
    getHiddenSkills: (page?: number, size?: number) =>
      api<ApiResponse<Skill[]>>(`/admin/moderation/hidden`, { query: { page, size } }),
    hideSkill: (id: string, reason: string, note: string) =>
      api<ApiResponse<Skill>>(`/admin/skills/${id}/hide`, {
        method: 'POST',
        body: { reason, note }
      }),
    unhideSkill: (id: string, note: string) =>
      api<ApiResponse<Skill>>(`/admin/skills/${id}/unhide`, {
        method: 'POST',
        body: { note }
      }),

    // Trusted Publishers
    getTrustedPublishers: (slug: string) =>
      api<ApiResponse<TrustedPublisher[]>>(`/skills/${slug}/trusted-publishers`),
    createTrustedPublisher: (slug: string, data: TrustedPublisherRequest) =>
      api<ApiResponse<TrustedPublisher>>(`/skills/${slug}/trusted-publishers`, {
        method: 'POST',
        body: data
      }),
    deleteTrustedPublisher: (slug: string, publisherId: string) =>
      api<ApiResponse<void>>(`/skills/${slug}/trusted-publishers/${publisherId}`, {
        method: 'DELETE'
      }),

    // OIDC Token Exchange
    exchangeOidcToken: (data: OidcTokenRequest) =>
      api<ApiResponse<OidcTokenResponse>>('/auth/oidc/token', {
        method: 'POST',
        body: data
      }),
  }
}
