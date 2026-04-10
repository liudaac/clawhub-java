<template>
  <div class="space-y-6">
    <h1 class="text-3xl font-bold">Moderation</h1>

    <!-- Tabs -->
    <div class="flex gap-2 border-b">
      <button
        @click="activeTab = 'pending'"
        :class="[
          'px-3 sm:px-4 py-2 text-sm font-medium border-b-2 transition-colors flex-1 sm:flex-none text-center',
          activeTab === 'pending'
            ? 'border-primary text-primary'
            : 'border-transparent text-muted-foreground hover:text-foreground'
        ]"
      >
        <span class="sm:hidden">Pending</span>
        <span class="hidden sm:inline">Pending Review</span>
      </button>
      <button
        @click="activeTab = 'hidden'"
        :class="[
          'px-3 sm:px-4 py-2 text-sm font-medium border-b-2 transition-colors flex-1 sm:flex-none text-center',
          activeTab === 'hidden'
            ? 'border-primary text-primary'
            : 'border-transparent text-muted-foreground hover:text-foreground'
        ]"
      >
        <span class="sm:hidden">Hidden</span>
        <span class="hidden sm:inline">Hidden Skills</span>
      </button>
    </div>

    <!-- Data Table -->
    <DataTable
      :data="skills"
      :columns="columns"
      row-key="id"
      pagination
      :page-size="20"
      :total="total"
      v-model:page="page"
      v-model:sort-by="sortBy"
      v-model:sort-dir="sortDir"
    >
      <template #displayName="{ row }">
        <NuxtLink :to="`/skills/${row.slug}`" class="font-medium hover:underline">
          {{ row.displayName }}
        </NuxtLink>
      </template>
      
      <template #owner="{ row }">
        <span class="text-muted-foreground">@{{ row.owner.handle }}</span>
      </template>
      
      <template #status="{ row }">
        <span
          :class="[
            'px-2 py-0.5 rounded text-xs font-medium',
            row.moderationStatus === 'hidden' ? 'bg-destructive/10 text-destructive' :
            row.moderationStatus === 'removed' ? 'bg-muted text-muted-foreground' :
            'bg-success/10 text-success'
          ]"
        >
          {{ row.moderationStatus }}
        </span>
      </template>
      
      <template #tags="{ row }">
        <div class="flex flex-wrap gap-1">
          <span
            v-for="tag in row.capabilityTags?.slice(0, 2)"
            :key="tag"
            class="px-1.5 py-0.5 rounded text-xs bg-primary/10 text-primary"
          >
            {{ tag }}
          </span>
          <span v-if="row.capabilityTags?.length > 2" class="text-xs text-muted-foreground">
            +{{ row.capabilityTags.length - 2 }}
          </span>
        </div>
      </template>
      
      <template #actions="{ row }">
        <div class="flex gap-2">
          <button
            v-if="activeTab === 'pending'"
            @click="openHideDialog(row)"
            class="px-2 py-1 rounded text-xs font-medium bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            Hide
          </button>
          <button
            v-if="activeTab === 'hidden'"
            @click="openUnhideDialog(row)"
            class="px-2 py-1 rounded text-xs font-medium bg-success text-success-foreground hover:bg-success/90"
          >
            Unhide
          </button>
        </div>
      </template>
    </DataTable>

    <!-- Hide/Unhide Dialog -->
    <div v-if="showDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-background rounded-lg p-4 sm:p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">
          {{ dialogAction === 'hide' ? 'Hide Skill' : 'Unhide Skill' }}
        </h3>
        <p class="text-sm text-muted-foreground mb-4">
          {{ selectedSkill?.displayName }}
        </p>
        
        <div class="space-y-4">
          <div v-if="dialogAction === 'hide'">
            <label class="block text-sm font-medium mb-1">Reason</label>
            <select
              v-model="moderationReason"
              class="w-full px-3 py-2 rounded-md border border-input bg-background"
            >
              <option value="">Select a reason...</option>
              <option value="spam">Spam</option>
              <option value="inappropriate">Inappropriate content</option>
              <option value="malicious">Malicious code</option>
              <option value="copyright">Copyright violation</option>
              <option value="other">Other</option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium mb-1">
              Moderation Note <span class="text-destructive">*</span>
            </label>
            <textarea
              v-model="moderationNote"
              rows="3"
              placeholder="Enter detailed notes about this action..."
              class="w-full px-3 py-2 rounded-md border border-input bg-background resize-none"
            />
          </div>
        </div>

        <div class="flex flex-col sm:flex-row justify-end gap-2 mt-6">
          <button
            @click="showDialog = false"
            class="w-full sm:w-auto px-4 py-2 rounded-md border border-input hover:bg-accent"
          >
            Cancel
          </button>
          <button
            @click="submitAction"
            :disabled="!moderationNote.trim() || submitting"
            :class="[
              'w-full sm:w-auto px-4 py-2 rounded-md text-primary-foreground disabled:opacity-50',
              dialogAction === 'hide' ? 'bg-destructive hover:bg-destructive/90' : 'bg-success hover:bg-success/90'
            ]"
          >
            {{ submitting ? 'Processing...' : (dialogAction === 'hide' ? 'Hide' : 'Unhide') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Skill } from '~/types'

const api = useApi()
const toast = inject('toast') as { success: (msg: string) => void, error: (msg: string) => void }

const activeTab = ref<'pending' | 'hidden'>('pending')
const page = ref(0)
const sortBy = ref('createdAt')
const sortDir = ref<'asc' | 'desc'>('desc')
const total = ref(0)

const showDialog = ref(false)
const dialogAction = ref<'hide' | 'unhide'>('hide')
const selectedSkill = ref<Skill | null>(null)
const moderationReason = ref('')
const moderationNote = ref('')
const submitting = ref(false)

const columns = [
  { key: 'displayName', title: 'Name', sortable: true },
  { key: 'owner', title: 'Owner', sortable: false },
  { key: 'status', title: 'Status', sortable: true },
  { key: 'tags', title: 'Tags', sortable: false },
  { key: 'statsDownloads', title: 'Downloads', sortable: true },
  { key: 'createdAt', title: 'Created', sortable: true },
  { key: 'actions', title: 'Actions', sortable: false },
]

const { data: pendingData, refresh: refreshPending } = await useAsyncData(
  () => `moderation-pending-${page.value}-${sortBy.value}-${sortDir.value}`,
  async () => {
    const res = await api.getPendingReview(page.value, 20)
    total.value = res.total || 0
    return res
  },
  { watch: [page, sortBy, sortDir, activeTab] }
)

const { data: hiddenData, refresh: refreshHidden } = await useAsyncData(
  () => `moderation-hidden-${page.value}-${sortBy.value}-${sortDir.value}`,
  async () => {
    const res = await api.getHiddenSkills(page.value, 20)
    total.value = res.total || 0
    return res
  },
  { watch: [page, sortBy, sortDir, activeTab] }
)

const skills = computed(() => {
  if (activeTab.value === 'pending') {
    return pendingData.value?.data || []
  }
  return hiddenData.value?.data || []
})

function openHideDialog(skill: Skill) {
  selectedSkill.value = skill
  dialogAction.value = 'hide'
  moderationReason.value = ''
  moderationNote.value = ''
  showDialog.value = true
}

function openUnhideDialog(skill: Skill) {
  selectedSkill.value = skill
  dialogAction.value = 'unhide'
  moderationReason.value = ''
  moderationNote.value = ''
  showDialog.value = true
}

async function submitAction() {
  if (!selectedSkill.value || !moderationNote.value.trim()) return
  
  submitting.value = true
  try {
    if (dialogAction.value === 'hide') {
      await api.hideSkill(selectedSkill.value.id, moderationReason.value, moderationNote.value)
      toast.success('Skill hidden successfully')
      await refreshPending()
    } else {
      await api.unhideSkill(selectedSkill.value.id, moderationNote.value)
      toast.success('Skill unhidden successfully')
      await refreshHidden()
    }
    showDialog.value = false
  } catch (error) {
    toast.error(error instanceof Error ? error.message : 'Action failed')
  } finally {
    submitting.value = false
  }
}
</script>