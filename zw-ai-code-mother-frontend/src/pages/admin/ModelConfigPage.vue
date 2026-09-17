<template>
  <div id="modelConfigPage">
    <a-alert
      type="info"
      show-icon
      message="模型配置可插拔"
      description="同一种能力可配置多份参数，由“默认”且“启用”的那一份生效；保存后立即热生效，无需重启服务。未配置时自动回退 application.yml 中的参数。"
      style="margin-bottom: 16px"
    />
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="fetchData">
      <a-form-item label="能力">
        <a-select
          v-model:value="searchParams.capability"
          :options="capabilitySelectOptions"
          placeholder="全部"
          allow-clear
          style="width: 170px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="searchParams.status"
          :options="statusSelectOptions"
          placeholder="全部"
          allow-clear
          style="width: 120px"
        />
      </a-form-item>
      <a-form-item label="配置名称">
        <a-input v-model:value="searchParams.configName" placeholder="模糊匹配" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
        <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 操作区 -->
    <a-space style="margin-bottom: 12px">
      <a-button type="primary" @click="openAddDrawer">新增配置</a-button>
      <a-button :loading="refreshing" @click="doRefreshCache">刷新模型缓存</a-button>
    </a-space>
    <!-- 表格 -->
    <a-table
      row-key="id"
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'capability'">
          <a-tag color="blue">{{ record.capabilityText ?? record.capability }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'provider'">
          {{ record.providerText ?? record.provider }}
        </template>
        <template v-else-if="column.dataIndex === 'apiKey'">
          <span v-if="record.apiKeyConfigured">{{ record.apiKeyMasked }}</span>
          <a-tag v-else color="orange">未配置</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'isDefault'">
          <a-tag v-if="record.isDefault === 1" color="green">默认</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            :checked="record.status === 1"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked: boolean) => doToggleStatus(record, checked)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ record.updateTime ? dayjs(record.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEditDrawer(record)">编辑</a-button>
            <a-button size="small" :loading="testingId === record.id" @click="doTest(record)">
              测试连接
            </a-button>
            <a-button
              size="small"
              :disabled="record.isDefault === 1 || record.status !== 1"
              @click="doSetDefault(record)"
            >
              设为默认
            </a-button>
            <a-button size="small" danger @click="doDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 新增 / 编辑抽屉 -->
    <a-drawer
      :open="drawerOpen"
      :title="isEdit ? '编辑模型配置' : '新增模型配置'"
      width="560"
      @close="closeDrawer"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="能力类型" required>
          <a-select
            v-model:value="form.capability"
            :options="capabilitySelectOptions"
            placeholder="请选择能力类型"
            @change="onCapabilityChange"
          />
          <div class="field-tip">
            {{ currentCapabilityTip }}
          </div>
        </a-form-item>
        <a-form-item label="提供方" required>
          <a-select
            v-model:value="form.provider"
            :options="providerSelectOptions"
            placeholder="请先选择能力类型"
          />
        </a-form-item>
        <a-form-item label="配置名称" required>
          <a-input v-model:value="form.configName" placeholder="全局唯一，如 chat-default" />
        </a-form-item>
        <a-form-item label="接口地址">
          <a-input v-model:value="form.baseUrl" placeholder="OpenAI 兼容接口必填，如 https://api.xxx.com/v1" />
        </a-form-item>
        <a-form-item label="模型名称">
          <a-input v-model:value="form.modelName" placeholder="如 DeepSeek-V4.1-Flash / wan2.2-t2i-flash" />
        </a-form-item>
        <a-form-item label="API 密钥">
          <a-input-password
            v-model:value="form.apiKey"
            :placeholder="isEdit ? '留空表示不修改原密钥' : '请输入密钥明文，落库前自动加密'"
          />
          <div v-if="isEdit" class="field-tip">
            当前：{{ editApiKeyMasked || '未配置' }}
          </div>
        </a-form-item>
        <a-form-item label="扩展参数（JSON）">
          <a-textarea
            v-model:value="form.params"
            :rows="4"
            :placeholder="paramsPlaceholder"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="form.remark" placeholder="用途说明" />
        </a-form-item>
        <a-form-item>
          <a-space size="large">
            <span>
              设为默认
              <a-switch
                :checked="form.isDefault === 1"
                @change="(checked: boolean) => (form.isDefault = checked ? 1 : 0)"
              />
            </span>
            <span>
              启用
              <a-switch
                :checked="form.status === 1"
                @change="(checked: boolean) => (form.status = checked ? 1 : 0)"
              />
            </span>
          </a-space>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="closeDrawer">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="doSubmit">保存</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addModelConfig,
  deleteModelConfig,
  listModelCapabilities,
  listModelConfig,
  refreshModelConfigCache,
  setDefaultModelConfig,
  testModelConfig,
  toggleModelConfigStatus,
  updateModelConfig,
} from '@/api/modelConfigController.ts'

