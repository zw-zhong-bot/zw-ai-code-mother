<template>
  <div class="chat-page">
    <!-- 顶部栏 -->
    <div class="topbar">
      <div class="app-name">{{ appName || `应用 #${appId}` }}</div>
      <div class="topbar-actions">
        <a-button @click="showAppInfo = true" style="margin-right: 12px"> 应用详情 </a-button>
        <a-button :loading="downloading" @click="doDownloadCode" style="margin-right: 12px">
          <template #icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M14,2H6A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2M18,20H6V4H13V9H18V20Z"
              />
            </svg>
          </template>
          下载代码
        </a-button>
        <a-button type="primary" :loading="deploying" @click="doDeploy">
          <template #icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 应用详情悬浮窗 -->
    <a-modal v-model:open="showAppInfo" title="应用详情" footer="" width="480px">
      <div v-if="appDetail" class="app-info">
        <div class="info-item">
          <span class="label">创建者：</span>
          <div class="creator-info">
            <a-avatar
              :src="getFullAvatarUrl(appDetail.user?.userAvatar || '')"
              :size="32"
              class="creator-avatar"
            >
              <template #icon>
                <user-outlined />
              </template>
            </a-avatar>
            <span class="creator-name">{{ appDetail.user?.userName || '未知用户' }}</span>
          </div>
        </div>
        <div class="info-item">
          <span class="label">创建时间：</span>
          <span class="value">{{ formatTime(appDetail.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="label">应用ID：</span>
          <span class="value">{{ appDetail.id }}</span>
        </div>
        <div class="info-item">
          <span class="label">优先级：</span>
          <span class="value">{{ appDetail.priority }}</span>
        </div>
        <div class="info-item">
          <span class="label">生成类型：</span>
          <span class="value">
            <a-tag v-if="appDetail.codeGenType" color="blue">
              {{ getCodeGenTypeLabel(appDetail.codeGenType) }}
            </a-tag>
            <span v-else>未设置</span>
          </span>
        </div>
        <div v-if="isCurrentUserCreator || isAdmin" class="action-buttons">
          <a-button type="primary" @click="goEdit">
            <template #icon>
              <edit-outlined />
            </template>
            修改
          </a-button>
          <a-button danger @click="handleDelete">
            <template #icon>
              <delete-outlined />
            </template>
            删除
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 部署成功弹窗 -->
    <a-modal
      v-model:open="showDeploySuccess"
      title="部署成功"
      :footer="null"
      :closable="true"
      width="480px"
      centered
    >
      <div class="deploy-success-content">
        <div class="success-icon">
          <check-circle-filled />
        </div>
        <div class="success-message">网站部署成功!</div>
        <div class="success-desc">你的网站已经成功部署,可以通过以下链接访问:</div>
        <div class="url-container">
          <a-input v-model:value="deployUrl" readonly class="url-input" size="large" />
          <a-button type="primary" @click="copyDeployUrl" class="copy-btn">
            <template #icon>
              <copy-outlined />
            </template>
            复制
          </a-button>
        </div>
        <div class="action-buttons">
          <a-button type="primary" @click="visitWebsite" size="large"> 访问网站 </a-button>
          <a-button @click="closeDeploySuccess" size="large"> 关闭 </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 核心内容区域 -->
    <div class="content">
      <!-- 左侧对话区域 -->
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">用户消息</span>
        </div>

        <!-- 消息区域 -->
        <div class="messages" ref="msgBoxRef">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button
              type="text"
              :loading="loadingHistory"
              @click="loadChatHistory(true)"
              class="load-more-btn"
            >
              <template #icon v-if="!loadingHistory">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z" />
                </svg>
              </template>
              {{ loadingHistory ? '加载中...' : '加载更多历史消息' }}
            </a-button>
          </div>

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
            <a-tooltip
              :title="!isCurrentUserCreator && !isAdmin ? '无法在别人的作品下对话哦~' : ''"
              placement="top"
            >
              <a-textarea
                v-model:value="inputText"
                :rows="3"
                :disabled="!isCurrentUserCreator && !isAdmin"
                :placeholder="
                  isCurrentUserCreator || isAdmin
                    ? '请描述你想生成的网站，越详细效果越好哦'
                    : '无法在别人的作品下对话哦~'
                "
                @keydown.enter.prevent="sendMsg"
                class="message-input"
              />
            </a-tooltip>
            <div v-if="!isCurrentUserCreator && !isAdmin" class="input-disabled-tip">
              无法在别人的作品下对话哦~<br />
              <a-button type="link" @click="() => router.push('/')">创建自己的应用</a-button>
            </div>
            <div class="input-actions" v-if="isCurrentUserCreator || isAdmin">
              <div class="action-buttons">
                <a-button size="small" class="action-btn" @click="handleUpload">
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
          <div class="header-actions">
            <a-button
              v-if="!previewUrl && !sending"
              @click="manualRefreshPreview"
              size="small"
              title="手动刷新预览"
            >
              <template #icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path
                    d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"
                  />
                </svg>
              </template>
              刷新
            </a-button>
            <a-button type="primary" :disabled="!previewUrl" @click="openInNewWindow" size="small">
              <template #icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path
                    d="M14,3V5H17.59L7.76,14.83L9.17,16.24L19,6.41V10H21V3M19,19H5V5H12V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V12H19V19Z"
                  />
                </svg>
              </template>
              在新窗口打开
            </a-button>
          </div>
        </div>

        <div class="preview-content">
          <div v-if="previewUrl" class="preview-container">
            <iframe
              :src="previewUrl + '?t=' + Date.now()"
              class="preview"
              @load="onIframeLoad"
              @error="onIframeError"
              ref="previewIframe"
            ></iframe>
            <div class="preview-overlay" v-if="iframeLoading">
              <div class="loading-content">
                <div class="loading-icon">⏳</div>
                <div class="loading-text">正在加载网页...</div>
              </div>
            </div>
          </div>
          <div v-else class="empty-preview">
            <div class="empty-content">
              <div class="empty-icon">🚀</div>
              <div class="empty-text">等待代码生成完成后展示</div>
              <div class="empty-desc">AI正在为您生成网站，请稍候...</div>
              <!-- 调试信息 -->
              <div class="debug-info" v-if="!previewUrl">
                <div class="debug-item">
                  <span class="debug-label">生成状态:</span>
                  <span class="debug-value" :class="{ active: sending }">
                    {{ sending ? '正在生成...' : '生成完成' }}
                  </span>
                </div>
                <div class="debug-item">
                  <span class="debug-label">预览设置:</span>
                  <span class="debug-value" :class="{ active: previewUrlSetupPromise }">
                    {{ previewUrlSetupPromise ? '设置中...' : '等待触发' }}
                  </span>
                </div>
                <div class="debug-item">
                  <span class="debug-label">预期路径:</span>
                  <span class="debug-path">{{
                    (() => {
                      const codeGenType = appDetail?.codeGenType
                      let path = `${getStaticBasePath()}/${codeGenType}_${appId}`
                      if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
                        path += '/dist'
                      }
                      return path
                    })()
                  }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getAppById,
  deployApp,
  deleteMyApp,
  deleteApp,
  downloadAppCode,
} from '@/api/appController.ts'
import { listAppChatHistoryByPage } from '@/api/chatHistoryController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import {
  UserOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleFilled,
  CopyOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { getFullResourceUrl, getStaticBasePath, getApiUrl } from '@/config/env'
import { CodeGenTypeEnum, CODE_GEN_TYPE_MAP } from '@/constants/codeGenType'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const route = useRoute()
const router = useRouter()
const appId = String(route.params.id as string)
const initText = (route.query.init as string) || ''
// 移除 viewMode，改为根据对话历史判断是否自动发送消息

const loginUserStore = useLoginUserStore()

const appName = ref('')
const sending = ref(false)
const deploying = ref(false)
const downloading = ref(false)
const inputText = ref('')
const previewUrl = ref('')
const msgBoxRef = ref<HTMLDivElement>()
const showAppInfo = ref(false)
const showDeploySuccess = ref(false)
const deployUrl = ref('')
const appDetail = ref<API.AppVO>()
const isCurrentUserCreator = ref(false)
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

// 对话历史相关状态
const chatHistory = ref<API.ChatHistory[]>([])
const loadingHistory = ref(false)
const hasMoreHistory = ref(true)
const lastCreateTime = ref<string>('')

// iframe 状态管理
const iframeLoading = ref(false)
const previewIframe = ref<HTMLIFrameElement>()

// 监听previewUrl的变化
watch(previewUrl, (newUrl, oldUrl) => {
  console.log('previewUrl changed:', { oldUrl, newUrl })

  if (newUrl && newUrl !== oldUrl) {
    console.log('✅ Preview URL successfully set:', newUrl)
    // 开始加载iframe
    iframeLoading.value = true

    // 验证URL是否真的可以访问
    fetch(newUrl, { method: 'HEAD', cache: 'no-cache' })
      .then((response) => {
        if (response.ok) {
          console.log('✅ Preview URL verified as accessible')
        } else {
          console.warn('⚠️ Preview URL returned status:', response.status)
        }
      })
      .catch((error) => {
        console.warn('⚠️ Preview URL verification failed:', error)
      })
  }
})

// 监听sending状态变化
watch(sending, (newValue, oldValue) => {
  console.log('sending status changed:', { oldValue, newValue })
  if (!newValue && oldValue) {
    console.log('🏁 Sending completed')
  }
})

// 格式化时间
const formatTime = (timeStr: string | undefined): string => {
  if (!timeStr) return ''
  return dayjs(timeStr).format('YYYY-MM-DD HH:mm:ss')
}

// 拼接完整的头像URL
const getFullAvatarUrl = (avatarPath: string): string => {
  if (!avatarPath) return ''
  if (avatarPath.startsWith('http')) return avatarPath
  return getFullResourceUrl(avatarPath)
}

/**
 * 获取代码生成类型标签
 * @param codeGenType 代码生成类型
 * @returns 代码生成类型标签
 */
const getCodeGenTypeLabel = (codeGenType: string | undefined): string => {
  if (!codeGenType) return '未知类型'
  return CODE_GEN_TYPE_MAP[codeGenType] || codeGenType
}

// 头像配置
const userAvatar = computed(() => {
  const avatar = loginUserStore.loginUser.userAvatar
  if (!avatar) {
    return '/src/assets/ping/touxiang.jpg'
  }
  return getFullAvatarUrl(avatar)
})
const aiAvatar = '/src/assets/ping/touxiang.jpg'

// 创建markdown-it实例，配置代码高亮
const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        const highlighted = hljs.highlight(str, { language: lang }).value
        return `<pre class="hljs" data-lang="${lang}"><code>${highlighted}</code></pre>`
      } catch {
        // 忽略错误，使用默认转义
      }
    }
    return `<pre class="hljs" data-lang="${lang || 'text'}"><code>${str.replace(/[&<>"']/g, (m) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m] || m)}</code></pre>`
  },
})

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

