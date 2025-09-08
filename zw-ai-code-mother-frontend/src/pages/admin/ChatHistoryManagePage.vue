<template>
  <div>
    <a-form layout="inline" :model="query" @finish="onSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="query.appName" placeholder="请输入应用名称" style="width: 200px" />
      </a-form-item>
      <a-form-item label="消息内容">
        <a-input v-model:value="query.message" placeholder="请输入消息内容" style="width: 200px" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select
          v-model:value="query.messageType"
          placeholder="请选择消息类型"
          allow-clear
          style="width: 200px"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="user">用户消息</a-select-option>
          <a-select-option value="ai">AI消息</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="应用ID">
        <a-input v-model:value="query.appId" placeholder="请输入应用ID" style="width: 200px" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
        <a-button style="margin-left: 8px" @click="onReset">重置</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <a-table :data-source="list" :pagination="pagination" row-key="id" @change="onTableChange">
      <a-table-column title="ID" dataIndex="id" width="80" />
      <a-table-column title="应用名称" key="appName" width="150" :customRender="appNameRender" />
      <a-table-column title="消息内容" key="message" width="300" :customRender="messageRender" />
      <a-table-column
        title="消息类型"
        key="messageType"
        width="100"
        :customRender="messageTypeRender"
      />
      <a-table-column title="状态" key="status" width="100" :customRender="statusRender" />
      <a-table-column title="消息顺序" dataIndex="messageOrder" width="100" />
      <a-table-column title="创建时间" dataIndex="createTime" width="180" />
      <a-table-column title="操作" key="action" width="150" :customRender="actionRender" />
    </a-table>

    <!-- 消息详情弹窗 -->
    <a-modal
      v-model:open="showMessageDetail"
      title="消息详情"
      :footer="null"
      width="800px"
      :bodyStyle="{ maxHeight: '600px', overflowY: 'auto' }"
    >
      <div v-if="selectedMessage" class="message-detail">
        <div class="detail-item">
          <span class="label">消息ID：</span>
          <span class="value">{{ selectedMessage.id }}</span>
        </div>
        <div class="detail-item">
          <span class="label">应用ID：</span>
          <span class="value">{{ selectedMessage.appId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">用户ID：</span>
          <span class="value">{{ selectedMessage.userId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">消息类型：</span>
          <a-tag :color="selectedMessage.messageType === 'user' ? 'blue' : 'green'">
            {{ selectedMessage.messageType === 'user' ? '用户消息' : 'AI消息' }}
          </a-tag>
        </div>
        <div class="detail-item">
          <span class="label">状态：</span>
          <a-tag :color="getStatusColor(selectedMessage.status)">
            {{ getStatusText(selectedMessage.status) }}
          </a-tag>
        </div>
        <div class="detail-item">
          <span class="label">消息顺序：</span>
          <span class="value">{{ selectedMessage.messageOrder }}</span>
        </div>
        <div class="detail-item">
          <span class="label">创建时间：</span>
          <span class="value">{{ selectedMessage.createTime }}</span>
        </div>
        <div class="detail-item">
          <span class="label">更新时间：</span>
          <span class="value">{{ selectedMessage.updateTime }}</span>
        </div>
        <div v-if="selectedMessage.errorMessage" class="detail-item">
          <span class="label">错误信息：</span>
          <span class="value error-text">{{ selectedMessage.errorMessage }}</span>
        </div>
        <div v-if="selectedMessage.errorCode" class="detail-item">
          <span class="label">错误代码：</span>
          <span class="value error-text">{{ selectedMessage.errorCode }}</span>
        </div>
        <div class="detail-item full-width">
          <span class="label">消息内容：</span>
          <div class="message-content" v-html="formatMessageContent(selectedMessage.message)"></div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 对话管理页面
 *
 * 功能：
 * - 对话历史记录的查询和展示
 * - 支持按应用名称、消息内容、消息类型、应用ID等条件搜索
 * - 支持分页展示对话记录
 * - 提供消息详情查看功能
 * - 支持删除对话记录操作
 *
 * 作者：ZW
 * 创建时间：2025-01-08
 * 版本：1.0.0
 */

import { computed, h, onMounted, reactive, ref } from 'vue'
import { listChatHistoryByPage } from '@/api/chatHistoryController.ts'
import { message, Tag, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const router = useRouter()

// 查询参数
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  message: '',
  messageType: '',
  appId: '',
})

// 数据状态
const total = ref(0)
const list = ref<API.ChatHistory[]>([])
const showMessageDetail = ref(false)
const selectedMessage = ref<API.ChatHistory>()

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
    return `<pre class="hljs" data-lang="${lang || 'text'}"><code>${str.replace(/[&<>\"']/g, (m) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '\"': '&quot;', "'": '&#39;' })[m] || m)}</code></pre>`
  },
})

/**
 * 获取对话历史数据
 */
const fetchData = async () => {
  try {
    // 构建查询参数，过滤空值
    const queryParams = {
      ...query,
      appName: query.appName?.trim() || undefined,
      message: query.message?.trim() || undefined,
      messageType: query.messageType || undefined,
      appId: query.appId?.trim() ? Number(query.appId) : undefined,
    }

    // 移除undefined值
    Object.keys(queryParams).forEach((key) => {
      if (queryParams[key as keyof typeof queryParams] === undefined) {
        delete queryParams[key as keyof typeof queryParams]
      }
    })

    const res = await listChatHistoryByPage(queryParams as any)
    if (res.data.code === 0 && res.data.data) {
      list.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message || '获取数据失败')
    }
  } catch (error) {
    console.error('获取对话历史失败:', error)
    message.error('获取对话历史失败')
  }
}

