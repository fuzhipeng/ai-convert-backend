<template>
  <div class="shared-file-list">
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

      <el-table-column label="分享时间" prop="shareTime" sortable="custom" width="180">
        <template #default="{ row }">
          {{ formatDate(row.shareTime) }}
        </template>
      </el-table-column>

      <el-table-column label="过期时间" prop="shareExpireTime" sortable="custom" width="180">
        <template #default="{ row }">
          <span :class="{ 'expired': isExpired(row.shareExpireTime) }">
            {{ row.shareExpireTime ? formatDate(row.shareExpireTime) : '永久有效' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="分享链接" min-width="200">
        <template #default="{ row }">
          <el-input
            v-model="row.shareUrl"
            readonly
            size="small"
          >
            <template #append>
              <el-button @click="copyShareUrl(row.shareUrl)">复制</el-button>
            </template>
          </el-input>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button-group>
            <el-button 
              type="primary" 
              link
              @click="handleRenewShare(row)"
            >
              续期
            </el-button>
            <el-button 
              type="danger" 
              link
              @click="handleCancelShare(row)"
            >
              取消分享
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog
      v-model="renewDialogVisible"
      title="续期分享"
      width="400px"
    >
      <div class="renew-dialog-content">
        <el-form :model="renewForm" label-width="100px">
          <el-form-item label="过期时间">
            <el-select v-model="renewForm.expireTime">
              <el-option label="1小时" :value="1" />
              <el-option label="1天" :value="24" />
              <el-option label="7天" :value="168" />
              <el-option label="30天" :value="720" />
              <el-option label="永久" :value="-1" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="renewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="renewShare">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { format, isAfter } from 'date-fns'

// 列表数据
const loading = ref(false)
const fileList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const sortField = ref('shareTime')
const sortOrder = ref('desc')

// 续期对话框
const renewDialogVisible = ref(false)
const renewForm = ref({
  expireTime: 24
})
const currentRenewFile = ref(null)

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

// 检查是否过期
const isExpired = (date) => {
  if (!date) return false
  return isAfter(new Date(), new Date(date))
}

// 获取分享文件列表
const fetchSharedFiles = async () => {
  loading.value = true
  try {
    const response = await fetch(
      `/api/files/shared?pageNum=${currentPage.value}&pageSize=${pageSize.value}&sortField=${sortField.value}&sortOrder=${sortOrder.value}`
    )
    if (!response.ok) {
      throw new Error('获取分享文件列表失败')
    }

    const result = await response.json()
    if (result.code !== 0) {
      throw new Error(result.message)
    }

    fileList.value = result.data.records.map(file => ({
      ...file,
      shareUrl: `${window.location.origin}/share/${file.shareUrl}`
    }))
    total.value = result.data.total
  } catch (error) {
    console.error('获取分享文件列表失败:', error)
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

// 处理排序
const handleSortChange = ({ prop, order }) => {
  sortField.value = prop
  sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  fetchSharedFiles()
}

// 处理页码改变
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchSharedFiles()
}

// 处理每页数量改变
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchSharedFiles()
}

// 复制分享链接
const copyShareUrl = async (url) => {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('复制成功')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 处理续期分享
const handleRenewShare = (file) => {
  renewDialogVisible.value = true
  currentRenewFile.value = file
  renewForm.value.expireTime = 24
}

// 续期分享
const renewShare = async () => {
  try {
    const response = await fetch(`/api/files/share/${currentRenewFile.value.id}?expireTime=${renewForm.value.expireTime}`, {
      method: 'PUT'
    })

    if (!response.ok) {
      throw new Error('续期分享失败')
    }

    const result = await response.json()
    if (result.code !== 0) {
      throw new Error(result.message)
    }

    ElMessage.success('续期成功')
    renewDialogVisible.value = false
    fetchSharedFiles()
  } catch (error) {
    console.error('续期分享失败:', error)
    ElMessage.error(error.message)
  }
}

// 处理取消分享
const handleCancelShare = (file) => {
  ElMessageBox.confirm(
    '确定要取消分享该文件吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await fetch(`/api/files/share/${file.id}`, {
        method: 'DELETE'
      })

      if (!response.ok) {
        throw new Error('取消分享失败')
      }

      const result = await response.json()
      if (result.code !== 0) {
        throw new Error(result.message)
      }

      ElMessage.success('取消分享成功')
      fetchSharedFiles()
    } catch (error) {
      console.error('取消分享失败:', error)
      ElMessage.error(error.message)
    }
  }).catch(() => {})
}

// 组件挂载时获取分享文件列表
onMounted(() => {
  fetchSharedFiles()
})
</script>

<style scoped>
.shared-file-list {
  width: 100%;
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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.expired {
  color: var(--el-color-danger);
}

.renew-dialog-content {
  padding: 20px 0;
}

:deep(.el-table .cell) {
  white-space: nowrap;
}
</style> 