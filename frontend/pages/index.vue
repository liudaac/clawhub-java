<template>
  <div class="space-y-12">
    <!-- Hero -->
    <section class="text-center py-8 sm:py-12">
      <h1 class="text-3xl sm:text-4xl font-bold mb-4">Discover & Share Skills</h1>
      <p class="text-lg sm:text-xl text-muted-foreground mb-6 sm:mb-8 max-w-2xl mx-auto px-4 sm:px-0">
        ClawHub is a registry for AI skills and agent souls.
        Find, install, and publish skills for your agents.
      </p>
      <div class="flex flex-col sm:flex-row justify-center gap-3 sm:gap-4 px-4 sm:px-0">
        <NuxtLink to="/skills">
          <button class="w-full sm:w-auto px-6 sm:px-8 py-3 rounded-md text-base sm:text-lg font-medium bg-primary text-primary-foreground hover:bg-primary/90 inline-flex items-center justify-center">
            Browse Skills
            <ArrowRight class="ml-2 h-5 w-5" />
          </button>
        </NuxtLink>
        <NuxtLink to="/upload">
          <button class="w-full sm:w-auto px-6 sm:px-8 py-3 rounded-md text-base sm:text-lg font-medium border border-input hover:bg-accent inline-flex items-center justify-center">
            Publish Skill
          </button>
        </NuxtLink>
      </div>
    </section>

    <!-- Trending -->
    <section>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold">Trending Skills</h2>
        <NuxtLink to="/skills?sort=downloads">
          <button class="text-sm font-medium hover:underline">View all</button>
        </NuxtLink>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <SkillCard v-for="skill in trendingSkills" :key="skill.id" :skill="skill" />
      </div>
    </section>

    <!-- New -->
    <section>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold">New Skills</h2>
        <NuxtLink to="/skills">
          <button class="text-sm font-medium hover:underline">View all</button>
        </NuxtLink>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <SkillCard v-for="skill in newSkills" :key="skill.id" :skill="skill" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight } from 'lucide-vue-next'
import type { Skill } from '~/types'

const api = useApi()

const { data: trendingData } = await useAsyncData('trending', () => 
  api.getSkills({ page: 0, size: 6, sort: 'downloads' })
)

const { data: newData } = await useAsyncData('new', () => 
  api.getSkills({ page: 0, size: 6, sort: 'createdAt' })
)

const trendingSkills = computed(() => trendingData.value?.data || [])
const newSkills = computed(() => newData.value?.data || [])
</script>
