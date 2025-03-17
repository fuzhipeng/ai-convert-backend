<template>
  <div class="file-upload">
    <div class="upload-area" @drop.prevent="handleDrop" @dragover.prevent>
      <div v-if="!uploading" class="upload-placeholder">
        <el-upload
          ref="upload"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          :accept="acceptTypes"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持以下格式：{{ acceptTypes }}，单个文件大小不超过 {{ maxFileSize }}MB
            </div>
          </template>
        </el-upload>
      </div>
      <div v-else class="upload-progress">
        <div class="file-info">
          <el-icon><document /></el-icon>
          <span class="file-name">{{ currentFile.name }}</span>
          <span class="file-size">{{ formatFileSize(currentFile.size) }}</span>
          <span v-if="uploadSpeed" class="upload-speed">{{ uploadSpeed }}</span>
        </div>
        <el-progress 
          :percentage="uploadProgress" 
          :status="uploadStatus"
          :format="progressFormat"
        />
        <div class="upload-options">
          <el-switch
            v-model="isPublic"
            active-text="公开访问"
            inactive-text="私有"
          />
        </div>
        <div class="upload-actions">
          <el-button 
            v-if="uploadStatus === 'exception'" 
            type="primary" 
            @click="retryUpload"
          >
            重试
          </el-button>
          <el-button 
            v-if="uploadStatus !== 'success'" 
            @click="cancelUpload"
          >
            取消
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/convert'
import SparkMD5 from 'spark-md5'

const props = defineProps({
  maxFileSize: {
    type: Number,
    default: 1024 // 默认最大1GB
  },
  acceptTypes: {
    type: String,
    default: '.pdf,.doc,.docx,.txt,.md,.json,.xml,.html,.csv'
  }
})

const emit = defineEmits(['upload-success', 'upload-error'])

// 上传相关的响应式变量
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref('')
const currentFile = ref(null)
const isPublic = ref(true)
const uploadSpeed = ref(null)
const uploadStartTime = ref(0)
const uploadedSize = ref(0)

// 计算上传进度提示文本
const progressFormat = (percentage) => {
  if (uploadStatus.value === 'success') {
    return '上传完成'
  } else if (uploadStatus.value === 'exception') {
    return '上传失败'
  } else {
    return uploadSpeed.value ? `${percentage}% - ${uploadSpeed.value}` : `${percentage}%`
  }
}

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

// 计算上传速度
const calculateUploadSpeed = () => {
  const now = Date.now()
  const duration = (now - uploadStartTime.value) / 1000 // 转换为秒
  if (duration > 0) {
    const speed = uploadedSize.value / duration // 字节/秒
    if (speed < 1024) {
      uploadSpeed.value = `${speed.toFixed(2)} B/s`
    } else if (speed < 1024 * 1024) {
      uploadSpeed.value = `${(speed / 1024).toFixed(2)} KB/s`
    } else {
      uploadSpeed.value = `${(speed / (1024 * 1024)).toFixed(2)} MB/s`
    }
  }
}

// 处理文件选择
const handleFileChange = async (file) => {
  // 检查文件类型
  const extension = file.name.split('.').pop().toLowerCase()
  const acceptList = props.acceptTypes.split(',').map(type => 
    type.trim().replace('.', '').toLowerCase()
  )
  if (!acceptList.includes(extension)) {
    ElMessage.error(`不支持的文件类型：${extension}`)
    return
  }

  // 检查文件大小
  if (file.size > props.maxFileSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxFileSize}MB`)
    return
  }

  currentFile.value = file
  uploading.value = true
  uploadStatus.value = ''
  uploadProgress.value = 0
  uploadStartTime.value = Date.now()
  uploadedSize.value = 0

  try {
    const formData = new FormData()
    formData.append('file', file)

    const result = await uploadFile(formData)
    uploadProgress.value = 100
    uploadStatus.value = 'success'
    emit('upload-success', result)
  } catch (error) {
    console.error('上传失败:', error)
    uploadStatus.value = 'exception'
    emit('upload-error', error)
  }
}

// 重试上传
const retryUpload = () => {
  uploadStatus.value = ''
  uploadProgress.value = 0
  handleFileChange(currentFile.value)
}

// 取消上传
const cancelUpload = () => {
  uploadStatus.value = 'canceled'
  uploading.value = false
  currentFile.value = null
  uploadSpeed.value = ''
}

// 处理拖拽
const handleDrop = (e) => {
  const file = e.dataTransfer.files[0]
  if (file) {
    handleFileChange(file)
  }
}
</script>

<style scoped>
.file-upload {
  width: 100%;
}

.upload-area {
  border: 2px dashed var(--el-border-color);
  border-radius: 6px;
  padding: 20px;
  text-align: center;
  transition: border-color 0.3s;
}

.upload-area:hover {
  border-color: var(--el-color-primary);
}

.upload-placeholder {
  cursor: pointer;
}

.upload-progress {
  padding: 20px;
}

.file-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  color: var(--el-text-color-regular);
}

.file-info .el-icon {
  margin-right: 8px;
  font-size: 20px;
}

.file-name {
  margin-right: 10px;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  margin-right: 10px;
  color: var(--el-text-color-secondary);
}

.upload-speed {
  color: var(--el-color-primary);
  font-size: 14px;
  min-width: 100px;
  text-align: right;
}

.upload-options {
  margin: 15px 0;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.upload-actions {
  margin-top: 15px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

:deep(.el-icon--upload) {
  font-size: 48px;
  margin-bottom: 10px;
  color: var(--el-color-primary);
}

:deep(.el-upload__text) {
  font-size: 16px;
  margin-bottom: 10px;
}

:deep(.el-upload__tip) {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

:deep(.el-progress) {
  margin-bottom: 15px;
}

:deep(.el-switch) {
  --el-switch-on-color: var(--el-color-success);
}
</style> 