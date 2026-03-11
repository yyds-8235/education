# 后端对接-教务处人员管理（教师与学生）API

本文档对应当前前端人员管理模块，覆盖以下页面：

- `src/pages/Personnel/PersonnelList.tsx`
- `src/pages/Personnel/PersonnelDetail.tsx`
- `src/pages/Personnel/PersonnelForm.tsx`

适用角色：`admin`

## 1. 通用约定

### 1.1 Base URL

- 前端请求基地址：`http://localhost:8082/api`
- 请求封装文件：`src/utils/request.ts:1`

### 1.2 认证

- Header：`Authorization: <token>`
- 注意：当前前端不会自动拼接 `Bearer `，后端需直接接受纯 token。

### 1.3 响应结构

前端约定统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `code = 200` 或 `0` 表示成功
- 其他 code 会被前端拦截并按失败处理

### 1.4 错误码建议

| code | 含义 | 说明 |
|---|---|---|
| 200 / 0 | 成功 | 通用成功 |
| 400 | 请求错误 | 参数格式不正确 |
| 401 | 未认证 | token 无效或过期 |
| 403 | 无权限 | 非 admin 访问 admin 接口 |
| 404 | 资源不存在 | 人员不存在 |
| 409 | 资源冲突 | 用户名、学号、教工号重复 |
| 422 | 校验失败 | 必填字段缺失、枚举值非法 |
| 500 | 服务异常 | 未知错误 |

---

## 2. 数据库映射

基于现有表：

- `users`
- `student_profiles`
- `teacher_profiles`

### 2.1 学生对象返回字段

建议后端返回：

```json
{
  "id": "user-id",
  "username": "stu2026001",
  "realName": "张三",
  "email": "stu@example.com",
  "phone": "13800000000",
  "avatar": "http://36.133.40.172:9000/...",
  "status": "active",
  "createdAt": "2026-03-11 10:00:00",
  "studentNo": "2026001",
  "grade": "高一",
  "className": "1班",
  "guardian": "李四"
}
```

字段映射：

| API 字段 | 数据库字段 |
|---|---|
| id | users.id |
| username | users.username |
| realName | users.real_name |
| email | users.email |
| phone | users.phone |
| avatar | users.avatar |
| status | users.status |
| createdAt | users.created_at |
| studentNo | student_profiles.student_no |
| grade | student_profiles.grade |
| className | student_profiles.class_name |
| guardian | student_profiles.guardian |

### 2.2 教师对象返回字段

建议后端返回：

```json
{
  "id": "user-id",
  "username": "tch2026001",
  "realName": "王老师",
  "email": "teacher@example.com",
  "phone": "13900000000",
  "avatar": "http://36.133.40.172:9000/...",
  "status": "active",
  "createdAt": "2026-03-11 10:00:00",
  "teacherNo": "T2026001",
  "department": "高中部",
  "subjects": ["数学", "物理"]
}
```

字段映射：

| API 字段 | 数据库字段 |
|---|---|
| id | users.id |
| username | users.username |
| realName | users.real_name |
| email | users.email |
| phone | users.phone |
| avatar | users.avatar |
| status | users.status |
| createdAt | users.created_at |
| teacherNo | teacher_profiles.teacher_no |
| department | teacher_profiles.department |
| subjects | teacher_profiles.subjects_json |

---

## 3. 学生管理接口

### 3.1 获取学生列表

- Method: `GET`
- Path: `/admin/students`
- Auth: `admin`

#### 成功响应

`data` 支持两种结构，前端都兼容：

```json
[
  {
    "id": "student-1",
    "username": "stu2026001",
    "realName": "张三",
    "email": "stu1@example.com",
    "phone": "13800000001",
    "avatar": "",
    "status": "active",
    "createdAt": "2026-03-11 10:00:00",
    "studentNo": "2026001",
    "grade": "高一",
    "className": "1班",
    "guardian": "李女士"
  }
]
```

或：

```json
{
  "list": []
}
```

### 3.2 获取学生详情

- Method: `GET`
- Path: `/admin/students/{studentId}`
- Auth: `admin`

#### 成功响应

`data` 为单个学生对象，结构同 3.1。

### 3.3 新增学生

- Method: `POST`
- Path: `/admin/students`
- Auth: `admin`

#### 请求体

```json
{
  "username": "stu2026999",
  "realName": "新学生",
  "email": "newstu@example.com",
  "phone": "13800009999",
  "avatar": "https://...",
  "status": "active",
  "studentNo": "2026999",
  "grade": "高一",
  "className": "2班",
  "guardian": "监护人姓名"
}
```

说明：

- 当前前端页面没有“密码”输入项，建议后端按系统规则生成初始密码。
- `users.role` 固定写入 `student`。

#### 成功响应

`data` 为新增后的学生对象。

### 3.4 编辑学生

- Method: `PUT`
- Path: `/admin/students/{studentId}`
- Auth: `admin`

#### 请求体

与新增接口一致。

#### 成功响应

`data` 为更新后的学生对象。

### 3.5 删除学生

- Method: `DELETE`
- Path: `/admin/students/{studentId}`
- Auth: `admin`

#### 成功响应

```json
{
  "id": "student-1"
}
```

---

## 4. 教师管理接口

### 4.1 获取教师列表

- Method: `GET`
- Path: `/admin/teachers`
- Auth: `admin`

#### 成功响应

`data` 支持数组或 `{ list: [] }`，单项结构如下：

