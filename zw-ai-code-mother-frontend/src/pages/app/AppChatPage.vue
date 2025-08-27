<template>
  <div class="chat-page">
    <!-- 顶部栏 -->
    <div class="topbar">
      <div class="app-name">{{ appName || `应用 #${appId}` }}</div>
      <a-button type="primary" :loading="deploying" @click="doDeploy">
        <template #icon>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
          </svg>
        </template>
        部署
      </a-button>
    </div>

    <!-- 核心内容区域 -->
    <div class="content">
      <!-- 左侧对话区域 -->
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">用户消息</span>
        </div>

        <!-- 消息区域 -->
        <div class="messages" ref="msgBoxRef">
          <div v-for="m in messages" :key="m.id" class="msg" :class="m.role">
            <div class="avatar">
              <img :src="m.role === 'user' ? userAvatar : aiAvatar" :alt="m.role" />
            </div>
            <div class="bubble" v-html="m.html"></div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-area">
          <div class="input-container">
            <a-textarea
              v-model:value="inputText"
              :rows="3"
              placeholder="描述越详细，页面越具体，可以一步一步完善生成效果"
              @keydown.enter.prevent="sendMsg"
              class="message-input"
            />
            <div class="input-actions">
              <div class="action-buttons">
                <a-button size="small" class="action-btn">
                  <template #icon>📎</template>
                  上传
                </a-button>
                <a-button size="small" class="action-btn">
                  <template #icon>✏️</template>
                  编辑
                </a-button>
                <a-button size="small" class="action-btn">
                  <template #icon>✨</template>
                  优化
                </a-button>
              </div>
              <a-button
                type="primary"
                :loading="sending"
                @click="sendMsg"
                class="send-btn"
                shape="circle"
              >
                <template #icon>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
                  </svg>
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="right-panel">
        <div class="panel-header">
          <span class="panel-title">生成后的网页展示</span>
          <a-button type="primary" :loading="deploying" @click="doDeploy" size="small">
            <template #icon>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </template>
            部署
          </a-button>
        </div>

        <div class="preview-content">
          <div v-if="previewUrl" class="preview-container">
            <iframe :src="previewUrl" class="preview"></iframe>
          </div>
          <div v-else class="empty-preview">
            <div class="empty-content">
              <div class="empty-icon">🚀</div>
              <div class="empty-text">等待代码生成完成后展示</div>
              <div class="empty-desc">AI正在为您生成网站，请稍候...</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getAppById, deployApp } from '@/api/appController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const route = useRoute()
const appId = String(route.params.id as string)
const initText = (route.query.init as string) || ''

const loginUserStore = useLoginUserStore()

const appName = ref('')
const sending = ref(false)
const deploying = ref(false)
const inputText = ref('')
const previewUrl = ref('')
const msgBoxRef = ref<HTMLDivElement>()

// 监听previewUrl的变化
import { watch } from 'vue'
watch(previewUrl, (newUrl, oldUrl) => {
  console.log('previewUrl changed:', { oldUrl, newUrl })
})

// 拼接完整的头像URL
const getFullAvatarUrl = (avatarPath: string): string => {
  if (!avatarPath) return ''
  if (avatarPath.startsWith('http')) return avatarPath
  return `http://localhost:8123${avatarPath}`
}

// 头像配置
const userAvatar = computed(() => {
  const avatar = loginUserStore.loginUser.userAvatar
  if (!avatar) {
    return `https://picsum.photos/40/40?random=user`
  }
  return getFullAvatarUrl(avatar)
})
const aiAvatar = '/src/assets/ping/touxiang.jpg'

type Msg = { id: number; role: 'user' | 'ai'; html: string }
const messages = reactive<Msg[]>([])
let eventSource: EventSource | null = null

const scrollToBottom = async () => {
  await nextTick()
  msgBoxRef.value?.scrollTo({ top: msgBoxRef.value.scrollHeight, behavior: 'smooth' })
}

const appendAiMsg = () => {
  const id = Date.now()
  messages.push({ id, role: 'ai', html: '' })
  return id
}

const updateMsgHtml = (id: number, chunk: string) => {
  const m = messages.find((x) => x.id === id)
  if (m) {
    m.html += chunk
  }
}

const handleSSE = (text: string) => {
  // 简单把代码块转为预格式文本
  return text
    .replace(/```(\w+)?/g, '<pre>')
    .replace(/```/g, '</pre>')
    .replace(/\n/g, '<br/>')
}