// 消息缓存，用于收集SSE流片段
const msgBuffer = new Map<number, string>()

// 原来的 updateMsgHtml 函数已被下面的缓存机制替代

// 新增：处理SSE流片段的缓存和拼接
const appendToMsgBuffer = (id: number, chunk: string) => {
  const currentBuffer = msgBuffer.get(id) || ''
  msgBuffer.set(id, currentBuffer + chunk)
}

// 优化：实时更新消息内容，确保所有内容在同一个气泡中
const updateMsgFromBuffer = (id: number) => {
  const bufferedText = msgBuffer.get(id) || ''
  if (!bufferedText.trim()) return

  // 处理文本：对没有换行符的连续文本进行段落化处理
  const processedText = processTextForParagraphs(bufferedText)
  const renderedHtml = handleSSE(processedText)

  // 更新消息HTML - 始终使用完整的缓存内容
  const m = messages.find((x) => x.id === id)
  if (m) {
    m.html = renderedHtml
  }
}

// 当流结束时最终处理消息
const finalizeMsgBuffer = (id: number) => {
  updateMsgFromBuffer(id)
  // 流结束后清空该消息的缓存
  msgBuffer.delete(id)
}

// 优化：处理文本格式化，保持内容的连续性
const processTextForParagraphs = (text: string): string => {
  // 保留原始格式，不进行过度的段落化处理
  // 只处理明显的格式问题

  // 1. 处理多个连续空格（4个或以上），转换为段落分隔
  let processedText = text.replace(/\s{4,}/g, '\n\n')

  // 2. 如果文本包含代码块标记，保持原样
  if (
    processedText.includes('```') ||
    processedText.includes('<!DOCTYPE') ||
    processedText.includes('<html')
  ) {
    return processedText
  }

  // 3. 对于长段文本，在句号后且后面跟大写字母的位置适当添加换行（仅当没有换行符时）
  if (!processedText.includes('\n') && processedText.length > 100) {
    processedText = processedText.replace(/([.!?])\s+(?=[A-Z][a-z])/g, '$1\n\n')
  }

  // 4. 确保标题格式正确
  processedText = processedText.replace(/^(#{1,6})\s*(.+)$/gm, '$1 $2\n')

  return processedText
}

const handleSSE = (text: string) => {
  // 使用markdown-it解析Markdown内容
  try {
    return md.render(text)
  } catch (error) {
    console.error('Markdown parsing error:', error)
    // 如果解析失败，返回原始文本
    return text.replace(/\n/g, '<br/>')
  }
}

// 预览URL设置状态管理
const previewUrlSetupPromise = ref<Promise<void> | null>(null)

// 统一的预览URL设置函数
const setupPreviewUrl = async (delay = 2000) => {
  // 防止重复设置
  if (previewUrlSetupPromise.value) {
    console.log('Preview URL setup already in progress, skipping...')
    return previewUrlSetupPromise.value
  }

  console.log(`Starting preview URL setup with ${delay}ms delay...`)

  previewUrlSetupPromise.value = new Promise(async (resolve) => {
    // 延迟给后端时间生成文件
    await new Promise((r) => setTimeout(r, delay))

    // 确保appDetail已经加载，如果没有则等待
    if (!appDetail.value) {
      console.log('Waiting for app detail to load...')
      await fetchAppDetail()
    }

    const codeGenType = appDetail.value?.codeGenType

    // 根据代码生成类型判断是否需要添加dist后缀
    let generatedUrl = `${getStaticBasePath()}/${codeGenType}_${appId}`
    if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
      generatedUrl += '/dist'
    }

    console.log('Setting preview URL to:', generatedUrl, 'with codeGenType:', codeGenType)

    // 尝试多次验证URL，直到可访问或超时
    let attempts = 0
    const maxAttempts = 10
    let urlAccessible = false

    while (attempts < maxAttempts && !urlAccessible) {
      try {
        console.log(`Verifying URL accessibility (attempt ${attempts + 1}/${maxAttempts})...`)
        const response = await fetch(generatedUrl, { method: 'HEAD', cache: 'no-cache' })

        if (response.ok) {
          console.log('✅ Preview URL is accessible')
          urlAccessible = true
          previewUrl.value = generatedUrl
          break
        } else {
          console.warn(`⚠️ Preview URL returned status: ${response.status}`)
        }
      } catch (error) {
        console.warn(`⚠️ Preview URL verification failed (attempt ${attempts + 1}):`, error)
      }

      attempts++
      if (attempts < maxAttempts) {
        // 等待1秒后重试
        await new Promise((r) => setTimeout(r, 1000))
      }
    }

    if (!urlAccessible) {
      console.error('❌ Failed to verify preview URL after all attempts, setting anyway...')
      previewUrl.value = generatedUrl
    }

    resolve()
  })

  return previewUrlSetupPromise.value
}

const sendMsg = async () => {
  if (!inputText.value.trim()) return
  const userId = Date.now()
  // 对用户消息也进行markdown解析
  const userHtml = handleSSE(inputText.value)
  messages.push({ id: userId, role: 'user', html: userHtml })
  await scrollToBottom()

  // 创建AI消息并初始化缓存
  const aiId = appendAiMsg()
  msgBuffer.set(aiId, '') // 为新消息初始化空缓存
  await scrollToBottom()

  sending.value = true
  // 重置预览URL设置状态
  previewUrlSetupPromise.value = null

  const url = getApiUrl(
    `/app/chat/gen/code?appId=${encodeURIComponent(appId)}&message=${encodeURIComponent(inputText.value)}`,
  )
  inputText.value = ''
  eventSource?.close()
  eventSource = new EventSource(url, { withCredentials: true })

  eventSource.onmessage = async (e) => {
    if (!e?.data) return
    console.log('SSE message received:', e.data)

    // 检查是否是 done 事件（空数据表示流结束）
    if (e.data === '') {
      console.log('SSE done event received - stream completed')
      // 最终处理剩余的缓存内容
      finalizeMsgBuffer(aiId)
      eventSource?.close()
      // 使用统一的预览URL设置函数，缩短延迟因为有重试机制
      setupPreviewUrl(1000)
      await scrollToBottom()
      sending.value = false
      return
    }

    try {
      const obj = JSON.parse(e.data)
      if (obj && typeof obj.d === 'string') {
        // 将数据添加到缓存并实时更新显示
        appendToMsgBuffer(aiId, obj.d)
        updateMsgFromBuffer(aiId)
        await scrollToBottom()
      }
    } catch {
      // 兼容纯文本片段
      appendToMsgBuffer(aiId, e.data)
      updateMsgFromBuffer(aiId)
      await scrollToBottom()
    }
  }

  // 监听 done 事件（如果后端发送了特定的事件类型）
  eventSource.addEventListener('done', async () => {
    console.log('SSE done event listener triggered')
    // 最终处理剩余的缓存内容
    finalizeMsgBuffer(aiId)
    eventSource?.close()
    // 使用统一的预览URL设置函数
    setupPreviewUrl(1000)
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
    // 错误时也要处理剩余的缓存内容
    finalizeMsgBuffer(aiId)
    sending.value = false
    eventSource?.close()
    // 即使发生错误，也尝试设置预览URL（可能部分代码已生成）
    setupPreviewUrl(500) // 较短延迟
  }

  // 监听连接关闭
  eventSource.addEventListener('close', () => {
    console.log('SSE connection closed')
    // 最终处理剩余的缓存内容
    finalizeMsgBuffer(aiId)

    // 如果还没有启动预览URL设置，现在启动
    if (!previewUrlSetupPromise.value) {
      console.log('Connection closed, starting preview URL setup as fallback')
      setupPreviewUrl(500) // 更短延迟，因为连接已关闭说明流可能已结束
    }
  })

  // 超时保险机制：如果10秒后还没有设置预览URL，强制尝试设置
  setTimeout(() => {
    if (!previewUrlSetupPromise.value && sending.value) {
      console.log('Timeout reached, forcing preview URL setup as last resort')
      setupPreviewUrl(0) // 立即尝试
    }
  }, 10000) // 10秒超时
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
        deployUrl.value = url
        showDeploySuccess.value = true
      }
    } else {
      message.error(res.data.message)
    }
  } finally {
    deploying.value = false
  }
}

