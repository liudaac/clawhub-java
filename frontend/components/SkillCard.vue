<template>
  <NuxtLink :to="`/skills/${skill.slug}`">
    <div class="rounded-lg border bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow h-full">
      <div class="flex flex-col space-y-1.5 p-6">
        <h3 class="text-lg font-semibold leading-none tracking-tight">{{ skill.displayName }}</h3>
        <p class="text-sm text-muted-foreground">@{{ skill.owner.handle }}</p>
      </div>
      <div class="p-6 pt-0">
        <p class="text-sm text-muted-foreground line-clamp-2 mb-4">
          {{ skill.summary || 'No description' }}
        </p>
        <div class="flex items-center space-x-4 text-sm text-muted-foreground">
          <span class="flex items-center">
            <Download class="w-4 h-4 mr-1" />
            {{ formatNumber(skill.statsDownloads) }}
          </span>
          <span class="flex items-center">
            <Star class="w-4 h-4 mr-1" />
            {{ formatNumber(skill.statsStars) }}
          </span>
        </div>
      </div>
    </div>
  </NuxtLink>
</template>

<script setup lang="ts">
import { Download, Star } from 'lucide-vue-next'
import type { Skill } from '~/types'

defineProps<{
  skill: Skill
}>()

function formatNumber(num: number): string {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}
</script>