// 分页配置
const pagination = computed(() => ({
  current: query.pageNum ?? 1,
  pageSize: query.pageSize ?? 10,
  total: total.value,
}))

/**
 * 表格分页变化处理
 */
const onTableChange = (p: { current: number; pageSize: number }) => {
  query.pageNum = p.current
  query.pageSize = p.pageSize
  fetchData()
}

/**
 * 搜索处理
 */
const onSearch = () => {
  query.pageNum = 1
  fetchData()
}

/**
 * 重置搜索条件
 */
const onReset = () => {
  query.appName = ''
  query.message = ''
  query.messageType = ''
  query.appId = ''
  query.pageNum = 1
  fetchData()
}

/**
 * 删除对话记录
 */
const doDelete = async (id: number) => {
  Modal.confirm({
    title: '确定要删除这条对话记录吗？',
    content: '删除后无法恢复，请谨慎操作。',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        // 这里需要调用删除接口，暂时用message提示
        message.info('删除功能开发中...')
        // 删除成功后刷新数据
        // fetchData()
      } catch (error) {
        console.error('删除对话记录失败:', error)
        message.error('删除对话记录失败')
      }
    },
  })
}

/**
 * 查看消息详情
 */
const viewMessageDetail = (record: API.ChatHistory) => {
  selectedMessage.value = record
  showMessageDetail.value = true
}

/**
 * 格式化消息内容
 */
const formatMessageContent = (content: string | undefined): string => {
  if (!content) return '无内容'
  try {
    return md.render(content)
  } catch (error) {
    console.error('Markdown解析错误:', error)
    return content.replace(/\n/g, '<br/>')
  }
}

/**
 * 获取状态颜色
 */
const getStatusColor = (status: number | undefined): string => {
  switch (status) {
    case 0:
      return 'success'
    case 1:
      return 'error'
    default:
      return 'default'
  }
}

/**
 * 获取状态文本
 */
const getStatusText = (status: number | undefined): string => {
  switch (status) {
    case 0:
      return '正常'
    case 1:
      return '异常'
    default:
      return '未知'
  }
}

// 应用名称渲染
const appNameRender = ({ record }: { record: API.ChatHistory }) => {
  return h('span', {}, `应用 #${record.appId}`)
}

// 消息内容渲染
const messageRender = ({ record }: { record: API.ChatHistory }) => {
  const message = record.message || '无内容'
  const displayText = message.length > 50 ? message.substring(0, 50) + '...' : message
  return h('span', { title: message, style: 'cursor: pointer' }, displayText)
}

// 消息类型渲染
const messageTypeRender = ({ record }: { record: API.ChatHistory }) => {
  const isUser = record.messageType === 'user'
  return h(Tag, { color: isUser ? 'blue' : 'green' }, () => (isUser ? '用户消息' : 'AI消息'))
}

// 状态渲染
const statusRender = ({ record }: { record: API.ChatHistory }) => {
  return h(Tag, { color: getStatusColor(record.status) }, () => getStatusText(record.status))
}

// 操作按钮渲染
const actionRender = ({ record }: { record: API.ChatHistory }) => {
  return h('div', { style: 'display: flex; gap: 8px; flex-wrap: wrap' }, [
    h(
      Tag,
      {
        color: 'processing',
        style: 'cursor: pointer; margin: 0; transition: all 0.2s ease;',
        onClick: () => viewMessageDetail(record),
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
      () => '详情',
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
  ])
}

onMounted(fetchData)
</script>

<style scoped>
/* 消息详情样式 */
.message-detail {
  padding: 16px 0;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.detail-item.full-width {
  flex-direction: column;
  align-items: stretch;
}

.label {
  font-weight: 600;
  color: #374151;
  min-width: 100px;
  margin-right: 16px;
  flex-shrink: 0;
}

.value {
  color: #6b7280;
  flex: 1;
  word-break: break-word;
}

.error-text {
  color: #ef4444;
}

.message-content {
  margin-top: 8px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.6;
}

/* Markdown样式 */
.message-content h1,
.message-content h2,
.message-content h3 {
  margin: 16px 0 8px 0;
  color: #1f2937;
}

.message-content h1 {
  font-size: 1.5em;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 4px;
}

.message-content h2 {
  font-size: 1.3em;
}

.message-content h3 {
  font-size: 1.1em;
}

.message-content p {
  margin: 8px 0;
}

.message-content ul,
.message-content ol {
  margin: 8px 0;
  padding-left: 20px;
}

.message-content li {
  margin: 4px 0;
}

.message-content code {
  background: #f1f5f9;
  color: #e11d48;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 0.9em;
}

.message-content pre {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  overflow-x: auto;
  font-size: 13px;
}

.message-content pre code {
  background: none;
  color: #212529;
  padding: 0;
  border-radius: 0;
}

.message-content blockquote {
  border-left: 4px solid #e5e7eb;
  margin: 16px 0;
  padding-left: 16px;
  color: #6b7280;
}

.message-content a {
  color: #3b82f6;
  text-decoration: none;
}

.message-content a:hover {
  text-decoration: underline;
}
</style>
