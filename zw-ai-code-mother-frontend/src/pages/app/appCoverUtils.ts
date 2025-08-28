/**
 * 应用封面工具函数
 * @author ZW
 * @since 2024-07-18
 * @version 1.0.3
 * @modifyDate 2024-07-19
 */

/**
 * 拼接完整的封面URL
 * @param coverPath 封面路径
 * @returns 完整的封面URL
 */
export const getFullCoverUrl = (coverPath: string): string => {
  if (!coverPath) return ''

  // 如果已经是完整的URL，直接返回
  if (coverPath.startsWith('http')) {
    console.log('封面路径已是完整URL:', coverPath)
    return coverPath
  }

  // 确保路径格式正确，避免重复添加斜杠
  const baseUrl = 'http://localhost:8123/api'
  const normalizedPath = coverPath.startsWith('/') ? coverPath.substring(1) : coverPath
  const fullUrl = `${baseUrl}/${normalizedPath}`

  console.log('拼接后的封面URL:', fullUrl)
  return fullUrl
}

/**
 * 获取默认封面URL
 * @param appId 应用ID
 * @returns 默认封面URL
 */
export const getDefaultCoverUrl = (appId?: number | string): string => {
  // 使用picsum.photos提供随机图片作为占位符
  return `https://picsum.photos/80/48?random=${appId || 'default'}`
}

/**
 * 获取完整的应用封面URL，如果没有封面则返回默认封面
 * @param coverPath 封面路径
 * @param appId 应用ID
 * @returns 完整的封面URL
 */
export const getAppCoverUrl = (coverPath?: string, appId?: number | string): string => {
  if (coverPath) {
    return getFullCoverUrl(coverPath)
  }
  return getDefaultCoverUrl(appId)
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
 * @param appId 应用ID
 * @returns Promise<string> 可靠的封面URL
 */
export const getReliableAppCoverUrl = async (
  coverPath?: string,
  appId?: number | string,
): Promise<string> => {
  if (!coverPath) {
    return getDefaultCoverUrl(appId)
  }

  const fullUrl = getFullCoverUrl(coverPath)

  try {
    // 检查图片是否能正常加载
    const isValid = await checkImageUrlValidity(fullUrl)
    if (isValid) {
      return fullUrl
    } else {
      console.warn(`封面图片无法加载，使用默认封面: ${fullUrl}`)
      return getDefaultCoverUrl(appId)
    }
  } catch (error) {
    console.error('检查封面图片有效性失败:', error)
    return getDefaultCoverUrl(appId)
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
