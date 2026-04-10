<template>
  <div class="markdown-viewer prose dark:prose-invert max-w-none" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'

const props = defineProps<{
  content: string
}>()

// Configure marked with syntax highlighting
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
}))

marked.setOptions({
  breaks: true,
  gfm: true,
})

const renderedContent = computed(() => {
  if (!props.content) return ''
  return marked.parse(props.content)
})
</script>

<style scoped>
.markdown-viewer :deep(h1) {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
  margin-top: 1.5rem;
}

.markdown-viewer :deep(h1:first-child) {
  margin-top: 0;
}

.markdown-viewer :deep(h2) {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  margin-top: 1.25rem;
}

.markdown-viewer :deep(h3) {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  margin-top: 1rem;
}

.markdown-viewer :deep(p) {
  margin-bottom: 0.75rem;
  line-height: 1.6;
}

.markdown-viewer :deep(code) {
  background-color: hsl(var(--muted));
  padding: 0.2rem 0.4rem;
  border-radius: 0.25rem;
  font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
  font-size: 0.875em;
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
  font-size: 0.875rem;
  line-height: 1.5;
}

.markdown-viewer :deep(ul) {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.markdown-viewer :deep(ol) {
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.markdown-viewer :deep(li) {
  margin: 0.25rem 0;
}

.markdown-viewer :deep(a) {
  color: hsl(var(--primary));
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-viewer :deep(a:hover) {
  text-decoration: none;
}

.markdown-viewer :deep(blockquote) {
  border-left: 4px solid hsl(var(--border));
  padding-left: 1rem;
  margin: 1rem 0;
  color: hsl(var(--muted-foreground));
}

.markdown-viewer :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
}

.markdown-viewer :deep(th),
.markdown-viewer :deep(td) {
  border: 1px solid hsl(var(--border));
  padding: 0.5rem;
  text-align: left;
}

.markdown-viewer :deep(th) {
  background-color: hsl(var(--muted));
  font-weight: 600;
}

.markdown-viewer :deep(hr) {
  border: none;
  border-top: 1px solid hsl(var(--border));
  margin: 1.5rem 0;
}

/* Syntax highlighting styles */
.markdown-viewer :deep(.hljs) {
  display: block;
  overflow-x: auto;
  padding: 0;
  background: transparent;
  color: hsl(var(--foreground));
}

.markdown-viewer :deep(.hljs-keyword),
.markdown-viewer :deep(.hljs-selector-tag),
.markdown-viewer :deep(.hljs-literal),
.markdown-viewer :deep(.hljs-section),
.markdown-viewer :deep(.hljs-link) {
  color: hsl(var(--primary));
  font-weight: 600;
}

.markdown-viewer :deep(.hljs-string),
.markdown-viewer :deep(.hljs-title),
.markdown-viewer :deep(.hljs-name),
.markdown-viewer :deep(.hljs-type),
.markdown-viewer :deep(.hljs-attribute),
.markdown-viewer :deep(.hljs-symbol),
.markdown-viewer :deep(.hljs-bullet),
.markdown-viewer :deep(.hljs-addition) {
  color: hsl(142, 76%, 36%);
}

.dark-mode .markdown-viewer :deep(.hljs-string) {
  color: hsl(142, 70%, 45%);
}

.markdown-viewer :deep(.hljs-comment),
.markdown-viewer :deep(.hljs-quote),
.markdown-viewer :deep(.hljs-deletion) {
  color: hsl(var(--muted-foreground));
}

.markdown-viewer :deep(.hljs-number),
.markdown-viewer :deep(.hljs-regexp),
.markdown-viewer :deep(.hljs-literal),
.markdown-viewer :deep(.hljs-variable),
.markdown-viewer :deep(.hljs-template-variable) {
  color: hsl(0, 84%, 60%);
}

.markdown-viewer :deep(.hljs-built_in),
.markdown-viewer :deep(.hljs-builtin-name) {
  color: hsl(262, 83%, 58%);
}
</style>
