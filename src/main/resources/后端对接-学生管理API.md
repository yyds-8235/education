# 学生管理系统 - 后端接口文档

## 概述

本文档定义学生基本信息管理的后端接口规范，包括学生的增删改查、导入、权限管理等功能。

**基础路径**: `/api/students`

**认证方式**: Bearer Token（需要管理员权限）

---

## 1. 获取学生列表

### 接口信息
- **路径**: `GET /api/students`
- **权限**: 管理员
- **描述**: 分页获取学生列表，支持关键词搜索、年级筛选、分类归档筛选

### 请求参数（Query）

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| page | number | 是 | 页码，从1开始 | 1 |
| pageSize | number | 是 | 每页数量 | 10 |
| keyword | string | 否 | 关键词搜索（学号/姓名/账号） | "张三" |
| grade | string | 否 | 年级筛选 | "初一" |
| archiveFilter | string | 否 | 分类归档筛选 | "困难" 或 "funded" |

**archiveFilter 可选值**:
- `"非困难"` - 非困难学生
- `"一般困难"` - 一般困难学生
- `"困难"` - 困难学生
- `"特别困难"` - 特别困难学生
- `"funded"` - 资助对象

### 请求示例

```http
GET /api/students?page=1&pageSize=10&keyword=张三&grade=初一
Authorization: Bearer {token}
```

### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "student_001",
        "studentNo": "2024001",
        "name": "张三",
        "username": "zhangsan",
        "avatar": "https://example.com/avatar.jpg",
        "grade": "初一",
        "class": "1班",
        "guardian": "张父",
        "email": "zhangsan@example.com",
        "phone": "13800138000",
        "povertyLevel": "非困难",
        "isSponsored": false,
        "householdType": "城镇",
        "isLeftBehind": false,
        "isDisabled": false,
        "isSingleParent": false,
        "isKeyConcern": false,
        "canView": true,
        "canEdit": false,
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 2. 获取学生详情

### 接口信息
- **路径**: `GET /api/students/:id`
- **权限**: 管理员
- **描述**: 获取指定学生的详细信息

### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 学生ID |

### 请求示例

```http
GET /api/students/student_001
Authorization: Bearer {token}
```

### 响应数据

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "student_001",
    "studentNo": "2024001",
    "name": "张三",
    "username": "zhangsan",
    "avatar": "https://example.com/avatar.jpg",
    "grade": "初一",
    "class": "1班",
    "guardian": "张父",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "povertyLevel": "非困难",
    "isSponsored": false,
    "householdType": "城镇",
    "isLeftBehind": false,
    "isDisabled": false,
    "isSingleParent": false,
    "isKeyConcern": false,
    "canView": true,
    "canEdit": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

---

## 3. 创建学生

### 接口信息
- **路径**: `POST /api/students`
- **权限**: 管理员
- **描述**: 创建新学生档案

### 请求体（JSON）

```json
{
  "studentNo": "2024001",
  "name": "张三",
  "username": "zhangsan",
  "password": "123456",
  "grade": "初一",
  "class": "1班",
  "guardian": "张父",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "povertyLevel": "非困难",
  "isSponsored": false,
  "householdType": "城镇",
  "isLeftBehind": false,
  "isDisabled": false,
  "isSingleParent": false,
  "isKeyConcern": false,
  "canView": true,
  "canEdit": false
}
```

### 字段说明

| 字段名 | 类型 | 必填 | 说明 | 可选值 |
|--------|------|------|------|--------|
| studentNo | string | 是 | 学号，唯一 | - |
| name | string | 是 | 姓名 | - |
| username | string | 是 | 账号，唯一 | - |
| password | string | 是 | 密码（创建时必填） | - |
| grade | string | 是 | 年级 | "初一", "初二", "初三", "高一", "高二", "高三" |
| class | string | 是 | 班级 | "1班", "2班", "3班", "4班" |
| guardian | string | 是 | 监护人姓名 | - |
| email | string | 否 | 邮箱 | - |
| phone | string | 否 | 手机号 | - |
| povertyLevel | string | 是 | 贫困等级 | "非困难", "一般困难", "困难", "特别困难" |
| isSponsored | boolean | 是 | 是否资助对象 | true/false |
| householdType | string | 是 | 户籍类型 | "城镇", "农村" |
| isLeftBehind | boolean | 是 | 是否留守儿童 | true/false |
| isDisabled | boolean | 是 | 是否残疾 | true/false |
| isSingleParent | boolean | 是 | 是否单亲家庭 | true/false |
| isKeyConcern | boolean | 是 | 是否重点关注 | true/false |
| canView | boolean | 是 | 查看权限 | true/false |
| canEdit | boolean | 是 | 编辑权限 | true/false |

