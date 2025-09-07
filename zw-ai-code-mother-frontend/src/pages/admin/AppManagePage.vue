<template>
  <div>
    <a-form layout="inline" :model="query" @finish="onSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="query.appName" placeholder="请输入应用名称" style="width: 200px" />
      </a-form-item>
      <a-form-item label="创建者">
        <a-input v-model:value="query.userId" placeholder="请输入用户ID" style="width: 200px" />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="query.codeGenType"
          placeholder="请选择生成类型"
          allow-clear
          style="width: 200px"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option
            v-for="option in CODE_GEN_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
        <a-button style="margin-left: 8px" @click="onReset">重置</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <a-table :data-source="list" :pagination="pagination" row-key="id" @change="onTableChange">
      <a-table-column title="ID" dataIndex="id" width="120" />
      <a-table-column title="应用名称" dataIndex="appName" width="150" />
      <a-table-column
        title="封面"
        key="cover"
        width="100"
        :customRender="
          ({ record }: { record: any }) =>
            h('img', {
              src: getAppCoverUrl(record.cover || ''),
              style: 'width:80px;height:48px;object-fit:cover;border-radius:6px',
              onerror: (event: Event) => {
                const imgElement = event.target as HTMLImageElement
                imgElement.src = getAppCoverUrl('')
                console.warn(`应用ID ${record.id} 的封面图片加载失败，已切换到默认封面`)
              },
            })
        "
      />
      <a-table-column
        title="初始提示词"
        key="initialPrompt"
        width="300"
        :customRender="initialPromptRender"
      />
      <a-table-column
        title="生成类型"
        key="codeGenType"
        width="120"
        :customRender="codeGenTypeRender"
      />
      <a-table-column title="优先级" key="priority" width="100" :customRender="priorityRender" />
      <a-table-column
        title="部署时间"
        key="deployTime"
        width="150"
        :customRender="deployTimeRender"
      />
      <a-table-column title="创建者" key="creator" width="200" :customRender="creatorRender" />
      <a-table-column title="创建时间" dataIndex="createTime" width="150" />
      <a-table-column title="操作" key="action" width="200" :customRender="actionRender" />
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { listAppByPage, deleteApp, updateApp } from '@/api/appController.ts'
import { message, Tag, Avatar, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getAppCoverUrl } from '@/pages/app/appCoverUtils'
import { CODE_GEN_TYPE_OPTIONS, CODE_GEN_TYPE_MAP, CodeGenTypeEnum } from '@/constants/codeGenType'
import { PRIORITY_CONFIG, PriorityEnum } from '@/constants/priority'
import { UserOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  userId: '',
  codeGenType: '',
})
const total = ref(0)
const list = ref<any[]>([])

const fetchData = async () => {
  // 构建查询参数，过滤空值
  const queryParams = {
    ...query,
    appName: query.appName?.trim() || undefined,
    userId: query.userId?.trim() || undefined,
    codeGenType: query.codeGenType || undefined,
  }

  // 移除undefined值
  Object.keys(queryParams).forEach((key) => {
    if (queryParams[key as keyof typeof queryParams] === undefined) {
      delete queryParams[key as keyof typeof queryParams]
    }
  })

  const res = await listAppByPage(queryParams as any)
  if (res.data.code === 0 && res.data.data) {
    list.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message)
  }
}

const pagination = computed(() => ({
  current: query.pageNum ?? 1,
  pageSize: query.pageSize ?? 10,
  total: total.value,
}))

const onTableChange = (p: { current: number; pageSize: number }) => {
  query.pageNum = p.current
  query.pageSize = p.pageSize
  fetchData()
}

const onSearch = () => {
  query.pageNum = 1
  fetchData()
}

const onReset = () => {
  query.appName = ''
  query.userId = ''
  query.codeGenType = ''
  query.pageNum = 1
  fetchData()
}

