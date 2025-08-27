<template>
  <div>
    <a-form layout="inline" :model="query" @finish="onSearch">
      <a-form-item label="名称">
        <a-input v-model:value="query.appName" placeholder="按名称查询" />
      </a-form-item>
      <a-form-item label="类型">
        <a-input v-model:value="query.codeGenType" placeholder="类型" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <a-table :data-source="list" :pagination="pagination" row-key="id" @change="onTableChange">
      <a-table-column title="ID" dataIndex="id" />
      <a-table-column title="名称" dataIndex="appName" />
      <a-table-column
        title="封面"
        key="cover"
        :customRender="
          ({ record }) =>
            h('img', {
              src: record.cover || `https://picsum.photos/80/48?random=${record.id}`,
              style: 'width:80px;height:48px;object-fit:cover;border-radius:6px',
            })
        "
      />
      <a-table-column title="优先级" dataIndex="priority" />
      <a-table-column title="用户ID" dataIndex="userId" />
      <a-table-column title="创建时间" dataIndex="createTime" />
      <a-table-column title="操作" key="action" :customRender="actionRender" />
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { listAppByPage, deleteApp, updateApp } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const query = reactive<API.AppQueryRequest>({ pageNum: 1, pageSize: 10 })
const total = ref(0)
const list = ref<API.AppVO[]>([])

const fetchData = async () => {
  const res = await listAppByPage(query)
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

const doDelete = async (id: number) => {
  const res = await deleteApp({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else message.error(res.data.message)
}

const doFeature = async (record: API.AppVO) => {
  const res = await updateApp({ id: record.id, priority: 99 })
  if (res.data.code === 0) {
    message.success('已设为精选')
    fetchData()
  } else message.error(res.data.message)
}

const actionRender = ({ record }: { record: API.AppVO }) =>
  h('div', {}, [
    h(
      'a',
      {
        style: 'margin-right:12px',
        onClick: () => router.push(`/app/edit/${record.id}`),
      },
      '编辑',
    ),
    h(
      'a',
      { style: 'margin-right:12px;color:#ff4d4f', onClick: () => doDelete(record.id) },
      '删除',
    ),
    h('a', { onClick: () => doFeature(record) }, '精选'),
  ])

onMounted(fetchData)
</script>