### 请求示例

```http
POST /api/students
Authorization: Bearer {token}
Content-Type: application/json

{
  "studentNo": "2024001",
  "name": "张三",
  "username": "zhangsan",
  "password": "123456",
  "grade": "初一",
  "class": "1班",
  "guardian": "张父",
  "povertyLevel": "非困难",
  "isSponsored": false,
  "householdType": "城镇",
  "isLeftBehind": false,
  "isDisabled": false,
  "isSingleParent": false,
  "isKeyConcern": false,
  "canView": true,
  "canEdit": false
}
```

### 响应数据

```json
{
  "code": 200,
  "message": "学生创建成功",
  "data": {
    "id": "student_001",
    "studentNo": "2024001",
    "name": "张三",
    "username": "zhangsan",
    "grade": "初一",
    "class": "1班",
    "guardian": "张父",
    "povertyLevel": "非困难",
    "isSponsored": false,
    "householdType": "城镇",
    "isLeftBehind": false,
    "isDisabled": false,
    "isSingleParent": false,
    "isKeyConcern": false,
    "canView": true,
    "canEdit": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "学号已存在",
  "data": null
}
```

---

## 4. 更新学生信息

### 接口信息
- **路径**: `PUT /api/students/:id`
- **权限**: 管理员
- **描述**: 更新学生基本信息（不包括密码）

### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 学生ID |

### 请求体（JSON）

```json
{
  "studentNo": "2024001",
  "name": "张三",
  "username": "zhangsan",
  "grade": "初一",
  "class": "1班",
  "guardian": "张父",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "povertyLevel": "一般困难",
  "isSponsored": true,
  "householdType": "城镇",
  "isLeftBehind": false,
  "isDisabled": false,
  "isSingleParent": false,
  "isKeyConcern": true,
  "canView": true,
  "canEdit": false
}
```

**注意**:
- 更新时不需要传递 `password` 字段
- 如需修改密码，请使用专门的密码重置接口

### 请求示例

```http
PUT /api/students/student_001
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "张三",
  "grade": "初二",
  "class": "2班",
  "povertyLevel": "一般困难",
  "isSponsored": true
}
```

### 响应数据

```json
{
  "code": 200,
  "message": "学生信息更新成功",
  "data": {
    "id": "student_001",
    "studentNo": "2024001",
    "name": "张三",
    "username": "zhangsan",
    "grade": "初二",
    "class": "2班",
    "guardian": "张父",
    "povertyLevel": "一般困难",
    "isSponsored": true,
    "householdType": "城镇",
    "isLeftBehind": false,
    "isDisabled": false,
    "isSingleParent": false,
    "isKeyConcern": false,
    "canView": true,
    "canEdit": false,
    "updatedAt": "2024-01-02T00:00:00Z"
  }
}
```

---

## 5. 删除学生

### 接口信息
- **路径**: `DELETE /api/students/:id`
- **权限**: 管理员
- **描述**: 删除学生档案（软删除或硬删除）

### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 学生ID |

### 请求示例

```http
DELETE /api/students/student_001
Authorization: Bearer {token}
```

### 响应数据

```json
{
  "code": 200,
  "message": "学生档案已删除",
  "data": null
}
```

### 错误响应

```json
{
  "code": 404,
  "message": "学生不存在",
  "data": null
}
```

---

## 6. 批量导入学生

### 接口信息
- **路径**: `POST /api/students/import`
- **权限**: 管理员
- **描述**: 通过 Excel 文件批量导入学生信息

### 请求格式
- **Content-Type**: `multipart/form-data`

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | Excel 文件（.xlsx 或 .xls） |

### Excel 文件格式要求

**必填列**:
- 学号 (studentNo)
- 姓名 (name)
- 账号 (username)
- 密码 (password)
- 年级 (grade)
- 班级 (class)
- 监护人 (guardian)
- 贫困等级 (povertyLevel)
- 户籍类型 (householdType)

