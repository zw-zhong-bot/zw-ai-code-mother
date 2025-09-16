<template>
  <a-card style="max-width: 900px; margin: 24px auto">
    <template #title>
      <div class="card-title">
        <span class="app-name">{{ detail?.appName || '应用详情' }}</span>
        <a-tag v-if="detail?.codeGenType" color="blue" class="gen-type-tag">
          {{ getCodeGenTypeLabel(detail.codeGenType) }}
        </a-tag>
      </div>
    </template>
    <a-descriptions bordered column="1" v-if="detail">
      <a-descriptions-item label="ID">{{ detail.id }}</a-descriptions-item>
      <a-descriptions-item label="封面">
        <img
          :src="getAppCoverUrl(detail?.cover)"
          style="width: 200px; height: 120px; object-fit: cover; border-radius: 8px"
          @error="handleCoverLoadError"
         alt=""/>
      </a-descriptions-item>
      <a-descriptions-item label="优先级">{{ detail.priority }}</a-descriptions-item>
      <a-descriptions-item label="生成类型">
        <a-tag v-if="detail.codeGenType" color="blue">
          {{ getCodeGenTypeLabel(detail.codeGenType) }}
        </a-tag>
        <span v-else>未设置</span>
      </a-descriptions-item>
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
import { CODE_GEN_TYPE_MAP } from '@/constants/codeGenType'

const route = useRoute()
const router = useRouter()
const id = String(route.params.id as string)
const detail = ref<API.AppVO | undefined>()

/**
 * 获取代码生成类型标签
 * @param codeGenType 代码生成类型
 * @returns 代码生成类型标签
 */
const getCodeGenTypeLabel = (codeGenType: string | undefined): string => {
  if (!codeGenType) return '未知类型'
  return CODE_GEN_TYPE_MAP[codeGenType] || codeGenType
}

/**
 * 获取应用详情
 */
const fetchDetail = async () => {
  const res = await getAppById({ id })
  console.log('id用户信息', (detail.value = res.data.data?.codeGenType))
  if (res.data.code === 0) detail.value = res.data.data as API.AppVO
  // const irl=getAppCoverUrl(detail?.cover);
  // console.log('id用户信息', irl)
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
  imgElement.src = getAppCoverUrl('')
  imgElement.alt = '默认封面'
}

onMounted(fetchDetail)
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.gen-type-tag {
  font-size: 12px;
  font-weight: 500;
}
</style>
