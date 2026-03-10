# 后端对接：学生端课程系统 API

## 1. 范围说明

本文档覆盖当前学生端课程页面已经接入的真实请求，包含：

1. 我的课程列表
2. 公开课程列表
3. 课程详情
4. 学生加入公开课程
5. 课程资源预览 / 下载

教师端课程管理相关接口已在 `docs/后端对接-教师端课程系统API.md` 说明，本文不重复展开。

当前前端涉及文件：

- `src/services/course.ts`
- `src/store/slices/courseSlice.ts`
- `src/pages/Course/CourseList.tsx`
- `src/pages/Course/CourseDetail.tsx`

---

## 2. 鉴权与角色约束

- 所有接口均要求已登录
- 学生端课程接口仅允许 `student` 角色访问
- 建议统一挂载在：`/student/courses/**`
- 课程资源访问接口继续复用：`/course-resources/**`

后端建议统一校验：

1. 当前登录用户必须是学生
2. 学生只能查看自己可见的课程
3. 非公开且未加入的课程，不允许主动加入
4. 资源预览 / 下载仅允许课程教师或已加入学生访问

---

## 3. 前端字段约定

为减少前端适配成本，建议后端返回字段尽量沿用当前前端结构。

### 3.1 课程对象 `Course`

在原教师端 `Course` 基础上，学生端建议额外返回 `joined` 字段，用于标记当前学生是否已加入课程。

```json
{
  "id": "course-1",
  "name": "高一物理力学基础",
  "description": "面向高一学生的力学入门课程",
  "grade": "高一",
  "class": "1班",
  "subject": "物理",
  "teacherId": "teacher-1",
  "teacherName": "李老师",
  "visibility": "public",
  "coverImage": "",
  "studentCount": 42,
  "joined": true,
  "status": "active",
  "createdAt": "2026-03-10T08:00:00.000Z",
  "updatedAt": "2026-03-10T09:30:00.000Z",
  "chapters": [
    {
      "id": "chapter-1",
      "courseId": "course-1",
      "title": "力与运动基础",
      "description": "第一章",
      "order": 1,
      "createdAt": "2026-03-10T08:10:00.000Z",
      "resources": []
    }
  ]
}
```

### 3.2 加入课程返回对象

前端当前 `studentJoinCourse` 期望返回如下结构：

```json
{
  "courseId": "course-1",
  "student": {
    "id": "course-student-1",
    "courseId": "course-1",
    "studentId": "student-1",
    "studentName": "王同学",
    "studentNo": "2026001",
    "joinedAt": "2026-03-10T10:00:00.000Z",
    "progress": 0
  }
}
```

### 3.3 分页对象 `PaginatedResponse<Course>`

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "pageSize": 12,
  "totalPages": 0
}
```

---

## 4. 接口定义

### 4.1 查询学生课程列表

- Method: `GET`
- Path: `/student/courses`
- Auth: 是（student）

#### Query 参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 是 | 页码，从 1 开始 |
| pageSize | number | 是 | 每页条数 |
| scope | string | 是 | `joined` / `discover` / `all` |
| keyword | string | 否 | 关键字，匹配课程名称 / 简介 / 学科 |
| grade | string | 否 | 年级筛选 |
| class | string | 否 | 班级筛选 |
| subject | string | 否 | 学科筛选 |
| status | string | 否 | 课程状态筛选 |

#### 语义说明

- `scope=joined`：返回当前学生已加入课程
- `scope=discover`：返回当前学生可发现、且尚未加入的公开课程
- `scope=all`：返回当前学生“已加入课程 + 可发现公开课程”的并集

#### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "course-1",
        "name": "高一物理力学基础",
        "description": "面向高一学生的力学入门课程",
        "grade": "高一",
        "class": "1班",
        "subject": "物理",
        "teacherId": "teacher-1",
        "teacherName": "李老师",
        "visibility": "public",
        "studentCount": 42,
        "joined": true,
        "status": "active",
        "createdAt": "2026-03-10T08:00:00.000Z",
        "updatedAt": "2026-03-10T09:30:00.000Z",
        "chapters": []
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 12,
    "totalPages": 1
  }
}
```

