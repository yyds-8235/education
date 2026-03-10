# 后端对接：教师端课程系统 API（含数据库与 MinIO 方案）

## 1. 目标范围

本文档只覆盖**教师端课程系统**的真实请求对接，不包含学生端公开加入课程、授课系统、测试系统等其他模块。

本次前端已接入的教师端能力包括：

1. 教师查询自己的课程列表
2. 教师查看课程详情
3. 教师创建课程
4. 教师编辑课程
5. 教师删除课程
6. 教师查看课程内学生
7. 教师拉取学生加入课程
8. 教师移除课程内学生
9. 教师上传课件文件（后端落 MinIO）
10. 课程资源预览
11. 课程资源下载

前端涉及文件：

- `src/services/course.ts`
- `src/store/slices/courseSlice.ts`
- `src/pages/Course/CourseCreate.tsx`
- `src/pages/Course/CourseList.tsx`

---

## 2. 鉴权与角色约束

- 所有接口均要求登录
- 所有接口仅允许 `teacher` 角色访问
- 建议统一挂载在：`/teacher/courses/**`
- 课件上传接口建议挂载在：`/teacher/course-resources/upload`

建议后端统一校验：

1. 当前登录用户必须是教师
2. 当前课程必须属于当前教师本人
3. 当前教师只能操作自己创建的课程及其章节/资源/学生名单

---

## 3. 前端字段约定

为减少前端适配成本，建议 API 返回字段尽量沿用当前前端字段名：

