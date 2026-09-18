<template>
  <div id="authShell" class="zw-page-bg">
    <div class="auth-card">
      <!-- 左侧：品牌展示区（桌面端） -->
      <aside class="brand-panel">
        <div class="brand-grid" aria-hidden="true"></div>
        <div class="brand-glow" aria-hidden="true"></div>
        <div class="brand-inner">
          <RouterLink to="/" class="brand-logo">
            <img src="@/assets/touxiang.jpg" alt="ZW 应用生成" />
            <span class="brand-name">ZW 应用生成</span>
          </RouterLink>

          <h2 class="brand-slogan">一句话，生成完整应用</h2>
          <p class="brand-desc">不写一行代码，把想法直接变成可访问的网站应用。</p>

          <ul class="brand-features">
            <li v-for="item in features" :key="item">
              <CheckCircleFilled class="feat-icon" />
              <span>{{ item }}</span>
            </li>
          </ul>
        </div>
      </aside>

      <!-- 右侧：表单区 -->
      <section class="form-panel">
        <!-- 移动端紧凑品牌头 -->
        <RouterLink to="/" class="mobile-brand">
          <img src="@/assets/touxiang.jpg" alt="ZW 应用生成" />
          <span>ZW 应用生成</span>
        </RouterLink>

        <header class="form-head">
          <h1 class="form-title">{{ title }}</h1>
          <p v-if="subtitle" class="form-subtitle">{{ subtitle }}</p>
        </header>

        <slot />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircleFilled } from '@ant-design/icons-vue'

interface Props {
  /** 表单区主标题 */
  title: string
  /** 表单区副标题 */
  subtitle?: string
}

withDefaults(defineProps<Props>(), {
  subtitle: '',
})

const features = ['多模型可插拔，随时热切换', '生成即部署，一键获得访问链接', '对话式迭代，边聊边改']
</script>

<style scoped>
#authShell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: max(560px, calc(100vh - 164px));
  padding: var(--zw-space-7) var(--zw-space-5);
}

/* ---------------- 卡片容器 ---------------- */
.auth-card {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  width: 100%;
  max-width: 940px;
  overflow: hidden;
  background: var(--zw-surface);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--zw-radius-xl);
  box-shadow: var(--zw-shadow-lg);
}

/* ---------------- 品牌展示区 ---------------- */
.brand-panel {
  position: relative;
  display: flex;
  align-items: center;
  padding: var(--zw-space-7) var(--zw-space-6);
  background: linear-gradient(135deg, #1e3a8a 0%, #4c1d95 55%, #065f46 100%);
  overflow: hidden;
}

.brand-grid,
.brand-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.brand-grid {
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.07) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at 30% 20%, #000 0%, transparent 75%);
  -webkit-mask-image: radial-gradient(circle at 30% 20%, #000 0%, transparent 75%);
}

.brand-glow {
  background:
    radial-gradient(circle at 15% 85%, rgba(59, 130, 246, 0.55) 0%, transparent 55%),
    radial-gradient(circle at 85% 15%, rgba(139, 92, 246, 0.5) 0%, transparent 55%),
    radial-gradient(circle at 60% 60%, rgba(16, 185, 129, 0.32) 0%, transparent 60%);
  animation: brandPulse 9s ease-in-out infinite alternate;
}

@keyframes brandPulse {
  0% {
    opacity: 0.75;
    transform: scale(1);
  }
  100% {
    opacity: 1;
    transform: scale(1.06);
  }
}

.brand-inner {
  position: relative;
  z-index: 1;
  width: 100%;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: var(--zw-space-3);
  margin-bottom: var(--zw-space-6);
  text-decoration: none;
}

.brand-logo img {
  width: 44px;
  height: 44px;
  border-radius: var(--zw-radius-md);
  border: 1px solid rgba(255, 255, 255, 0.35);
  object-fit: cover;
}

.brand-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--zw-text-inverse);
  letter-spacing: 0.02em;
}

.brand-slogan {
  margin: 0 0 var(--zw-space-3);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--zw-text-inverse);
  letter-spacing: -0.01em;
}

.brand-desc {
  margin: 0 0 var(--zw-space-6);
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.78);
}

.brand-features {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--zw-space-3);
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: var(--zw-space-2);
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.9);
}

.feat-icon {
  flex-shrink: 0;
  font-size: 15px;
  color: #6ee7b7;
}

/* ---------------- 表单区 ---------------- */
.form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: var(--zw-space-7) var(--zw-space-6);
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: var(--zw-space-2);
  margin-bottom: var(--zw-space-5);
  font-size: 16px;
  font-weight: 600;
  color: var(--zw-text-1);
  text-decoration: none;
}

.mobile-brand img {
  width: 32px;
  height: 32px;
  border-radius: var(--zw-radius-sm);
}

.form-head {
  margin-bottom: var(--zw-space-6);
}

.form-title {
  margin: 0 0 var(--zw-space-2);
  font-size: 26px;
  font-weight: 700;
  color: var(--zw-text-1);
  letter-spacing: -0.01em;
}

.form-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--zw-text-3);
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 900px) {
  .auth-card {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .brand-panel {
    display: none;
  }

  .mobile-brand {
    display: flex;
  }

  .form-panel {
    padding: var(--zw-space-6) var(--zw-space-5);
  }
}

@media (max-width: 480px) {
  #authShell {
    padding: var(--zw-space-5) var(--zw-space-3);
  }

  .form-title {
    font-size: 22px;
  }

  .form-panel {
    padding: var(--zw-space-5) var(--zw-space-4);
  }
}
</style>
