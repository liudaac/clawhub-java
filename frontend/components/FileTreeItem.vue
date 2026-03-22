<template>
  <div class="file-tree-item">
    <div 
      class="item-row"
      :class="{ 'selected': isSelected, 'file': item.isFile }"
      @click="handleClick"
    >
      <span class="icon">
        <template v-if="item.isFile">
          📄
        </template>
        <template v-else>
          {{ isExpanded ? '📂' : '📁' }}
        </template>
      </span>
      <span class="name">{{ item.name }}</span>
      <span v-if="item.isFile && item.size" class="size">
        {{ formatSize(item.size) }}
      </span>
    </div>
    
    <div v-if="!item.isFile && isExpanded" class="children">
      <FileTreeItem
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :current-path="currentPath"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  item: Object,
  currentPath: String
});

const emit = defineEmits(['select']);

const isExpanded = ref(false);

const isSelected = computed(() => {
  return props.currentPath === props.item.path;
});

function handleClick() {
  if (props.item.isFile) {
    emit('select', props.item.path);
  } else {
    isExpanded.value = !isExpanded.value;
  }
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
</script>

<style scoped>
.file-tree-item {
  user-select: none;
}

.item-row {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 4px;
  gap: 6px;
}

.item-row:hover {
  background: #e5e7eb;
}

.item-row.selected {
  background: #dbeafe;
  color: #1e40af;
}

.icon {
  font-size: 14px;
}

.name {
  flex: 1;
  font-size: 13px;
  color: #374151;
}

.item-row.selected .name {
  color: #1e40af;
}

.size {
  font-size: 11px;
  color: #9ca3af;
}

.children {
  padding-left: 16px;
}
</style>
