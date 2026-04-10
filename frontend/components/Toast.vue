<template>
  <Teleport to="body">
    <TransitionGroup
      tag="div"
      class="fixed bottom-4 right-4 z-50 space-y-2"
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="transform translate-y-2 opacity-0"
      enter-to-class="transform translate-y-0 opacity-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="transform translate-y-0 opacity-100"
      leave-to-class="transform translate-y-2 opacity-0"
    >
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="[
          'px-4 py-3 rounded-lg shadow-lg max-w-sm',
          toast.type === 'error' ? 'bg-destructive text-destructive-foreground' :
          toast.type === 'warning' ? 'bg-warning text-warning-foreground' :
          toast.type === 'success' ? 'bg-success text-success-foreground' :
          'bg-primary text-primary-foreground'
        ]"
      >
        <div class="flex items-start gap-2">
          <div class="flex-1">
            <p v-if="toast.title" class="font-semibold text-sm">{{ toast.title }}</p>
            <p class="text-sm">{{ toast.message }}</p>
          </div>
          <button
            @click="removeToast(toast.id)"
            class="opacity-70 hover:opacity-100"
          >
            <X class="w-4 h-4" />
          </button>
        </div>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup lang="ts">
import { X } from 'lucide-vue-next'

interface Toast {
  id: string
  type: 'info' | 'success' | 'warning' | 'error'
  title?: string
  message: string
}

const toasts = ref<Toast[]>([])

function showToast(type: Toast['type'], message: string, title?: string) {
  const id = Math.random().toString(36).substring(2, 9)
  toasts.value.push({ id, type, message, title })
  
  setTimeout(() => {
    removeToast(id)
  }, 5000)
}

function removeToast(id: string) {
  const index = toasts.value.findIndex(t => t.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

// Export for global use
provide('toast', {
  show: showToast,
  success: (message: string, title?: string) => showToast('success', message, title),
  error: (message: string, title?: string) => showToast('error', message, title),
  warning: (message: string, title?: string) => showToast('warning', message, title),
  info: (message: string, title?: string) => showToast('info', message, title),
})
</script>
