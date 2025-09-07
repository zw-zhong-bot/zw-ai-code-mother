<template>
  <div class="user-update-page">
    <a-card title="修改用户信息" :bordered="false" style="max-width: 600px; margin: 50px auto">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" @finish="handleSubmit">
        <a-form-item label="账号" name="userAccount">
          <a-input v-model:value="form.userAccount" placeholder="请输入账号" disabled />
        </a-form-item>

        <a-form-item label="用户名" name="userName">
          <a-input v-model:value="form.userName" placeholder="请输入用户名" />
        </a-form-item>

        <a-form-item label="头像" name="userAvatar">
          <a-upload
            v-model:file-list="fileList"
            name="file"
            list-type="picture-card"
            class="avatar-uploader"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
            @change="handleAvatarChange"
          >
            <img
              v-if="form.userAvatar"
              :src="getFullAvatarUrl(form.userAvatar)"
              alt="avatar"
              style="width: 100%"
            />
            <div v-else>
              <plus-outlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
        </a-form-item>

        <a-form-item label="简介" name="userProfile">
          <a-textarea v-model:value="form.userProfile" placeholder="请输入用户简介" :rows="4" />
        </a-form-item>

        <a-form-item label="用户角色" name="userRole">
          <a-select v-model:value="form.userRole" placeholder="请选择用户角色" disabled>
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading"> 保存修改 </a-button>
            <a-button @click="goBack"> 返回 </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { UploadChangeParam } from 'ant-design-vue'
import { updateUser, getLoginUser, uploadUserAvatar } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { getFullResourceUrl } from '@/config/env'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 拼接完整的头像URL
const getFullAvatarUrl = (avatarPath: string): string => {
  if (!avatarPath) return ''
  if (avatarPath.startsWith('http')) return avatarPath
  return getFullResourceUrl(avatarPath)
}

const formRef = ref<FormInstance>()
const fileList = ref([])
const loading = ref(false)

const form = reactive<API.UserUpdateRequest & { userAccount?: string }>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

// 图片上传前验证
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG 格式的图片!')
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!')
  }
  return isJpgOrPng && isLt2M
}

// 自定义上传
const customUpload = async (options: {
  file: File
  onSuccess: (data: string) => void
  onError: (error: Error) => void
}) => {
  const { file, onSuccess, onError } = options

  try {
    console.log('开始上传头像，文件信息:', {
      name: file.name,
      size: file.size,
      type: file.type,
    })

    // 创建 FormData，只包含文件
    const formData = new FormData()
    formData.append('file', file)

    // 调用上传API，将 userId 作为 URL 参数传递
    const res = await uploadUserAvatar({ userId: form.id }, formData)

    console.log('上传响应:', res.data)

    if (res.data.code === 0 && res.data.data) {
      // 拼接完整的头像URL
      const fullAvatarUrl = getFullAvatarUrl(res.data.data)
      form.userAvatar = fullAvatarUrl
      onSuccess(fullAvatarUrl)
      message.success('头像上传成功')
    } else {
      onError(new Error(res.data.message))
      message.error('头像上传失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    onError(error instanceof Error ? error : new Error('上传失败'))
    message.error('头像上传失败')
  }
}

// 处理头像上传变化
const handleAvatarChange = (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    return
  }
  if (info.file.status === 'done') {
    // 上传成功，头像URL已经在customUpload中设置
    console.log('头像上传完成')
  }
}

// 获取当前用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      const userInfo = res.data.data
      Object.assign(form, {
        id: userInfo.id,
        userAccount: userInfo.userAccount,
        userName: userInfo.userName,
        userAvatar: userInfo.userAvatar,
        userProfile: userInfo.userProfile,
        userRole: userInfo.userRole,
      })
    } else {
      message.error('获取用户信息失败')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    message.error('获取用户信息失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true

    const res = await updateUser(form)
    if (res.data.code === 0) {
      message.success('用户信息更新成功')
      // 更新本地存储的用户信息
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userName: form.userName,
        userAvatar: form.userAvatar,
        userProfile: form.userProfile,
      })
      // 跳转到主页
      await router.push('/user')
    } else {
      message.error('更新失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('更新用户信息失败:', error)
    message.error('更新用户信息失败')
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 页面加载时获取用户信息
onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.user-update-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.avatar-uploader {
  text-align: center;
}

.avatar-uploader .ant-upload {
  width: 128px;
  height: 128px;
  margin: 0 auto;
}
</style>
