<template>
  <a-modal
    v-model:open="visible"
    :title="title || '应用详情'"
    :footer="null"
    :width="width || 480"
    @cancel="onClose"
  >
    <div v-if="appDetailData" class="app-info">
      <!-- 应用封面 -->
      <div class="info-item cover-item" v-if="showCover">
        <span class="label">封面：</span>
        <div class="cover-container">
          <div class="cover-wrapper">
            <img
              :src="getAppCoverUrl(appDetailData.cover)"
              class="app-cover-image"
              @error="handleImageError($event, ImageType.APP_COVER)"
            />
          </div>
        </div>
      </div>

      <!-- 创建者信息 -->
      <div class="info-item">
        <span class="label">创建者：</span>
        <div class="creator-info">
          <a-avatar
            :src="getFullAvatarUrl(appDetailData.user?.userAvatar || '')"
            :size="32"
            class="creator-avatar"
            @error="handleImageError($event, ImageType.USER_AVATAR_FILE)"
          >
            <template #icon>
              <user-outlined />
            </template>
          </a-avatar>
          <span class="creator-name">{{ appDetailData.user?.userName || '未知用户' }}</span>
        </div>
      </div>

      <!-- 应用ID -->
      <div class="info-item">
        <span class="label">应用ID：</span>
        <span class="value">{{ appDetailData.id }}</span>
      </div>

      <!-- 应用名称 -->
      <div class="info-item">
        <span class="label">应用名称：</span>
        <span class="value">{{ appDetailData.appName || '未命名应用' }}</span>
      </div>

      <!-- 创建时间 -->
      <div class="info-item">
        <span class="label">创建时间：</span>
        <span class="value">{{ formatTime(appDetailData.createTime) }}</span>
      </div>

      <!-- 更新时间 -->
      <div class="info-item" v-if="showUpdateTime && appDetailData.updateTime">
        <span class="label">更新时间：</span>
        <span class="value">{{ formatTime(appDetailData.updateTime) }}</span>
      </div>

      <!-- 部署时间 -->
      <div class="info-item" v-if="showDeployTime && appDetailData.deployedTime">
        <span class="label">部署时间：</span>
        <span class="value">{{ formatTime(appDetailData.deployedTime) }}</span>
      </div>

      <!-- 优先级 -->
      <div class="info-item">
        <span class="label">优先级：</span>
        <span class="value">
          <a-tag :color="getPriorityColor(appDetailData.priority)">
            {{ getPriorityLabel(appDetailData.priority) }}
          </a-tag>
        </span>
      </div>

      <!-- 生成类型 -->
      <div class="info-item" v-if="showGenType && appDetailData.codeGenType">
        <span class="label">生成类型：</span>
        <span class="value">
          <a-tag color="blue">{{ getCodeGenTypeLabel(appDetailData.codeGenType) }}</a-tag>
        </span>
      </div>

      <!-- 初始提示词 -->
      <div class="info-item prompt-item" v-if="showPrompt && appDetailData.initPrompt">
        <span class="label">初始提示词：</span>
        <div class="prompt-content">{{ appDetailData.initPrompt }}</div>
      </div>

      <!-- 自定义内容插槽 -->
      <slot name="extra-content" :app-detail="appDetailData"></slot>

      <!-- 操作按钮 -->
      <div v-if="showActions" class="action-buttons">
        <slot name="actions" :app-detail="appDetailData">
          <a-button type="primary" @click="onEdit" v-if="canEdit">
            <template #icon>
              <edit-outlined />
            </template>
            修改
          </a-button>
          <a-button @click="onChat">
            <template #icon>
              <message-outlined />
            </template>
            对话
          </a-button>
          <a-button danger @click="onDelete" v-if="canDelete">
            <template #icon>
              <delete-outlined />
            </template>
            删除
          </a-button>
        </slot>
      </div>
    </div>

    <!-- 加载中状态 -->
    <div v-else-if="loading" class="loading-container">
      <a-spin />
      <div class="loading-text">加载中...</div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-container">
      <a-empty>
        <template #description>
          <div>
            <p>无法加载应用详情</p>
            <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
            <a-button type="primary" size="small" @click="fetchAppDetail">重试</a-button>
          </div>
        </template>
      </a-empty>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * 应用详情弹窗组件
 * 用于展示应用的详细信息，包括创建者、时间、优先级等
 * @author ZW
 * @since 2025-09-08
 * @version 1.0.0
 */
