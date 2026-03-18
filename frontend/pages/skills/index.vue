<template>
  <div class="space-y-6">
    <h1 class="text-3xl font-bold">Skills</h1>

    <!-- Sort tabs -->
    <div class="flex gap-2">
      <NuxtLink to="/skills?sort=createdAt">
        <button :class="['px-4 py-2 rounded-md text-sm font-medium', sort === 'createdAt' ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']">
          Newest
        </button>
      </NuxtLink>
      <NuxtLink to="/skills?sort=downloads">
        <button :class="['px-4 py-2 rounded-md text-sm font-medium', sort === 'downloads' ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']">
          Most Downloaded
        </button>
      </NuxtLink>
      <NuxtLink to="/skills?sort=stars">
        <button :class="['px-4 py-2 rounded-md text-sm font-medium', sort === 'stars' ? 'bg-primary text-primary-foreground' : 'border border-input hover:bg-accent']">
          Most Starred
        </button>
      </NuxtLink>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <SkillCard v-for="skill in skills" :key="skill.id" :skill="skill" />
    </div>

    <!-- Pagination -->
    <div class="flex justify-center gap-2 mt-8">
      <NuxtLink :to="`/skills?page=${page - 1}&sort=${sort}`">
        <button :disabled="page === 0" class="px-4 py-2 rounded-md text-sm font-medium border border-input hover:bg-accent disabled:opacity-50">
          Previous
        </button>
      </NuxtLink>
      <NuxtLink :to="`/skills?page=${page + 1}&sort=${sort}`">
        <button :disabled="skills.length < 12" class="px-4 py-2 rounded-md text-sm font-medium border border-input hover:bg-accent disabled:opacity-50">
          Next
        </button>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const api = useApi()

const page = computed(() => parseInt(route.query.page as string) || 0)
const sort = computed(() => (route.query.sort as string) || 'createdAt')

const { data } = await useAsyncData(
  () => `skills-${page.value}-${sort.value}`,
  () => api.getSkills({ page: page.value, size: 12, sort: sort.value }),
  { watch: [page, sort] }
)

const skills = computed(() => data.value?.data || [])
</script>
