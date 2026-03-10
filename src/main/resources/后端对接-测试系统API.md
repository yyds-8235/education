# 后端对接：测试系统 API

## 1. 范围说明

本文档覆盖当前前端 `src/pages/Test` 模块涉及的教师端与学生端测试系统真实请求对接，包含：

1. 教师查询测试列表
2. 教师创建测试
3. 教师编辑测试
4. 教师发布测试
5. 教师查看测试详情
6. 教师查看提交列表
7. 教师批改单份提交
8. 教师批量批改客观题
9. 教师查看统计分析
10. 学生查询可参加测试 / 已完成测试
11. 学生查看测试详情
12. 学生提交测试
13. 学生查看个人提交
14. 学生发起成绩申诉

本文档优先服务当前前端页面与类型定义，字段设计尽量对齐：

- `src/types/test.ts`
- `src/store/slices/testSlice.ts`
- `src/pages/Test/TestList.tsx`
- `src/pages/Test/TestDetail.tsx`
- `src/pages/Test/TestAnswer.tsx`
- `src/pages/Test/TestGrading.tsx`
- `src/pages/Test/TestStatistics.tsx`

---

## 2. 角色与鉴权规则

### 2.1 通用要求

- 所有接口均要求登录
- 请求头继续沿用前端现有约定：`Authorization: {token}`
- 统一响应格式沿用项目现有 `code/message/data`

### 2.2 角色边界

- 教师接口仅允许 `teacher`
- 学生接口仅允许 `student`
- 学生只能访问自己已加入课程下的测试
- 教师只能操作自己课程下的测试

### 2.3 路由建议

- 教师端统一挂载：`/teacher/tests/**`
- 学生端统一挂载：`/student/tests/**`

---

## 3. 前端类型对齐

## 3.1 `Test`

```json
{
  "id": "test-1",
  "courseId": "course-2",
  "courseName": "初二数学进阶",
  "title": "一次函数与勾股定理小测",
  "description": "覆盖函数代入计算、方程与定理应用。",
  "duration": 30,
  "totalScore": 100,
  "showAnswer": true,
  "status": "published",
  "questions": [],
  "submissions": [],
  "createdAt": "2026-03-10T08:00:00.000Z",
  "updatedAt": "2026-03-10T08:30:00.000Z"
}
```

说明：

- 当前前端默认 `Test` 内包含 `questions` 和 `submissions`
- 为减少适配成本，建议：
  - 列表接口至少返回 `questions` 简化数组或完整数组
  - 列表接口可返回 `submissions` 简化数组，便于直接显示“已提交数量 / 学生个人状态”
- 若后端希望减小列表体积，也可以列表返回轻量结构，但前端需同步适配

## 3.2 `TestQuestion`

```json
{
  "id": "question-1",
  "testId": "test-1",
  "type": "single_choice",
  "content": "一次函数 y = 2x + 1，当 x = 3 时，y 等于多少？",
  "options": [
    { "id": "option-1", "label": "A", "content": "5" },
    { "id": "option-2", "label": "B", "content": "7" }
  ],
  "answer": "B",
  "score": 20,
  "order": 1,
  "analysis": "代入 x = 3，得到 y = 7。"
}
```

## 3.3 `TestSubmission`

```json
{
  "id": "submission-1",
  "testId": "test-1",
  "studentId": "student-1",
  "studentName": "王同学",
  "studentNo": "S202601",
  "answers": [],
  "totalScore": 90,
  "status": "graded",
  "submittedAt": "2026-03-10T09:30:00.000Z",
  "gradedAt": "2026-03-10T10:00:00.000Z",
  "appealReason": null,
  "appealStatus": null,
  "analysisSummary": "函数代入题完成较好，简答题表达较完整。",
  "createdAt": "2026-03-10T09:00:00.000Z"
}
```

## 3.4 `SubmissionAnswer`