import { ref, computed, watch } from 'vue'
import { getAppById, deleteApp, deleteMyApp } from '@/api/appController'
import { message, Modal } from 'ant-design-vue'
import { UserOutlined, EditOutlined, DeleteOutlined, MessageOutlined } from '@ant-design/icons-vue'
import { getAppCoverUrl, handleImageError, ImageType } from '@/utils/imageUtils'
import { useLoginUserStore } from '@/stores/loginUser'
import { CODE_GEN_TYPE_MAP } from '@/constants/codeGenType'
import { PRIORITY_CONFIG } from '@/constants/priority'
import dayjs from 'dayjs'

/**
 * 组件属性定义
 */
interface Props {
  /** 应用ID */
  appId?: string | number
  /** 应用详情数据（可选，如果提供则不会发起请求） */
  appDetail?: API.AppVO
  /** 弹窗标题 */
  title?: string
  /** 弹窗宽度 */
  width?: number
  /** 是否显示封面 */
  showCover?: boolean
  /** 是否显示更新时间 */
  showUpdateTime?: boolean
  /** 是否显示部署时间 */
  showDeployTime?: boolean
  /** 是否显示生成类型 */
  showGenType?: boolean
  /** 是否显示初始提示词 */
  showPrompt?: boolean
  /** 是否显示操作按钮 */
  showActions?: boolean
  /** 是否可编辑（默认根据用户权限判断） */
  editable?: boolean
  /** 是否可删除（默认根据用户权限判断） */
  deletable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  appId: undefined,
  appDetail: undefined,
  title: '应用详情',
  width: 480,
  showCover: true,
  showUpdateTime: false,
  showDeployTime: false,
  showGenType: true,
  showPrompt: true,
  showActions: true,
  editable: undefined,
  deletable: undefined,
})

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  /** 关闭弹窗事件 */
  (e: 'close'): void
  /** 编辑按钮点击事件 */
  (e: 'edit', id: string): void
  /** 对话按钮点击事件 */
  (e: 'chat', id: string): void
  /** 删除按钮点击事件 */
  (e: 'delete', id: string): void
  /** 应用详情加载完成事件 */
  (e: 'loaded', detail: API.AppVO): void
}>()

// 内部状态
const visible = ref(true)
const loading = ref(false)
const appDetailData = ref<API.AppVO | undefined>(props.appDetail)
const errorMessage = ref<string>('')
const loginUserStore = useLoginUserStore()

// 计算属性：当前用户是否是应用创建者
const isCurrentUserCreator = computed(() => {
  if (!appDetailData.value || !loginUserStore.loginUser.id) return false
  return loginUserStore.loginUser.id === appDetailData.value.userId
})

// 计算属性：当前用户是否是管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

// 计算属性：是否可以编辑应用
const canEdit = computed(() => {
  if (props.editable !== undefined) return props.editable
  return isCurrentUserCreator.value || isAdmin.value
})

// 计算属性：是否可以删除应用
const canDelete = computed(() => {
  if (props.deletable !== undefined) return props.deletable
  return isCurrentUserCreator.value || isAdmin.value
})

/**
 * 获取完整的头像URL
 * @param avatarPath 头像路径
 * @returns 完整的头像URL
 */
const getFullAvatarUrl = (avatarPath: string): string => {
  if (!avatarPath) return ''
  const avatarUrl = 'http://localhost:8123' + avatarPath
  return avatarUrl
}

/**
 * 格式化时间
 * @param timeStr 时间字符串
 * @returns 格式化后的时间字符串
 */
