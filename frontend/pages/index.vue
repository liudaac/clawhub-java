<template>
  <div class="space-y-12">
    <!-- Hero -->
    <section class="text-center py-12">
      <h1 class="text-4xl font-bold mb-4">Discover & Share Skills</h1>
      <p class="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
        ClawHub is a registry for AI skills and agent souls. 
        Find, install, and publish skills for your agents.
      </p>
      <div class="flex justify-center space-x-4">
        <NuxtLink to="/skills">
          <button class="px-8 py-3 rounded-md text-lg font-medium bg-primary text-primary-foreground hover:bg-primary/90 inline-flex items-center">
            Browse Skills
            <ArrowRight class="ml-2 h-5 w-5" />
          </button>
        </NuxtLink>
        <NuxtLink to="/upload">
          <button class="px-8 py-3 rounded-md text-lg font-medium border border-input hover:bg-accent inline-flex items-center">
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
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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
