/**
 * 图片处理工具模块
 * 统一管理项目中所有图片的访问路径和处理逻辑
 * @author ZW
 * @since 2024-12-07
 * @version 1.1.0
 * @modifyDate 2025-09-08
 */

import { getFullResourceUrl, API_BASE_URL } from '@/config/env'

/**
 * 图片类型枚举
 */
export enum ImageType {
  /** 用户头像文件 */
  USER_AVATAR_FILE = 'user_avatar_file',
  /** 用户头像API */
  USER_AVATAR_API = 'user_avatar_api',
  /** 应用封面 */
  APP_COVER = 'app_cover',
  /** 默认头像 */
  DEFAULT_AVATAR = 'default_avatar',
  /** 默认封面 */
  DEFAULT_COVER = 'default_cover',
}

/**
 * 默认图片路径配置
 */
const DEFAULT_IMAGES = {
  avatar: '/src/assets/ping/touxiang.jpg',
  cover: '/src/assets/ping/touxiang.jpg', // 可以替换为默认封面图片
} as const

/**
 * 图片缓存管理
 */
class ImageCache {
  private cache = new Map<string, string>()
  private loadingPromises = new Map<string, Promise<string>>()

  /**
   * 获取缓存的图片URL
   */
  get(key: string): string | undefined {
    return this.cache.get(key)
  }

  /**
   * 设置缓存的图片URL
   */
  set(key: string, url: string): void {
    this.cache.set(key, url)
  }

  /**
   * 清除指定缓存
   */
  delete(key: string): void {
    this.cache.delete(key)
    this.loadingPromises.delete(key)
  }

  /**
   * 清除所有缓存
   */
  clear(): void {
    this.cache.clear()
    this.loadingPromises.clear()
  }

  /**
   * 获取或创建加载Promise
   */
  getOrCreateLoadingPromise(key: string, loader: () => Promise<string>): Promise<string> {
    if (this.loadingPromises.has(key)) {
      return this.loadingPromises.get(key)!
    }

    const promise = loader().finally(() => {
      this.loadingPromises.delete(key)
    })

    this.loadingPromises.set(key, promise)
    return promise
  }
}

// 全局图片缓存实例
const imageCache = new ImageCache()

/**
 * 检查图片URL是否有效
 * @param url 图片URL
 * @param timeout 超时时间（毫秒）
 * @returns Promise<boolean> 是否有效
 */
