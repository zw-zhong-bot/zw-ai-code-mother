<template>
  <a-card :title="detail?.appName || '应用详情'" style="max-width: 900px; margin: 24px auto">
    <a-descriptions bordered column="1" v-if="detail">
      <a-descriptions-item label="ID">{{ detail.id }}</a-descriptions-item>
      <a-descriptions-item label="封面">
        <img
          :src="detail?.cover || `https://picsum.photos/200/120?random=${detail?.id}`"
          style="width: 200px; height: 120px; object-fit: cover; border-radius: 8px"
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

const route = useRoute()
const router = useRouter()
const id = String(route.params.id as string)
const detail = ref<API.AppVO | undefined>()

const fetchDetail = async () => {
  const res = await getAppById({ id: id as any })
  if (res.data.code === 0) detail.value = res.data.data as any
}

const goChat = () => router.push(`/app/chat/${String(id)}`)
const goEdit = () => router.push(`/app/edit/${String(id)}`)

onMounted(fetchDetail)
</script>
