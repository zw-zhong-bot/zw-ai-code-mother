<template>
  <AuthShell title="欢迎回来" subtitle="登录后即可继续创建和迭代你的应用">
    <a-alert
      v-if="errorMessage"
      class="error-alert"
      type="error"
      show-icon
      :message="errorMessage"
      role="alert"
      aria-live="assertive"
    />

    <a-form
      :model="formState"
      name="login"
      autocomplete="on"
      layout="vertical"
      :disabled="submitting"
      @finish="handleSubmit"
    >
      <a-form-item
        label="用户名 / 邮箱"
        name="userAccount"
        :rules="[{ required: true, message: '请输入用户名或邮箱' }]"
      >
        <a-input
          v-model:value="formState.userAccount"
          size="large"
          placeholder="请输入用户名或邮箱"
          autocomplete="username"
          allow-clear
          @change="errorMessage = ''"
        >
          <template #prefix>
            <UserOutlined class="field-icon" />
          </template>
        </a-input>
      </a-form-item>

      <a-form-item
        label="密码"
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
        ]"
      >
        <a-input-password
          v-model:value="formState.userPassword"
          size="large"
          placeholder="请输入密码"
          autocomplete="current-password"
          @change="errorMessage = ''"
        >
          <template #prefix>
            <LockOutlined class="field-icon" />
          </template>
        </a-input-password>
      </a-form-item>

      <div class="form-meta">
        <a-checkbox v-model:checked="rememberAccount">记住账号</a-checkbox>
        <RouterLink class="link" to="/user/register">没有账号？立即注册</RouterLink>
      </div>

      <a-button
        class="submit-btn"
        type="primary"
        size="large"
        html-type="submit"
        block
        :loading="submitting"
      >
        {{ submitting ? '登录中…' : '登 录' }}
      </a-button>
    </a-form>

    <p class="footnote">
      登录即表示你同意本平台的服务条款与隐私政策，我们会妥善保护你的账号信息。
    </p>
  </AuthShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import AuthShell from '@/components/AuthShell.vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

/** localStorage 中保存「记住的账号」的键名 */
const REMEMBER_KEY = 'zw_remember_account'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

/** 记住账号（仅保存账号，不保存密码，详见实现说明） */
const rememberAccount = ref(false)
/** 页面内联错误提示 */
const errorMessage = ref('')
/** 提交中状态，用于防重复提交 */
const submitting = ref(false)

/** 初始化：回填上次记住的账号 */
onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    formState.userAccount = saved
    rememberAccount.value = true
  }
})

/**
 * 从接口响应/异常中提取可读的错误信息
 * @param error 捕获到的异常对象
 */
const resolveErrorMessage = (error: unknown): string => {
  const fallback = '登录失败，请稍后重试'
  if (error && typeof error === 'object') {
    const res = (error as { response?: { data?: { message?: string } } }).response
    if (res?.data?.message) {
      return res.data.message
    }
  }
  return fallback
}

/**
 * 提交登录表单
 * @param values 表单值
 */
const handleSubmit = async (values: API.UserLoginRequest) => {
  if (submitting.value) {
    return
  }
  submitting.value = true
  errorMessage.value = ''

  try {
    const res = await userLogin(values)

    if (res.data.code === 0 && res.data.data) {
      // 处理「记住账号」的持久化
      if (rememberAccount.value) {
        localStorage.setItem(REMEMBER_KEY, values.userAccount)
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }

      // 同步全局登录态
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')

      // 支持拦截器写入的 redirect 参数，登录后回到原页面
      const redirect = route.query.redirect
      const target = typeof redirect === 'string' && redirect ? redirect : '/'
      await router.push({ path: target, replace: true })
    } else {
      errorMessage.value = res.data.message || '登录失败，请检查用户名和密码'
    }
  } catch (error) {
    console.error('登录请求失败：', error)
    errorMessage.value = resolveErrorMessage(error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* 内联错误提示 */
.error-alert {
  margin-bottom: var(--zw-space-5);
  border-radius: var(--zw-radius-md);
}

/* 表单标签与控件间距 */
:deep(.ant-form-item) {
  margin-bottom: var(--zw-space-4);
}

:deep(.ant-form-item-label > label) {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--zw-text-2);
}

/* 输入框：统一圆角、描边与聚焦态 */
:deep(.ant-input-affix-wrapper),
:deep(.ant-input) {
  border-radius: var(--zw-radius-md);
  padding-top: 9px;
  padding-bottom: 9px;
  transition: var(--zw-transition);
}

:deep(.ant-input-affix-wrapper:hover),
:deep(.ant-input:hover) {
  border-color: var(--zw-primary);
}

:deep(.ant-input-affix-wrapper-focused),
:deep(.ant-input:focus) {
  border-color: var(--zw-primary);
  box-shadow: 0 0 0 3px var(--zw-primary-ring);
}

.field-icon {
  color: var(--zw-text-4);
  transition: color 0.2s var(--zw-ease);
}

:deep(.ant-input-affix-wrapper-focused) .field-icon {
  color: var(--zw-primary);
}

/* 记住账号 + 注册链接 */
.form-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zw-space-3);
  margin-bottom: var(--zw-space-5);
  font-size: 13.5px;
}

.form-meta :deep(.ant-checkbox-wrapper) {
  color: var(--zw-text-2);
}

.link {
  color: var(--zw-primary);
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s var(--zw-ease);
}

.link:hover {
  color: var(--zw-primary-hover);
  text-decoration: underline;
}

/* 主按钮：品牌渐变 + 悬浮抬升 */
.submit-btn {
  height: 46px;
  border: none;
  border-radius: var(--zw-radius-md);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.12em;
  background: var(--zw-gradient-brand);
  box-shadow: var(--zw-shadow-brand);
  transition: var(--zw-transition);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 16px 32px rgba(59, 130, 246, 0.34);
  filter: brightness(1.04);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
  filter: brightness(0.97);
}

/* 底部说明 */
.footnote {
  margin: var(--zw-space-5) 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--zw-text-4);
  text-align: center;
}

/* 移动端微调 */
@media (max-width: 480px) {
  .form-meta {
    font-size: 13px;
  }

  .submit-btn {
    height: 44px;
  }
}
</style>
