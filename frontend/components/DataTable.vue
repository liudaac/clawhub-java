<template>
  <!-- Desktop Table -->
  <div class="hidden md:block rounded-md border overflow-x-auto">
    <table class="w-full text-sm">
      <thead class="bg-muted">
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            :class="[
              'h-12 px-4 text-left align-middle font-medium text-muted-foreground whitespace-nowrap',
              column.sortable && 'cursor-pointer hover:text-foreground'
            ]"
            @click="column.sortable && toggleSort(column.key)"
          >
            <div class="flex items-center gap-1">
              {{ column.title }}
              <span v-if="column.sortable" class="text-xs">
                <template v-if="sortBy === column.key">
                  {{ sortDir === 'asc' ? '↑' : '↓' }}
                </template>
                <template v-else>⇅</template>
              </span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in sortedData"
          :key="getRowKey(row)"
          class="border-t transition-colors hover:bg-muted/50"
        >
          <td
            v-for="column in columns"
            :key="column.key"
            class="p-4 align-middle"
          >
            <slot :name="column.key" :row="row" :value="getValue(row, column.key)">
              {{ getValue(row, column.key) }}
            </slot>
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columns.length" class="p-8 text-center text-muted-foreground">
            No data available
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Mobile Cards -->
  <div class="md:hidden space-y-4">
    <div
      v-for="row in sortedData"
      :key="getRowKey(row)"
      class="rounded-lg border bg-card p-4 space-y-3"
    >
      <div v-for="column in columns" :key="column.key" class="flex flex-col gap-1">
        <span class="text-xs font-medium text-muted-foreground">{{ column.title }}</span>
        <div class="text-sm">
          <slot :name="column.key" :row="row" :value="getValue(row, column.key)">
            {{ getValue(row, column.key) }}
          </slot>
        </div>
      </div>
    </div>
    <div v-if="data.length === 0" class="p-8 text-center text-muted-foreground">
      No data available
    </div>
  </div>

  <!-- Pagination -->
  <div v-if="pagination" class="flex flex-col sm:flex-row items-center justify-between gap-4 px-2 py-4">
    <div class="text-sm text-muted-foreground">
      Showing {{ startIndex + 1 }} to {{ endIndex }} of {{ total }} entries
    </div>
    <div class="flex items-center gap-2">
      <button
        @click="page--"
        :disabled="page === 0"
        class="px-3 py-2 rounded-md text-sm font-medium border border-input hover:bg-accent disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Previous
      </button>
      <span class="text-sm text-muted-foreground">
        Page {{ page + 1 }} of {{ totalPages }}
      </span>
      <button
        @click="page++"
        :disabled="page >= totalPages - 1"
        class="px-3 py-2 rounded-md text-sm font-medium border border-input hover:bg-accent disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends Record<string, any>">
interface Column {
  key: string
  title: string
  sortable?: boolean
}

const props = defineProps<{
  data: T[]
  columns: Column[]
  rowKey?: string
  pagination?: boolean
  pageSize?: number
  total?: number
}>()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:sortBy': [sortBy: string]
  'update:sortDir': [sortDir: 'asc' | 'desc']
}>()

const page = defineModel<number>('page', { default: 0 })
const sortBy = defineModel<string>('sortBy', { default: '' })
const sortDir = defineModel<'asc' | 'desc'>('sortDir', { default: 'asc' })

function getRowKey(row: T): string {
  if (props.rowKey) {
    return String(row[props.rowKey])
  }
  return JSON.stringify(row)
}

function getValue(row: T, key: string): any {
  return key.split('.').reduce((obj, k) => obj?.[k], row)
}

function toggleSort(key: string) {
  if (sortBy.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = key
    sortDir.value = 'asc'
  }
}

const sortedData = computed(() => {
  let result = [...props.data]
  
  // Client-side sorting if not using server-side
  if (sortBy.value && props.data.length > 0) {
    result.sort((a, b) => {
      const aVal = getValue(a, sortBy.value)
      const bVal = getValue(b, sortBy.value)
      
      if (aVal === bVal) return 0
      if (aVal === null || aVal === undefined) return 1
      if (bVal === null || bVal === undefined) return -1
      
      const comparison = aVal < bVal ? -1 : 1
      return sortDir.value === 'asc' ? comparison : -comparison
    })
  }
  
  // Client-side pagination if not using server-side
  if (props.pagination && props.pageSize) {
    const start = page.value * props.pageSize
    result = result.slice(start, start + props.pageSize)
  }
  
  return result
})

const totalPages = computed(() => {
  if (!props.pageSize || !props.total) return 1
  return Math.ceil(props.total / props.pageSize)
})

const startIndex = computed(() => page.value * (props.pageSize || 10))
const endIndex = computed(() => Math.min(startIndex.value + (props.pageSize || 10), props.total || 0))
</script>
