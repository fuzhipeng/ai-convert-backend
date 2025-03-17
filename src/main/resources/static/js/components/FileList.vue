<template>
  <div class="file-list">
    <div class="file-list-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文件"
        prefix-icon="Search"
        clearable
        @input="handleSearch"
      />
      <el-button type="primary" @click="$emit('add)">
        <el-icon><plus /></el-icon>
        上传文件
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="fileList"
      style="width: 100%"
      @sort-change="handleSortChange"
    >
      <el-table-column label="文件名" prop="fileName" sortable="custom" min-width="200">
        <template #default="{ row }">
          <div class="file-name-cell">
            <el-icon><document /></el-icon>
            <span>{{ row.fileName }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="大小" prop="fileSize" sortable="custom" width="120">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>

      <el-table-column label="类型" prop="fileType" width="120">
        <template #default="{ row }">
          {{ row.fileType || '-' }}
        </template>
      </el-table-column>

      <el-table-column label="上传时间" prop="createTime" sortable="custom" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>

      <el-table-column label="状态" prop="status" width="120">
        <template #default="{ row }">
          <div class="status-cell">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
            <el-progress
              v-if="row.status === 'UPLOADING'"
              :percentage="row.uploadProgress"
              :format="percentageFormat"
              :stroke-width="5"
              class="upload-progress"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button-group>
            <el-button
              v-if="row.status === 'UPLOADED'"
              type="primary"
              link
              @click="handleDownload(row)"
            >
              <el-icon><download /></el-icon>
              下载
            </el-button>
            <el-button
              v-if="row.status === 'UPLOADED'"
              type="primary"
              link
              @click="handleShare(row)"
            >
              <el-icon><share /></el-icon>
              分享
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
            >
              <el-icon><delete /></el-icon>
              删除
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog
      v-model="shareDialogVisible"
      title="分享文件"
      width="500px"
    >
      <el-form :model="shareForm" label-width="100px">
        <el-form-item label="过期时间">
          <el-input-number
            v-model="shareForm.expireTime"
            :min="1"
            :max="168"
            controls-position="right"
          />
          <span class="expire-time-unit">小时</span>
        </el-form-item>
        <el-form-item v-if="shareUrl" label="分享链接">
          <el-input
            v-model="shareUrl"
            readonly
          >
            <template #append>
              <el-button @click="copyShareUrl">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="shareDialogVisible = false">关闭</el-button>
          <el-button
            v-if="!shareUrl"
            type="primary"
            @click="createShareLink"
          >
            生成链接
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Search, Download, Share, Delete } from '@element-plus/icons-vue'
import { format } from 'date-fns'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['add', 'update'])

// 列表数据
const fileList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const sortField = ref('createTime')
const sortOrder = ref('desc')

// 分享对话框
const shareDialogVisible = ref(false)
const shareForm = ref({
  expireTime: 24
})
const shareUrl = ref('')
const currentShareFile = ref(null)

// 获取文件列表
const fetchFileList = async () => {
  try {
    const response = await fetch(`/api/files/list?pageNum=${currentPage.value}&pageSize=${pageSize.value}`)
    if (!response.ok) {
      throw new Error('获取文件列表失败')
    }

    const result = await response.json()
    if (result.code !== 0) {
      throw new Error(result.message)
    }

    fileList.value = result.data.records
    total.value = result.data.total
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error(error.message)
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

// 格式化日期
const formatDate = (date) => {
  return format(new Date(date), 'yyyy-MM-dd HH:mm:ss')
}

// 格式化进度
const percentageFormat = (percentage) => {
  return percentage === 100 ? '完成' : `${percentage}%`
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'UPLOADING':
      return 'warning'
    case 'UPLOADED':
      return 'success'
    case 'ERROR':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'UPLOADING':
      return '上传中'
    case 'UPLOADED':
      return '已上传'
    case 'ERROR':
      return '错误'
    default:
      return '未知'
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFileList()
}

// 处理排序
const handleSortChange = ({ prop, order }) => {
  sortField.value = prop
  sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  fetchFileList()
}

// 处理页码改变
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchFileList()
}

// 处理每页数量改变
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchFileList()
}

// 处理下载
const handleDownload = async (file) => {
  try {
    window.open(`/api/files/download/${file.id}`)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

// 处理分享
const handleShare = (file) => {
  currentShareFile.value = file
  shareDialogVisible.value = true
  shareUrl.value = ''
}

// 创建分享链接
const createShareLink = async () => {
  try {
    const response = await fetch(`/api/files/share/${currentShareFile.value.id}?expireTime=${shareForm.value.expireTime}`, {
      method: 'POST'
    })
    if (!response.ok) {
      throw new Error('创建分享链接失败')
    }

    const result = await response.json()
    if (result.code !== 0) {
      throw new Error(result.message)
    }

    shareUrl.value = result.data
  } catch (error) {
    console.error('创建分享链接失败:', error)
    ElMessage.error(error.message)
  }
}

// 复制分享链接
const copyShareUrl = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('复制成功')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 处理删除
const handleDelete = (file) => {
  ElMessageBox.confirm(
    '确定要删除该文件吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await fetch(`/api/files/${file.id}`, {
        method: 'DELETE'
      })
      if (!response.ok) {
        throw new Error('删除文件失败')
      }

      const result = await response.json()
      if (result.code !== 0) {
        throw new Error(result.message)
      }

      ElMessage.success('删除成功')
      fetchFileList()
      emit('update')
    } catch (error) {
      console.error('删除文件失败:', error)
      ElMessage.error(error.message)
    }
  }).catch(() => {})
}

// 组件挂载时获取文件列表
onMounted(() => {
  fetchFileList()
})
</script>

<style scoped>
.file-list {
  width: 100%;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.file-list-header .el-input {
  width: 300px;
  margin-right: 16px;
}

.file-name-cell {
  display: flex;
  align-items: center;
}

.file-name-cell .el-icon {
  margin-right: 8px;
  font-size: 20px;
  color: var(--el-text-color-secondary);
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-progress {
  width: 80px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.expire-time-unit {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

:deep(.el-table .cell) {
  white-space: nowrap;
}
</style> 