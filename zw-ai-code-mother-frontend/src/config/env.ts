/**
 * 环境配置
 */

import { CodeGenTypeEnum } from '@/constants/codeGenType'
// 基础 API 地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8123/api'

// 应用部署域名
export const DEPLOY_HOST = import.meta.env.VITE_DEPLOY_HOST ?? 'http://localhost'

// 应用生成预览域名
export const PREVIEW_HOST = import.meta.env.VITE_PREVIEW_HOST ?? 'http://localhost:8123'

/**
 * 静态资源基础路径（走预览域名）
 * 例如：PREVIEW_HOST=http://localhost:8123 -> http://localhost:8123/api/static
 */
export const STATIC_BASE_PATH = `${PREVIEW_HOST}/api/static`

/**
 * 获取 API 完整地址
 * @param path
 */
export const getApiUrl = (path: string) => {
  return `${API_BASE_URL}${path}`
}

/**
 * 获取应用预览地址
 * @param id
 */
export const getAppPreviewUrl = (id: string) => {
  return `${PREVIEW_HOST}/app/view/${id}`
}

/**
 * 获取应用部署地址
 * @param deployKey
 */
export const getAppDeployUrl = (deployKey: string) => {
  return `${DEPLOY_HOST}/${deployKey}`
}

/**
 * 获取完整的静态资源地址
 * @param path
 */
export const getFullResourceUrl = (path?: string) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  // 兼容已带 /api/static 或 /static 前缀的路径，避免重复拼接
  const trimmed = path.replace(/^\/+/, '')
  const normalized = trimmed.replace(/^(api\/static\/|static\/)/, '')
  return `${STATIC_BASE_PATH}/${normalized}`
}

/**
 * 获取静态资源基础路径
 */
export const getStaticBasePath = () => {
  return STATIC_BASE_PATH
}
