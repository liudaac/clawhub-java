<template>
  <header class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur">
    <div class="container mx-auto px-4 flex h-14 items-center">
      <!-- Logo -->
      <NuxtLink to="/" class="mr-4 md:mr-6 flex items-center space-x-2">
        <span class="text-lg md:text-xl font-bold">ClawHub</span>
      </NuxtLink>

      <!-- Search - Hidden on mobile, shown on md+ -->
      <form @submit.prevent="handleSearch" class="hidden md:block flex-1 max-w-md">
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

      <!-- Desktop Navigation -->
      <nav class="hidden md:flex ml-auto items-center space-x-4">
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
          Login
        </button>
      </nav>

      <!-- Mobile Menu Button -->
      <button
        @click="mobileMenuOpen = !mobileMenuOpen"
        class="md:hidden ml-auto p-2 rounded-md hover:bg-accent"
      >
        <Menu v-if="!mobileMenuOpen" class="h-5 w-5" />
        <X v-else class="h-5 w-5" />
      </button>
    </div>

    <!-- Mobile Menu -->
    <div v-if="mobileMenuOpen" class="md:hidden border-t bg-background max-h-[calc(100vh-3.5rem)] overflow-y-auto">
      <!-- Mobile Search -->
      <form @submit.prevent="handleSearch" class="p-4 border-b">
        <div class="relative">
          <Search class="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <input
            v-model="searchQuery"
            type="search"
            placeholder="Search skills..."
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-8 text-sm"
          />
        </div>
      </form>

      <!-- Mobile Navigation -->
      <nav class="p-4 space-y-2">
        <NuxtLink to="/skills" @click="mobileMenuOpen = false">
          <button :class="['w-full px-4 py-3 rounded-md text-sm font-medium text-left', route.path.startsWith('/skills') ? 'bg-primary text-primary-foreground' : 'hover:bg-accent']">
            Skills
          </button>
        </NuxtLink>

        <button
          @click="toggleColorMode"
          class="w-full px-4 py-3 rounded-md text-sm font-medium text-left hover:bg-accent flex items-center gap-2"
        >
          <Sun v-if="colorMode.value === 'dark'" class="h-4 w-4" />
          <Moon v-else class="h-4 w-4" />
          {{ colorMode.value === 'dark' ? 'Light Mode' : 'Dark Mode' }}
        </button>

        <template v-if="authStore.isAuthenticated">
          <NuxtLink to="/upload" @click="mobileMenuOpen = false">
            <button class="w-full px-4 py-3 rounded-md text-sm font-medium text-left hover:bg-accent">
              Upload Skill
            </button>
          </NuxtLink>
          <button @click="() => { authStore.logout(); mobileMenuOpen = false }" class="w-full px-4 py-3 rounded-md text-sm font-medium text-left hover:bg-accent">
            Logout
          </button>
        </template>
        <button v-else @click="() => { handleLogin(); mobileMenuOpen = false }" class="w-full px-4 py-3 rounded-md text-sm font-medium text-left bg-primary text-primary-foreground">
          Login with GitHub
        </button>
      </nav>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Search, Sun, Moon, Upload, Menu, X } from 'lucide-vue-next'

const route = useRoute()
const authStore = useAuthStore()
const colorMode = useColorMode()

const searchQuery = ref('')
const mobileMenuOpen = ref(false)

function handleSearch() {
  if (searchQuery.value.trim()) {
    navigateTo(`/search?q=${encodeURIComponent(searchQuery.value)}`)
    mobileMenuOpen.value = false
  }
}

function handleLogin() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}

function toggleColorMode() {
  colorMode.preference = colorMode.value === 'dark' ? 'light' : 'dark'
}

// Close mobile menu on route change
watch(() => route.path, () => {
  mobileMenuOpen.value = false
})
</script>
