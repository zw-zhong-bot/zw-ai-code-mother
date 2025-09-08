# 应用组件库

## 概述

本组件库提供了一系列与应用相关的可复用组件，用于统一管理应用卡片、应用详情弹窗等UI元素，提高代码复用性和可维护性。

**作者:** ZW  
**创建时间:** 2025-09-08  
**修改时间:** 2025-09-08  
**版本:** 1.0.0

## 组件列表

### 1. AppCard 应用卡片组件

应用卡片组件用于展示应用的基本信息，包括封面、名称、创建者等。

#### 主要特性

- 统一的卡片样式和交互效果
- 自动处理图片加载错误
- 支持自定义操作按钮
- 响应式设计，适配不同屏幕尺寸

#### 使用方法

```vue
<template>
  <app-card
    :app-data="appData"
    @card-click="handleCardClick"
    @chat-click="handleChatClick"
    @view-click="handleViewClick"
  >
    <template #actions>
      <!-- 自定义操作按钮 -->
      <a-button type="primary" size="small">自定义按钮</a-button>
    </template>
  </app-card>
</template>

<script setup>
import AppCard from '@/components/app/AppCard.vue'

const appData = {
  id: 1,
  appName: '示例应用',
  cover: 'path/to/cover.jpg',
  user: {
    id: 1,
    userName: '用户名',
  },
  deployKey: 'deploy-key',
}

const handleCardClick = (id) => {
  console.log('卡片点击:', id)
}

const handleChatClick = (id) => {
  console.log('查看对话:', id)
}

const handleViewClick = (deployKey) => {
  console.log('查看作品:', deployKey)
}
</script>
```

### 2. AppDetailModal 应用详情弹窗组件

应用详情弹窗组件用于展示应用的详细信息，包括创建者、时间、优先级等。

#### 主要特性

- 统一的弹窗样式和交互效果
- 自动加载应用详情数据
- 支持自定义内容和操作按钮
- 权限控制，根据用户角色显示不同操作

#### 使用方法

```vue
<template>
  <app-detail-modal
    :app-id="appId"
    @close="handleClose"
    @edit="handleEdit"
    @chat="handleChat"
    @delete="handleDelete"
    @loaded="handleLoaded"
  >
    <template #extra-content="{ appDetail }">
      <!-- 自定义额外内容 -->
      <div class="custom-content">自定义内容: {{ appDetail.customField }}</div>
    </template>

    <template #actions="{ appDetail }">
      <!-- 自定义操作按钮 -->
      <a-button type="primary">自定义按钮</a-button>
    </template>
  </app-detail-modal>
</template>

<script setup>
import AppDetailModal from '@/components/app/AppDetailModal.vue'

const appId = '123'

const handleClose = () => {
  console.log('关闭弹窗')
}

const handleEdit = (id) => {
  console.log('编辑应用:', id)
}

const handleChat = (id) => {
  console.log('查看对话:', id)
}

const handleDelete = (id) => {
  console.log('删除应用:', id)
}

const handleLoaded = (detail) => {
  console.log('应用详情加载完成:', detail)
}
</script>
```

### 3. AppCardGrid 应用卡片网格组件

应用卡片网格组件用于展示应用卡片列表，支持分页、搜索等功能。

#### 主要特性

- 响应式网格布局
- 内置搜索和分页功能
- 支持紧凑模式
- 自定义头部和卡片操作

#### 使用方法

```vue
<template>
  <app-card-grid
    :apps="appList"
    :total-items="totalCount"
    :loading="loading"
    @search="handleSearch"
    @page-change="handlePageChange"
    @card-click="handleCardClick"
    @chat-click="handleChatClick"
    @view-click="handleViewClick"
  >
    <template #header>
      <!-- 自定义头部 -->
      <div class="custom-header">
        <h2>自定义标题</h2>
        <a-button type="primary">新建应用</a-button>
      </div>
    </template>

    <template #cardActions="{ app }">
      <!-- 自定义卡片操作 -->
      <a-button type="primary" size="small">编辑</a-button>
      <a-button size="small">删除</a-button>
    </template>
  </app-card-grid>
</template>

<script setup>
import AppCardGrid from '@/components/app/AppCardGrid.vue'
import { ref } from 'vue'

const appList = ref([])
const totalCount = ref(0)
const loading = ref(false)

const handleSearch = (query) => {
  console.log('搜索:', query)
  // 执行搜索逻辑
}

const handlePageChange = (page, pageSize) => {
  console.log('页码变化:', page, pageSize)
  // 执行分页逻辑
}

const handleCardClick = (id) => {
  console.log('卡片点击:', id)
}

const handleChatClick = (id) => {
  console.log('查看对话:', id)
}

const handleViewClick = (deployKey) => {
  console.log('查看作品:', deployKey)
}
</script>
```

## 图片工具优化

为了统一管理应用封面和用户头像的处理逻辑，我们对 `imageUtils.ts` 进行了优化：

1. 添加了图片类型枚举，便于区分不同类型的图片
2. 实现了图片缓存机制，减少重复请求
3. 提供了统一的图片错误处理函数
4. 支持异步验证图片有效性
5. 优化了图片文件验证逻辑

### 使用方法

```typescript
import {
  getAppCoverUrl,
  getUserAvatarApiUrl,
  handleImageError,
  ImageType,
  validateImageFile,
} from '@/utils/imageUtils'

// 获取应用封面URL
const coverUrl = getAppCoverUrl(app.cover)

// 获取用户头像URL
const avatarUrl = getUserAvatarApiUrl(user.id)

// 处理图片加载错误
const onImageError = (event) => {
  handleImageError(event, ImageType.APP_COVER)
}

// 验证上传的图片文件
const validateImage = (file) => {
  const result = validateImageFile(file)
  if (!result.isValid) {
    console.error(result.message)
  }
  return result.isValid
}
```

## 注意事项

1. 所有组件都支持自定义插槽，可以根据需要进行扩展
2. 图片处理工具会自动处理图片加载错误，提供默认图片
3. 应用详情弹窗组件会根据用户权限自动控制编辑和删除按钮的显示
4. 组件样式已经适配移动端，无需额外处理响应式布局
