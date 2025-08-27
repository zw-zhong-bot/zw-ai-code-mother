# 头像上传功能修复说明

## 问题描述

用户在使用头像上传功能时遇到以下错误：
```
org.springframework.web.multipart.MultipartException: Current request is not a multipart request
```

## 问题原因分析

1. **前端请求格式问题**：请求没有正确设置为multipart格式
2. **后端参数绑定问题**：@RequestParam注解配置不当
3. **配置文件不完整**：缺少详细的multipart配置

## 修复内容

### 1. 后端接口优化

#### UserController.java
- 修改了头像上传接口的参数绑定方式
- 添加了更详细的错误处理
- 新增了测试上传接口

```java
@PostMapping("/upload/avatar")
public BaseResponse<String> uploadUserAvatar(
    @RequestParam(value = "file", required = false) MultipartFile file,
    @RequestParam(value = "userId", required = false) Long userId) {
    // 详细的参数验证和错误处理
}
```

### 2. 配置文件完善

#### application.yml
- 添加了详细的multipart配置
- 设置了文件大小阈值和临时目录

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
      enabled: true
      file-size-threshold: 2KB
      location: ${java.io.tmpdir}
      resolve-lazily: false
```

### 3. 新增配置类

#### FileUploadConfig.java
- 创建了专门的文件上传配置类
- 配置了MultipartResolver和MultipartConfigElement



## 使用方法

### 1. 前端正确调用方式

```javascript
// 正确的请求方式
const formData = new FormData();
formData.append('file', file);
formData.append('userId', userId);

const response = await fetch('/api/user/upload/avatar', {
    method: 'POST',
    body: formData
    // 不要手动设置 Content-Type
});
```

### 2. 测试方法

#### 使用curl
```bash
curl -X POST "http://localhost:8123/api/user/upload/avatar" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@avatar.jpg" \
  -F "userId=1"
```

## 常见问题解决

### 1. 仍然出现MultipartException

**检查项：**
- 确保前端使用FormData对象
- 不要手动设置Content-Type头
- 确保文件大小在限制范围内

### 2. 文件上传失败

**检查项：**
- 文件格式是否支持（JPG、PNG、GIF）
- 文件大小是否超过5MB
- 用户ID是否有效

### 3. 头像无法显示

**检查项：**
- 检查文件是否成功保存到 `src/main/resources/static/ping/` 目录
- 确认静态资源访问路径是否正确
- 检查文件权限

## API接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传头像 | POST | `/api/user/upload/avatar` | 上传用户头像 |
| 删除头像 | POST | `/api/user/delete/avatar` | 删除用户头像 |
| 获取头像 | GET | `/api/user/avatar` | 获取用户头像URL |

## 文件结构

```
src/
├── main/
│   ├── java/
│   │   └── com/zw/zwaicodemother/
│   │       ├── config/
│   │       │   └── FileUploadConfig.java          # 新增：文件上传配置
│   │       └── controller/
│   │           └── UserController.java            # 修改：优化头像上传接口
│   └── resources/
│       ├── static/
│       │   └── ping/                              # 头像存储目录
│       │       └── README.md
│       └── application.yml                        # 修改：完善multipart配置
```

## 验证步骤

1. 启动应用程序
2. 使用curl或其他工具测试头像上传功能
3. 检查文件是否成功保存到 `src/main/resources/static/ping/` 目录
4. 验证头像访问URL是否正常

## 注意事项

1. 确保应用程序有写入 `src/main/resources/static/ping/` 目录的权限
2. 生产环境建议将文件存储路径配置为绝对路径
3. 定期清理临时文件目录
4. 考虑添加文件类型和内容的双重验证
