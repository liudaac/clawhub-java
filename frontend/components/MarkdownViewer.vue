<template>
  <div class="markdown-viewer prose dark:prose-invert max-w-none" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

// Simple markdown parser
// In production, use a proper markdown library like marked or markdown-it
const renderedContent = computed(() => {
  if (!props.content) return ''
  
  let html = props.content
    // Escape HTML
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // Headers
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // Bold and Italic
    .replace(/\*\*\*(.*?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // Code blocks
    .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Links
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    // Lists
    .replace(/^\s*- (.*$)/gim, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
    // Paragraphs
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
  
  return '<p>' + html + '</p>'
})
</script>

<style scoped>
.markdown-viewer :deep(h1) {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.markdown-viewer :deep(h2) {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
}

.markdown-viewer :deep(h3) {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.markdown-viewer :deep(code) {
  background-color: hsl(var(--muted));
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-family: ui-monospace, monospace;
}

.markdown-viewer :deep(pre) {
  background-color: hsl(var(--muted));
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin: 1rem 0;
}

.markdown-viewer :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.markdown-viewer :deep(ul) {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.markdown-viewer :deep(li) {
  margin: 0.25rem 0;
}

.markdown-viewer :deep(a) {
  color: hsl(var(--primary));
  text-decoration: underline;
}

.markdown-viewer :deep(a:hover) {
  text-decoration: none;
}
</style>