const doDelete = async (id: any) => {
  const res = await deleteApp({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else message.error(res.data.message)
}

const doFeature = async (record: any) => {
  if (!record.id) {
    message.error('应用ID不存在')
    return
  }

  // 判断当前是否为精选状态，进行状态切换
  const isFeatured = record.priority === PriorityEnum.FEATURED
  const newPriority = isFeatured ? PriorityEnum.NORMAL : PriorityEnum.FEATURED
  const actionText = isFeatured ? '取消精选' : '设为精选'

  const res = await updateApp({ id: record.id, priority: newPriority })
  if (res.data.code === 0) {
    message.success(`${actionText}成功`)
    fetchData()
  } else message.error(res.data.message)
}

// 头像点击放大功能
const showAvatarModal = (avatarUrl: string, userName: string) => {
  Modal.info({
    title: `${userName}的头像`,
    content: h('div', { style: 'text-align: center; padding: 20px' }, [
      h('img', {
        src: avatarUrl,
        style:
          'max-width: 100%; max-height: 400px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15)',
        onerror: (event: Event) => {
          const imgElement = event.target as HTMLImageElement
          imgElement.src =
            'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9IiNmNWY1ZjUiLz4KPHN2ZyB4PSIxNiIgeT0iMTYiIHdpZHRoPSIzMiIgaGVpZ2h0PSIzMiIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgo8cGF0aCBkPSJNMTIgMTJDMTQuMjA5MSAxMiAxNiAxMC4yMDkxIDE2IDhDMTYgNS43OTA5IDE0LjIwOTEgNCAxMiA0QzkuNzkwODYgNCA4IDUuNzkwOSA4IDhDOCAxMC4yMDkxIDkuNzkwODYgMTIgMTIgMTJaIiBmaWxsPSIjYmZiZmJmIi8+CjxwYXRoIGQ9Ik0xMiAxNEM5LjMzIDEzLjk5IDcgMTYuMzYgNyAxOVYyMEgxN1YxOUMxNyAxNi4zNiAxNC42NyAxMy45OSAxMiAxNFoiIGZpbGw9IiNiZmJmYmYiLz4KPC9zdmc+Cjwvc3ZnPgo='
        },
      }),
    ]),
    okText: '关闭',
    width: 500,
  })
}

// 初始提示词渲染
const initialPromptRender = ({ record }: { record: any }) => {
  const prompt = record.initPrompt || '暂无提示词'
  const displayText = prompt.length > 50 ? prompt.substring(0, 50) + '...' : prompt
  return h('span', { title: prompt }, displayText)
}

// 生成类型渲染
const codeGenTypeRender = ({ record }: { record: any }) => {
  const typeLabel =
    CODE_GEN_TYPE_MAP[record.codeGenType as CodeGenTypeEnum] || record.codeGenType || '未知类型'
  return h(Tag, { color: 'blue' }, () => typeLabel)
}

// 优先级渲染
const priorityRender = ({ record }: { record: any }) => {
  const priority = record.priority as PriorityEnum
  const config = PRIORITY_CONFIG[priority] || PRIORITY_CONFIG[PriorityEnum.NORMAL]
  return h(Tag, { color: config.color }, () => config.label)
}

// 部署时间渲染
const deployTimeRender = ({ record }: { record: any }) => {
  return h('span', {}, record.deployedTime || '应用未部署')
}

// 创建者渲染
const creatorRender = ({ record }: { record: any }) => {
  // 优化用户名显示逻辑
  const displayName = record.userName && record.userName.trim() ? record.userName : '匿名用户'

  return h(
    'div',
    {
      style: 'display: flex; align-items: center; gap: 12px; min-width: 0;',
    },
    [
      h(Avatar, {
        size: 48,
        src: record.userAvatar,
        icon: h(UserOutlined),
        style: {
          cursor: 'pointer',
          transition: 'all 0.3s ease',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          flexShrink: 0,
        },
        onClick: () => {
          if (record.userAvatar) {
            showAvatarModal(record.userAvatar, displayName)
          }
        },
        onMouseenter: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'scale(1.05)'
          target.style.boxShadow = '0 4px 16px rgba(0,0,0,0.15)'
        },
        onMouseleave: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'scale(1)'
          target.style.boxShadow = '0 2px 8px rgba(0,0,0,0.1)'
        },
      }),
      h(
        'div',
        {
          style: 'min-width: 0; flex: 1;',
        },
        [
          h(
            'div',
            {
              style:
                'font-weight: 500; color: #262626; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;',
            },
            displayName,
          ),
          h(
            'div',
            {
              style: 'font-size: 12px; color: #8c8c8c; margin-top: 2px;',
            },
            `ID: ${record.userId}`,
          ),
        ],
      ),
    ],
  )
}

// 操作按钮渲染
const actionRender = ({ record }: { record: any }) => {
  const isFeatured = record.priority === PriorityEnum.FEATURED
  const featureButtonText = isFeatured ? '取消精选' : '精选'
  const featureButtonColor = isFeatured ? 'warning' : 'success'

  return h('div', { style: 'display: flex; gap: 8px; flex-wrap: wrap' }, [
    h(
      Tag,
      {
        color: 'processing',
        style: 'cursor: pointer; margin: 0; transition: all 0.2s ease;',
        onClick: () => router.push(`/app/edit/${record.id}`),
        onMouseenter: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'translateY(-1px)'
          target.style.boxShadow = '0 2px 8px rgba(24, 144, 255, 0.3)'
        },
        onMouseleave: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'translateY(0)'
          target.style.boxShadow = 'none'
        },
      },
      () => '编辑',
    ),
    h(
      Tag,
      {
        color: 'error',
        style: 'cursor: pointer; margin: 0; transition: all 0.2s ease;',
        onClick: () => record.id && doDelete(record.id),
        onMouseenter: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'translateY(-1px)'
          target.style.boxShadow = '0 2px 8px rgba(255, 77, 79, 0.3)'
        },
        onMouseleave: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'translateY(0)'
          target.style.boxShadow = 'none'
        },
      },
      () => '删除',
    ),
    h(
      Tag,
      {
        color: featureButtonColor,
        style: 'cursor: pointer; margin: 0; transition: all 0.2s ease;',
        onClick: () => doFeature(record),
        onMouseenter: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'translateY(-1px)'
          const shadowColor = isFeatured ? 'rgba(250, 173, 20, 0.3)' : 'rgba(82, 196, 26, 0.3)'
          target.style.boxShadow = `0 2px 8px ${shadowColor}`
        },
        onMouseleave: (e: MouseEvent) => {
          const target = e.currentTarget as HTMLElement
          target.style.transform = 'translateY(0)'
          target.style.boxShadow = 'none'
        },
      },
      () => featureButtonText,
    ),
  ])
}

onMounted(fetchData)
</script>