const sendMsg = async () => {
  if (!inputText.value.trim()) return
  const userId = Date.now()
  messages.push({ id: userId, role: 'user', html: inputText.value })
  await scrollToBottom()
  const aiId = appendAiMsg()
  await scrollToBottom()

  sending.value = true
  const url = `http://localhost:8123/api/app/chat/gen/code?appId=${encodeURIComponent(appId)}&message=${encodeURIComponent(
    inputText.value,
  )}`
  inputText.value = ''
  eventSource?.close()
  eventSource = new EventSource(url, { withCredentials: true })

  eventSource.onmessage = async (e) => {
    if (!e?.data) return
    console.log('SSE message received:', e.data)

    // 检查是否是 done 事件（空数据表示流结束）
    if (e.data === '') {
      console.log('SSE done event received - stream completed')
      eventSource?.close()
      // 设置预览URL
      previewUrl.value = `http://localhost:8123/api/static/web_${appId}/`
      console.log('Setting preview URL:', previewUrl.value)
      // 测试预览URL
      setTimeout(() => {
        testPreviewUrl()
      }, 1000) // 延迟1秒测试，给后端一些时间生成文件
      await scrollToBottom()
      sending.value = false
      return
    }

    try {
      const obj = JSON.parse(e.data)
      if (obj && typeof obj.d === 'string') {
        updateMsgHtml(aiId, handleSSE(obj.d))
        await scrollToBottom()
      }
    } catch {
      // 兼容纯文本片段
      updateMsgHtml(aiId, handleSSE(e.data))
      await scrollToBottom()
    }
  }

  // 监听 done 事件（如果后端发送了特定的事件类型）
  eventSource.addEventListener('done', async () => {
    console.log('SSE done event listener triggered')
    eventSource?.close()
    previewUrl.value = `http://localhost:8123/api/static/web_${appId}/`
    console.log('Setting preview URL from done event:', previewUrl.value)
    // 测试预览URL
    setTimeout(() => {
      testPreviewUrl()
    }, 1000) // 延迟1秒测试，给后端一些时间生成文件
    await scrollToBottom()
    sending.value = false
  })

  // 监听 open 事件
  eventSource.onopen = () => {
    console.log('SSE connection opened')
  }

  // 监听 error 事件
  eventSource.onerror = (e) => {
    console.error('SSE error:', e)
    message.error('对话连接异常')
    sending.value = false
    eventSource?.close()
  }

  // 监听连接关闭
  eventSource.addEventListener('close', () => {
    console.log('SSE connection closed')
    // 如果连接关闭但没有设置预览URL，可能是流结束了
    if (!previewUrl.value && !sending.value) {
      console.log('Connection closed without preview URL, setting it now')
      previewUrl.value = `http://localhost:8123/api/static/web_${appId}/`
      testPreviewUrl()
    }
  })
}

const doDeploy = async () => {
  try {
    deploying.value = true
    const res = await deployApp({ appId: appId as unknown as number })
    if (res.data.code === 0) {
      const url = res.data.data
      message.success('部署成功')
      if (url) {
        previewUrl.value = url
      }
    } else {
      message.error(res.data.message)
    }
  } finally {
    deploying.value = false
  }
}

const fetchApp = async () => {
  const res = await getAppById({ id: appId as unknown as number })
  if (res.data.code === 0 && res.data.data) {
    appName.value = res.data.data.appName || ''
  }
}

// 测试预览URL的函数
const testPreviewUrl = () => {
  console.log('Current previewUrl:', previewUrl.value)
  if (previewUrl.value) {
    console.log('Testing preview URL...')
    // 创建一个测试iframe来检查URL是否可访问
    const testIframe = document.createElement('iframe')
    testIframe.src = previewUrl.value
    testIframe.style.display = 'none'
    testIframe.onload = () => {
      console.log('Preview URL loaded successfully')
      document.body.removeChild(testIframe)
    }
    testIframe.onerror = () => {
      console.error('Preview URL failed to load')
      document.body.removeChild(testIframe)
    }
    document.body.appendChild(testIframe)
  }
}

onMounted(async () => {
  await fetchApp()
  if (initText) {
    inputText.value = initText
    await sendMsg()
  }
})

onUnmounted(() => eventSource?.close())
</script>

<style scoped>
.chat-page {
  padding: 16px;
  background: #f5f5f5;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部栏样式 */
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.app-name {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

/* 核心内容区域 */
.content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  height: calc(100vh - 140px);
  min-height: 600px;
}

/* 左侧面板 */
.left-panel {
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

/* 消息区域 */
.messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  background: #f9fafb;
  min-height: 0;
}

.msg {
  margin: 16px 0;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.msg.user {
  flex-direction: row-reverse;
}

.msg.ai {
  flex-direction: row;
}

.avatar {
  flex-shrink: 0;
}

.avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.bubble {
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 20px;
  word-wrap: break-word;
  word-break: break-word;
  line-height: 1.5;
  overflow-wrap: break-word;
  font-size: 14px;
}

.msg.user .bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.msg.ai .bubble {
  background: #fff;
  color: #374151;
  border-bottom-left-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #e5e7eb;
}

/* 输入区域 */
.input-area {
  padding: 20px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
  flex-shrink: 0;
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-input {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  resize: none;
}

.message-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
  font-size: 12px;
  height: 32px;
  padding: 0 12px;
}

.action-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:hover {
  transform: scale(1.05);
  transition: transform 0.2s ease;
}

/* 右侧面板 */
.right-panel {
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.preview-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.preview {
  flex: 1;
  border: 0;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
  min-height: 0;
  background: #fff;
}

/* 空状态 */
.empty-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.empty-content {
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #6b7280;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .topbar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 18px;
  }
}
</style>