const columns = [
  { title: '配置名称', dataIndex: 'configName' },
  { title: '能力', dataIndex: 'capability' },
  { title: '提供方', dataIndex: 'provider' },
  { title: '模型名称', dataIndex: 'modelName' },
  { title: '接口地址', dataIndex: 'baseUrl', ellipsis: true },
  { title: '密钥', dataIndex: 'apiKey' },
  { title: '默认', dataIndex: 'isDefault' },
  { title: '状态', dataIndex: 'status' },
  { title: '更新时间', dataIndex: 'updateTime' },
  { title: '操作', key: 'action', width: 300 },
]

// 列表数据
const data = ref<API.ModelConfigVO[]>([])
const loading = ref(false)
const refreshing = ref(false)
const submitting = ref(false)
const testingId = ref<string>()

// 能力与提供方选项（由后端枚举驱动，新增提供方无需改前端）
const capabilityOptions = ref<API.ModelCapabilityOptionVO[]>([])

// 搜索条件
const searchParams = reactive<API.ModelConfigQueryRequest>({})

// 抽屉表单
const drawerOpen = ref(false)
const isEdit = ref(false)
const editApiKeyMasked = ref('')
const form = reactive<API.ModelConfigUpdateRequest>({
  status: 1,
  isDefault: 0,
})

const capabilitySelectOptions = computed(() =>
  capabilityOptions.value.map((item) => ({
    value: item.capability,
    label: item.capabilityText,
  })),
)

const statusSelectOptions = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' },
]

const providerSelectOptions = computed(() => {
  const target = capabilityOptions.value.find((item) => item.capability === form.capability)
  return (target?.providers ?? []).map((item) => ({ value: item.value, label: item.text }))
})

const currentCapabilityTip = computed(() => {
  if (form.capability === 'IMAGE_GEN') {
    return '图像生成：扩展参数支持 size（如 512*512）与 n（张数）'
  }
  if (form.capability === 'IMAGE_SEARCH') {
    return '图片搜索：只需接口地址与密钥，模型名称可留空'
  }
  return '对话类：扩展参数支持 maxTokens、temperature、timeout（秒或 120s）、logRequests、logResponses'
})

const paramsPlaceholder = computed(() => {
  if (form.capability === 'IMAGE_GEN') {
    return '{"size":"512*512","n":1}'
  }
  if (form.capability === 'IMAGE_SEARCH') {
    return '{}'
  }
  return '{"maxTokens":8192,"temperature":0.7,"timeout":"120s"}'
})

// 获取配置列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listModelConfig({ ...searchParams })
    if (res.data.code === 0) {
      data.value = res.data.data ?? []
    } else {
      message.error('获取模型配置失败，' + res.data.message)
    }
  } finally {
    loading.value = false
  }
}

// 获取能力选项
const fetchCapabilities = async () => {
  const res = await listModelCapabilities()
  if (res.data.code === 0) {
    capabilityOptions.value = res.data.data ?? []
  }
}

const resetSearch = () => {
  searchParams.capability = undefined
  searchParams.status = undefined
  searchParams.configName = undefined
  fetchData()
}

// 能力切换后校正提供方
const onCapabilityChange = () => {
  const supported = providerSelectOptions.value.map((item) => item.value)
  if (!supported.includes(form.provider)) {
    form.provider = supported[0]
  }
}