```json
{
  "questionId": "question-1",
  "answer": "B",
  "score": 20,
  "feedback": "回答正确",
  "isCorrect": true
}
```

## 3.5 `TestStatistics`

```json
{
  "testId": "test-1",
  "totalSubmissions": 36,
  "averageScore": 82.5,
  "highestScore": 98,
  "lowestScore": 45,
  "passRate": 88.89,
  "questionStats": [
    {
      "questionId": "question-1",
      "correctRate": 91.67,
      "wrongCount": 3,
      "correctCount": 33,
      "averageScore": 18.33
    }
  ],
  "wrongDistribution": [
    {
      "questionId": "question-3",
      "content": "请简述勾股定理并举一个应用场景。",
      "wrongRate": 47.22
    }
  ],
  "learningBrief": "学生整体基础较稳，主观题表达层次仍需加强。",
  "adaptiveRecommendations": [
    "加强简答题表达训练",
    "复习勾股定理实际应用场景"
  ]
}
```

---

## 4. 状态流转规则

## 4.1 测试状态 `Test.status`

- `draft`：草稿，教师可编辑，不允许学生作答
- `published`：已发布，学生可进入答题
- `ended`：已结束，学生不可再次作答，教师可继续查看统计与批改

建议流转：

- `draft -> published`
- `published -> ended`
- 已发布后原则上不建议再修改题目；若允许修改，需要后端明确是否影响已提交记录

## 4.2 提交状态 `TestSubmission.status`

- `draft`：保存中 / 未正式交卷
- `submitted`：已提交待批改
- `graded`：已批改

建议流转：

- `draft -> submitted`
- `submitted -> graded`

## 4.3 申诉状态 `appealStatus`

- `pending`
- `accepted`
- `rejected`

当前前端已接入的是“学生提交申诉”，教师端暂未做申诉处理页，因此后端先支持提交申诉即可。

---

## 5. 教师端接口

## 5.1 查询测试列表

- Method: `GET`
- Path: `/teacher/tests`
- Auth: `teacher`

### Query

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 是 | 页码 |
| pageSize | number | 是 | 每页条数 |
| courseId | string | 否 | 按课程筛选 |
| keyword | string | 否 | 按标题/描述筛选 |

### 返回

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 0,
    "page": 1,
    "pageSize": 10,
    "totalPages": 0
  }
}
```

### 过滤规则

- 仅返回当前教师自己课程下的测试
- 列表项建议返回：
  - 测试基本信息
  - 题目数组 `questions`
  - 提交数组 `submissions` 或至少提交数与学生本人提交状态摘要

## 5.2 查询测试详情

- Method: `GET`
- Path: `/teacher/tests/{testId}`
- Auth: `teacher`

返回完整 `Test` 对象，含：

- `questions`
- `submissions`

## 5.3 创建测试

- Method: `POST`
- Path: `/teacher/tests`
- Auth: `teacher`

### Body

```json
{
  "courseId": "course-2",
  "title": "一次函数与勾股定理小测",
  "description": "覆盖函数代入计算、方程与定理应用。",
  "duration": 30,
  "showAnswer": true,
  "questions": [
    {
      "type": "single_choice",
      "content": "一次函数 y = 2x + 1，当 x = 3 时，y 等于多少？",
      "options": [
        { "id": "tmp-a", "label": "A", "content": "5" },
        { "id": "tmp-b", "label": "B", "content": "7" }
      ],
      "answer": "B",
      "score": 20,
      "analysis": "代入 x = 3，得到 y = 7。"
    }
  ]
}
```

### 规则

- `questions` 至少 1 题
- `single_choice` 必须有 `options`
- `fill_blank` / `short_answer` 可无 `options`
- `totalScore` 建议由后端按题目分值自动汇总，不要求前端传
- 创建后默认状态建议为 `draft`

## 5.4 更新测试

- Method: `PUT`
- Path: `/teacher/tests/{testId}`
- Auth: `teacher`

### 规则

- 仅允许修改当前教师自己的测试
- 若测试已有正式提交，建议限制题干结构性修改，避免影响历史成绩
- 返回完整更新后的 `Test`

## 5.5 发布测试

- Method: `POST`
- Path: `/teacher/tests/{testId}/publish`
- Auth: `teacher`

### 返回

返回更新后的 `Test`

### 规则

- `draft` 才能发布
- 已发布测试再次发布可直接幂等返回

## 5.6 查询提交列表

- Method: `GET`
- Path: `/teacher/tests/{testId}/submissions`
- Auth: `teacher`

### 返回

返回 `TestSubmission[]`

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "submission-1",
      "testId": "test-1",
      "studentId": "student-1",
      "studentName": "王同学",
      "studentNo": "S202601",
      "answers": [],
      "totalScore": 90,
      "status": "graded",
      "submittedAt": "2026-03-10T09:30:00.000Z",
      "gradedAt": "2026-03-10T10:00:00.000Z",
      "createdAt": "2026-03-10T09:00:00.000Z"
    }
  ]
}
```

