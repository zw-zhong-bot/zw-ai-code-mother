<template>
  <div id="userManagePage">
    <!--搜索表单-->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="showCreateModal">创建用户</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!--    表单-->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" @click="showUpdateModal(record)">更新</a-button>
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 创建用户对话框 -->
    <a-modal
      v-model:open="createModalVisible"
      title="创建用户"
      @ok="handleCreateUser"
      @cancel="handleCancelCreate"
      :confirmLoading="createLoading"
      okText="创建"
      cancelText="取消"
    >
      <a-form ref="createFormRef" :model="createForm" :rules="createFormRules" layout="vertical">
        <a-form-item label="账号" name="userAccount">
          <a-input v-model:value="createForm.userAccount" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="用户名" name="userName">
          <a-input v-model:value="createForm.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像" name="userAvatar">
          <a-upload
            v-model:file-list="fileList"
            name="file"
            list-type="picture-card"
            class="avatar-uploader"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            @change="handleAvatarChange"
          >
            <img
              v-if="createForm.userAvatar"
              :src="createForm.userAvatar"
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
          <a-textarea
            v-model:value="createForm.userProfile"
            placeholder="请输入用户简介"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="用户角色" name="userRole">
          <a-select v-model:value="createForm.userRole" placeholder="请选择用户角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 更新用户对话框 -->
    <a-modal
      v-model:open="updateModalVisible"
      title="更新用户信息"
      @ok="handleUpdateUser"
      @cancel="handleCancelUpdate"
      :confirmLoading="updateLoading"
      okText="更新"
      cancelText="取消"
    >
      <a-form ref="updateFormRef" :model="updateForm" :rules="updateFormRules" layout="vertical">
        <a-form-item label="账号" name="userAccount">
          <a-input v-model:value="updateForm.userAccount" placeholder="请输入账号" disabled />
        </a-form-item>
        <a-form-item label="用户名" name="userName">
          <a-input v-model:value="updateForm.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像" name="userAvatar">
          <a-upload
            v-model:file-list="updateFileList"
            name="file"
            list-type="picture-card"
            class="avatar-uploader"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            @change="handleUpdateAvatarChange"
          >
            <img
              v-if="updateForm.userAvatar"
              :src="updateForm.userAvatar"
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
          <a-textarea
            v-model:value="updateForm.userProfile"
            placeholder="请输入用户简介"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="用户角色" name="userRole">
          <a-select v-model:value="updateForm.userRole" placeholder="请选择用户角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage, addUser, updateUser } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { UploadChangeParam } from 'ant-design-vue'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

//从后端获取数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
//搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 创建用户相关
const createModalVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const fileList = ref([])
const createForm = reactive<API.UserAddRequest>({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

// 更新用户相关
const updateModalVisible = ref(false)
const updateLoading = ref(false)
const updateFormRef = ref<FormInstance>()
const updateFileList = ref([])
const updateForm = reactive<API.UserUpdateRequest & { userAccount?: string }>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const createFormRules = {
  userAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userRole: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
}

const updateFormRules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userRole: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
}

// 显示创建用户对话框
const showCreateModal = () => {
  createModalVisible.value = true
  // 重置表单
  Object.assign(createForm, {
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
  })
}

// 处理创建用户
const handleCreateUser = async () => {
  try {
    await createFormRef.value?.validate()
    createLoading.value = true

    const res = await addUser(createForm)
    if (res.data.code === 0) {
      message.success('创建用户成功')
      createModalVisible.value = false
      // 刷新数据
      fetchData()
    } else {
      message.error('创建用户失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('创建用户失败:', error)
    message.error('创建用户失败')
  } finally {
    createLoading.value = false
  }
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

// 处理头像上传变化
const handleAvatarChange = (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    return
  }
  if (info.file.status === 'done') {
    // 这里假设后端返回的数据结构包含图片URL
    // 实际使用时需要根据后端API调整
    if (info.file.originFileObj) {
      createForm.userAvatar =
        info.file.response?.data || URL.createObjectURL(info.file.originFileObj)
    }
  }
}

// 取消创建用户
const handleCancelCreate = () => {
  createModalVisible.value = false
  createFormRef.value?.resetFields()
  fileList.value = []
  createForm.userAvatar = ''
}

// 显示更新用户对话框
const showUpdateModal = (record: API.UserVO) => {
  updateModalVisible.value = true
  // 填充表单数据
  Object.assign(updateForm, {
    id: record.id,
    userAccount: record.userAccount,
    userName: record.userName,
    userAvatar: record.userAvatar,
    userProfile: record.userProfile,
    userRole: record.userRole,
  })
  updateFileList.value = []
}

// 处理更新用户头像变化
const handleUpdateAvatarChange = (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    return
  }
  if (info.file.status === 'done') {
    if (info.file.originFileObj) {
      updateForm.userAvatar =
        info.file.response?.data || URL.createObjectURL(info.file.originFileObj)
    }
  }
}

// 处理更新用户
const handleUpdateUser = async () => {
  try {
    await updateFormRef.value?.validate()
    updateLoading.value = true

    const res = await updateUser(updateForm)
    if (res.data.code === 0) {
      message.success('更新用户成功')
      updateModalVisible.value = false
      // 刷新数据
      fetchData()
    } else {
      message.error('更新用户失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('更新用户失败:', error)
    message.error('更新用户失败')
  } finally {
    updateLoading.value = false
  }
}

// 取消更新用户
const handleCancelUpdate = () => {
  updateModalVisible.value = false
  updateFormRef.value?.resetFields()
  updateFileList.value = []
  updateForm.userAvatar = ''
}

//获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败' + res.data.message)
  }
}

//分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChangger: true,
    showTotal: (total: number) => `共${total}条`,
  }
})
//表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}
//获取数据
const doSearch = () => {
  //重置页码
  searchParams.pageNum = 1
  fetchData()
}
//删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    //刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}
//页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>
