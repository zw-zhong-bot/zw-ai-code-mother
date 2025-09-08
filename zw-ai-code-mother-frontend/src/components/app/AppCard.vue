<template>
  <div class="app-card" @click="onCardClick">
    <!-- 应用封面 -->
    <div class="app-cover">
      <img
        :src="coverUrl"
        :alt="appData.appName || '应用封面'"
        class="cover-image"
        @error="handleImageError($event, ImageType.APP_COVER)"
      />
    </div>

    <!-- 应用信息 -->
    <div class="app-info">
      <div class="user-avatar">
        <img
          :src="avatarUrl"
          :alt="appData.user?.userName || '用户头像'"
          class="avatar-image"
          @error="handleImageError($event, ImageType.USER_AVATAR_API)"
        />
      </div>
      <div class="app-details">
        <div class="app-name">{{ appData.appName || '未命名应用' }}</div>
        <div class="user-name">{{ appData.user?.userName || '匿名用户' }}</div>
      </div>
    </div>

    <!-- 应用操作按钮 -->
    <div class="app-actions">
      <slot name="actions">
        <a-button type="primary" size="small" @click.stop="onChatClick"> 查看对话 </a-button>
        <a-button size="small" @click.stop="onViewClick" v-if="appData.deployKey">
          查看作品
        </a-button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 应用卡片组件
 * 用于展示应用的基本信息，包括封面、名称、创建者等
 * @author ZW
 * @since 2025-09-08
 * @version 1.1.0
 * @modifyDate 2025-09-08
 */
import { computed, onMounted, ref } from 'vue'
import {
  getAppCoverUrl,
  getUserAvatarApiUrl,
  getDefaultAvatarUrl,
  handleImageError,
  ImageType,
  getReliableUserAvatarUrl,
} from '@/utils/imageUtils'
import { getAppDeployUrl } from '@/config/env'

/**
 * 组件属性定义
 */
interface Props {
  /** 应用数据 */
  appData: API.AppVO
}

const props = defineProps<Props>()

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  /** 卡片点击事件 */
  (e: 'card-click', id: string): void
  /** 查看对话按钮点击事件 */
  (e: 'chat-click', id: string): void
  /** 查看作品按钮点击事件 */
  (e: 'view-click', deployKey: string): void
}>()

/**
 * 计算属性：封面URL
 */
const coverUrl = computed(() => getAppCoverUrl(props.appData.cover))

/**
 * 用户头像URL
 */
const avatarUrl = ref<string>(getDefaultAvatarUrl())

/**
 * 初始化用户头像
 */
const initUserAvatar = async () => {
  try {
    // 首先设置一个默认值，确保界面不会出现空白
    avatarUrl.value = getUserAvatarApiUrl(props.appData.user?.id || 0)

    // 异步获取可靠的头像URL
    if (props.appData.user) {
      const reliableUrl = await getReliableUserAvatarUrl(
        props.appData.user.id,
        props.appData.user.userAvatar,
      )
      avatarUrl.value = reliableUrl
    } else {
      avatarUrl.value = getDefaultAvatarUrl()
    }
  } catch (error) {
    console.error('获取用户头像失败:', error)
    avatarUrl.value = getDefaultAvatarUrl()
  }
}

/**
 * 卡片点击事件处理
 */
const onCardClick = () => {
  emit('card-click', props.appData.id as unknown as string)
}

/**
 * 查看对话按钮点击事件处理
 */
const onChatClick = () => {
  emit('chat-click', props.appData.id as unknown as string)
}

/**
 * 查看作品按钮点击事件处理
 */
const onViewClick = () => {
  if (props.appData.deployKey) {
    emit('view-click', props.appData.deployKey)
  }
}

// 组件挂载时初始化头像
onMounted(() => {
  initUserAvatar()
})
</script>

<style scoped>
/* 应用卡片样式 */
.app-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 应用封面样式 */
.app-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  position: relative;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .cover-image {
  transform: scale(1.05);
}

/* 应用信息样式 */
.app-info {
  padding: 16px;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 用户头像样式 */
.user-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 应用详情样式 */
.app-details {
  flex: 1;
  min-width: 0; /* 允许内容缩小 */
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name {
  font-size: 14px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 应用操作按钮样式 */
.app-actions {
  padding: 0 16px 16px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.app-card:hover .app-actions {
  opacity: 1;
}
</style>
