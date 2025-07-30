<template>
  <a-layout-header class="global-header">
    <div class="header-content">
      <!-- 左侧 Logo 和标题 -->
      <div class="header-left">
        <img src="@/assets/ping/logo.jpg" alt="Logo" class="logo" />
        <h1 class="site-title">AI 零代码应用生成平台</h1>
      </div>

      <!-- 中间菜单 -->
      <div class="header-menu">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </div>

      <!-- 右侧用户信息 -->
      <div class="header-right">
        <a-button type="primary" @click="handleLogin">
          <template #icon>
            <UserOutlined />
          </template>
          登录
        </a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const selectedKeys = ref<string[]>(['/'])


// 菜单配置
const menuItems = [
  {
    key: 'home',
    label: '首页',
    path: '/',
  },
  {
    key: 'about',
    label: '关于',
    path: '/about',
  },
  {
    key: 'tools',
    label: '工具',
    children: [
      {
        key: 'code-generator',
        label: '代码生成器',
        path: '/tools/code-generator',
      },
      {
        key: 'api-docs',
        label: 'API 文档',
        path: '/tools/api-docs',
      },
    ],
  },
  {
    key: 'resources',
    label: '资源',
    children: [
      {
        key: 'tutorials',
        label: '教程',
        path: '/resources/tutorials',
      },
      {
        key: 'books',
        label: '书籍',
        path: '/resources/books',
      },
    ],
  },
]

// 处理菜单点击
const handleMenuClick = ({ key }: { key: string }) => {
  const menuItem = findMenuItemByKey(menuItems, key)
  if (menuItem && menuItem.path) {
    router.push(menuItem.path)
  }
}

/*// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}*/

// 递归查找菜单项
const findMenuItemByKey = (items: any[], key: string): any => {
  for (const item of items) {
    if (item.key === key) {
      return item
    }
    if (item.children) {
      const found = findMenuItemByKey(item.children, key)
      if (found) return found
    }
  }
  return null
}

// 处理登录
const handleLogin = () => {
  console.log('登录功能待实现')
}
</script>

<style scoped>
.global-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 64px;
  line-height: 64px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
}

.site-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}

.header-menu {
  flex: 1;
  display: flex;
  justify-content: center;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .site-title {
    font-size: 16px;
  }

  .header-menu {
    display: none;
  }
}

@media (max-width: 480px) {
  .header-left {
    gap: 8px;
  }

  .logo {
    width: 32px;
    height: 32px;
  }

  .site-title {
    font-size: 14px;
  }
}
</style>
