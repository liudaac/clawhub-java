import { useAuthStore } from '~/stores/auth'

export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore()
  
  if (!authStore.isAuthenticated) {
    return navigateTo('/')
  }
  
  const user = authStore.user
  if (!user || (user.role !== 'moderator' && user.role !== 'admin')) {
    return navigateTo('/')
  }
})
