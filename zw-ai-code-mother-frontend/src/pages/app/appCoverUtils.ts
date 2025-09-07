/**
 * 应用封面工具函数
 * @author ZW
 * @since 2024-07-18
 * @version 1.0.3
 * @modifyDate 2024-07-19
 */

import { getFullResourceUrl } from '@/config/env'

/**
 * 拼接完整的封面URL
 * @param coverPath 封面路径
 * @returns 完整的封面URL
 */
export const getFullCoverUrl = (coverPath: string): string => {
  if (!coverPath) return ''
  return getFullResourceUrl(coverPath)
}

/**
 * 获取默认封面URL
 * @returns 默认封面URL
 */
export const getDefaultCoverUrl = (): string => {
  // 使用本地图片作为默认封面
  return '/src/assets/ping/touxiang.jpg'
}

/**
 * 获取完整的应用封面URL，如果没有封面则返回默认封面
 * @param coverPath 封面路径
 * @returns 完整的封面URL
 */
export const getAppCoverUrl = (coverPath?: string): string => {
  if (coverPath) {
    return getFullCoverUrl(coverPath)
  }
  return getDefaultCoverUrl()
}

/**
 * 检查图片URL是否有效（是否能成功加载）
 * @param url 图片URL
 * @param timeout 超时时间（毫秒）
 * @returns Promise<boolean> 是否有效
 */
export const checkImageUrlValidity = async (
  url: string,
  timeout: number = 3000,
): Promise<boolean> => {
  return new Promise((resolve) => {
    const img = new Image()
    const timer = setTimeout(() => {
      img.src = ''
      resolve(false)
    }, timeout)

    img.onload = () => {
      clearTimeout(timer)
      resolve(true)
    }

    img.onerror = () => {
      clearTimeout(timer)
      resolve(false)
    }

    img.src = url
  })
}

/**
 * 获取可靠的应用封面URL，如果指定URL无效则返回默认封面
 * @param coverPath 封面路径
 * @returns Promise<string> 可靠的封面URL
 */
export const getReliableAppCoverUrl = async (coverPath?: string): Promise<string> => {
  if (!coverPath) {
    return getDefaultCoverUrl()
  }

  const fullUrl = getFullCoverUrl(coverPath)

  try {
    // 检查图片是否能正常加载
    const isValid = await checkImageUrlValidity(fullUrl)
    if (isValid) {
      return fullUrl
    } else {
      console.warn(`封面图片无法加载，使用默认封面: ${fullUrl}`)
      return getDefaultCoverUrl()
    }
  } catch (error) {
    console.error('检查封面图片有效性失败:', error)
    return getDefaultCoverUrl()
  }
}

/**
 * 验证图片文件是否符合要求
 * @param file 图片文件
 * @returns 验证结果对象
 */
export const validateImageFile = (file: File): { isValid: boolean; message?: string } => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    return {
      isValid: false,
      message: '只能上传 JPG/PNG 格式的图片!',
    }
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    return {
      isValid: false,
      message: '图片大小不能超过 2MB!',
    }
  }
  return { isValid: true }
}
