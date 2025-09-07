<template>
  <div class="app-edit-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-left">
        <a-button type="text" @click="goBack" class="back-btn">
          <template #icon>
            <left-outlined />
          </template>
          返回
        </a-button>
      </div>
      <div class="header-title">编辑应用信息</div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <a-card :bordered="false" class="edit-card">
        <!-- 基本信息 -->
        <div class="form-section">
          <h3 class="section-title">基本信息</h3>

          <a-form-item
            label="* 应用名称"
            name="appName"
            :rules="[{ required: true, message: '请输入应用名称' }]"
          >
            <a-input
              v-model:value="form.appName"
              :maxlength="50"
              show-count
              placeholder="请输入应用名称"
            />
          </a-form-item>

          <a-form-item label="应用封面" name="cover">
            <div class="cover-upload-container">
              <div class="cover-preview" @click="handleCoverUpload">
                <img
                  v-if="form.cover"
                  :src="getAppCoverUrl(appDetail?.cover)"
                  alt="应用封面"
                  class="cover-image"
                />
                <div v-else class="cover-placeholder">
                  <plus-outlined />
                  <div class="upload-text">点击上传封面</div>
                </div>
              </div>
            </div>
            <div class="form-hint">支持图片链接,建议尺寸:400x300</div>
          </a-form-item>
        </div>

        <!-- 优先级 -->
        <div class="form-section">
          <h3 class="section-title">优先级</h3>

          <a-form-item label="优先级" name="priority">
            <a-select v-model:value="form.priority" placeholder="请选择优先级" style="width: 100%">
              <a-select-option :value="0">一般级</a-select-option>
              <a-select-option :value="99">精选级</a-select-option>
            </a-select>
            <div class="form-hint">精选级应用将在首页优先展示</div>
          </a-form-item>
        </div>

        <!-- 初始提示词 -->
        <div class="form-section">
          <h3 class="section-title">初始提示词</h3>

          <a-form-item label="初始提示词" name="initPrompt">
            <a-textarea
              v-model:value="form.initPrompt"
              :rows="4"
              :maxlength="1000"
              show-count
              placeholder="请输入初始提示词"
              disabled
            />
            <div class="form-hint">初始提示词不可修改</div>
          </a-form-item>
        </div>

        <!-- 生成类型 -->
        <div class="form-section">
          <h3 class="section-title">生成类型</h3>

          <a-form-item label="生成类型" name="genType">
            <a-input v-model:value="form.codeGenType" placeholder="生成类型" disabled />
            <div class="form-hint">生成类型不可修改</div>
          </a-form-item>
        </div>

        <!-- 部署密钥 -->
        <div class="form-section">
          <h3 class="section-title">部署密钥</h3>

          <a-form-item label="部署密钥" name="deployKey">
            <a-input v-model:value="form.deployKey" placeholder="部署密钥" disabled />
            <div class="form-hint">部署密钥不可修改</div>
          </a-form-item>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <a-space>
            <a-button type="primary" @click="handleSave" :loading="saving" size="large">
              保存修改
            </a-button>
            <a-button @click="handleReset" size="large"> 重置 </a-button>
            <a-button @click="goToChat" size="large"> 进入对话 </a-button>
          </a-space>
        </div>
      </a-card>

      <!-- 应用信息展示 -->
      <a-card :bordered="false" class="info-card" title="应用信息">
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">应用ID:</span>
            <span class="info-value">{{ appDetail?.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建者:</span>
            <div class="creator-info">
              <a-avatar :src="getFullAvatarUrl(appDetail?.user?.userAvatar || '')" :size="24">
                <template #icon>
                  <user-outlined />
                </template>
              </a-avatar>
              <span class="creator-name">{{ appDetail?.user?.userName || '未知用户' }}</span>
            </div>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间:</span>
            <span class="info-value">{{ formatTime(appDetail?.createTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">更新时间:</span>
            <span class="info-value">{{ formatTime(appDetail?.updateTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">部署时间:</span>
            <span class="info-value">{{ formatTime(appDetail?.deployedTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">访问链接:</span>
            <a-button type="link" @click="viewPreview" class="preview-link"> 查看预览 </a-button>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAppById, updateMyApp, updateApp, uploadAppCover } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { LeftOutlined, UserOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { getAppCoverUrl } from './appCoverUtils'
import { getFullResourceUrl, getAppDeployUrl } from '@/config/env'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const appId = String(route.params.id as string)
const saving = ref(false)
const appDetail = ref<API.AppVO>()

const form = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

const loginUserStore = useLoginUserStore()

// 拼接完整的头像URL
const getFullAvatarUrl = (avatarPath: string): string => {
  if (!avatarPath) return ''
  if (avatarPath.startsWith('http')) return avatarPath
  return getFullResourceUrl(avatarPath)
}

// 格式化时间
const formatTime = (timeStr: string | undefined): string => {
  if (!timeStr) return ''
  return dayjs(timeStr).format('YYYY-MM-DD HH:mm:ss')
}

// 获取应用详情
const fetchDetail = async () => {
  try {
    const res = await getAppById({ id: appId as any })
    if (res.data.code === 0 && res.data.data) {
      appDetail.value = res.data.data
      form.appName = res.data.data.appName || ''
      form.cover = res.data.data.cover || ''
      form.priority = (res.data.data.priority as number) || 0
      form.initPrompt = res.data.data.initPrompt || ''
      form.codeGenType = res.data.data.codeGenType || ''
      form.deployKey = res.data.data.deployKey || ''
    } else {
      message.error('获取应用详情失败')
    }
  } catch (error) {
    console.error('获取应用详情失败:', error)
    message.error('获取应用详情失败')
  }
}

// 处理封面上传
const handleCoverUpload = () => {
  // 创建文件输入元素
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (event) => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (file) {
      try {
        // 验证文件类型和大小
        const isImage = file.type.startsWith('image/')
        if (!isImage) {
          message.error('只能上传图片文件')
          return
        }

        const isLt2M = file.size / 1024 / 1024 < 2
        if (!isLt2M) {
          message.error('图片大小不能超过 2MB')
          return
        }

        // 创建 FormData
        const formData = new FormData()
        formData.append('file', file)

        // 调用上传API
        const res = await uploadAppCover({ appId: appId }, formData)

        if (res.data.code === 0 && res.data.data) {
          // 更新封面URL
          form.cover = res.data.data
          message.success('封面上传成功')
        } else {
          message.error('封面上传失败: ' + res.data.message)
        }
      } catch (error) {
        console.error('封面上传失败:', error)
        message.error('封面上传失败')
      }
    }
  }
  input.click()
}

// 保存修改
const handleSave = async () => {
  try {
    saving.value = true
    if (loginUserStore.loginUser?.userRole === 'admin') {
      const res = await updateApp({
        id: appId as any,
        appName: form.appName,
        cover: form.cover,
        priority: form.priority,
      })
      if (res.data.code === 0) {
        message.success('保存成功')
        await fetchDetail() // 重新获取数据
      } else {
        message.error(res.data.message)
      }
    } else {
      const res = await updateMyApp({
        id: appId as any,
        appName: form.appName,
      })
      if (res.data.code === 0) {
        message.success('保存成功')
        await fetchDetail() // 重新获取数据
      } else {
        message.error(res.data.message)
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 重置表单
const handleReset = () => {
  if (appDetail.value) {
    form.appName = appDetail.value.appName || ''
    form.cover = appDetail.value.cover || ''
    form.priority = (appDetail.value.priority as number) || 0
    form.initPrompt = appDetail.value.initPrompt || ''
    form.codeGenType = appDetail.value.codeGenType || ''
    form.deployKey = appDetail.value.deployKey || ''
  }
  message.info('已重置为原始数据')
}

// 进入对话
const goToChat = () => {
  router.push(`/app/chat/${appId}`)
}

// 查看预览
const viewPreview = () => {
  if (appDetail.value?.deployKey) {
    const deployUrl = getAppDeployUrl(appDetail.value.deployKey)
    window.open(deployUrl, '_blank')
  } else {
    message.info('应用尚未部署，请先部署应用')
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.app-edit-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  flex: 1;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-right: 100px; /* 为返回按钮留出空间 */
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.edit-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.info-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  height: fit-content;
}

.form-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f3f4f6;
}

.form-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  line-height: 1.4;
}

.cover-upload-container {
  margin-top: 12px;
}

.cover-preview {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  overflow: hidden;
  max-width: 400px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.cover-preview:hover {
  border-color: #3b82f6;
  background: #f0f9ff;
}

.cover-image {
  width: 100%;
  height: auto;
  display: block;
  max-height: 225px;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6b7280;
  text-align: center;
}

.cover-placeholder .anticon {
  font-size: 32px;
  margin-bottom: 8px;
  color: #9ca3af;
}

.upload-text {
  font-size: 14px;
  color: #6b7280;
}

.form-actions {
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  text-align: center;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 500;
  color: #374151;
  min-width: 80px;
  margin-right: 16px;
}

.info-value {
  color: #6b7280;
  flex: 1;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.creator-name {
  color: #6b7280;
  font-size: 14px;
}

.preview-link {
  padding: 0;
  height: auto;
  color: #3b82f6;
}

.preview-link:hover {
  color: #2563eb;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .header-title {
    margin-right: 0;
  }
}

@media (max-width: 768px) {
  .app-edit-page {
    padding: 12px;
  }

  .page-header {
    padding: 12px 16px;
  }

  .header-title {
    font-size: 18px;
  }
}
</style>
