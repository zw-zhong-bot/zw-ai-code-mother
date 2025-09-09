# 对话管理页面说明文档

## 文件信息

- **文件名**: ChatHistoryManagePage.vue
- **作者**: ZW
- **创建时间**: 2025-01-08
- **修改时间**: 2025-01-08
- **版本**: 1.0.0

## 功能概述

对话管理页面是一个管理员专用的功能模块，用于管理和查看系统中所有的对话历史记录。该页面提供了完整的对话记录管理功能，包括搜索、筛选、查看详情和删除操作。

## 主要功能

### 1. 对话记录查询

- **应用名称搜索**: 支持按应用名称进行模糊搜索
- **消息内容搜索**: 支持按消息内容进行模糊搜索
- **消息类型筛选**: 可筛选用户消息或AI消息
- **应用ID搜索**: 支持按具体应用ID进行精确搜索

### 2. 数据展示

- **表格形式展示**: 使用Ant Design Vue的Table组件展示对话记录
- **分页功能**: 支持分页浏览，默认每页显示10条记录
- **字段展示**: 包含ID、应用名称、消息内容、消息类型、状态、消息顺序、创建时间等字段

### 3. 消息详情查看

- **详情弹窗**: 点击"详情"按钮可查看完整的消息信息
- **Markdown渲染**: 支持Markdown格式的消息内容渲染
- **代码高亮**: 集成highlight.js实现代码语法高亮
- **错误信息展示**: 显示消息的错误信息和错误代码（如有）

### 4. 操作功能

- **删除记录**: 支持删除单条对话记录（需确认）
- **搜索重置**: 一键重置所有搜索条件

## 技术实现

### 技术栈

- **Vue 3**: 使用Composition API
- **TypeScript**: 提供类型安全
- **Ant Design Vue**: UI组件库
- **Markdown-it**: Markdown解析和渲染
- **highlight.js**: 代码语法高亮

### 核心组件

- `a-form`: 搜索表单
- `a-table`: 数据表格
- `a-modal`: 消息详情弹窗
- `a-tag`: 状态和类型标签

### API接口

- `listChatHistoryByPage`: 分页查询对话历史记录

## 数据结构

### 查询参数 (ChatHistoryQueryRequest)

```typescript
{
  pageNum: number        // 页码
  pageSize: number       // 每页大小
  appName?: string       // 应用名称
  message?: string       // 消息内容
  messageType?: string   // 消息类型 (user/ai)
  appId?: number         // 应用ID
}
```

### 对话记录 (ChatHistory)

```typescript
{
  id: number             // 消息ID
  message: string        // 消息内容
  messageType: string    // 消息类型
  appId: number          // 应用ID
  userId: number         // 用户ID
  parentId?: number      // 父消息ID
  errorMessage?: string  // 错误信息
  errorCode?: string     // 错误代码
  status: number         // 状态
  messageOrder: number   // 消息顺序
  createTime: string     // 创建时间
  updateTime: string     // 更新时间
}
```

## 样式特性

### 响应式设计

- 表格支持横向滚动
- 弹窗内容支持纵向滚动
- 适配不同屏幕尺寸

### 交互效果

- 按钮悬停效果
- 表格行悬停高亮
- 加载状态提示

### Markdown样式

- 标题层级样式
- 代码块语法高亮
- 引用块样式
- 链接样式

## 使用方法

### 访问路径

```
/admin/chatHistoryManage
```

### 权限要求

- 需要管理员权限 (ACCESS_ENUM.ADMIN)

### 操作流程

1. 进入页面后自动加载对话记录
2. 使用搜索表单筛选数据
3. 点击"详情"查看完整消息内容
4. 点击"删除"删除不需要的记录

## 注意事项

### 性能优化

- 使用分页加载避免一次性加载大量数据
- 消息内容超长时进行截断显示
- 详情弹窗内容支持滚动

### 安全考虑

- 删除操作需要二次确认
- 仅管理员可访问此页面
- 错误信息安全展示

### 扩展性

- 支持添加更多搜索条件
- 可扩展批量操作功能
- 支持导出功能扩展

## 待完善功能

- 删除接口对接（当前为占位实现）
- 批量删除功能
- 数据导出功能
- 消息统计功能

## 更新日志

- **v1.0.0** (2025-01-08): 初始版本，实现基础的对话管理功能