## 5.7 批改单份提交

- Method: `PUT`
- Path: `/teacher/test-submissions/{submissionId}/grade`
- Auth: `teacher`

### Body

```json
{
  "answers": [
    {
      "questionId": "question-3",
      "score": 50,
      "feedback": "定理表述正确，应用场景可以再具体一些。"
    }
  ]
}
```

### 规则

- 允许教师对任意题型评分
- 已自动批改的客观题也允许教师二次修正
- 后端需重算 `totalScore`
- 批改完成后状态置为 `graded`

### 返回

返回更新后的 `TestSubmission`

## 5.8 批量批改客观题

- Method: `POST`
- Path: `/teacher/tests/{testId}/batch-grade-objective`
- Auth: `teacher`

### 用途

- 对 `single_choice`、`fill_blank` 自动批分
- `short_answer` 保留待教师人工评分

### 返回

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "testId": "test-1",
    "submissions": []
  }
}
```

## 5.9 查询统计分析

- Method: `GET`
- Path: `/teacher/tests/{testId}/statistics`
- Auth: `teacher`

### 返回

返回 `TestStatistics`

---

## 6. 学生端接口

## 6.1 查询测试列表

- Method: `GET`
- Path: `/student/tests`
- Auth: `student`

### Query

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 是 | 页码 |
| pageSize | number | 是 | 每页条数 |
| courseId | string | 否 | 按课程筛选 |
| keyword | string | 否 | 按标题/描述筛选 |

### 返回规则

- 仅返回当前学生已加入课程下的测试
- 不返回不属于学生课程范围的测试
- 建议返回完整 `Test`，并附带当前学生自己的提交信息在 `submissions` 中

### 状态可见性

- `draft`：学生不可见
- `published`：学生可见，可答题
- `ended`：学生可见，只读

## 6.2 查询测试详情

- Method: `GET`
- Path: `/student/tests/{testId}`
- Auth: `student`

### 规则

- 当前学生必须已加入测试所属课程
- 返回完整 `Test`
- `submissions` 中至少应包含当前学生自己的提交记录

## 6.3 提交测试

- Method: `POST`
- Path: `/student/tests/{testId}/submit`
- Auth: `student`

### Body

```json
{
  "answers": [
    { "questionId": "question-1", "answer": "B" },
    { "questionId": "question-2", "answer": "5" },
    { "questionId": "question-3", "answer": "勾股定理是直角三角形中两直角边平方和等于斜边平方。" }
  ]
}
```

### 规则

- 学生只能提交一次有效记录；若允许覆盖提交，需后端明确规则
- 当前前端更适合“同一学生一个测试仅一份有效提交，重复提交覆盖原记录”
- 客观题可在提交时自动评分
- 主观题等待教师批改
- 若已交卷且状态为 `graded`，前端默认只读

### 返回

返回更新后的 `TestSubmission`

## 6.4 查询个人提交

- Method: `GET`
- Path: `/student/tests/{testId}/submission`
- Auth: `student`

### 返回

返回当前登录学生对应的 `TestSubmission`；若不存在可返回 `null`

## 6.5 发起成绩申诉

- Method: `POST`
- Path: `/student/test-submissions/{submissionId}/appeal`
- Auth: `student`

### Body

```json
{
  "reason": "第 3 题答案要点已覆盖，申请复核评分。"
}
```

### 规则

- 仅允许申诉自己的提交
- 建议只允许对 `graded` 状态提交发起申诉
- 提交后将 `appealStatus` 置为 `pending`

### 返回

返回更新后的 `TestSubmission`

---

## 7. 统一错误码建议

| HTTP | 场景 | 建议 message |
| --- | --- | --- |
| 400 | 参数错误 / 状态不允许 | 参数校验失败 / 当前状态不可操作 |
| 401 | 未登录 | 未登录或登录已过期 |
| 403 | 无权限 | 无权访问该测试 |
| 404 | 测试或提交不存在 | 测试不存在 / 提交不存在 |
| 409 | 状态冲突 | 测试已发布不可修改 |
| 422 | 业务校验失败 | 题目内容不完整 / 不能重复申诉 |
| 500 | 服务异常 | 服务器内部错误 |

---

## 8. 前端动作与接口映射

## 8.1 `src/store/slices/testSlice.ts`

1. `fetchTests` ->
   - teacher: `GET /teacher/tests`
   - student: `GET /student/tests`
2. `fetchTestById` ->
   - teacher: `GET /teacher/tests/{id}`
   - student: `GET /student/tests/{id}`
3. `createTest` -> `POST /teacher/tests`
4. `updateTest` -> `PUT /teacher/tests/{id}`
5. `publishTest` -> `POST /teacher/tests/{id}/publish`
6. `submitTest` -> `POST /student/tests/{id}/submit`
7. `fetchSubmission` -> `GET /student/tests/{id}/submission`
8. `fetchSubmissions` -> `GET /teacher/tests/{id}/submissions`
9. `gradeSubmission` -> `PUT /teacher/test-submissions/{submissionId}/grade`
10. `batchGradeObjective` -> `POST /teacher/tests/{id}/batch-grade-objective`
11. `fetchStatistics` -> `GET /teacher/tests/{id}/statistics`
12. `submitAppeal` -> `POST /student/test-submissions/{submissionId}/appeal`

## 8.2 页面对应关系

- `TestList.tsx`
  - 教师：列表、创建、编辑、发布、跳转批改/统计
  - 学生：可参加测试列表、已完成测试列表、发起申诉
- `TestAnswer.tsx`
  - 学生答题、提交试卷
- `TestDetail.tsx`
  - 查看题目、答案、成绩与解析
- `TestGrading.tsx`
  - 教师查看提交列表、单份批改、客观题批量批改
- `TestStatistics.tsx`
  - 教师查看统计分析

---

## 9. 最小可交付接口清单

若后端希望先最小闭环支持当前测试系统页面，至少提供以下接口：

1. `GET /teacher/tests`
2. `GET /teacher/tests/{testId}`
3. `POST /teacher/tests`
4. `PUT /teacher/tests/{testId}`
5. `POST /teacher/tests/{testId}/publish`
6. `GET /teacher/tests/{testId}/submissions`
7. `PUT /teacher/test-submissions/{submissionId}/grade`
8. `POST /teacher/tests/{testId}/batch-grade-objective`
9. `GET /teacher/tests/{testId}/statistics`
10. `GET /student/tests`
11. `GET /student/tests/{testId}`
12. `POST /student/tests/{testId}/submit`
13. `GET /student/tests/{testId}/submission`
14. `POST /student/test-submissions/{submissionId}/appeal`

这样即可覆盖当前前端教师与学生测试主流程。
