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
      <div class="min-w-0">
        <h1 class="text-2xl sm:text-3xl font-bold">{{ skill.displayName }}</h1>
        <p class="text-muted-foreground mt-2">
          by @{{ skill.owner.handle }}
        </p>
      </div>
      <div class="flex flex-col sm:flex-row gap-2">
        <button 
          @click="toggleStar"
          :class="['px-4 py-2 rounded-md text-sm font-medium inline-flex items-center justify-center', hasStarred ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']"
        >
          <Star class="mr-2 h-4 w-4" />
          {{ hasStarred ? 'Starred' : 'Star' }}
          <span v-if="starCount !== undefined" class="ml-2">({{ formatNumber(starCount) }})</span>
        </button>
        <button class="px-4 py-2 rounded-md text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 inline-flex items-center justify-center">
          <Download class="mr-2 h-4 w-4" />
          Install
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="flex flex-wrap gap-4 sm:gap-6 text-sm text-muted-foreground">
      <span class="flex items-center">
        <Download class="w-4 h-4 mr-1" />
        {{ formatNumber(skill.statsDownloads) }} <span class="hidden sm:inline ml-1">downloads</span>
      </span>
      <span class="flex items-center">
        <Star class="w-4 h-4 mr-1" />
        {{ formatNumber(skill.statsStars) }} <span class="hidden sm:inline ml-1">stars</span>
      </span>
      <span class="flex items-center">
        <GitBranch class="w-4 h-4 mr-1" />
        {{ skill.statsVersions }} <span class="hidden sm:inline ml-1">versions</span>
      </span>
      <span class="flex items-center">
        <MessageCircle class="w-4 h-4 mr-1" />
        {{ skill.statsComments }} <span class="hidden sm:inline ml-1">comments</span>
      </span>
    </div>

    <!-- Capability Tags -->
    <div v-if="skill.capabilityTags && skill.capabilityTags.length > 0" class="flex flex-wrap gap-2">
      <span 
        v-for="tag in skill.capabilityTags" 
        :key="tag"
        class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary/10 text-primary"
      >
        {{ tag }}
      </span>
    </div>
    <div v-if="canEdit" class="mt-2">
      <button 
        @click="showTagEditor = true"
        class="text-sm text-primary hover:underline"
      >
        Edit tags
      </button>
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

    <!-- Tag Editor Modal -->
    <div v-if="showTagEditor" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-background rounded-lg p-4 sm:p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">Edit Capability Tags</h3>
        <div class="flex flex-wrap gap-2 mb-4">
          <span 
            v-for="tag in availableTags" 
            :key="tag"
            @click="toggleEditTag(tag)"
            :class="[
              'px-3 py-1 rounded-full text-sm font-medium cursor-pointer transition-colors',
              editTags.includes(tag)
                ? 'bg-primary text-primary-foreground'
                : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
            ]"
          >
            {{ tag }}
          </span>
        </div>
        <div class="flex flex-col sm:flex-row gap-2">
          <input 
            v-model="newTag"
            @keyup.enter="addNewTag"
            placeholder="Add custom tag..."
            class="flex-1 px-3 py-2 rounded-md border border-input bg-background"
          />
          <button 
            @click="addNewTag"
            class="px-4 py-2 rounded-md bg-primary text-primary-foreground"
          >
            Add
          </button>
        </div>
        <div class="flex flex-col sm:flex-row justify-end gap-2 mt-6">
          <button 
            @click="showTagEditor = false"
            class="px-4 py-2 rounded-md border border-input"
          >
            Cancel
          </button>
          <button 
            @click="saveTags"
            :disabled="saving"
            class="px-4 py-2 rounded-md bg-primary text-primary-foreground disabled:opacity-50"
          >
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
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

const { data: skillData, refresh: refreshSkill } = await useAsyncData(() => `skill-${slug}`, () => 
  api.getSkill(slug)
)

const skill = computed(() => skillData.value?.data)

// Check if current user can edit
const canEdit = computed(() => {
  if (!authStore.isAuthenticated || !skill.value) return false
  return skill.value.owner.id === authStore.user?.id || authStore.user?.role === 'admin'
})

// Tag editor state
const showTagEditor = ref(false)
const editTags = ref<string[]>([])
const newTag = ref('')
const saving = ref(false)

// Available capability tags
const availableTags = ['web-search', 'file-operation', 'browser', 'messaging', 'data-analysis', 'ai-generation', 'automation']

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

function toggleEditTag(tag: string) {
  const index = editTags.value.indexOf(tag)
  if (index > -1) {
    editTags.value.splice(index, 1)
  } else {
    editTags.value.push(tag)
  }
}

function addNewTag() {
  const tag = newTag.value.trim().toLowerCase()
  if (tag && !editTags.value.includes(tag)) {
    editTags.value.push(tag)
  }
  newTag.value = ''
}

async function saveTags() {
  saving.value = true
  try {
    await api.updateSkill(slug, { capabilityTags: editTags.value })
    await refreshSkill()
    showTagEditor.value = false
  } catch (error) {
    alert(error instanceof Error ? error.message : 'Failed to save tags')
  } finally {
    saving.value = false
  }
}

// Initialize edit tags when opening editor
watch(showTagEditor, (show) => {
  if (show && skill.value) {
    editTags.value = [...(skill.value.capabilityTags || [])]
  }
})

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
