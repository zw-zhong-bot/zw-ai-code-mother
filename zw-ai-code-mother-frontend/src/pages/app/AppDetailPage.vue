<template>
  <a-card :title="detail?.appName || '应用详情'" style="max-width: 900px; margin: 24px auto">
    <a-descriptions bordered column="1" v-if="detail">
      <a-descriptions-item label="ID">{{ detail.id }}</a-descriptions-item>
      <a-descriptions-item label="封面">
        <img
          :src="getAppCoverUrl(detail?.cover, detail?.id)"
          style="width: 200px; height: 120px; object-fit: cover; border-radius: 8px"
          @error="handleCoverLoadError"
        />
      </a-descriptions-item>
      <a-descriptions-item label="优先级">{{ detail.priority }}</a-descriptions-item>
      <a-descriptions-item label="初始提示词">{{ detail.initPrompt }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
      <a-descriptions-item label="预览">
        <a-button type="link" @click="goChat">进入对话生成</a-button>
        <a-button type="link" @click="goEdit">编辑</a-button>
      </a-descriptions-item>
    </a-descriptions>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAppById } from '@/api/appController.ts'
import { getAppCoverUrl } from './appCoverUtils'

const route = useRoute()
const router = useRouter()
const id = String(route.params.id as string)
const detail = ref<API.AppVO | undefined>()

/**
 * 获取应用详情
 */
const fetchDetail = async () => {
  const res = await getAppById({ id })
  if (res.data.code === 0) detail.value = res.data.data as API.AppVO
}

/**
 * 跳转到对话页面
 */
const goChat = () => {
  // 添加view=1参数，避免自动发送消息
  router.push({ path: `/app/chat/${String(id)}`, query: { view: '1' } })
}

/**
 * 跳转到编辑页面
 */
const goEdit = () => router.push(`/app/edit/${String(id)}`)

/**
 * 处理封面图片加载失败
 * @param event 错误事件
 */
const handleCoverLoadError = (event: Event) => {
  const imgElement = event.target as HTMLImageElement
  console.warn(`封面图片加载失败，使用默认封面`)
  imgElement.src = getAppCoverUrl('', detail.value?.id)
  imgElement.alt = '默认封面'
}

onMounted(fetchDetail)
</script>
