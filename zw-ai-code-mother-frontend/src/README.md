# 前端使用说明（应用生成与管理）

本说明文档概述了“AI 对话生成应用”前端的页面、路由、核心流程与接口调用方式，便于快速理解与二次开发。

## 页面结构

- 主页 `src/pages/HomePage.vue`
  - 顶部标题与提示词输入框（支持回车或按钮触发创建）。
  - 我的应用分页列表（按名称查询，分页大小 20）。
  - 精选应用分页列表（按名称查询，分页大小 20）。
- 应用生成对话页 `src/pages/app/AppChatPage.vue`
  - 顶部：左侧显示应用名称，右侧“部署”按钮。
  - 左侧：消息区（用户消息右对齐、AI 消息左对齐）+ 输入框。
  - 右侧：站点 iframe 预览（生成完成后展示 `http://localhost:8123/api/static/{codeGenType}_{appId}/`，部署后展示后端返回 URL）。
- 应用管理页（管理员） `src/pages/admin/AppManagePage.vue`
  - 列表分页查询、操作列含“编辑 / 删除 / 精选(优先级 99)”三项。
- 应用信息修改页 `src/pages/app/AppEditPage.vue`
  - 普通用户：仅可修改名称（走 `updateMyApp`）。
  - 管理员：可修改名称、封面、优先级（走 `updateApp`）。
- 应用详情页 `src/pages/app/AppDetailPage.vue`
  - 显示基本信息，提供“进入对话生成 / 编辑”入口。

## 路由

在 `src/router/index.ts` 中新增：

- `/app/chat/:id` 应用生成对话页（`USER` 权限）
- `/app/detail/:id` 应用详情（`USER` 权限）
- `/app/edit/:id` 应用信息修改（`USER` 权限，管理员可编辑更多字段）
- `/admin/appManage` 应用管理（`ADMIN` 权限）

权限常量见 `src/access/accessEnum.ts`，全局登录态在 `src/stores/loginUser.ts`。

## 核心业务流程

1. 在主页输入提示词，调用创建接口 `addApp`，成功后跳转到 `/app/chat/:id`，并以 `init` 查询参数携带初始提示词。
2. 对话页自动将初始提示词作为第一条消息发给 AI，使用 SSE 接口 `/api/app/chat/gen/code` 流式接收回复并渲染。
3. 流结束后，右侧 iframe 自动加载预览地址：`http://localhost:8123/api/static/web_{appId}/`（当前 `codeGenType` 采用 `web`）。
4. 点击“部署”按钮调用 `deployApp`，成功后使用后端返回的 URL 更新 iframe 预览。
5. 其余管理功能：
   - 我的应用列表：`listMyAppByPage`
   - 精选应用列表：`listFeaturedAppByPage`
   - 管理员分页：`listAppByPage`
   - 删除：用户 `deleteMyApp`、管理员 `deleteApp`
   - 查询详情：`getAppById`/`getAppByIdAdmin`
   - 更新：用户 `updateMyApp`、管理员 `updateApp`

## 主要接口封装

接口位于 `src/api/appController.ts`，依赖统一请求实例 `src/request.ts`：

- 创建应用：`addApp(body: API.AppAddRequest)`
- 对话生成（SSE）：`chatToGenCode(params: { appId: number; message: string })`
- 部署：`deployApp(body: { appId: number })`
- 我的应用分页：`listMyAppByPage(body: API.AppQueryRequest)`
- 精选分页：`listFeaturedAppByPage(body: API.AppQueryRequest)`
- 管理员分页：`listAppByPage(body: API.AppQueryRequest)`
- 更新（用户）：`updateMyApp(body: API.AppUpdateRequest)`
- 更新（管理员）：`updateApp(body: API.AppUpdateRequest)`
- 删除（用户）：`deleteMyApp(body: { id: number })`
- 删除（管理员）：`deleteApp(body: { id: number })`
- 查询详情：`getAppById(params: { id: number })`、`getAppByIdAdmin(params: { id: number })`

所有请求均以 `http://localhost:8123/api` 为 `baseURL`，并在 `myAxios` 响应拦截器中处理未登录跳转。

## 组件与样式说明

- 列表与表单使用 `ant-design-vue` 组件：`a-table`、`a-form`、`a-input`、`a-button`、`a-card` 等。
- 对话区将 SSE 文本进行轻量 HTML 转换，支持代码块的预格式化展示。
- 预览区域通过 `<iframe>` 加载部署或本地静态资源地址。

## 开发要点与扩展

- 若需支持更多 `codeGenType`，在创建应用与预览地址组装处同步修改。
- 对话消息持久化、历史记录、撤回/重试、文件上传等能力可在 `AppChatPage.vue` 中逐步增强。
- 管理页可进一步加入批量精选/删除、导出、列自定义显示等高级能力。

## 快速检查

- 未登录访问需要登录才能使用的路由时，会在拦截器中跳转到 `/user/login`。
- 若出现类型报错，`env.d.ts` 中提供了通用的 `.vue` 模块声明。

---

如需进一步帮助或新增页面，请在根 README 或此文档中补充需求说明。


