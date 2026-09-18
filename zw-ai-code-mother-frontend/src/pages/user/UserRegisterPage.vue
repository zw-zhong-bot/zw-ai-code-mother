<template>
  <AuthShell title="创建账号" subtitle="注册后即可开始生成你的第一个应用">
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
      name="register"
      autocomplete="on"
      layout="vertical"
      :disabled="submitting"
      @finish="handleSubmit"
    >
      <a-form-item
        label="用户名"
        name="userAccount"
        :rules="[
          { required: true, message: '请输入用户名' },
          { min: 4, message: '用户名长度不能小于 4 位' },
        ]"
      >
        <a-input
          v-model:value="formState.userAccount"
          size="large"
          placeholder="请输入用户名（至少 4 位）"
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
          placeholder="请输入密码（至少 8 位）"
          autocomplete="new-password"
          @change="errorMessage = ''"
        >
          <template #prefix>
            <LockOutlined class="field-icon" />
          </template>
        </a-input-password>
      </a-form-item>

      <a-form-item
        label="确认密码"
        name="checkPassword"
        :rules="[
          { required: true, message: '请再次输入密码' },
          { validator: validateCheckPassword },
        ]"
      >
        <a-input-password
          v-model:value="formState.checkPassword"
          size="large"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          @change="errorMessage = ''"
        >
          <template #prefix>
            <SafetyCertificateOutlined class="field-icon" />
          </template>
        </a-input-password>
      </a-form-item>

      <a-button
        class="submit-btn"
        type="primary"
        size="large"
        html-type="submit"
        block
        :loading="submitting"
      >
        {{ submitting ? '注册中…' : '注 册' }}
      </a-button>
    </a-form>

    <p class="switch-line">
      已有账号？
      <RouterLink class="link" to="/user/login">返回登录</RouterLink>
    </p>
  </AuthShell>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons-vue'
import AuthShell from '@/components/AuthShell.vue'
import { userRegister } from '@/api/userController.ts'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/** 页面内联错误提示 */
const errorMessage = ref('')
/** 提交中状态，用于防重复提交 */
const submitting = ref(false)

/**
 * 校验两次密码是否一致
 * @param rule 校验规则
 * @param value 当前字段值
 * @param callback 回调
 */
const validateCheckPassword = (
  rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 从接口响应/异常中提取可读的错误信息
 * @param error 捕获到的异常对象
 */
const resolveErrorMessage = (error: unknown): string => {
  const fallback = '注册失败，请稍后重试'
  if (error && typeof error === 'object') {
    const res = (error as { response?: { data?: { message?: string } } }).response
    if (res?.data?.message) {
      return res.data.message
    }
  }
  return fallback
}

/**
 * 提交注册表单
 * @param values 表单值
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  if (submitting.value) {
    return
  }
  submitting.value = true
  errorMessage.value = ''

  try {
    const res = await userRegister(values)
    if (res.data.code === 0) {
      message.success('注册成功，请登录')
      await router.push({ path: '/user/login', replace: true })
    } else {
      errorMessage.value = res.data.message || '注册失败，请稍后重试'
    }
  } catch (error) {
    console.error('注册请求失败：', error)
    errorMessage.value = resolveErrorMessage(error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.error-alert {
  margin-bottom: var(--zw-space-5);
  border-radius: var(--zw-radius-md);
}

:deep(.ant-form-item) {
  margin-bottom: var(--zw-space-4);
}

:deep(.ant-form-item-label > label) {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--zw-text-2);
}

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

.submit-btn {
  height: 46px;
  margin-top: var(--zw-space-1);
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

.switch-line {
  margin: var(--zw-space-5) 0 0;
  font-size: 13.5px;
  color: var(--zw-text-3);
  text-align: center;
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

@media (max-width: 480px) {
  .submit-btn {
    height: 44px;
  }
}
</style>
