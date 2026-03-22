<template>
  <div class="file-viewer">
    <!-- 文件浏览器侧边栏 -->
    <div class="file-browser" :class="{ 'collapsed': isCollapsed }">
      <div class="browser-header">
        <button @click="toggleCollapse" class="collapse-btn">
          {{ isCollapsed ? '→' : '←' }}
        </button>
        <span v-if="!isCollapsed" class="title">Files</span>
      </div>
      
      <div v-if="!isCollapsed" class="file-tree">
        <FileTreeItem
          v-for="item in fileTree"
          :key="item.path"
          :item="item"
          :current-path="currentPath"
          @select="selectFile"
        />
      </div>
    </div>

    <!-- 文件内容显示区 -->
    <div class="file-content">
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>Loading...</span>
      </div>
      
      <div v-else-if="error" class="error">
        <p>{{ error }}</p>
      </div>
      
      <div v-else-if="!currentFile" class="empty">
        <p>Select a file to view</p>
      </div>
      
      <div v-else class="content-wrapper">
        <!-- 文件头部信息 -->
        <div class="file-header">
          <span class="file-path">{{ currentFile.path }}</span>
          <span class="file-size">{{ formatSize(currentFile.size) }}</span>
          <button 
            v-if="currentFile.isText"
            @click="copyContent"
            class="copy-btn"
          >
            Copy
          </button>
        </div>

        <!-- 文本文件 -->
        <pre v-if="currentFile.isText" class="code-block"><code>{{ currentFile.content }}</code></pre>
        
        <!-- 图片文件 -->
        <img 
          v-else-if="currentFile.isImage" 
          :src="imageUrl" 
          :alt="currentFile.path"
          class="image-preview"
        />
        
        <!-- 二进制文件 -->
        <div v-else class="binary-file">
          <p>Binary file ({{ formatSize(currentFile.size) }})</p>
          <a :href="downloadUrl" download class="download-btn">
            Download
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import FileTreeItem from './FileTreeItem.vue';

const props = defineProps({
  skillSlug: String,
  version: String,
  apiBaseUrl: String
});

const files = ref([]);
const currentPath = ref('');
const currentFile = ref(null);
const loading = ref(false);
const error = ref(null);
const isCollapsed = ref(false);

const fileTree = computed(() => buildFileTree(files.value));

const imageUrl = computed(() => {
  if (!currentFile.value || !currentFile.value.isImage) return '';
  return `${props.apiBaseUrl}/api/v1/skills/${props.skillSlug}/versions/${props.version}/files/${currentPath.value}/raw`;
});

const downloadUrl = computed(() => {
  if (!currentFile.value) return '';
  return `${props.apiBaseUrl}/api/v1/skills/${props.skillSlug}/versions/${props.version}/files/${currentPath.value}/raw`;
});

// 加载文件列表
async function loadFiles() {
  try {
    const response = await fetch(
      `${props.apiBaseUrl}/api/v1/skills/${props.skillSlug}/versions/${props.version}/files`
    );
    const data = await response.json();
    files.value = data.data || [];
  } catch (e) {
    error.value = 'Failed to load files';
  }
}

// 选择文件
async function selectFile(path) {
  currentPath.value = path;
  loading.value = true;
  error.value = null;
  
  try {
    const response = await fetch(
      `${props.apiBaseUrl}/api/v1/skills/${props.skillSlug}/versions/${props.version}/files/${path}`
    );
    const data = await response.json();
    currentFile.value = data.data;
  } catch (e) {
    error.value = 'Failed to load file content';
    currentFile.value = null;
  } finally {
    loading.value = false;
  }
}

// 构建文件树
function buildFileTree(fileList) {
  const root = { name: '', path: '', children: [] };
  
  for (const file of fileList) {
    const parts = file.path.split('/');
    let current = root;
    
    for (let i = 0; i < parts.length; i++) {
      const part = parts[i];
      const isFile = i === parts.length - 1;
      const path = parts.slice(0, i + 1).join('/');
      
      let child = current.children.find(c => c.name === part);
      if (!child) {
        child = {
          name: part,
          path: path,
          isFile: isFile,
          children: [],
          size: isFile ? file.size : null
        };
        current.children.push(child);
      }
      current = child;
    }
  }
  
  return root.children;
}

// 格式化文件大小
function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

// 复制内容
async function copyContent() {
  if (!currentFile.value?.content) return;
  
  try {
    await navigator.clipboard.writeText(currentFile.value.content);
    // 可以显示一个 toast 提示
  } catch (e) {
    console.error('Failed to copy:', e);
  }
}

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value;
}

// 监听 skillSlug 和 version 变化
watch([() => props.skillSlug, () => props.version], () => {
  loadFiles();
  currentPath.value = '';
  currentFile.value = null;
}, { immediate: true });
</script>

<style scoped>
.file-viewer {
  display: flex;
  height: 600px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.file-browser {
  width: 250px;
  min-width: 250px;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  transition: width 0.3s, min-width 0.3s;
}

.file-browser.collapsed {
  width: 40px;
  min-width: 40px;
}

.browser-header {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.collapse-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
}

.title {
  margin-left: 8px;
  font-weight: 600;
}

.file-tree {
  padding: 8px;
  overflow-y: auto;
  height: calc(100% - 50px);
}

.file-content {
  flex: 1;
  overflow: auto;
  background: #fff;
}

.loading, .error, .empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.content-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.file-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  gap: 12px;
}

.file-path {
  font-family: monospace;
  font-size: 14px;
  color: #374151;
}

.file-size {
  color: #6b7280;
  font-size: 12px;
}

.copy-btn, .download-btn {
  margin-left: auto;
  padding: 4px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.copy-btn:hover, .download-btn:hover {
  background: #2563eb;
}

.code-block {
  flex: 1;
  margin: 0;
  padding: 16px;
  overflow: auto;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.5;
  background: #1f2937;
  color: #e5e7eb;
}

.image-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  padding: 16px;
}

.binary-file {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
}
</style>
