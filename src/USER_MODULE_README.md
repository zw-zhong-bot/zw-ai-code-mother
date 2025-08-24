# User 模块功能说明

## 概述
User 模块提供了完整的用户管理功能，包括用户注册、登录、注销、用户信息管理以及管理员用户管理。

## 功能列表

### 用户功能
1. **用户注册** - 用户注册新账号
2. **用户登录** - 用户登录系统
3. **用户注销** - 用户退出登录
4. **获取当前登录用户** - 获取当前登录用户信息
5. **更新用户信息** - 用户更新个人信息
6. **根据id获取用户信息** - 根据用户id获取用户信息（脱敏版本）

### 管理员功能
1. **创建用户** - 管理员创建新用户
2. **删除用户** - 管理员删除用户
3. **分页查询用户列表** - 管理员分页查询用户列表
4. **根据id获取用户详情** - 管理员根据id获取用户完整信息

## API 接口

### 用户接口

#### 1. 用户注册
- **URL**: `POST /user/register`
- **权限**: 公开接口
- **参数**: 
  ```json
  {
    "userAccount": "用户账号",
    "userPassword": "用户密码",
    "checkPassword": "确认密码"
  }
  ```
- **返回**: 用户ID

#### 2. 用户登录
- **URL**: `POST /user/login`
- **权限**: 公开接口
- **参数**:
  ```json
  {
    "userAccount": "用户账号",
    "userPassword": "用户密码"
  }
  ```
- **返回**: 脱敏后的用户登录信息

#### 3. 用户注销
- **URL**: `POST /user/logout`
- **权限**: 需要登录
- **参数**: 无
- **返回**: 注销结果

#### 4. 获取当前登录用户
- **URL**: `GET /user/get/login`
- **权限**: 需要登录
- **参数**: 无
- **返回**: 当前登录用户信息（脱敏）

#### 5. 更新用户信息
- **URL**: `POST /user/update`
- **权限**: 需要登录
- **参数**:
  ```json
  {
    "id": 1,
    "userName": "用户昵称",
    "userAvatar": "用户头像",
    "userProfile": "用户简介"
  }
  ```

#### 6. 根据id获取用户信息
- **URL**: `GET /user/get/vo?id=1`
- **权限**: 公开接口
- **参数**: id (用户ID)
- **返回**: 用户信息（脱敏版本）

### 管理员接口

#### 1. 创建用户
- **URL**: `POST /user/add`
- **权限**: 需要管理员权限
- **参数**:
  ```json
  {
    "userAccount": "用户账号",
    "userName": "用户昵称",
    "userAvatar": "用户头像",
    "userProfile": "用户简介",
    "userRole": "用户角色"
  }
  ```
- **说明**: 默认密码为 123456789

#### 2. 删除用户
- **URL**: `POST /user/delete`
- **权限**: 需要管理员权限
- **参数**:
  ```json
  {
    "id": 1
  }
  ```

#### 3. 分页查询用户列表
- **URL**: `POST /user/list/page/vo`
- **权限**: 需要管理员权限
- **参数**:
  ```json
  {
    "pageNum": 1,
    "pageSize": 10,
    "userName": "用户昵称",
    "userAccount": "用户账号",
    "userProfile": "用户简介",
    "userRole": "用户角色"
  }
  ```

#### 4. 根据id获取用户详情（管理员）
- **URL**: `GET /user/get?id=1`
- **权限**: 需要管理员权限
- **参数**: id (用户ID)
- **返回**: 用户完整信息（包含敏感信息）

## 数据模型

### User 实体
```java
public class User {
    private Long id;                    // 用户ID
    private String userAccount;         // 用户账号
    private String userPassword;        // 用户密码（加密）
    private String userName;            // 用户昵称
    private String userAvatar;          // 用户头像
    private String userProfile;         // 用户简介
    private String userRole;            // 用户角色
    private LocalDateTime editTime;     // 编辑时间
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime updateTime;   // 更新时间
    private Integer isDelete;           // 是否删除
    private LocalDateTime vipExpireTime; // 会员过期时间
    private String vipCode;             // 会员兑换码
    private Long vipNumber;             // 会员编号
    private String shareCode;           // 分享码
    private Long inviteUser;            // 邀请用户id
}
```

### UserVO 视图对象
UserVO 是 User 实体的脱敏版本，不包含密码等敏感信息。

### LoginUserVO 登录用户视图对象
LoginUserVO 是专门用于登录后返回的用户信息，包含登录状态相关信息。

## 权限控制

1. **公开接口**: 用户注册、用户登录、根据id获取用户信息
2. **登录权限**: 用户注销、获取当前登录用户、更新用户信息
3. **管理员权限**: 创建用户、删除用户、分页查询用户列表、获取用户详情

## 业务规则

1. **注册规则**:
   - 用户账号长度至少4位
   - 用户密码长度8-16位
   - 两次输入密码必须一致
   - 用户账号不能重复

2. **登录规则**:
   - 用户账号长度至少4位
   - 用户密码长度至少8位
   - 登录成功后会将用户信息存储在Session中

3. **密码安全**:
   - 密码使用MD5加密存储（加盐）
   - 盐值为 "zwpass"

4. **用户角色**:
   - user: 普通用户
   - admin: 管理员

5. **数据脱敏**:
   - 返回给前端的用户信息不包含密码等敏感信息
   - 管理员可以查看用户的完整信息

## 会话管理

1. **登录状态**: 使用Session存储用户登录状态
2. **会话常量**: `USER_LOGIN_STATE` 用于标识登录状态
3. **会话验证**: 每次需要登录的接口都会验证Session中的用户信息

## 错误处理

1. **参数错误**: 请求参数为空或格式错误
2. **未登录错误**: 用户未登录但访问需要登录的接口
3. **无权限错误**: 用户权限不足
4. **操作失败**: 数据库操作失败
5. **系统错误**: 系统内部异常
