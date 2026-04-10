<template>
  <div class="space-y-6">
    <h1 class="text-3xl font-bold">Skills</h1>

    <!-- Capability Tags Filter -->
    <div class="flex flex-wrap gap-2 items-center">
      <span class="text-sm text-muted-foreground mr-2">Filter by tags:</span>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tag in availableTags"
          :key="tag"
          @click="toggleTag(tag)"
          :class="[
            'px-3 py-1 rounded-full text-sm font-medium transition-colors',
            selectedTags.includes(tag)
              ? 'bg-primary text-primary-foreground'
              : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
          ]"
        >
          {{ tag }}
        </button>
      </div>
      <button
        v-if="selectedTags.length > 0"
        @click="clearTags"
        class="text-sm text-muted-foreground hover:text-foreground underline"
      >
        Clear
      </button>
    </div>

    <!-- Sort tabs -->
    <div class="flex flex-wrap gap-2">
      <NuxtLink :to="`/skills?sort=createdAt${tagQuery}`">
        <button :class="['px-3 sm:px-4 py-2 rounded-md text-sm font-medium', sort === 'createdAt' ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']">
          <span class="sm:hidden">New</span>
          <span class="hidden sm:inline">Newest</span>
        </button>
      </NuxtLink>
      <NuxtLink :to="`/skills?sort=downloads${tagQuery}`">
        <button :class="['px-3 sm:px-4 py-2 rounded-md text-sm font-medium', sort === 'downloads' ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']">
          <span class="sm:hidden">Popular</span>
          <span class="hidden sm:inline">Most Downloaded</span>
        </button>
      </NuxtLink>
      <NuxtLink :to="`/skills?sort=stars${tagQuery}`">
        <button :class="['px-3 sm:px-4 py-2 rounded-md text-sm font-medium', sort === 'stars' ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']">
          <span class="sm:hidden">Starred</span>
          <span class="hidden sm:inline">Most Starred</span>
        </button>
      </NuxtLink>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
      <SkillCard v-for="skill in skills" :key="skill.id" :skill="skill" />
    </div>

    <!-- Pagination -->
    <div class="flex flex-col sm:flex-row justify-center gap-2 mt-8">
      <NuxtLink :to="`/skills?page=${page - 1}&sort=${sort}${tagQuery}`">
        <button :disabled="page === 0" class="w-full sm:w-auto px-4 py-2 rounded-md text-sm font-medium border border-input hover:bg-accent disabled:opacity-50">
          Previous
        </button>
      </NuxtLink>
      <NuxtLink :to="`/skills?page=${page + 1}&sort=${sort}${tagQuery}`">
        <button :disabled="skills.length < 12" class="w-full sm:w-auto px-4 py-2 rounded-md text-sm font-medium border border-input hover:bg-accent disabled:opacity-50">
          Next
        </button>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const router = useRouter()
const api = useApi()

const page = computed(() => parseInt(route.query.page as string) || 0)
const sort = computed(() => (route.query.sort as string) || 'createdAt')
const selectedTags = computed(() => {
  const tags = route.query.capabilityTags
  return tags ? (Array.isArray(tags) ? tags : [tags]) : []
})

const tagQuery = computed(() => {
  return selectedTags.value.length > 0 
    ? '&' + selectedTags.value.map(t => `capabilityTags=${encodeURIComponent(t)}`).join('&')
    : ''
})

// Available capability tags (could be fetched from API)
const availableTags = ['web-search', 'file-operation', 'browser', 'messaging', 'data-analysis']

const { data } = await useAsyncData(
  () => `skills-${page.value}-${sort.value}-${selectedTags.value.join(',')}`,
  () => api.getSkills({ 
    page: page.value, 
    size: 12, 
    sort: sort.value,
    capabilityTags: selectedTags.value.length > 0 ? selectedTags.value : undefined
  }),
  { watch: [page, sort, selectedTags] }
)

const skills = computed(() => data.value?.data || [])

function toggleTag(tag: string) {
  const currentTags = [...selectedTags.value]
  const index = currentTags.indexOf(tag)
  
  if (index > -1) {
    currentTags.splice(index, 1)
  } else {
    currentTags.push(tag)
  }
  
  const query: Record<string, any> = { sort: sort.value }
  if (currentTags.length > 0) {
    query.capabilityTags = currentTags
  }
  
  router.push({ path: '/skills', query })
}

function clearTags() {
  router.push({ path: '/skills', query: { sort: sort.value } })
}
</script>
