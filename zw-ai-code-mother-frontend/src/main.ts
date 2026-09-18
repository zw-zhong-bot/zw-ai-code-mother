import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/styles/theme.css'

import '@/access'
import { initViewportHeight } from '@/utils/viewport'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

// 初始化动态视口高度（移动端键盘遮挡与地址栏伸缩适配）
initViewportHeight()

app.mount('#app')
