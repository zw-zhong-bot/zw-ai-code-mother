# 用户头像存储目录

此目录用于存储用户上传的头像文件。

## 存储路径
- 物理路径：`src/main/resources/static/ping/`
- 访问路径：`/api/static/ping/`

## 文件命名规则
- 格式：`avatar_{UUID}.{扩展名}`
- 示例：`avatar_1234567890abcdef1234567890abcdef.jpg`

## 支持的文件格式
- JPG/JPEG
- PNG
- GIF

## 文件大小限制
- 最大文件大小：5MB

## 访问URL格式
- 格式：`http://localhost:8123/api/static/ping/{fileName}`
- 示例：`http://localhost:8123/api/static/ping/avatar_1234567890abcdef1234567890abcdef.jpg`

## API接口

### 上传头像
- **POST** `/api/user/upload/avatar`
- 参数：
  - `file`: 头像文件（MultipartFile）
  - `userId`: 用户ID（Long）

### 删除头像
- **POST** `/api/user/delete/avatar`
- 参数：
  - `userId`: 用户ID（Long）

### 获取头像
- **GET** `/api/user/avatar`
- 参数：
  - `userId`: 用户ID（Long）
