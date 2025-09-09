# Vue项目预览优化文档

## 功能说明

在AI对话页面的"生成后的网页展示"功能中，针对Vue项目类型增加了特殊的URL处理逻辑。

## 修改内容

### 1. 导入枚举类型
在 `src/pages/app/AppChatPage.vue` 中导入了代码生成类型枚举：
```javascript
import { CodeGenTypeEnum } from '@/constants/codeGenType'
```

### 2. 预览URL生成逻辑优化
在 `setupPreviewUrl` 函数中添加了Vue项目的特殊处理：
```javascript
// 根据代码生成类型判断是否需要添加dist后缀
let generatedUrl = `${getStaticBasePath()}/${codeGenType}_${appId}`
if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
  generatedUrl += '/dist'
}
```

### 3. 调试信息显示优化
在调试信息的预期路径显示中也添加了相同的逻辑：
```javascript
(() => {
  const codeGenType = appDetail?.codeGenType
  let path = `${getStaticBasePath()}/${codeGenType}_${appId}`
  if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
    path += '/dist'
  }
  return path
})()
```

## 功能特点

1. **类型安全**: 使用了枚举类型进行判断，避免硬编码字符串
2. **向后兼容**: 不影响现有的HTML和多文件模式的预览功能
3. **调试友好**: 调试信息中也能正确显示Vue项目的预期路径
4. **代码整洁**: 没有修改已有代码的核心逻辑，只是在关键位置添加了判断

## 支持的代码生成类型

- `HTML`: 原生HTML模式 - 无后缀
- `MULTI_FILE`: 原生多文件模式 - 无后缀  
- `VUE_PROJECT`: Vue项目模式 - 添加 `/dist` 后缀

## 使用场景

当用户在AI对话页面生成Vue项目代码时，系统会自动在预览URL后添加 `/dist` 后缀，确保能够正确访问Vue项目构建后的静态文件。

## 作者信息

- 作者: ZW
- 创建时间: 2025/9/10
- 修改时间: 2025/9/10
- 版本信息: v1.0.0

## 注意事项

1. 该功能依赖于 `appDetail.codeGenType` 字段的正确设置
2. Vue项目的构建产物必须位于 `dist` 目录下
3. 后端静态文件服务需要支持访问子目录结构