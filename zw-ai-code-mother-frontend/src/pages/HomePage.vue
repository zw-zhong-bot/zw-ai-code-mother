<template>
  <div class="home-page">
    <div class="hero">
      <h1 class="title">AI 应用生成平台</h1>
      <p class="sub" style="opacity: 0.95">一句话轻松创建网站应用</p>
      <div class="prompt-box">
        <a-input-search
          v-model:value="promptText"
          :loading="creating"
          enter-button="创建应用"
          size="large"
          placeholder="帮我创建个人博客网站"
          @search="doCreate"
        />
      </div>
    </div>

    <!-- 我的作品区域 -->
    <div class="section">
      <app-card-grid
        title="我的作品"
        :apps="myList"
        :total-items="myTotal"
        :current-page-prop="myQuery.pageNum"
        :page-size-prop="myQuery.pageSize"
        :loading="myLoading"
        @search="onMySearch"
        @page-change="onMyTableChange"
        @card-click="toDetail"
        @chat-click="goChat"
        @view-click="viewApp"
      />
    </div>

    <!-- 精选案例区域 -->
    <div class="section">
      <app-card-grid
        title="精选案例"
        :apps="goodList"
        :total-items="goodTotal"
        :current-page-prop="goodQuery.pageNum"
        :page-size-prop="goodQuery.pageSize"
        :loading="goodLoading"
        @search="onGoodSearch"
        @page-change="onGoodTableChange"
        @card-click="toDetail"
        @chat-click="goChat"
        @view-click="viewApp"
      />
    </div>

    <!-- 应用详情弹窗 -->
    <app-detail-modal
      v-if="showAppDetail"
      :app-id="currentAppId"
      :show-update-time="true"
      :show-deploy-time="true"
      @close="showAppDetail = false"
      @edit="onEditApp"
      @chat="goChat"
      @delete="onDeleteApp"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 首页组件
 * 展示应用创建入口、我的作品和精选案例
 * @author ZW
 * @since 2025-09-08
 * @version 1.0.0
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listMyAppByPage, listFeaturedAppByPage } from '@/api/appController'
import AppCardGrid from '@/components/app/AppCardGrid.vue'
import AppDetailModal from '@/components/app/AppDetailModal.vue'

const router = useRouter()

// 创建应用输入
const creating = ref(false)
const promptText = ref('')

// 应用详情弹窗状态
const showAppDetail = ref(false)
const currentAppId = ref('')

// 公共分页 state
type PageQuery = API.AppQueryRequest
const myQuery = reactive<PageQuery>({ pageNum: 1, pageSize: 20, appName: '' })
const goodQuery = reactive<PageQuery>({ pageNum: 1, pageSize: 20, appName: '' })

// 列表数据
const myTotal = ref(0)
const goodTotal = ref(0)
const myList = ref<API.AppVO[]>([])
const goodList = ref<API.AppVO[]>([])

// 加载状态
const myLoading = ref(false)
const goodLoading = ref(false)

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

/**
 * 获取我的应用列表
 */
const fetchMyApps = async () => {
  try {
    myLoading.value = true
    const res = await listMyAppByPage(myQuery)
    if (res.data.code === 0 && res.data.data) {
      myList.value = res.data.data.records ?? []
      myTotal.value = res.data.data.totalRow ?? 0
    }
  } catch (error) {
    console.error('获取我的应用列表失败:', error)
    message.error('获取我的应用列表失败')
  } finally {
    myLoading.value = false
  }
}

/**
 * 获取精选应用列表
 */
const fetchGoodApps = async () => {
  try {
    goodLoading.value = true
    const res = await listFeaturedAppByPage(goodQuery)
    if (res.data.code === 0 && res.data.data) {
      goodList.value = res.data.data.records ?? []
      goodTotal.value = res.data.data.totalRow ?? 0
    }
  } catch (error) {
    console.error('获取精选应用列表失败:', error)
    message.error('获取精选应用列表失败')
  } finally {
    goodLoading.value = false
  }
}

/**
 * 处理我的应用搜索
 * @param query 搜索关键词
 */
const onMySearch = (query: string) => {
  myQuery.appName = query
  myQuery.pageNum = 1
  fetchMyApps()
}

/**
 * 处理精选应用搜索
 * @param query 搜索关键词
 */
const onGoodSearch = (query: string) => {
  goodQuery.appName = query
  goodQuery.pageNum = 1
  fetchGoodApps()
}

/**
 * 处理我的应用列表分页变化
 * @param page 页码
 * @param pageSize 每页条数
 */
const onMyTableChange = (page: number, pageSize: number) => {
  myQuery.pageNum = page
  myQuery.pageSize = pageSize
  fetchMyApps()
}

/**
 * 处理精选应用列表分页变化
 * @param page 页码
 * @param pageSize 每页条数
 */
const onGoodTableChange = (page: number, pageSize: number) => {
  goodQuery.pageNum = page
  goodQuery.pageSize = pageSize
  fetchGoodApps()
}

/**
 * 跳转到应用详情页
 * @param id 应用ID
 */
const toDetail = (id: string) => {
  currentAppId.value = id
  showAppDetail.value = true
}

/**
 * 跳转到对话页面
 * @param id 应用ID
 */
const goChat = (id: string) => {
  router.push({ path: `/app/chat/${id}`, query: { view: '1' } })
}

/**
 * 跳转到编辑页面
 * @param id 应用ID
 */
const onEditApp = (id: string) => {
  router.push(`/app/edit/${id}`)
}

/**
 * 删除应用后的处理
 * @param id 应用ID
 */
const onDeleteApp = (id: string) => {
  // 刷新列表
  fetchMyApps()
  fetchGoodApps()
  message.success('应用已删除')
}

/**
 * 在新窗口打开部署的应用
 * @param deployKey 部署密钥
 */
const viewApp = (deployKey: string) => {
  // 由AppCardGrid组件内部处理
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

/* 响应式设计 */
@media (max-width: 768px) {
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