const resetForm = () => {
  form.id = undefined
  form.configName = undefined
  form.capability = undefined
  form.provider = undefined
  form.baseUrl = undefined
  form.apiKey = undefined
  form.modelName = undefined
  form.params = undefined
  form.remark = undefined
  form.isDefault = 0
  form.status = 1
  editApiKeyMasked.value = ''
}

const openAddDrawer = () => {
  resetForm()
  isEdit.value = false
  drawerOpen.value = true
}

const openEditDrawer = (record: API.ModelConfigVO) => {
  resetForm()
  isEdit.value = true
  form.id = record.id
  form.configName = record.configName
  form.capability = record.capability
  form.provider = record.provider
  form.baseUrl = record.baseUrl
  form.modelName = record.modelName
  form.params = record.params
  form.remark = record.remark
  form.isDefault = record.isDefault
  form.status = record.status
  editApiKeyMasked.value = record.apiKeyConfigured ? (record.apiKeyMasked ?? '') : ''
  drawerOpen.value = true
}

const closeDrawer = () => {
  drawerOpen.value = false
}

// 校验并保存
const doSubmit = async () => {
  if (!form.capability) {
    message.warning('请选择能力类型')
    return
  }
  if (!form.provider) {
    message.warning('请选择提供方')
    return
  }
  if (!form.configName) {
    message.warning('请填写配置名称')
    return
  }
  if (!isEdit.value && !form.apiKey) {
    message.warning('请填写 API 密钥')
    return
  }
  if (form.params) {
    try {
      JSON.parse(form.params)
    } catch (e) {
      message.warning('扩展参数不是合法 JSON')
      return
    }
  }
  submitting.value = true
  try {
    const res = isEdit.value
      ? await updateModelConfig({ ...form })
      : await addModelConfig({ ...form })
    if (res.data.code === 0) {
      message.success(isEdit.value ? '修改成功，已热生效' : '新增成功，已热生效')
      drawerOpen.value = false
      await fetchData()
      // 该能力此前没有默认配置时，后端会自动把新增项提升为默认，这里明确告知
      if (!isEdit.value) {
        const newId = String(res.data.data)
        const created = data.value.find((item) => String(item.id) === newId)
        if (created?.isDefault === 1 && form.isDefault !== 1) {
          message.info('该能力此前没有默认配置，已自动设为默认并立即生效')
        }
      }
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

const doToggleStatus = async (record: API.ModelConfigVO, checked: boolean) => {
  const res = await toggleModelConfigStatus({
    id: record.id as string,
    status: checked ? 1 : 0,
  })
  if (res.data.code === 0) {
    message.success(checked ? '已启用' : '已停用')
    await fetchData()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

const doSetDefault = async (record: API.ModelConfigVO) => {
  const res = await setDefaultModelConfig({ id: record.id as string })
  if (res.data.code === 0) {
    message.success('已设为该能力的默认配置，立即生效')
    await fetchData()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

const doTest = async (record: API.ModelConfigVO) => {
  testingId.value = record.id
  try {
    const res = await testModelConfig({ id: record.id })
    if (res.data.code === 0) {
      const result = res.data.data
      Modal.info({
        title: result?.success ? '连通性测试通过' : '连通性测试失败',
        content: `耗时 ${result?.elapsedMs ?? '-'} ms\n${result?.message ?? ''}`,
      })
    } else {
      message.error('测试失败，' + res.data.message)
    }
  } finally {
    testingId.value = undefined
  }
}

const doDelete = (record: API.ModelConfigVO) => {
  Modal.confirm({
    title: '确认删除该模型配置？',
    content: `配置名称：${record.configName}`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      const res = await deleteModelConfig({ id: record.id as string })
      if (res.data.code === 0) {
        message.success('删除成功')
        await fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

const doRefreshCache = async () => {
  refreshing.value = true
  try {
    const res = await refreshModelConfigCache()
    if (res.data.code === 0) {
      message.success('模型缓存已刷新')
    } else {
      message.error('刷新失败，' + res.data.message)
    }
  } finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  await fetchCapabilities()
  await fetchData()
})
</script>

<style scoped>
#modelConfigPage {
  padding: 16px;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}
</style>
