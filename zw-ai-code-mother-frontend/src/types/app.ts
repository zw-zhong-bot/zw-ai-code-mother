import { CodeGenTypeEnum } from '@/constants/codeGenType'

/**
 * 应用信息接口
 */
export interface AppInfo {
  id: number
  appName: string
  appDesc?: string
  appIcon?: string
  appType: number
  scoringStrategy: number
  reviewStatus: number
  reviewMessage?: string
  reviewerId?: number
  reviewTime?: string
  userId: number
  userName?: string
  userAvatar?: string
  createTime: string
  updateTime: string
  isDelete: number
  priority: number
  deployTime?: string
  initialPrompt?: string
  codeGenType: CodeGenTypeEnum
}

/**
 * 应用搜索参数
 */
export interface AppSearchParams {
  appName?: string
  userId?: string
  codeGenType?: CodeGenTypeEnum
  current: number
  pageSize: number
}

/**
 * 应用列表响应
 */
export interface AppListResponse {
  records: AppInfo[]
  total: number
  current: number
  size: number
}
