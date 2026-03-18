<template>
  <header class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur">
    <div class="container mx-auto px-4 flex h-14 items-center">
      <!-- Logo -->
      <NuxtLink to="/" class="mr-6 flex items-center space-x-2">
        <span class="text-xl font-bold">ClawHub</span>
      </NuxtLink>

      <!-- Search -->
      <form @submit.prevent="handleSearch" class="flex-1 max-w-md">
        <div class="relative">
          <Search class="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <input
            v-model="searchQuery"
            type="search"
            placeholder="Search skills..."
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-8 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>
      </form>

      <!-- Navigation -->
      <nav class="ml-auto flex items-center space-x-4">
        <NuxtLink to="/skills">
          <button :class="['px-4 py-2 rounded-md text-sm font-medium', route.path.startsWith('/skills') ? 'bg-primary text-primary-foreground' : 'hover:bg-accent']">
            Skills
          </button>
        </NuxtLink>

        <!-- Dark Mode Toggle -->
        <button
          @click="toggleColorMode"
          class="p-2 rounded-md hover:bg-accent"
        >
          <Sun v-if="colorMode.value === 'dark'" class="h-4 w-4" />
          <Moon v-else class="h-4 w-4" />
        </button>

        <!-- Auth -->
        <template v-if="authStore.isAuthenticated">
          <NuxtLink to="/upload">
            <button class="p-2 rounded-md hover:bg-accent">
              <Upload class="h-4 w-4" />
            </button>
          </NuxtLink>
          <button @click="authStore.logout" class="px-4 py-2 rounded-md text-sm font-medium hover:bg-accent">
            Logout
          </button>
        </template>
        <button v-else @click="handleLogin" class="px-4 py-2 rounded-md text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90">
          Login with GitHub
        </button>
      </nav>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Search, Sun, Moon, Upload } from 'lucide-vue-next'

const route = useRoute()
const authStore = useAuthStore()
const colorMode = useColorMode()

const searchQuery = ref('')

function handleSearch() {
  if (searchQuery.value.trim()) {
    navigateTo(`/search?q=${encodeURIComponent(searchQuery.value)}`)
  }
}

function handleLogin() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}

function toggleColorMode() {
  colorMode.preference = colorMode.value === 'dark' ? 'light' : 'dark'
}
</script>