### 3.1 课程对象 `Course`

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
      "resources": [
        {
          "id": "resource-1",
          "chapterId": "chapter-1",
          "name": "第一章课件.pdf",
          "type": "pdf",
          "url": "https://minio.example.com/course-assets/...pdf",
          "bucketName": "course-assets",
          "objectKey": "courses/teacher-1/2026/03/uuid-first.pdf",
          "size": 102400,
          "duration": null,
          "order": 1,
          "createdAt": "2026-03-10T08:12:00.000Z"
        }
      ]
    }
  ]
}
```

### 3.2 课程学生对象 `CourseStudent`

```json
{
  "id": "course-student-1",
  "courseId": "course-1",
  "studentId": "student-1",
  "studentName": "王同学",
  "studentNo": "2026001",
  "joinedAt": "2026-03-10T10:00:00.000Z",
  "progress": 0
}
```

### 3.3 候选学生对象 `CourseSelectableStudent`

```json
{
  "id": "student-2",
  "username": "student02",
  "realName": "陈同学",
  "studentNo": "2026002",
  "grade": "高一",
  "class": "1班"
}
```

---

## 4. API 设计

统一响应建议：

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

### 4.1 查询教师自己的课程列表

- Method: `GET`
- Path: `/teacher/courses`

#### Query 参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |
| keyword | string | 否 | 课程名称/简介/学科搜索 |
| grade | string | 否 | 年级 |
| class | string | 否 | 班级 |
| subject | string | 否 | 学科 |
| status | string | 否 | `draft/active/archived` |
| scope | string | 否 | 前端会传 `mine`，后端可忽略或校验 |

#### 成功响应（`data`）

```json
{
  "list": [],
  "total": 15,
  "page": 1,
  "pageSize": 12,
  "totalPages": 2
}
```

---

### 4.2 查询课程详情

- Method: `GET`
- Path: `/teacher/courses/{courseId}`

#### 成功响应（`data`）

返回单个 `Course` 对象。

---

### 4.3 创建课程

- Method: `POST`
- Path: `/teacher/courses`

#### 请求体

```json
{
  "name": "高一物理力学基础",
  "description": "面向高一学生的力学入门课程",
  "grade": "高一",
  "class": "1班",
  "subject": "物理",
  "visibility": "public",
  "status": "draft",
  "coverImage": "",
  "chapters": [
    {
      "id": "chapter-temp-1",
      "title": "第一章 力与运动",
      "description": "章节简介",
      "resources": [
        {
          "id": "upload-temp-1",
          "name": "第一章课件.pdf",
          "type": "pdf",
          "url": "https://minio.example.com/course-assets/...pdf",
          "bucketName": "course-assets",
          "objectKey": "courses/teacher-1/2026/03/uuid-first.pdf",
          "size": 102400
        }
      ]
    }
  ]
}
```

#### 处理建议

1. 后端根据登录态自动写入 `teacher_id`
2. 创建 `courses`
3. 批量创建 `course_chapters`
4. 批量创建 `course_resources`
5. `student_count` 初始为 `0`

#### 成功响应（`data`）

返回创建后的完整 `Course`。

---

### 4.4 编辑课程

- Method: `PUT`
- Path: `/teacher/courses/{courseId}`

#### 请求体

与创建课程基本一致。

#### 更新策略建议

建议后端采用“课程主表更新 + 章节全量覆盖”策略，理由：

1. 当前前端编辑页会提交完整章节结构
2. 章节和资源的增删改在一个表单内完成
3. 全量覆盖比细粒度 diff 更容易落地

建议步骤：

1. 校验课程归属
2. 更新 `courses`
3. 删除该课程旧章节与旧资源记录
4. 重新插入新章节与新资源

#### 成功响应（`data`）

返回更新后的完整 `Course`。

---

### 4.5 删除课程

- Method: `DELETE`
- Path: `/teacher/courses/{courseId}`

#### 成功响应（`data`）

```json
{
  "id": "course-1"
}
```

#### 删除建议

推荐物理删除顺序：

1. `course_students`
2. `course_resources`
3. `course_chapters`
4. `courses`

如果资源文件需要同步删除 MinIO 对象，则应在删除 `course_resources` 前先读取 `bucket_name/object_key` 并执行对象删除。

---

### 4.6 查询课程内学生

- Method: `GET`
- Path: `/teacher/courses/{courseId}/students`

#### 成功响应（`data`）

返回 `CourseStudent[]`。

---

### 4.7 查询可拉取加入课程的候选学生

- Method: `GET`
- Path: `/teacher/courses/{courseId}/candidate-students`

#### Query 参数（可选）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 按姓名/学号/账号搜索 |

#### 成功响应（`data`）

返回 `CourseSelectableStudent[]`。

#### 查询建议

排除条件：

1. 已在 `course_students` 中的学生
2. 已停用/冻结的学生（按业务决定是否排除）

---

### 4.8 拉取学生加入课程

- Method: `POST`
- Path: `/teacher/courses/{courseId}/students`

#### 请求体

```json
{
  "studentIds": ["student-1", "student-2"]
}
```

#### 成功响应（`data`）

```json
{
  "courseId": "course-1",
  "students": []
}
```

#### 处理建议

1. 过滤重复学生
2. 校验学生真实存在
3. 写入 `course_students`
4. 回写 `courses.student_count`

---

### 4.9 移除课程学生

- Method: `DELETE`
- Path: `/teacher/courses/{courseId}/students/{studentId}`

#### 成功响应（`data`）

```json
{
  "courseId": "course-1",
  "studentId": "student-1"
}
```

---

### 4.10 上传课件文件（后端转存 MinIO）

- Method: `POST`
- Path: `/teacher/course-resources/upload`
- Content-Type: `multipart/form-data`

#### 请求体

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | binary | 是 | MP4 / MOV / WEBM / PPT / PPTX / DOC / DOCX / PDF |

#### 成功响应（`data`）

```json
{
  "id": "upload-temp-1",
  "name": "第一章课件.pdf",
  "type": "pdf",
  "url": "https://minio.example.com/course-assets/courses/teacher-1/2026/03/uuid-first.pdf",
  "bucketName": "course-assets",
  "objectKey": "courses/teacher-1/2026/03/uuid-first.pdf",
  "size": 102400
}
```

#### 说明

该接口只负责：

1. 接收文件
2. 上传到 MinIO
3. 返回资源元数据给前端

真正的章节资源关联关系，在“创建课程 / 编辑课程”时一起落库。

---

### 4.11 获取课程资源预览地址

- Method: `GET`
- Path: `/course-resources/{resourceId}/preview-url`
- Auth: 是（teacher / 已加入课程的 student）

#### 成功响应（`data`）

```json
{
  "resourceId": "resource-1",
  "fileName": "第一章课件.pdf",
  "url": "https://minio.example.com/course-assets/...signed-preview-url",
  "expiresIn": 600
}
```

#### 后端处理建议

1. 根据 `resourceId` 找到 `course_resources`
2. 关联章节与课程，校验当前用户是否有访问权限
3. 根据 `bucket_name/object_key` 生成 MinIO 预签名预览地址
4. 对于 PDF / 视频类资源，建议设置可内联预览的响应头
5. 返回短时效 URL（如 5~10 分钟）

> 如果后端不想返回签名 URL，也可以直接做文件流转发，但前端当前实现更适合“返回预览地址”模式。

---

### 4.12 获取课程资源下载地址

- Method: `GET`
- Path: `/course-resources/{resourceId}/download-url`
- Auth: 是（teacher / 已加入课程的 student）

#### 成功响应（`data`）

```json
{
  "resourceId": "resource-1",
  "fileName": "第一章课件.pdf",
  "url": "https://minio.example.com/course-assets/...signed-download-url",
  "expiresIn": 600
}
```

#### 后端处理建议

1. 校验当前用户对该资源所属课程有访问权限
2. 基于 `bucket_name/object_key` 生成下载用预签名 URL
3. 下载时建议设置 `Content-Disposition: attachment; filename=...`
4. 若资源不存在或对象已被删除，应返回 `404`

---

## 5. 数据库设计（仅新增课程域表）

说明：

- 现有 `users`、`student_profiles`、`teacher_profiles` 已存在
- 本次只新增课程系统相关表
- 以下以 MySQL 8.x 为例

### 5.1 表：`courses`

```sql
CREATE TABLE `courses` (
  `id`            VARCHAR(36)  NOT NULL,
  `name`          VARCHAR(128) NOT NULL,
  `description`   TEXT         NULL,
  `grade`         VARCHAR(16)  NOT NULL,
  `class_name`    VARCHAR(16)  NOT NULL,
  `subject`       VARCHAR(32)  NOT NULL,
  `teacher_id`    VARCHAR(36)  NOT NULL,
  `visibility`    ENUM('public','private') NOT NULL DEFAULT 'public',
  `cover_image`   VARCHAR(255) NULL,
  `student_count` INT          NOT NULL DEFAULT 0,
  `status`        ENUM('draft','active','archived') NOT NULL DEFAULT 'draft',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_teacher` (`teacher_id`),
  KEY `idx_course_grade_class` (`grade`, `class_name`),
  KEY `idx_course_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.2 表：`course_chapters`

```sql
CREATE TABLE `course_chapters` (
  `id`           VARCHAR(36)  NOT NULL,
  `course_id`    VARCHAR(36)  NOT NULL,
  `title`        VARCHAR(128) NOT NULL,
  `description`  TEXT         NULL,
  `sort_order`   INT          NOT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chapter_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.3 表：`course_resources`

```sql
CREATE TABLE `course_resources` (
  `id`               VARCHAR(36)  NOT NULL,
  `chapter_id`       VARCHAR(36)  NOT NULL,
  `name`             VARCHAR(255) NOT NULL,
  `type`             ENUM('video','ppt','word','pdf','other') NOT NULL,
  `url`              VARCHAR(500) NOT NULL,
  `bucket_name`      VARCHAR(64)  NULL,
  `object_key`       VARCHAR(255) NULL,
  `size`             BIGINT       NOT NULL,
  `duration`         INT          NULL,
  `sort_order`       INT          NOT NULL,
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resource_chapter` (`chapter_id`),
  KEY `idx_resource_object_key` (`object_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.4 表：`course_students`

```sql
CREATE TABLE `course_students` (
  `id`           VARCHAR(36)   NOT NULL,
  `course_id`    VARCHAR(36)   NOT NULL,
  `student_id`   VARCHAR(36)   NOT NULL,
  `student_no`   VARCHAR(32)   NOT NULL,
  `joined_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `progress`     DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
  KEY `idx_course_students_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 6. MinIO 设计建议

### 6.1 Bucket 建议

- Bucket：`course-assets`

### 6.2 对象路径建议

```text
courses/{teacherId}/{yyyy}/{MM}/{uuid}-{originalFilename}
```

示例：

```text
courses/teacher-1/2026/03/3e4e2f8b-first-chapter.pdf
```

### 6.3 上传流程建议

1. 前端在课程编辑页选择文件
2. 调用 `POST /teacher/course-resources/upload`
3. 后端接收文件流
4. 后端上传到 MinIO
5. 后端返回 `name/type/url/bucketName/objectKey/size`
6. 前端把这些资源元数据暂存到章节表单
7. 最终提交创建/编辑课程时，把资源元数据随课程结构一并提交
8. 后端在 `course_resources` 表中保存 `bucket_name/object_key/url`

### 6.4 删除策略建议

当课程删除或章节资源删除时：

1. 先查出 `bucket_name/object_key`
2. 调用 MinIO SDK 删除对象
3. 删除数据库记录

### 6.5 预览 / 下载建议

建议 MinIO Bucket 维持私有读权限，不直接暴露长期公开地址。

推荐做法：

1. 前端点击“预览” -> 调用 `/course-resources/{resourceId}/preview-url`
2. 后端校验课程访问权限后返回短时效签名 URL
3. 前端新开标签页打开该 URL
4. 前端点击“下载” -> 调用 `/course-resources/{resourceId}/download-url`
5. 后端返回下载签名 URL，并设置下载响应头

这样可以避免：

1. 资源长期暴露
2. 任意人拿到静态 URL 后可持续访问
3. 前端直接拼接 MinIO 路径带来的安全问题

---

## 7. 数据库字段映射

### 7.1 `courses`

| API 字段 | 数据库字段 |
|---|---|
| id | courses.id |
| name | courses.name |
| description | courses.description |
| grade | courses.grade |
| class | courses.class_name |
| subject | courses.subject |
| teacherId | courses.teacher_id |
| visibility | courses.visibility |
| coverImage | courses.cover_image |
| studentCount | courses.student_count |
| status | courses.status |
| createdAt | courses.created_at |
| updatedAt | courses.updated_at |

### 7.2 `course_chapters`

| API 字段 | 数据库字段 |
|---|---|
| id | course_chapters.id |
| courseId | course_chapters.course_id |
| title | course_chapters.title |
| description | course_chapters.description |
| order | course_chapters.sort_order |
| createdAt | course_chapters.created_at |

### 7.3 `course_resources`

| API 字段 | 数据库字段 |
|---|---|
| id | course_resources.id |
| chapterId | course_resources.chapter_id |
| name | course_resources.name |
| type | course_resources.type |
| url | course_resources.url |
| bucketName | course_resources.bucket_name |
| objectKey | course_resources.object_key |
| size | course_resources.size |
| duration | course_resources.duration |
| order | course_resources.sort_order |
| createdAt | course_resources.created_at |

### 7.4 `course_students`

| API 字段 | 数据库字段 |
|---|---|
| id | course_students.id |
| courseId | course_students.course_id |
| studentId | course_students.student_id |
| studentNo | course_students.student_no |
| joinedAt | course_students.joined_at |
| progress | course_students.progress |

---

## 8. 错误码建议

| code | message | 场景 |
|---|---|---|
| 401 | 未登录 | token 失效 |
| 403 | 无权限访问 | 非教师访问教师接口 |
| 403 | 课程不属于当前教师 | 越权访问他人课程 |
| 404 | 课程不存在 | 课程 ID 不存在 |
| 404 | 学生不存在 | 拉取学生时学生 ID 不存在 |
| 409 | 学生已在课程中 | 重复拉取 |
| 413 | 文件过大 | 上传文件超限制 |
| 415 | 文件类型不支持 | 非允许格式 |
| 422 | 参数校验失败 | 缺少必填项 |
| 500 | 服务器异常 | 未知错误 |

---

## 9. 与当前前端实现的对应关系

### 已接入真实请求的教师端能力

1. `fetchCourses`（教师） -> `GET /teacher/courses`
2. `fetchCourseById`（教师） -> `GET /teacher/courses/{id}`
3. `createCourse`（教师） -> `POST /teacher/courses`
4. `updateCourse`（教师） -> `PUT /teacher/courses/{id}`
5. `deleteCourse`（教师） -> `DELETE /teacher/courses/{id}`
6. `fetchCourseStudents`（教师） -> `GET /teacher/courses/{id}/students`
7. `fetchCourseCandidateStudents`（教师） -> `GET /teacher/courses/{id}/candidate-students`
8. `addStudentsToCourse`（教师） -> `POST /teacher/courses/{id}/students`
9. `removeStudentFromCourse`（教师） -> `DELETE /teacher/courses/{id}/students/{studentId}`
10. `uploadTeacherCourseResourceApi` -> `POST /teacher/course-resources/upload`
11. `getCourseResourcePreviewUrlApi` -> `GET /course-resources/{resourceId}/preview-url`
12. `getCourseResourceDownloadUrlApi` -> `GET /course-resources/{resourceId}/download-url`

### 当前仍保留前端演示逻辑的部分

1. 学生端公开加入课程
2. 学生端课程发现/加入逻辑
3. 非教师角色下的课程本地筛选行为

如果后端后续要继续接学生端课程系统，可在此文档基础上扩展 `/student/courses/**` 接口。