```json
{
  "id": "teacher-1",
  "username": "tch2026001",
  "realName": "王老师",
  "email": "teacher@example.com",
  "phone": "13900000000",
  "avatar": "",
  "status": "active",
  "createdAt": "2026-03-11 10:00:00",
  "teacherNo": "T2026001",
  "department": "高中部",
  "subjects": ["数学", "物理"]
}
```

### 4.2 获取教师详情

- Method: `GET`
- Path: `/admin/teachers/{teacherId}`
- Auth: `admin`

#### 成功响应

`data` 为单个教师对象，结构同 4.1。

### 4.3 新增教师

- Method: `POST`
- Path: `/admin/teachers`
- Auth: `admin`

#### 请求体

```json
{
  "username": "tch2026999",
  "realName": "新教师",
  "email": "newteacher@example.com",
  "phone": "13900009999",
  "avatar": "https://...",
  "status": "active",
  "teacherNo": "T2026999",
  "department": "高中部",
  "subjects": ["数学", "信息技术"]
}
```

说明：

- 当前前端页面没有“密码”输入项，建议后端按系统规则生成初始密码。
- `users.role` 固定写入 `teacher`。

#### 成功响应

`data` 为新增后的教师对象。

### 4.4 编辑教师

- Method: `PUT`
- Path: `/admin/teachers/{teacherId}`
- Auth: `admin`

#### 请求体

与新增接口一致。

#### 成功响应

`data` 为更新后的教师对象。

### 4.5 删除教师

- Method: `DELETE`
- Path: `/admin/teachers/{teacherId}`
- Auth: `admin`

#### 成功响应

```json
{
  "id": "teacher-1"
}
```

---

## 5. Excel 导入接口

当前前端导入按钮位于学生/教师列表页顶部，文件类型为 Excel：

- `.xlsx`
- `.xls`

### 5.1 学生 Excel 导入

- Method: `POST`
- Path: `/admin/students/import`
- Auth: `admin`
- Content-Type: `multipart/form-data`

#### 表单字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | file | 是 | Excel 文件 |

#### Excel 首个工作表表头

| 列名 | 说明 |
|---|---|
| username | 账号 |
| realName | 姓名 |
| email | 邮箱 |
| phone | 手机号 |
| status | 状态：`active / inactive / suspended` |
| studentNo | 学号 |
| grade | 年级 |
| className | 班级 |
| guardian | 监护人 |
| avatar | 头像 URL，可空 |

#### 成功响应

```json
{
  "importedCount": 20,
  "skippedCount": 2,
  "failedRows": [
    {
      "rowNumber": 5,
      "reason": "studentNo duplicated"
    }
  ]
}
```

### 5.2 教师 Excel 导入

- Method: `POST`
- Path: `/admin/teachers/import`
- Auth: `admin`
- Content-Type: `multipart/form-data`

#### Excel 首个工作表表头

| 列名 | 说明 |
|---|---|
| username | 账号 |
| realName | 姓名 |
| email | 邮箱 |
| phone | 手机号 |
| status | 状态：`active / inactive / suspended` |
| teacherNo | 教工号 |
| department | 所属学部 |
| subjects | 任教学科，多个值用 `|` 或 `,` 分隔 |
| avatar | 头像 URL，可空 |

#### 成功响应

结构同学生导入接口。

---

## 6. 头像上传接口

当前编辑页点击头像后，会立即调用后端上传接口并回填头像 URL。

### 6.1 上传头像

- Method: `POST`
- Path: `/admin/personnel/avatar`
- Auth: `admin`
- Content-Type: `multipart/form-data`

#### 表单字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | file | 是 | 图片文件 |

#### 成功响应

```json
{
  "url": "https://cdn.example.com/avatar/2026/03/11/abc.png"
}
```

说明：

- 前端仅保存返回的 `url`，最终在新增/编辑接口中回传给后端。
- 支持后端返回 `url` 或 `avatarUrl`，当前前端都兼容。

---

## 7. 与当前前端代码的映射

### 7.1 已对接页面

- `src/pages/Personnel/PersonnelList.tsx:1`
- `src/pages/Personnel/PersonnelDetail.tsx:1`
- `src/pages/Personnel/PersonnelForm.tsx:1`

### 7.2 已对接服务

- `src/services/personnel.ts:1`

已实现的真实请求包括：

1. 获取学生列表：`GET /admin/students`
2. 获取教师列表：`GET /admin/teachers`
3. 获取学生详情：`GET /admin/students/{id}`
4. 获取教师详情：`GET /admin/teachers/{id}`
5. 新增学生：`POST /admin/students`
6. 新增教师：`POST /admin/teachers`
7. 更新学生：`PUT /admin/students/{id}`
8. 更新教师：`PUT /admin/teachers/{id}`
9. 删除学生：`DELETE /admin/students/{id}`
10. 删除教师：`DELETE /admin/teachers/{id}`
11. 学生 Excel 导入：`POST /admin/students/import`
12. 教师 Excel 导入：`POST /admin/teachers/import`
13. 人员头像上传：`POST /admin/personnel/avatar`

### 7.3 后端实现建议

- 列表接口建议直接联表返回扁平结构，减少前端二次拼装成本。
- 若后端已有 DTO 命名为蛇形字段，当前前端也兼容以下别名：
  - `real_name`
  - `created_at`
  - `student_no`
  - `class_name`
  - `teacher_no`
  - `subjects_json`
- 导入接口建议在返回中包含失败行号与原因，便于前端提示与后续补充导入。
