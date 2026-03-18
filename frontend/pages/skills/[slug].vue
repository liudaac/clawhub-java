<template>
  <div v-if="skill" class="space-y-8">
    <!-- Back button -->
    <NuxtLink to="/skills">
      <button class="inline-flex items-center px-4 py-2 rounded-md text-sm font-medium hover:bg-accent">
        <ArrowLeft class="mr-2 h-4 w-4" />
        Back to Skills
      </button>
    </NuxtLink>

    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold">{{ skill.displayName }}</h1>
        <p class="text-muted-foreground mt-2">
          by @{{ skill.owner.handle }}
        </p>
      </div>
      <div class="flex gap-2">
        <button 
          @click="toggleStar"
          :class="['px-4 py-2 rounded-md text-sm font-medium inline-flex items-center', hasStarred ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']"
        >
          <Star class="mr-2 h-4 w-4" />
          {{ hasStarred ? 'Starred' : 'Star' }}
          <span v-if="starCount !== undefined" class="ml-2">({{ formatNumber(starCount) }})</span>
        </button>
        <button class="px-4 py-2 rounded-md text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 inline-flex items-center">
          <Download class="mr-2 h-4 w-4" />
          Install
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="flex gap-6 text-sm text-muted-foreground">
      <span class="flex items-center">
        <Download class="w-4 h-4 mr-1" />
        {{ formatNumber(skill.statsDownloads) }} downloads
      </span>
      <span class="flex items-center">
        <Star class="w-4 h-4 mr-1" />
        {{ formatNumber(skill.statsStars) }} stars
      </span>
      <span class="flex items-center">
        <GitBranch class="w-4 h-4 mr-1" />
        {{ skill.statsVersions }} versions
      </span>
      <span class="flex items-center">
        <MessageCircle class="w-4 h-4 mr-1" />
        {{ skill.statsComments }} comments
      </span>
    </div>

    <!-- Description -->
    <div v-if="skill.summary" class="rounded-lg border bg-card text-card-foreground shadow-sm">
      <div class="flex flex-col space-y-1.5 p-6">
        <h3 class="text-2xl font-semibold leading-none tracking-tight">Description</h3>
      </div>
      <div class="p-6 pt-0">
        <p class="whitespace-pre-wrap">{{ skill.summary }}</p>
      </div>
    </div>

    <!-- Latest Version -->
    <div v-if="skill.latestVersion" class="rounded-lg border bg-card text-card-foreground shadow-sm">
      <div class="flex flex-col space-y-1.5 p-6">
        <h3 class="text-2xl font-semibold leading-none tracking-tight">Latest Version</h3>
        <p class="text-sm text-muted-foreground">{{ skill.latestVersion.version }}</p>
      </div>
      <div class="p-6 pt-0">
        <div class="space-y-4">
          <div>
            <h4 class="font-medium mb-2">Changelog</h4>
            <p class="text-sm text-muted-foreground whitespace-pre-wrap">{{ skill.latestVersion.changelog }}</p>
          </div>
          <div class="text-sm text-muted-foreground">
            Released on {{ formatDate(skill.latestVersion.createdAt) }}
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="text-center py-12">
    Loading...
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Star, Download, GitBranch, MessageCircle } from 'lucide-vue-next'

const route = useRoute()
const api = useApi()
const authStore = useAuthStore()

const slug = route.params.slug as string

const { data: skillData } = await useAsyncData(() => `skill-${slug}`, () => 
  api.getSkill(slug)
)

const skill = computed(() => skillData.value?.data)

// Star status
const { data: starData } = await useAsyncData(() => `star-${slug}`, () => 
  authStore.isAuthenticated ? api.checkStar(slug) : Promise.resolve(null)
)

const hasStarred = computed(() => starData.value?.data?.hasStarred ?? false)
const starCount = computed(() => starData.value?.data?.count ?? skill.value?.statsStars)

async function toggleStar() {
  if (!authStore.isAuthenticated) {
    navigateTo('/')
    return
  }
  
  if (hasStarred.value) {
    await api.unstarSkill(slug)
  } else {
    await api.starSkill(slug)
  }
  // Refresh star status
  refreshNuxtData(`star-${slug}`)
}

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
</script>
