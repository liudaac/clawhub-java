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

    <!-- Trusted Publishing (Owner Only) -->
    <div v-if="canEdit" class="rounded-lg border bg-card text-card-foreground shadow-sm">
      <div class="flex flex-col space-y-1.5 p-6">
        <h3 class="text-2xl font-semibold leading-none tracking-tight">Trusted Publishing</h3>
        <p class="text-sm text-muted-foreground">Configure GitHub Actions OIDC trusted publishers</p>
      </div>
      <div class="p-6 pt-0 space-y-4">
        <!-- Trusted Publishers List -->
        <div v-if="trustedPublishers.length > 0" class="space-y-3">
          <div v-for="publisher in trustedPublishers" :key="publisher.id" class="flex items-center justify-between p-3 rounded-md border bg-muted/50">
            <div class="min-w-0">
              <p class="font-medium truncate">{{ publisher.repository }}</p>
              <p class="text-sm text-muted-foreground">{{ publisher.workflowFilename }} • {{ publisher.environment }}</p>
            </div>
            <button
              @click="deletePublisher(publisher.id)"
              :disabled="deletingPublisher === publisher.id"
              class="px-3 py-1.5 rounded-md text-sm font-medium text-destructive hover:bg-destructive/10 disabled:opacity-50"
            >
              {{ deletingPublisher === publisher.id ? 'Deleting...' : 'Delete' }}
            </button>
          </div>
        </div>
        <div v-else class="text-sm text-muted-foreground">
          No trusted publishers configured. Add one below to enable OIDC publishing.
        </div>

        <!-- Add Trusted Publisher Form -->
        <div class="border-t pt-4">
          <h4 class="font-medium mb-3">Add Trusted Publisher</h4>
          <div class="space-y-3">
            <div>
              <label class="text-sm font-medium mb-1.5 block">Repository (owner/repo)</label>
              <input
                v-model="newPublisher.repository"
                placeholder="e.g., myorg/myrepo"
                class="w-full px-3 py-2 rounded-md border border-input bg-background"
              />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-sm font-medium mb-1.5 block">Repository ID</label>
                <input
                  v-model="newPublisher.repositoryId"
                  placeholder="GitHub repo ID"
                  class="w-full px-3 py-2 rounded-md border border-input bg-background"
                />
              </div>
              <div>
                <label class="text-sm font-medium mb-1.5 block">Repository Owner ID</label>
                <input
                  v-model="newPublisher.repositoryOwnerId"
                  placeholder="GitHub owner ID"
                  class="w-full px-3 py-2 rounded-md border border-input bg-background"
                />
              </div>
            </div>
            <div>
              <label class="text-sm font-medium mb-1.5 block">Workflow Filename</label>
              <input
                v-model="newPublisher.workflowFilename"
                placeholder="e.g., publish.yml"
                class="w-full px-3 py-2 rounded-md border border-input bg-background"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-1.5 block">Environment</label>
              <input
                v-model="newPublisher.environment"
                placeholder="e.g., production"
                class="w-full px-3 py-2 rounded-md border border-input bg-background"
              />
            </div>
            <button
              @click="addPublisher"
              :disabled="addingPublisher || !isValidPublisher"
              class="w-full px-4 py-2 rounded-md text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {{ addingPublisher ? 'Adding...' : 'Add Trusted Publisher' }}
            </button>
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

// Trusted publishers state
const trustedPublishers = ref<TrustedPublisher[]>([])
const newPublisher = ref<Partial<TrustedPublisherRequest>>({
  repository: '',
  repositoryId: '',
  repositoryOwner: '',
  repositoryOwnerId: '',
  workflowFilename: 'package-publish.yml',
  environment: 'production'
})
const addingPublisher = ref(false)
const deletingPublisher = ref<string | null>(null)

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

// Load trusted publishers
async function loadTrustedPublishers() {
  if (!canEdit.value || !skill.value) return
  try {
    const response = await api.getTrustedPublishers(slug)
    trustedPublishers.value = response.data || []
  } catch (error) {
    console.error('Failed to load trusted publishers:', error)
  }
}

// Add trusted publisher
async function addPublisher() {
  if (!skill.value || !isValidPublisher.value) return
  addingPublisher.value = true
  try {
    const [owner, repo] = newPublisher.value.repository!.split('/')
    await api.createTrustedPublisher(slug, {
      repository: newPublisher.value.repository!,
      repositoryId: newPublisher.value.repositoryId!,
      repositoryOwner: owner,
      repositoryOwnerId: newPublisher.value.repositoryOwnerId!,
      workflowFilename: newPublisher.value.workflowFilename!,
      environment: newPublisher.value.environment!
    })
    // Reset form
    newPublisher.value = {
      repository: '',
      repositoryId: '',
      repositoryOwner: '',
      repositoryOwnerId: '',
      workflowFilename: 'package-publish.yml',
      environment: 'production'
    }
    await loadTrustedPublishers()
  } catch (error) {
    alert(error instanceof Error ? error.message : 'Failed to add trusted publisher')
  } finally {
    addingPublisher.value = false
  }
}

// Delete trusted publisher
async function deletePublisher(publisherId: string) {
  if (!skill.value) return
  deletingPublisher.value = publisherId
  try {
    await api.deleteTrustedPublisher(slug, publisherId)
    await loadTrustedPublishers()
  } catch (error) {
    alert(error instanceof Error ? error.message : 'Failed to delete trusted publisher')
  } finally {
    deletingPublisher.value = null
  }
}

// Computed
const isValidPublisher = computed(() => {
  return newPublisher.value.repository?.includes('/') &&
    newPublisher.value.repositoryId &&
    newPublisher.value.repositoryOwnerId &&
    newPublisher.value.workflowFilename
})

// Load trusted publishers on mount
onMounted(() => {
  loadTrustedPublishers()
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
