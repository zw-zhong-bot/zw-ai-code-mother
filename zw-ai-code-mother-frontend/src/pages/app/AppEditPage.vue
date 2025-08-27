<template>
  <a-card title="编辑应用" style="max-width: 640px; margin: 24px auto">
    <a-form layout="vertical" :model="form" @finish="onSubmit">
      <a-form-item
        label="应用名称"
        name="appName"
        :rules="[{ required: true, message: '请输入名称' }]"
      >
        <a-input v-model:value="form.appName" />
      </a-form-item>
      <a-form-item label="封面URL (管理员可改)">
        <a-input v-model:value="form.cover" />
      </a-form-item>
      <a-form-item label="优先级 (管理员可改)">
        <a-input-number v-model:value="form.priority" :min="0" :max="999" style="width: 100%" />
      </a-form-item>
      <a-space>
        <a-button type="primary" html-type="submit" :loading="saving">保存</a-button>
        <a-button @click="goBack">返回</a-button>
      </a-space>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAppById, updateMyApp, updateApp } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const appId = String(route.params.id as string)
const saving = ref(false)
const form = reactive<API.AppUpdateRequest>({
  id: appId as any,
  appName: '',
  cover: '',
  priority: 0,
})

const login = useLoginUserStore()

const fetchDetail = async () => {
  const res = await getAppById({ id: appId as any })
  if (res.data.code === 0 && res.data.data) {
    form.appName = res.data.data.appName || ''
    form.cover = res.data.data.cover || ''
    form.priority = (res.data.data.priority as number) || 0
  }
}

const onSubmit = async () => {
  try {
    saving.value = true
    if (login.loginUser?.userRole === 'admin') {
      const res = await updateApp(form)
      if (res.data.code === 0) message.success('保存成功')
      else message.error(res.data.message)
    } else {
      const res = await updateMyApp({ id: appId as any, appName: form.appName })
      if (res.data.code === 0) message.success('保存成功')
      else message.error(res.data.message)
    }
  } finally {
    saving.value = false
  }
}

const goBack = () => router.back()

onMounted(fetchDetail)
</script>