export const checkImageValidity = (url: string, timeout: number = 3000): Promise<boolean> => {
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
 * 获取用户头像URL（通过API端点）
 * 用于动态生成的头像或通过API获取的头像
 * @param userId 用户ID
 * @returns 头像URL
 */
export const getUserAvatarApiUrl = (userId: number | string): string => {
  if (!userId) {
    return DEFAULT_IMAGES.avatar
  }
  // 用户头像API端点，不使用静态资源路径
  return `${API_BASE_URL}/user/avatar?userId=${userId}&t=${Date.now()}`
}

/**
 * 获取用户头像文件URL（静态资源）
 * 用于用户上传的头像文件
 * @param avatarPath 头像文件路径
 * @returns 头像URL
 */
export const getUserAvatarFileUrl = (avatarPath: string): string => {
  if (!avatarPath) {
    return DEFAULT_IMAGES.avatar
  }

  // 如果已经是完整URL，直接返回
  if (avatarPath.startsWith('http://') || avatarPath.startsWith('https://')) {
    return avatarPath
  }

  // 使用静态资源基础路径
  return getFullResourceUrl(avatarPath)
}

/**
 * 获取应用封面URL
 * @param coverPath 封面路径
 * @returns 封面URL
 */
export const getAppCoverUrl = (coverPath?: string): string => {
  if (!coverPath) {
    return DEFAULT_IMAGES.cover
  }

  // 如果已经是完整URL，直接返回
  if (coverPath.startsWith('http://') || coverPath.startsWith('https://')) {
    return coverPath
  }

  // 使用静态资源基础路径
  return getFullResourceUrl(coverPath)
}

/**
 * 获取默认头像URL
 * @returns 默认头像URL
 */
export const getDefaultAvatarUrl = (): string => {
  return DEFAULT_IMAGES.avatar
}

/**
 * 获取默认封面URL
 * @returns 默认封面URL
 */
export const getDefaultCoverUrl = (): string => {
  return DEFAULT_IMAGES.cover
}

/**
 * 获取可靠的用户头像URL（带缓存和错误处理）
 * @param userId 用户ID
 * @param avatarPath 头像文件路径（可选）
 * @returns Promise<string> 可靠的头像URL
 */
export const getReliableUserAvatarUrl = async (
  userId?: number | string,
  avatarPath?: string,
): Promise<string> => {
  // 如果有头像文件路径，优先使用文件
  if (avatarPath) {
    const fileUrl = getUserAvatarFileUrl(avatarPath)
    const cacheKey = `avatar_file_${avatarPath}`

    // 检查缓存
    const cached = imageCache.get(cacheKey)
    if (cached) {
      return cached
    }

    // 检查图片有效性
    return imageCache.getOrCreateLoadingPromise(cacheKey, async () => {
      const isValid = await checkImageValidity(fileUrl)
      const result = isValid ? fileUrl : DEFAULT_IMAGES.avatar
      imageCache.set(cacheKey, result)
      return result
    })
  }

  // 如果有用户ID，使用API端点
  if (userId) {
    const apiUrl = getUserAvatarApiUrl(userId)
    const cacheKey = `avatar_api_${userId}`

    // 检查缓存
    const cached = imageCache.get(cacheKey)
    if (cached) {
      return cached
    }

    // 检查图片有效性
    return imageCache.getOrCreateLoadingPromise(cacheKey, async () => {
      const isValid = await checkImageValidity(apiUrl)
      const result = isValid ? apiUrl : DEFAULT_IMAGES.avatar
      imageCache.set(cacheKey, result)
      return result
    })
  }

  // 都没有，返回默认头像
  return DEFAULT_IMAGES.avatar
}

/**
 * 获取可靠的应用封面URL（带缓存和错误处理）
 * @param coverPath 封面路径
 * @returns Promise<string> 可靠的封面URL
 */
export const getReliableAppCoverUrl = async (coverPath?: string): Promise<string> => {
  if (!coverPath) {
    return DEFAULT_IMAGES.cover
  }

  const coverUrl = getAppCoverUrl(coverPath)
  const cacheKey = `cover_${coverPath}`

  // 检查缓存
  const cached = imageCache.get(cacheKey)
  if (cached) {
    return cached
  }

  // 检查图片有效性
  return imageCache.getOrCreateLoadingPromise(cacheKey, async () => {
    const isValid = await checkImageValidity(coverUrl)
    const result = isValid ? coverUrl : DEFAULT_IMAGES.cover
    imageCache.set(cacheKey, result)
    return result
  })
}

/**
 * 处理图片加载错误的通用函数
 * @param event 错误事件
 * @param imageType 图片类型
 * @param fallbackUrl 备用URL（可选）
 */
export const handleImageError = (
  event: Event,
  imageType: ImageType,
  fallbackUrl?: string,
): void => {
  const imgElement = event.target as HTMLImageElement

  switch (imageType) {
    case ImageType.USER_AVATAR_FILE:
    case ImageType.USER_AVATAR_API:
    case ImageType.DEFAULT_AVATAR:
      imgElement.src = fallbackUrl || DEFAULT_IMAGES.avatar
      imgElement.alt = '默认头像'
      break

    case ImageType.APP_COVER:
    case ImageType.DEFAULT_COVER:
      imgElement.src = fallbackUrl || DEFAULT_IMAGES.cover
      imgElement.alt = '默认封面'
      break

    default:
      console.warn('未知的图片类型:', imageType)
      imgElement.src = fallbackUrl || DEFAULT_IMAGES.avatar
      imgElement.alt = '默认图片'
  }
}

/**
 * 验证上传的图片文件
 * @param file 图片文件
 * @returns 验证结果
 */
export const validateImageFile = (file: File): { isValid: boolean; message?: string } => {
  // 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    return {
      isValid: false,
      message: '只能上传 JPG、PNG、GIF、WebP 格式的图片！',
    }
  }

  // 检查文件大小（2MB）
  const maxSize = 2 * 1024 * 1024
  if (file.size > maxSize) {
    return {
      isValid: false,
      message: '图片大小不能超过 2MB！',
    }
  }

  return { isValid: true }
}

/**
 * 清除图片缓存
 * @param pattern 缓存键模式（可选）
 */
export const clearImageCache = (pattern?: string): void => {
  if (pattern) {
    // 清除匹配模式的缓存
    const keys = Array.from(imageCache['cache'].keys())
    keys.forEach((key) => {
      if (key.includes(pattern)) {
        imageCache.delete(key)
      }
    })
  } else {
    // 清除所有缓存
    imageCache.clear()
  }
}