const formatTime = (timeStr: string | undefined): string => {
  if (!timeStr) return ''
  return dayjs(timeStr).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 获取优先级标签
 * @param priority 优先级值
 * @returns 优先级标签
 */
const getPriorityLabel = (priority: number | undefined): string => {
  if (priority === undefined) return '普通'
  return PRIORITY_CONFIG[priority]?.label || '普通'
}

/**
 * 获取优先级颜色
 * @param priority 优先级值
 * @returns 优先级颜色
 */
const getPriorityColor = (priority: number | undefined): string => {
  if (priority === undefined) return 'default'
  return PRIORITY_CONFIG[priority]?.color || 'default'
}

/**
 * 获取生成类型标签
 * @param codeGenType 生成类型
 * @returns 生成类型标签
 */
const getCodeGenTypeLabel = (codeGenType: string | undefined): string => {
  if (!codeGenType) return '未知类型'
  return CODE_GEN_TYPE_MAP[codeGenType] || codeGenType
}

/**
 * 获取应用详情
 */
const fetchAppDetail = async () => {
  if (!props.appId) {
    errorMessage.value = '应用ID不能为空'
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''
    console.log('正在获取应用详情，ID:', props.appId)

    // 确保ID是字符串类型
    const appId = String(props.appId)
    const res = await getAppById({ id: appId })
    console.log('应用详情API响应:', res)

    if (res.data && res.data.code === 0 && res.data.data) {
      appDetailData.value = res.data.data
      emit('loaded', res.data.data)
    } else {
      console.error('获取应用详情失败，响应数据:', res.data)
      errorMessage.value = '获取应用详情失败: ' + (res.data?.message || '未知错误')
      message.error(errorMessage.value)
      appDetailData.value = undefined
    }
  } catch (error) {
    console.error('获取应用详情失败:', error)
    errorMessage.value =
      '获取应用详情失败: ' + (error instanceof Error ? error.message : '未知错误')
    message.error(errorMessage.value)
    appDetailData.value = undefined
  } finally {
    loading.value = false
  }
}

/**
 * 关闭弹窗
 */
const onClose = () => {
  visible.value = false
  emit('close')
}

/**
 * 编辑按钮点击事件处理
 */
const onEdit = () => {
  if (!appDetailData.value) return
  emit('edit', appDetailData.value.id as unknown as string)
}

/**
 * 对话按钮点击事件处理
 */
const onChat = () => {
  if (!appDetailData.value) return
  emit('chat', appDetailData.value.id as unknown as string)
}

/**
 * 删除按钮点击事件处理
 */
const onDelete = () => {
  if (!appDetailData.value) return

  Modal.confirm({
    title: '确定要删除这个应用吗？',
    content: '删除后无法恢复，请谨慎操作。',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        const id = appDetailData.value?.id as number
        if (isAdmin.value) {
          await deleteApp({ id })
        } else {
          await deleteMyApp({ id })
        }
        message.success('应用删除成功')
        emit('delete', id as unknown as string)
        onClose()
      } catch (error) {
        console.error('删除应用失败:', error)
        message.error('删除应用失败')
      }
    },
  })
}

// 监听appId变化，重新获取应用详情
watch(
  () => props.appId,
  (newVal) => {
    if (newVal) {
      // 无论是否有props.appDetail，都重新获取应用详情
      // 这样可以确保每次打开弹窗时都获取最新数据
      fetchAppDetail()
    } else {
      // 如果appId为空，清空应用详情数据
      appDetailData.value = undefined
    }
  },
  { immediate: true },
)

// 监听appDetail属性变化
watch(
  () => props.appDetail,
  (newVal) => {
    if (newVal) {
      appDetailData.value = newVal
    }
  },
)
</script>

<style scoped>
/* 应用详情卡片样式 */
.app-info {
  padding: 16px 0;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  color: #374151;
  min-width: 80px;
  margin-right: 16px;
}

.value {
  color: #6b7280;
  flex: 1;
}

/* 创建者信息样式 */
.creator-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.creator-avatar {
  flex-shrink: 0;
}

.creator-name {
  color: #374151;
  font-weight: 500;
}

/* 封面样式 */
.cover-item {
  align-items: flex-start;
}

.cover-container {
  flex: 1;
  display: flex;
  justify-content: center;
}

.cover-wrapper {
  width: 100%;
  max-width: 320px;
  height: 180px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: #f5f5f5;
  position: relative;
}

.app-cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.3s ease;
}

.app-cover-image:hover {
  transform: scale(1.05);
}

/* 提示词样式 */
.prompt-item {
  flex-direction: column;
  align-items: flex-start;
}

.prompt-content {
  margin-top: 8px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  width: 100%;
  color: #374151;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 加载中状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading-text {
  margin-top: 16px;
  color: #6b7280;
}

/* 错误状态样式 */
.error-container {
  padding: 20px 0;
}

.error-message {
  color: #f56c6c;
  margin-bottom: 12px;
  font-size: 14px;
}

/* 响应式样式 */
@media (max-width: 480px) {
  .cover-wrapper {
    height: 160px;
  }

  .label {
    min-width: 70px;
    margin-right: 8px;
  }
}
</style>
