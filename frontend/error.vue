<template>
  <div class="min-h-screen flex items-center justify-center bg-background">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-primary mb-4">{{ error.statusCode }}</h1>
      <h2 class="text-2xl font-semibold mb-4">{{ error.statusMessage || 'An error occurred' }}</h2>
      <p class="text-muted-foreground mb-8 max-w-md mx-auto">
        {{ error.description || 'Sorry, something went wrong. Please try again later.' }}
      </p>
      <div class="flex gap-4 justify-center">
        <NuxtLink to="/">
          <button class="px-6 py-3 rounded-md bg-primary text-primary-foreground hover:bg-primary/90 font-medium">
            Go Home
          </button>
        </NuxtLink>
        <button 
          @click="handleError"
          class="px-6 py-3 rounded-md border border-input hover:bg-accent font-medium"
        >
          Try Again
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  error: {
    statusCode: number
    statusMessage?: string
    description?: string
  }
}>()

const handleError = () => {
  clearError()
  navigateTo('/')
}
</script>
