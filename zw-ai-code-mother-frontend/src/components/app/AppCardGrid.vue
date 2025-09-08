<template>
  <div class="app-grid-container">
    <!-- 搜索和筛选区域 -->
    <div class="grid-header" v-if="showHeader">
      <slot name="header">
        <div class="section-header">
          <h2>{{ title }}</h2>
          <a-input
            v-if="showSearch"
            v-model:value="searchQuery"
            :placeholder="searchPlaceholder"
            style="max-width: 260px"
            @pressEnter="onSearch"
          />
        </div>
      </slot>
    </div>

    <!-- 应用卡片网格 -->
    <div class="app-grid" :class="{ 'compact-grid': compact }">
      <app-card
        v-for="app in appList"
        :key="app.id"
        :app-data="app"
        @card-click="onCardClick"
        @chat-click="onChatClick"
        @view-click="onViewClick"
      >
        <template #actions v-if="$slots.cardActions">
          <slot name="cardActions" :app="app"></slot>
        </template>
      </app-card>

      <!-- 空状态 -->
      <div v-if="appList.length === 0 && !loading" class="empty-state">
        <a-empty :description="emptyText" />
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <a-spin />
      </div>
    </div>

    <!-- 分页区域 -->
    <div class="pagination-container" v-if="showPagination && totalItems > 0">
      <a-pagination
        v-model:current="currentPage"
        v-model:pageSize="pageSize"
        :total="totalItems"
        :show-total="(total) => `共 ${total} 条`"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 应用卡片网格组件
 * 用于展示应用卡片列表，支持分页、搜索等功能
 * @author ZW
 * @since 2025-09-08
 * @version 1.0.0
 */
import { ref, computed, watch } from 'vue'
import AppCard from './AppCard.vue'
import { getAppDeployUrl } from '@/config/env'

/**
 * 组件属性定义
 */
interface Props {
  /** 应用列表数据 */
  apps?: API.AppVO[]
  /** 标题 */
  title?: string
  /** 是否显示头部区域 */
  showHeader?: boolean
  /** 是否显示搜索框 */
  showSearch?: boolean
  /** 搜索框占位文本 */
  searchPlaceholder?: string
  /** 是否显示分页 */
  showPagination?: boolean
  /** 总条目数 */
  totalItems?: number
  /** 当前页码 */
  currentPageProp?: number
  /** 每页条数 */
  pageSizeProp?: number
  /** 空状态文本 */
  emptyText?: string
  /** 是否紧凑模式 */
  compact?: boolean
  /** 是否加载中 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  apps: () => [],
  title: '应用列表',
  showHeader: true,
  showSearch: true,
  searchPlaceholder: '按名称搜索',
  showPagination: true,
  totalItems: 0,
  currentPageProp: 1,
  pageSizeProp: 20,
  emptyText: '暂无应用',
  compact: false,
  loading: false,
})

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  /** 搜索事件 */
  (e: 'search', query: string): void
  /** 页码变化事件 */
  (e: 'page-change', page: number, pageSize: number): void
  /** 卡片点击事件 */
  (e: 'card-click', id: string): void
  /** 查看对话按钮点击事件 */
  (e: 'chat-click', id: string): void
  /** 查看作品按钮点击事件 */
  (e: 'view-click', deployKey: string): void
}>()

// 内部状态
const searchQuery = ref('')
const currentPage = ref(props.currentPageProp)
const pageSize = ref(props.pageSizeProp)

// 计算属性：应用列表
const appList = computed(() => props.apps || [])

/**
 * 搜索事件处理
 */
const onSearch = () => {
  currentPage.value = 1
  emit('search', searchQuery.value)
}

/**
 * 页码变化事件处理
 * @param page 页码
 * @param size 每页条数
 */
const onPageChange = (page: number, size?: number) => {
  currentPage.value = page
  if (size !== undefined) {
    pageSize.value = size
  }
  emit('page-change', page, pageSize.value)
}

/**
 * 卡片点击事件处理
 * @param id 应用ID
 */
const onCardClick = (id: string) => {
  emit('card-click', id)
}

/**
 * 查看对话按钮点击事件处理
 * @param id 应用ID
 */
const onChatClick = (id: string) => {
  emit('chat-click', id)
}

/**
 * 查看作品按钮点击事件处理
 * @param deployKey 部署密钥
 */
const onViewClick = (deployKey: string) => {
  emit('view-click', deployKey)
  // 在新窗口打开部署的应用
  window.open(getAppDeployUrl(deployKey), '_blank')
}

// 监听属性变化
watch(
  () => props.currentPageProp,
  (val) => {
    currentPage.value = val
  },
)

watch(
  () => props.pageSizeProp,
  (val) => {
    pageSize.value = val
  },
)
</script>

<style scoped>
.app-grid-container {
  width: 100%;
}

.grid-header {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h2 {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
}

/* 应用卡片网格布局 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

/* 紧凑模式 */
.compact-grid {
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

/* 空状态样式 */
.empty-state {
  grid-column: 1 / -1;
  padding: 40px 0;
  text-align: center;
}

/* 加载状态样式 */
.loading-state {
  grid-column: 1 / -1;
  padding: 40px 0;
  text-align: center;
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

  .compact-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