### 4.2 查询课程详情

- Method: `GET`
- Path: `/student/courses/{courseId}`
- Auth: 是（student）

#### 返回要求

- 返回完整 `Course` 对象
- 建议带上 `joined` 字段
- 对于未加入但公开课程，允许查看详情
- 对于私有课程或无权限课程，返回 `403`

### 4.3 学生加入公开课程

- Method: `POST`
- Path: `/student/courses/{courseId}/join`
- Auth: 是（student）

#### 业务规则

1. 课程必须存在
2. 课程必须是 `public`
3. 已加入时不可重复加入
4. 加入成功后更新 `course_students` 关系表与课程人数

#### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "courseId": "course-1",
    "student": {
      "id": "course-student-1",
      "courseId": "course-1",
      "studentId": "student-1",
      "studentName": "王同学",
      "studentNo": "2026001",
      "joinedAt": "2026-03-10T10:00:00.000Z",
      "progress": 0
    }
  }
}
```

#### 失败响应建议

- 课程不存在：`404`
- 非公开课程：`400` 或 `403`
- 已加入课程：`400`

### 4.4 课程资源预览地址

- Method: `GET`
- Path: `/course-resources/{resourceId}/preview-url`
- Auth: 是（teacher / 已加入 student）

返回结构继续沿用教师端文档定义：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "resourceId": "resource-1",
    "fileName": "第一章课件.pdf",
    "url": "https://example.com/preview-url",
    "expiresIn": 600
  }
}
```

### 4.5 课程资源下载地址

- Method: `GET`
- Path: `/course-resources/{resourceId}/download-url`
- Auth: 是（teacher / 已加入 student）

返回结构与预览接口一致，仅 URL 用于下载。

---

## 5. 统一响应格式

与当前项目 `src/utils/request.ts` 保持一致：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

前端约定：

- `code === 200` 或 `code === 0` 视为成功
- 其他状态将走统一错误处理

---

## 6. 当前前端调用映射

### 已接入的学生端真实请求

1. `fetchCourses`（学生） -> `GET /student/courses`
2. `fetchCourseById`（学生） -> `GET /student/courses/{id}`
3. `studentJoinCourse` -> `POST /student/courses/{id}/join`
4. `getCourseResourcePreviewUrlApi` -> `GET /course-resources/{resourceId}/preview-url`
5. `getCourseResourceDownloadUrlApi` -> `GET /course-resources/{resourceId}/download-url`

### 前端补充说明

1. 学生进入课程列表页时，前端会按当前 Tab 调用 `/student/courses`
2. 当列表处于无筛选状态时，前端还会额外请求一次 `scope=all`，用于同步“我的课程 / 公开课程”的联合缓存
3. 学生加入公开课程成功后，前端会立即把该课程标记为已加入，并刷新“我的课程”列表
4. 课程详情页会调用学生端详情接口，不再依赖本地 mock 数据

---

## 7. 建议的后端落库关系

建议直接复用现有数据设计：

- `courses`
- `course_chapters`
- `course_resources`
- `course_students`

关键点：

1. `joined` 字段不必真实落库，可由当前用户与 `course_students` 关系动态计算
2. `studentCount` 可查询聚合，也可走冗余字段
3. `scope=discover` 本质上是：`visibility = public` 且当前学生未加入

---

## 8. 最低可交付接口清单

如果后端希望先最小闭环支持学生端课程页面，至少提供：

1. `GET /student/courses?scope=joined`
2. `GET /student/courses?scope=discover`
3. `GET /student/courses?scope=all`
4. `GET /student/courses/{courseId}`
5. `POST /student/courses/{courseId}/join`
6. `GET /course-resources/{resourceId}/preview-url`
7. `GET /course-resources/{resourceId}/download-url`

这样即可完整支撑当前学生端课程列表、详情、公开加入和资源访问流程。
