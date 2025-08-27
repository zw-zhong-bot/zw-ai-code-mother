# 用户头像上传API文档

## 概述

用户头像上传功能允许用户上传、删除和获取头像文件。头像文件存储在 `src/main/resources/static/ping` 目录下。

## 功能特性

- 支持多种图片格式：JPG、JPEG、PNG、GIF
- 文件大小限制：最大5MB
- 自动生成唯一文件名
- 自动更新用户头像信息
- 支持头像删除功能

## API接口

### 1. 上传用户头像

**接口地址：** `POST /api/user/upload/avatar`

**请求参数：**
- `file` (MultipartFile): 头像文件
- `userId` (Long): 用户ID

**请求示例：**
```bash
curl -X POST "http://localhost:8123/api/user/upload/avatar" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@avatar.jpg" \
  -F "userId=1"
```

**响应示例：**
```json
{
  "code": 0,
  "data": "/api/static/ping/avatar_1234567890abcdef1234567890abcdef.jpg",
  "message": "ok"
}
```

### 2. 删除用户头像

**接口地址：** `POST /api/user/delete/avatar`

**请求参数：**
- `userId` (Long): 用户ID

**请求示例：**
```bash
curl -X POST "http://localhost:8123/api/user/delete/avatar" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "userId=1"
```

**响应示例：**
```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

### 3. 获取用户头像

**接口地址：** `GET /api/user/avatar`

**请求参数：**
- `userId` (Long): 用户ID

**请求示例：**
```bash
curl -X GET "http://localhost:8123/api/user/avatar?userId=1"
```

**响应示例：**
```json
{
  "code": 0,
  "data": "/api/static/ping/avatar_1234567890abcdef1234567890abcdef.jpg",
  "message": "ok"
}
```

### 4. 访问头像文件

**访问地址：** `GET /api/static/ping/{fileName}`

**示例：**
```
http://localhost:8123/api/static/ping/avatar_1234567890abcdef1234567890abcdef.jpg
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 40000 | 请求参数错误 |
| 40400 | 请求数据不存在 |
| 50000 | 系统内部异常 |

## 文件存储规则

### 存储路径
- 物理路径：`src/main/resources/static/ping/`
- 访问路径：`/api/static/ping/`

### 文件命名规则
- 格式：`avatar_{UUID}.{扩展名}`
- 示例：`avatar_1234567890abcdef1234567890abcdef.jpg`

### 支持的文件格式
- JPG/JPEG
- PNG
- GIF

### 文件大小限制
- 最大文件大小：5MB

## 使用注意事项

1. **文件格式验证**：只支持图片格式文件
2. **文件大小限制**：单个文件不能超过5MB
3. **用户权限**：需要确保用户存在且有相应权限
4. **文件覆盖**：上传新头像会自动删除旧头像文件
5. **URL访问**：头像文件通过 `/api/static/ping/{fileName}` 访问

## 故障排除

### 常见错误及解决方案

#### 1. MultipartException: Current request is not a multipart request

**错误原因：**
- 前端请求没有正确设置 `Content-Type` 为 `multipart/form-data`
- 使用了错误的请求方式

**解决方案：**
```javascript
// 正确的请求方式
const formData = new FormData();
formData.append('file', file);
formData.append('userId', userId);

const response = await fetch('/api/user/upload/avatar', {
    method: 'POST',
    body: formData
    // 不要手动设置 Content-Type，让浏览器自动设置
});
```

#### 2. 文件上传失败

**检查项：**
- 文件大小是否超过限制（5MB）
- 文件格式是否支持（JPG、PNG、GIF）
- 用户ID是否有效
- 网络连接是否正常

#### 3. 头像无法显示

**检查项：**
- 头像URL是否正确
- 文件是否存在于服务器
- 静态资源访问是否正常
- 检查文件是否在 `src/main/resources/static/ping/` 目录中



## 前端集成示例

### JavaScript示例
```javascript
// 上传头像
async function uploadAvatar(file, userId) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('userId', userId);
  
  const response = await fetch('/api/user/upload/avatar', {
    method: 'POST',
    body: formData
  });
  
  return await response.json();
}

// 删除头像
async function deleteAvatar(userId) {
  const formData = new FormData();
  formData.append('userId', userId);
  
  const response = await fetch('/api/user/delete/avatar', {
    method: 'POST',
    body: formData
  });
  
  return await response.json();
}

// 获取头像
async function getAvatar(userId) {
  const response = await fetch(`/api/user/avatar?userId=${userId}`);
  return await response.json();
}
```

### HTML示例
```html
<!-- 头像上传表单 -->
<form id="avatarForm">
  <input type="file" name="file" accept="image/*" required>
  <input type="hidden" name="userId" value="1">
  <button type="submit">上传头像</button>
</form>

<!-- 显示头像 -->
<img id="avatar" src="" alt="用户头像">

<script>
document.getElementById('avatarForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const formData = new FormData(e.target);
  
  try {
    const result = await uploadAvatar(formData.get('file'), formData.get('userId'));
    if (result.code === 0) {
      document.getElementById('avatar').src = result.data;
    }
  } catch (error) {
    console.error('上传失败:', error);
  }
});
</script>
```
