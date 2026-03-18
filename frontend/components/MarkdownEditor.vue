<template>
  <div class="markdown-editor">
    <div ref="editorRef" class="editor-container min-h-[200px] border rounded-md p-4"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Editor, rootCtx } from '@milkdown/core'
import { commonmark } from '@milkdown/preset-commonmark'
import { nord } from '@milkdown/theme-nord'
import { Milkdown, useEditor } from '@milkdown/vue'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = ref<HTMLDivElement>()

// Simple markdown editor using textarea as fallback
// Full Milkdown integration can be added later
const content = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
</script>

<template>
  <textarea
    v-model="content"
    class="w-full min-h-[200px] p-4 rounded-md border border-input bg-background text-sm font-mono"
    placeholder="Write your description in Markdown..."
  ></textarea>
</template>

<style scoped>
.markdown-editor textarea {
  resize: vertical;
  line-height: 1.6;
}
</style>