**可选列**:
- 邮箱 (email)
- 手机号 (phone)
- 是否资助对象 (isSponsored): "是"/"否"
- 是否留守儿童 (isLeftBehind): "是"/"否"
- 是否残疾 (isDisabled): "是"/"否"
- 是否单亲家庭 (isSingleParent): "是"/"否"
- 是否重点关注 (isKeyConcern): "是"/"否"

**Excel 示例**:

| 学号 | 姓名 | 账号 | 密码 | 年级 | 班级 | 监护人 | 贫困等级 | 户籍类型 | 是否资助对象 |
|------|------|------|------|------|------|--------|----------|----------|--------------|
| 2024001 | 张三 | zhangsan | 123456 | 初一 | 1班 | 张父 | 非困难 | 城镇 | 否 |
| 2024002 | 李四 | lisi | 123456 | 初一 | 1班 | 李母 | 困难 | 农村 | 是 |

### 请求示例

```http
POST /api/students/import
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [Excel文件]
```

### 响应数据

```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "total": 100,
    "success": 95,
    "failed": 5,
    "errors": [
      {
        "row": 10,
        "studentNo": "2024010",
        "error": "学号已存在"
      },
      {
        "row": 25,
        "studentNo": "2024025",
        "error": "年级格式错误"
      }
    ]
  }
}
```

---

## 7. 更新学生权限

### 接口信息
- **路径**: `PATCH /api/students/:id/permissions`
- **权限**: 管理员
- **描述**: 单独更新学生的查看和编辑权限

### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 学生ID |

### 请求体（JSON）

```json
{
  "canView": true,
  "canEdit": false
}
```

### 请求示例

```http
PATCH /api/students/student_001/permissions
Authorization: Bearer {token}
Content-Type: application/json

{
  "canView": true,
  "canEdit": true
}
```

### 响应数据

```json
{
  "code": 200,
  "message": "权限更新成功",
  "data": {
    "id": "student_001",
    "canView": true,
    "canEdit": true,
    "updatedAt": "2024-01-02T00:00:00Z"
  }
}
```

---

## 8. 重置学生密码

### 接口信息
- **路径**: `POST /api/students/:id/reset-password`
- **权限**: 管理员
- **描述**: 重置学生登录密码

### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 学生ID |

### 请求体（JSON）

```json
{
  "newPassword": "newpassword123"
}
```

### 请求示例

```http
POST /api/students/student_001/reset-password
Authorization: Bearer {token}
Content-Type: application/json

{
  "newPassword": "123456"
}
```

### 响应数据

```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": null
}
```

---

## 通用错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token 无效或过期） |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如学号/账号已存在） |
| 500 | 服务器内部错误 |

---

## 数据模型

### Student 学生模型

```typescript
interface Student {
  // 基本信息
  id: string;                    // 学生ID（系统生成）
  studentNo: string;             // 学号（唯一）
  name: string;                  // 姓名
  username: string;              // 账号（唯一）
  avatar?: string;               // 头像URL
  grade: string;                 // 年级
  class: string;                 // 班级
  guardian: string;              // 监护人
  email?: string;                // 邮箱
  phone?: string;                // 手机号

  // 分类归档信息
  povertyLevel: string;          // 贫困等级
  isSponsored: boolean;          // 是否资助对象
  householdType: string;         // 户籍类型
  isLeftBehind: boolean;         // 是否留守儿童
  isDisabled: boolean;           // 是否残疾
  isSingleParent: boolean;       // 是否单亲家庭
  isKeyConcern: boolean;         // 是否重点关注

  // 权限设置
  canView: boolean;              // 查看权限
  canEdit: boolean;              // 编辑权限

  // 时间戳
  createdAt: string;             // 创建时间（ISO 8601）
  updatedAt: string;             // 更新时间（ISO 8601）
}
```

---

## 注意事项

1. **认证**: 所有接口都需要在请求头中携带有效的 Bearer Token
2. **权限**: 所有接口都需要管理员权限
3. **唯一性**: `studentNo` 和 `username` 必须全局唯一
4. **密码**: 创建时必须提供密码，更新时不能修改密码（需使用专门的重置密码接口）
5. **软删除**: 建议使用软删除，保留历史数据
6. **导入**: Excel 导入时，如果学号已存在，应跳过该行并记录错误
7. **时区**: 所有时间字段使用 ISO 8601 格式（UTC 时间）
8. **分页**: 列表接口必须支持分页，避免一次性返回大量数据

---

## 更新日志

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2024-03-11 | 初始版本，定义学生管理基本接口 |
