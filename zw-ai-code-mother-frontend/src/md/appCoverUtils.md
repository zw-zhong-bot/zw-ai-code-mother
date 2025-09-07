# 应用封面工具函数文档

## 1. 功能概述

`appCoverUtils.ts` 是一个用于处理应用封面相关操作的工具函数集合，主要提供以下功能：

- 拼接完整的封面URL（将相对路径转换为绝对路径）
- 获取默认封面URL（使用随机图片作为占位符）
- 获取应用封面URL（自动处理有封面和无封面的情况）
- 验证图片文件是否符合要求（格式和大小检查）

## 2. 函数说明

### 2.1 getFullCoverUrl

```typescript
/**
 * 拼接完整的封面URL
 * @param coverPath 封面路径
 * @returns 完整的封面URL
 */
export const getFullCoverUrl = (coverPath: string): string => {
  if (!coverPath) return ''
  if (coverPath.startsWith('http')) return coverPath
  return `http://localhost:8123${coverPath}`
}
```

**功能**：将相对路径的封面图片转换为完整的URL地址

**参数**：
- `coverPath`: 封面图片的路径，可以是相对路径或绝对URL

**返回值**：
- 完整的封面图片URL字符串

**说明**：
- 如果传入空字符串，返回空字符串
- 如果传入的路径已包含http开头（绝对URL），直接返回
- 否则将路径拼接到基础URL `http://localhost:8123` 后面

### 2.2 getDefaultCoverUrl

```typescript
/**
 * 获取默认封面URL
 * @param appId 应用ID
 * @returns 默认封面URL
 */
export const getDefaultCoverUrl = (appId?: number | string): string => {
  // 使用picsum.photos提供随机图片作为占位符
  return `https://picsum.photos/80/48?random=${appId || 'default'}`
}
```

**功能**：生成默认的封面图片URL

**参数**：
- `appId`: 可选，应用的ID，用于生成固定的随机图片

**返回值**：
- 默认封面图片的URL字符串

**说明**：
- 使用 picsum.photos 服务提供的随机图片
- 图片尺寸为 80x48 像素
- 如果提供了appId，则使用该ID作为随机种子，确保同一应用始终获得相同的默认图片

### 2.3 getAppCoverUrl

```typescript
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
```

**功能**：智能获取应用的封面URL，自动处理有封面和无封面的情况

**参数**：
- `coverPath`: 可选，应用的封面路径
- `appId`: 可选，应用的ID，用于在无封面时生成默认图片

**返回值**：
- 应用的封面图片URL字符串

**说明**：
- 如果提供了coverPath，则调用getFullCoverUrl处理
- 否则调用getDefaultCoverUrl生成默认图片URL

### 2.4 validateImageFile

```typescript
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
      message: '只能上传 JPG/PNG 格式的图片!'
    }
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    return {
      isValid: false,
      message: '图片大小不能超过 2MB!'
    }
  }
  return { isValid: true }
}
```

**功能**：验证上传的图片文件是否符合系统要求

**参数**：
- `file`: 要验证的File对象

**返回值**：
- 包含验证结果和错误消息（如果验证失败）的对象

**说明**：
- 检查文件格式是否为JPG或PNG
- 检查文件大小是否小于2MB
- 验证失败时返回详细的错误信息

## 3. 使用方法

### 3.1 在组件中引入工具函数

```typescript
import { getAppCoverUrl, validateImageFile } from './appCoverUtils'
```

### 3.2 显示应用封面

在模板中使用：

```vue
<img :src="getAppCoverUrl(app.cover, app.id)" alt="应用封面" />
```

或者在脚本中使用：

```typescript
const coverUrl = getAppCoverUrl(app.cover, app.id)
```

### 3.3 验证上传的图片文件

在上传前的验证函数中使用：

```typescript
const beforeUpload = (file: File) => {
  const validation = validateImageFile(file)
  if (!validation.isValid && validation.message) {
    message.error(validation.message)
  }
  return validation.isValid
}
```

## 4. 注意事项

1. **基础URL配置**：
   - 当前基础URL硬编码为 `http://localhost:8123`，如果后端服务地址变更，需要相应修改此配置

2. **图片尺寸**：
   - 默认图片尺寸为 80x48 像素，适合在应用列表中显示
   - 在其他场景使用时可能需要调整尺寸参数

3. **随机图片服务**：
   - 默认图片使用 picsum.photos 服务，需要确保网络连接正常
   - 如果需要离线使用，需要替换为本地图片资源

4. **文件验证规则**：
   - 当前仅允许JPG和PNG格式的图片
   - 文件大小限制为2MB
   - 如需修改规则，请直接调整validateImageFile函数

## 5. 版本信息

- **作者**：ZW
- **创建时间**：2024-07-18
- **修改时间**：2024-07-18
- **版本**：1.0.0

## 6. 相关文件

- `src/pages/app/AppEditPage.vue`：应用编辑页面，使用了本工具函数进行封面上传和显示
- `src/pages/HomePage.vue`：主页应用列表，使用了本工具函数显示应用封面