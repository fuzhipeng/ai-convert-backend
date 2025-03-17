<template>
  <div class="file-manage">
    <el-card class="file-card">
      <template #header>
        <div class="card-header">
          <span>文件管理</span>
          <div class="storage-info">
            存储空间：{{ formatFileSize(usedStorage) }} / {{ formatFileSize(storageLimit) }}
            <el-progress 
              :percentage="storageUsagePercentage" 
              :status="storageUsageStatus"
            />
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="全部文件" name="all">
          <file-list
            :loading="loading"
            @add="showUploadDialog"
            @update="fetchStorageInfo"
          />
        </el-tab-pane>
        <el-tab-pane label="已分享" name="shared">
          <shared-file-list />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <file-upload
        :max-file-size="remainingStorageInMB"
        @upload-success="handleUploadSuccess"
        @upload-error="handleUploadError"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import FileList from '../components/FileList.vue'
import FileUpload from '../components/FileUpload.vue'
import SharedFileList from '../components/SharedFileList.vue'

// 标签页
const activeTab = ref('all')

// 上传对话框
const uploadDialogVisible = ref(false)
const loading = ref(false)

// 存储信息
const usedStorage = ref(0)
const storageLimit = ref(0)

// 计算存储使用百分比
const storageUsagePercentage = computed(() => {
  if (storageLimit.value === 0) return 0
  return Math.min(Math.round((usedStorage.value / storageLimit.value) * 100), 100)
})

// 计算存储状态
const storageUsageStatus = computed(() => {
  const percentage = storageUsagePercentage.value
  if (percentage >= 90) return 'exception'
  if (percentage >= 70) return 'warning'
  return ''
})

// 计算剩余存储空间（MB）
const remainingStorageInMB = computed(() => {
  return Math.floor((storageLimit.value - usedStorage.value) / (1024 * 1024))
})

// 格式化文件大小
const formatFileSize = (size) => {
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index++
  }
  return `${size.toFixed(2)} ${units[index]}`
}

// 获取存储信息
const fetchStorageInfo = async () => {
  try {
    const response = await fetch('/api/users/storage-info')
    if (!response.ok) {
      throw new Error('获取存储信息失败')
    }

    const result = await response.json()
    if (result.code !== 0) {
      throw new Error(result.message)
    }

    usedStorage.value = result.data.usedStorage
    storageLimit.value = result.data.storageLimit
  } catch (error) {
    console.error('获取存储信息失败:', error)
    ElMessage.error(error.message)
  }
}

// 显示上传对话框
const showUploadDialog = () => {
  if (storageUsagePercentage.value >= 100) {
    ElMessage.warning('存储空间已满，请清理文件或升级存储空间')
    return
  }
  uploadDialogVisible.value = true
}

// 处理上传成功
const handleUploadSuccess = (file) => {
  ElMessage.success('上传成功')
  uploadDialogVisible.value = false
  fetchStorageInfo()
}

// 处理上传失败
const handleUploadError = (error) => {
  ElMessage.error(error.message || '上传失败')
}

// 组件挂载时获取存储信息
onMounted(() => {
  fetchStorageInfo()
})
</script>

<style scoped>
.file-manage {
  padding: 20px;
}

.file-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.storage-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.storage-info .el-progress {
  width: 200px;
}

:deep(.el-dialog__body) {
  padding-top: 10px;
}
</style> 