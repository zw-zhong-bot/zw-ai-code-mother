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
            <a-avatar :size="120" :src="userInfo.userAvatar" />
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
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getLoginUser } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import dayjs from 'dayjs'

const router = useRouter()
const loginUserStore = useLoginUserStore()

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
</style>
