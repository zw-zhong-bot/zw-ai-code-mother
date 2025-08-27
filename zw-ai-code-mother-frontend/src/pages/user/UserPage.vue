<template>
  <div class="user-page">
    <a-card title="用户信息" :bordered="false" style="max-width: 600px; margin: 50px auto">
      <a-form layout="vertical">
        <a-form-item label="用户ID">
          <a-input v-model:value="userInfo.id" disabled />
        </a-form-item>

        <a-form-item label="账号">
          <a-input v-model:value="userInfo.userAccount" disabled />
        </a-form-item>

        <a-form-item label="用户名">
          <a-input v-model:value="userInfo.userName" disabled />
        </a-form-item>

        <a-form-item label="头像">
          <div class="avatar-container">
            <a-avatar :size="120" :src="userAvatarUrl" />
            <div style="margin-top: 12px">
              <a-button size="small" @click="showAvatarUpload = true">修改头像</a-button>
            </div>
          </div>
        </a-form-item>

        <a-form-item label="简介">
          <a-textarea v-model:value="userInfo.userProfile" :rows="4" disabled />
        </a-form-item>

        <a-form-item label="用户角色">
          <a-tag v-if="userInfo.userRole === 'admin'" color="green">管理员</a-tag>
          <a-tag v-else color="blue">普通用户</a-tag>
        </a-form-item>

        <a-form-item label="创建时间">
          <a-input v-model:value="formattedCreateTime" disabled />
        </a-form-item>

        <a-form-item label="更新时间">
          <a-input v-model:value="formattedUpdateTime" disabled />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" @click="goToUpdate"> 修改信息 </a-button>
            <a-button @click="goBack"> 返回 </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 头像上传对话框 -->
    <a-modal
      v-model:open="showAvatarUpload"
      title="修改头像"
      @ok="handleAvatarUpload"
      @cancel="showAvatarUpload = false"
      :confirmLoading="uploading"
      okText="上传"
      cancelText="取消"
    >
      <a-upload
        v-model:file-list="avatarFileList"
        name="file"
        list-type="picture-card"
        class="avatar-uploader"
        :show-upload-list="false"
        :before-upload="beforeAvatarUpload"
        :custom-request="customAvatarUpload"
        @change="handleAvatarChange"
      >
        <img v-if="tempAvatarUrl" :src="tempAvatarUrl" alt="avatar" style="width: 100%" />
        <div v-else>
          <plus-outlined />
          <div style="margin-top: 8px">上传</div>
        </div>
      </a-upload>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getLoginUser, uploadUserAvatar } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { UploadChangeParam } from 'ant-design-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 拼接完整的头像URL
const getFullAvatarUrl = (avatarPath: string): string => {
  if (!avatarPath) return ''
  if (avatarPath.startsWith('http')) return avatarPath
  return `http://localhost:8123${avatarPath}`
}

// 计算用户头像URL
const userAvatarUrl = computed(() => {
  const avatar = userInfo.userAvatar
  if (!avatar) {
    return `https://picsum.photos/120/120?random=user`
  }
  return getFullAvatarUrl(avatar)
})

// 头像上传相关状态
const showAvatarUpload = ref(false)
const uploading = ref(false)
const avatarFileList = ref([])
const tempAvatarUrl = ref('')

const userInfo = reactive<API.LoginUserVO>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
  createTime: '',
  updateTime: '',
})

// 格式化创建时间
const formattedCreateTime = computed(() => {
  return userInfo.createTime ? dayjs(userInfo.createTime).format('YYYY-MM-DD HH:mm:ss') : ''
})

// 格式化更新时间
const formattedUpdateTime = computed(() => {
  return userInfo.updateTime ? dayjs(userInfo.updateTime).format('YYYY-MM-DD HH:mm:ss') : ''
})

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      Object.assign(userInfo, res.data.data)
    } else {
      message.error('获取用户信息失败')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    message.error('获取用户信息失败')
  }
}

// 跳转到修改信息页面
const goToUpdate = () => {
  router.push('/user/update')
}

// 返回上一页
const goBack = () => {
  router.push('/')
}

// 头像上传前验证
const beforeAvatarUpload = (file: File) => {
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

// 自定义头像上传
const customAvatarUpload = async (options: {
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

    // 创建 FormData
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', String(userInfo.id))

    // 调试：检查 FormData 内容
    for (const [key, value] of formData.entries()) {
      console.log('FormData 内容:', key, value)
    }

    // 调用上传API
    const res = await uploadUserAvatar(formData)

    console.log('上传响应:', res.data)

    if (res.data.code === 0 && res.data.data) {
      // 拼接完整的头像URL
      const fullAvatarUrl = getFullAvatarUrl(res.data.data)
      tempAvatarUrl.value = fullAvatarUrl
      onSuccess(fullAvatarUrl)
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
    console.log('头像上传完成')
  }
}

// 确认头像上传
const handleAvatarUpload = async () => {
  if (!tempAvatarUrl.value) {
    message.warning('请先选择头像文件')
    return
  }

  try {
    uploading.value = true
    // 更新用户信息
    userInfo.userAvatar = tempAvatarUrl.value
    // 更新本地存储
    loginUserStore.setLoginUser({
      ...loginUserStore.loginUser,
      userAvatar: tempAvatarUrl.value,
    })
    message.success('头像更新成功')
    showAvatarUpload.value = false
    tempAvatarUrl.value = ''
  } catch (error) {
    console.error('头像更新失败:', error)
    message.error('头像更新失败')
  } finally {
    uploading.value = false
  }
}

// 页面加载时获取用户信息
onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.user-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.avatar-container {
  text-align: center;
  margin: 10px 0;
}

.ant-form-item {
  margin-bottom: 24px;
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
