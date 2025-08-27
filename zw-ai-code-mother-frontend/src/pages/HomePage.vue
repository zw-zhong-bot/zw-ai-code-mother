<script setup lang="ts">
import { onMounted, reactive, ref, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listMyAppByPage, listFeaturedAppByPage } from '@/api/appController.ts'

const router = useRouter()

// 创建应用输入
const creating = ref(false)
const promptText = ref('')

const doCreate = async () => {
  if (!promptText.value?.trim()) {
    message.warning('请输入提示词')
    return
  }
  try {
    creating.value = true
    const res = await addApp({
      appName: promptText.value,
      cover: '',
      initPrompt: promptText.value,
      codeGenType: 'web',
    })
    if (res.data.code === 0 && res.data.data) {
      const appId = String(res.data.data as unknown as string)
      router.push({ path: `/app/chat/${appId}`, query: { init: promptText.value } })
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } finally {
    creating.value = false
  }
}

// 公共分页 state
type PageQuery = API.AppQueryRequest
const myQuery = reactive<PageQuery>({ pageNum: 1, pageSize: 20, appName: '' })
const goodQuery = reactive<PageQuery>({ pageNum: 1, pageSize: 20, appName: '' })

const myTotal = ref(0)
const goodTotal = ref(0)
const myList = ref<API.AppVO[]>([])
const goodList = ref<API.AppVO[]>([])

const fetchMyApps = async () => {
  const res = await listMyAppByPage(myQuery)
  if (res.data.code === 0 && res.data.data) {
    myList.value = res.data.data.records ?? []
    myTotal.value = res.data.data.totalRow ?? 0
  }
}
const fetchGoodApps = async () => {
  const res = await listFeaturedAppByPage(goodQuery)
  if (res.data.code === 0 && res.data.data) {
    goodList.value = res.data.data.records ?? []
    goodTotal.value = res.data.data.totalRow ?? 0
  }
}

const myPagination = computed(() => ({
  current: myQuery.pageNum ?? 1,
  pageSize: myQuery.pageSize ?? 20,
  total: myTotal.value,
  showTotal: (t: number) => `共${t}条`,
}))
const goodPagination = computed(() => ({
  current: goodQuery.pageNum ?? 1,
  pageSize: goodQuery.pageSize ?? 20,
  total: goodTotal.value,
  showTotal: (t: number) => `共${t}条`,
}))

const onMyTableChange = (page: { current: number; pageSize: number }) => {
  myQuery.pageNum = page.current
  myQuery.pageSize = page.pageSize
  fetchMyApps()
}
const onGoodTableChange = (page: { current: number; pageSize: number }) => {
  goodQuery.pageNum = page.current
  goodQuery.pageSize = page.pageSize
  fetchGoodApps()
}

const toDetail = (id: string | number) => {
  router.push(`/app/detail/${String(id)}`)
}

onMounted(() => {
  fetchMyApps()
  fetchGoodApps()
})
</script>

<template>
  <div class="home-page">
    <div class="hero">
      <h1 class="title">一句话 呈所想</h1>
      <p class="sub">与 AI 对话轻松创建应用和网站</p>
      <div class="prompt-box">
        <a-input-search
          v-model:value="promptText"
          :loading="creating"
          enter-button="创建应用"
          size="large"
          placeholder="使用 NoCode 创建一个高效的小工具，帮我计算……"
          @search="doCreate"
        />
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2>我的作品</h2>
        <a-input
          v-model:value="myQuery.appName"
          placeholder="按名称搜索"
          style="max-width: 260px"
          @pressEnter="
            () => {
              myQuery.pageNum = 1
              fetchMyApps()
            }
          "
        />
      </div>
      <a-table
        :data-source="myList"
        :pagination="myPagination"
        row-key="id"
        @change="onMyTableChange"
      >
        <a-table-column
          title="封面"
          key="cover"
          :customRender="
            ({ record }: { record: API.AppVO }) =>
              h('img', {
                src: record.cover || `https://picsum.photos/80/48?random=${record.id}`,
                style: 'width:80px;height:48px;object-fit:cover;border-radius:6px',
              })
          "
        />
        <a-table-column title="名称" dataIndex="appName" />
        <a-table-column title="优先级" dataIndex="priority" />
        <a-table-column title="创建时间" dataIndex="createTime" />
        <a-table-column
          title="操作"
          key="action"
          :customRender="
            ({ record }: { record: API.AppVO }) =>
              h('a', { onClick: () => toDetail(record.id as unknown as string) }, '查看')
          "
        />
      </a-table>
    </div>

    <div class="section">
      <div class="section-header">
        <h2>精选案例</h2>
        <a-input
          v-model:value="goodQuery.appName"
          placeholder="按名称搜索"
          style="max-width: 260px"
          @pressEnter="
            () => {
              goodQuery.pageNum = 1
              fetchGoodApps()
            }
          "
        />
      </div>
      <a-table
        :data-source="goodList"
        :pagination="goodPagination"
        row-key="id"
        @change="onGoodTableChange"
      >
        <a-table-column
          title="封面"
          key="cover"
          :customRender="
            ({ record }: { record: API.AppVO }) =>
              h('img', {
                src: record.cover || `https://picsum.photos/80/48?random=${record.id}`,
                style: 'width:80px;height:48px;object-fit:cover;border-radius:6px',
              })
          "
        />
        <a-table-column title="名称" dataIndex="appName" />
        <a-table-column title="优先级" dataIndex="priority" />
        <a-table-column title="创建时间" dataIndex="createTime" />
        <a-table-column
          title="操作"
          key="action"
          :customRender="
            ({ record }: { record: API.AppVO }) =>
              h('a', { onClick: () => toDetail(record.id as unknown as string) }, '查看')
          "
        />
      </a-table>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding: 24px;
}
.hero {
  text-align: center;
  padding: 48px 0 24px;
  background: linear-gradient(180deg, #f8fffd 0%, #e7faf5 100%);
  border-radius: 12px;
}
.title {
  font-size: 40px;
  margin: 0;
}
.sub {
  color: #6b7280;
  margin: 8px 0 20px;
}
.prompt-box {
  max-width: 720px;
  margin: 0 auto;
}
.section {
  margin-top: 32px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