/**
 * 下载应用代码
 * 调用后端接口下载ZIP包，处理文件流响应
 */
const doDownloadCode = async () => {
  try {
    downloading.value = true

    // 直接使用axios调用下载接口，设置响应类型为blob以处理二进制数据
    const response = await downloadAppCode(
      { appId: appId as unknown as number },
      {
        responseType: 'blob',
      },
    )

    // 检查响应是否成功
    if (response && response.data) {
      const blob = response.data

      // 从响应头获取文件名，如果没有则使用默认名称
      let fileName = `app_${appId}_code.zip`

      // 尝试从Content-Disposition头获取文件名
      const contentDisposition = response.headers['content-disposition']
      if (contentDisposition) {
        const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = fileNameMatch[1].replace(/['"]/g, '')
        }
      }

      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName

      // 触发下载
      document.body.appendChild(link)
      link.click()

      // 清理资源
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      message.success('代码下载成功')
    } else {
      message.error('下载失败，请稍后重试')
    }
  } catch (error) {
    console.error('下载代码失败:', error)
    message.error('下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

// 复制部署URL
const copyDeployUrl = async () => {
  try {
    await navigator.clipboard.writeText(deployUrl.value)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    message.error('复制失败')
  }
}

// 访问网站
const visitWebsite = () => {
  window.open(deployUrl.value, '_blank')
}

// 关闭部署成功弹窗
const closeDeploySuccess = () => {
  showDeploySuccess.value = false
}

// 在新窗口打开预览网页
const openInNewWindow = () => {
  if (previewUrl.value) {
    console.log('Opening preview in new window:', previewUrl.value)
    window.open(previewUrl.value, '_blank', 'width=1200,height=800,scrollbars=yes,resizable=yes')
  } else {
    message.warning('网页还未生成，请等待代码生成完成')
  }
}

// iframe加载完成事件
const onIframeLoad = () => {
  console.log('✅ Iframe loaded successfully')
  iframeLoading.value = false
}

// iframe加载错误事件
const onIframeError = () => {
  console.error('❌ Iframe failed to load')
  iframeLoading.value = false
  message.error('网页加载失败，请检查网址是否正确')
}

// 手动刷新预览
const manualRefreshPreview = () => {
  console.log('Manual refresh preview triggered')
  // 重置预览URL设置状态
  previewUrlSetupPromise.value = null
  // 立即尝试设置预览URL
  setupPreviewUrl(0)
  message.info('正在刷新预览，请稍候...')
}

// 获取应用详情
const fetchAppDetail = async () => {
  try {
    const res = await getAppById({ id: appId as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appDetail.value = res.data.data
      appName.value = res.data.data.appName || ''

      // 检查当前用户是否是应用创建者
      const currentUserId = loginUserStore.loginUser.id
      isCurrentUserCreator.value = !!currentUserId && currentUserId === res.data.data.userId
    }
  } catch (error) {
    console.error('获取应用详情失败:', error)
    message.error('获取应用详情失败')
  }
}

/**
 * 加载对话历史记录
 * @param loadMore 是否为加载更多操作
 */
const loadChatHistory = async (loadMore = false) => {
  try {
    loadingHistory.value = true

    const params: API.listAppChatHistoryByPageParams = {
      appId: appId as unknown as number,
      pageSize: 10,
    }

    // 如果是加载更多，使用游标分页
    if (loadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }

    const res = await listAppChatHistoryByPage(params)

    if (res.data.code === 0 && res.data.data) {
      const newHistory = res.data.data.records || []

      if (loadMore) {
        // 加载更多时，将新数据添加到现有历史记录前面（因为是按时间倒序获取的）
        chatHistory.value = [...newHistory, ...chatHistory.value]
      } else {
        // 首次加载时，直接设置历史记录
        chatHistory.value = newHistory
      }

      // 更新游标和是否还有更多数据的状态
      if (newHistory.length > 0) {
        lastCreateTime.value = newHistory[newHistory.length - 1].createTime || ''
        hasMoreHistory.value = newHistory.length === 10 // 如果返回的数据等于页面大小，说明可能还有更多数据
      } else {
        hasMoreHistory.value = false
      }

      // 将历史记录转换为消息格式并添加到消息列表
      if (!loadMore) {
        convertHistoryToMessages()
      } else {
        convertHistoryToMessages(true)
      }
    }
  } catch (error) {
    console.error('加载对话历史失败:', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

/**
 * 将对话历史转换为消息格式
 * @param prepend 是否添加到消息列表前面
 */
const convertHistoryToMessages = (prepend = false) => {
  const historyMessages: Msg[] = chatHistory.value
    .sort((a, b) => new Date(a.createTime || '').getTime() - new Date(b.createTime || '').getTime()) // 按时间升序排列
    .map((history) => ({
      id: history.id || Date.now(),
      role: history.messageType === 'user' ? 'user' : 'ai',
      html: handleSSE(history.message || ''),
    }))

  if (prepend) {
    // 加载更多时，将历史消息添加到现有消息前面
    const currentMessages = messages.slice()
    messages.length = 0
    messages.push(...historyMessages, ...currentMessages)
  } else {
    // 首次加载时，清空现有消息并添加历史消息
    messages.length = 0
    messages.push(...historyMessages)
  }
}

// 跳转到编辑页面
const goEdit = () => {
  showAppInfo.value = false
  router.push(`/app/edit/${appId}`)
}

// 处理删除操作
const handleDelete = () => {
  Modal.confirm({
    title: '确定要删除这个应用吗？',
    content: '删除后无法恢复，请谨慎操作。',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        if (isAdmin.value) {
          await deleteApp({ id: Number(appId) })
        } else {
          await deleteMyApp({ id: Number(appId) })
        }
        message.success('应用删除成功')
        showAppInfo.value = false
        // 跳转到首页
        router.push('/')
      } catch (error) {
        console.error('删除应用失败:', error)
        message.error('删除应用失败')
      }
    },
  })
}

// 处理文件上传
const handleUpload = () => {
  message.info('文件上传功能开发中...')
}

// 重写fetchApp函数，整合权限校验
const fetchApp = async () => {
  await fetchAppDetail()
}

// 监听showAppInfo变化，打开时重新获取详情
watch(showAppInfo, async (newVal) => {
  if (newVal) {
    await fetchAppDetail()
  }
})

onMounted(async () => {
  await fetchApp()
  // 加载对话历史
  await loadChatHistory()

  // 修改自动发送初始消息的逻辑：只有在是自己的app且没有对话历史时才自动发送
  if (initText && (isCurrentUserCreator.value || isAdmin.value) && chatHistory.value.length === 0) {
    inputText.value = initText
    await sendMsg()
  }

  // 修改网站展示逻辑：如果有至少2条对话记录，展示网站
  if (chatHistory.value.length >= 2) {
    setupPreviewUrl(0) // 立即设置预览URL
  }
})

onUnmounted(() => {
  eventSource?.close()
  // 清理消息缓存
  msgBuffer.clear()
})
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
  grid-template-columns: 2fr 3fr;
  gap: 12px;
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

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

/* 加载更多按钮容器 */
.load-more-container {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  padding: 10px 0;
}

.load-more-btn {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  padding: 8px 16px;
  color: #6b7280;
  font-size: 14px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.load-more-btn:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.load-more-btn:active {
  transform: translateY(0);
}

.msg {
  margin: 16px 0;
  display: flex;
  align-items: flex-start;
  gap: 12px;
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

/* 用户消息的Markdown样式 */
.msg.user .bubble h1,
.msg.user .bubble h2,
.msg.user .bubble h3 {
  color: #fff;
  margin: 8px 0 4px 0;
  font-weight: 600;
}

.msg.user .bubble h1 {
  font-size: 1.4em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  padding-bottom: 4px;
}

.msg.user .bubble h2 {
  font-size: 1.2em;
}

.msg.user .bubble h3 {
  font-size: 1.1em;
}

.msg.user .bubble p {
  margin: 4px 0;
  line-height: 1.5;
}

.msg.user .bubble strong {
  font-weight: 600;
  color: #fff;
}

.msg.user .bubble code {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Monaco', 'Consolas', 'Liberation Mono', 'Courier New', monospace;
  font-size: 0.9em;
}

.msg.ai .bubble {
  background: #fff;
  color: #374151;
  border-bottom-left-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #e5e7eb;
  line-height: 1.6;
  font-size: 14px;
}

/* Markdown样式 */
.msg.ai .bubble h1 {
  font-size: 1.5em;
  font-weight: 600;
  margin: 16px 0 8px 0;
  color: #1f2937;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 4px;
}

.msg.ai .bubble h2 {
  font-size: 1.3em;
  font-weight: 600;
  margin: 14px 0 6px 0;
  color: #1f2937;
}

.msg.ai .bubble h3 {
  font-size: 1.1em;
  font-weight: 600;
  margin: 12px 0 4px 0;
  color: #1f2937;
}

.msg.ai .bubble strong {
  font-weight: 600;
  color: #1f2937;
}

.msg.ai .bubble em {
  font-style: italic;
  color: #6b7280;
}

.msg.ai .bubble ul {
  margin: 8px 0;
  padding-left: 20px;
}

.msg.ai .bubble li {
  margin: 4px 0;
  line-height: 1.5;
}

.msg.ai .bubble p {
  margin: 8px 0;
  line-height: 1.6;
}

.msg.ai .bubble a {
  color: #3b82f6;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s ease;
}

.msg.ai .bubble a:hover {
  border-bottom-color: #3b82f6;
}

/* 代码样式 */
.msg.ai .bubble pre {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  overflow-x: auto;
  position: relative;
  font-size: 13px;
}

.msg.ai .bubble pre[data-lang]::before {
  content: attr(data-lang);
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 11px;
  color: #6c757d;
  background: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #e9ecef;
  text-transform: uppercase;
  font-family: 'Monaco', 'Consolas', monospace;
  font-weight: 500;
}

.msg.ai .bubble pre code {
  color: #212529;
  font-family: 'Fira Code', 'Monaco', 'Consolas', 'Liberation Mono', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.4;
  white-space: pre;
  background: none;
}

.msg.ai .bubble code {
  background: #f8f9fa;
  color: #e83e8c;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Monaco', 'Consolas', 'Liberation Mono', 'Courier New', monospace;
  font-size: 0.9em;
  border: 1px solid #e9ecef;
}

/* 代码高亮主题覆盖 - 浅色主题 */

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

/* 输入框禁用提示样式 */
.input-disabled-tip {
  text-align: center;
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
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
  position: relative;
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

/* iframe加载覆盖层 */
.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
  z-index: 10;
}

.loading-content {
  text-align: center;
  padding: 20px;
}

.loading-icon {
  font-size: 24px;
  margin-bottom: 8px;
  animation: pulse 2s infinite;
}

.loading-text {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
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

/* 调试信息样式 */
.debug-info {
  margin-top: 20px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  text-align: left;
  max-width: 300px;
  margin-left: auto;
  margin-right: auto;
}

.debug-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.debug-item:last-child {
  margin-bottom: 0;
}

.debug-label {
  color: #6b7280;
  font-weight: 500;
}

.debug-value {
  color: #374151;
  font-weight: 600;
}

.debug-value.active {
  color: #059669;
}

.debug-path {
  color: #3b82f6;
  font-family: monospace;
  font-size: 11px;
  word-break: break-all;
}

/* 应用详情卡片样式 */
.app-info {
  padding: 16px 0;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  color: #374151;
  min-width: 80px;
  margin-right: 16px;
}

.value {
  color: #6b7280;
  flex: 1;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.creator-avatar {
  flex-shrink: 0;
}

.creator-name {
  color: #374151;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 部署成功弹窗样式 */
.deploy-success-content {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  margin-bottom: 20px;
}

.success-message {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.success-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 24px;
}

.url-container {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
  align-items: center;
}

.url-input {
  flex: 1;
}

.copy-btn {
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
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
