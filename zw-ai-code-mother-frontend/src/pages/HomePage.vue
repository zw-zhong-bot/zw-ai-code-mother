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
      <div class="app-grid">
        <div
          v-for="app in myList"
          :key="app.id"
          class="app-card"
          @click="toDetail(app.id as unknown as string)"
        >
          <div class="app-cover">
            <img
              :src="getAppCoverUrl(app.cover, app.id)"
              :alt="app.appName || '应用封面'"
              class="cover-image"
              @error="handleImageError($event, app.id)"
            />
          </div>
          <div class="app-info">
            <div class="app-name">{{ app.appName || '未命名应用' }}</div>
            <div class="app-time">{{ formatDate(app.createTime) }}</div>
          </div>
          <div class="app-actions">
            <a-button type="primary" size="small" @click.stop="goChat(app.id as unknown as string)">
              查看对话
            </a-button>
            <a-button size="small" @click.stop="viewApp(app.deployKey)" v-if="app.deployKey">
              查看作品
            </a-button>
          </div>
        </div>
      </div>
      <div class="pagination-container">
        <a-pagination
          v-model:current="myQuery.pageNum"
          v-model:pageSize="myQuery.pageSize"
          :total="myTotal"
          :show-total="(total) => `共 ${total} 条`"
          @change="onMyTableChange"
        />
      </div>
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
      <div class="app-grid">
        <div
          v-for="app in goodList"
          :key="app.id"
          class="app-card"
          @click="toDetail(app.id as unknown as string)"
        >
          <div class="app-cover">
            <img
              :src="getAppCoverUrl(app.cover, app.id)"
              :alt="app.appName || '应用封面'"
              class="cover-image"
              @error="handleImageError($event, app.id)"
            />
          </div>
          <div class="app-info">
            <div class="app-name">{{ app.appName || '未命名应用' }}</div>
            <div class="app-time">{{ formatDate(app.createTime) }}</div>
          </div>
          <div class="app-actions">
            <a-button type="primary" size="small" @click.stop="goChat(app.id as unknown as string)">
              查看对话
            </a-button>
            <a-button size="small" @click.stop="viewApp(app.deployKey)" v-if="app.deployKey">
              查看作品
            </a-button>
          </div>
        </div>
      </div>
      <div class="pagination-container">
        <a-pagination
          v-model:current="goodQuery.pageNum"
          v-model:pageSize="goodQuery.pageSize"
          :total="goodTotal"
          :show-total="(total) => `共 ${total} 条`"
          @change="onGoodTableChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listMyAppByPage, listFeaturedAppByPage } from '@/api/appController.ts'
import { getAppCoverUrl } from './app/appCoverUtils'
import dayjs from 'dayjs'

const router = useRouter()

// 创建应用输入
const creating = ref(false)
const promptText = ref('')

/**
 * 创建新应用
 */
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

/**
 * 获取我的应用列表
 */
const fetchMyApps = async () => {
  const res = await listMyAppByPage(myQuery)
  if (res.data.code === 0 && res.data.data) {
    myList.value = res.data.data.records ?? []
    myTotal.value = res.data.data.totalRow ?? 0
  }
}

/**
 * 获取精选应用列表
 */
const fetchGoodApps = async () => {
  const res = await listFeaturedAppByPage(goodQuery)
  if (res.data.code === 0 && res.data.data) {
    goodList.value = res.data.data.records ?? []
    goodTotal.value = res.data.data.totalRow ?? 0
  }
}

/**
 * 处理我的应用列表分页变化
 * @param page 分页信息
 */
const onMyTableChange = (page: { current: number; pageSize: number }) => {
  myQuery.pageNum = page.current
  myQuery.pageSize = page.pageSize
  fetchMyApps()
}

/**
 * 处理精选应用列表分页变化
 * @param page 分页信息
 */
const onGoodTableChange = (page: { current: number; pageSize: number }) => {
  goodQuery.pageNum = page.current
  goodQuery.pageSize = page.pageSize
  fetchGoodApps()
}

/**
 * 跳转到应用详情页
 * @param id 应用ID
 */
const toDetail = (id: string) => {
  router.push(`/app/detail/${id}`)
}

/**
 * 跳转到对话页面
 * @param id 应用ID
 */
const goChat = (id: string) => {
  router.push({ path: `/app/chat/${id}`, query: { view: '1' } })
}

/**
 * 在新窗口打开部署的应用
 * @param deployKey 部署密钥
 */
const viewApp = (deployKey?: string) => {
  if (deployKey) {
    window.open(`http://localhost/${deployKey}`, '_blank')
  }
}

/**
 * 格式化日期
 * @param dateString 日期字符串
 * @returns 格式化后的日期
 */
const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  return dayjs(dateString).format('YYYY-MM-DD')
}

/**
 * 处理图片加载错误
 * @param event 错误事件
 * @param appId 应用ID
 */
const handleImageError = (event: Event, appId?: number | string) => {
  const imgElement = event.target as HTMLImageElement
  imgElement.src = getAppCoverUrl('', appId)
  imgElement.alt = '默认封面'
}

onMounted(() => {
  fetchMyApps()
  fetchGoodApps()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.hero {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.title {
  font-size: 48px;
  margin-bottom: 16px;
  font-weight: bold;
}

.sub {
  font-size: 20px;
  margin-bottom: 32px;
}

.prompt-box {
  max-width: 600px;
  margin: 0 auto;
}

.section {
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: bold;
}

/* 应用卡片网格布局 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

/* 应用卡片样式 */
.app-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 应用封面样式 */
.app-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  position: relative;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .cover-image {
  transform: scale(1.05);
}

/* 应用信息样式 */
.app-info {
  padding: 16px;
  flex: 1;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-time {
  font-size: 14px;
  color: #6b7280;
}

/* 应用操作按钮样式 */
.app-actions {
  padding: 0 16px 16px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.app-card:hover .app-actions {
  opacity: 1;
}

/* 分页容器样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .app-grid {
    grid-template-columns: 1fr;
  }

  .title {
    font-size: 36px;
  }

  .sub {
    font-size: 16px;
  }

  .hero {
    padding: 40px 20px;
  }

  .section {
    margin: 20px auto;
  }
}
</style>
