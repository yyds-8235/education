package com.example.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.education.common.exception.BusinessException;
import com.example.education.common.util.TimeUtils;
import com.example.education.mapper.CourseMapper;
import com.example.education.mapper.CourseStudentMapper;
import com.example.education.mapper.StudentProfileMapper;
import com.example.education.mapper.TestMapper;
import com.example.education.mapper.TestQuestionMapper;
import com.example.education.mapper.TestQuestionOptionMapper;
import com.example.education.mapper.TestSubmissionAnswerMapper;
import com.example.education.mapper.TestSubmissionMapper;
import com.example.education.mapper.UserMapper;
import com.example.education.pojo.dto.SubmissionAppealRequestDTO;
import com.example.education.pojo.dto.TestGradeAnswerRequestDTO;
import com.example.education.pojo.dto.TestGradeRequestDTO;
import com.example.education.pojo.dto.TestPageDataDTO;
import com.example.education.pojo.dto.TestQuestionOptionRequestDTO;
import com.example.education.pojo.dto.TestQuestionRequestDTO;
import com.example.education.pojo.dto.TestSaveRequestDTO;
import com.example.education.pojo.dto.TestSubmitAnswerRequestDTO;
import com.example.education.pojo.dto.TestSubmitRequestDTO;
import com.example.education.pojo.entity.CourseEntity;
import com.example.education.pojo.entity.CourseStudentEntity;
import com.example.education.pojo.entity.StudentProfileEntity;
import com.example.education.pojo.entity.TestEntity;
import com.example.education.pojo.entity.TestQuestionEntity;
import com.example.education.pojo.entity.TestQuestionOptionEntity;
import com.example.education.pojo.entity.TestSubmissionAnswerEntity;
import com.example.education.pojo.entity.TestSubmissionEntity;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.query.TestQuery;
import com.example.education.pojo.vo.SubmissionAnswerVO;
import com.example.education.pojo.vo.TestBatchGradeResultVO;
import com.example.education.pojo.vo.TestQuestionOptionVO;
import com.example.education.pojo.vo.TestQuestionStatVO;
import com.example.education.pojo.vo.TestQuestionVO;
import com.example.education.pojo.vo.TestStatisticsVO;
import com.example.education.pojo.vo.TestSubmissionVO;
import com.example.education.pojo.vo.TestVO;
import com.example.education.pojo.vo.TestWrongDistributionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private static final String TEST_STATUS_DRAFT = "draft";
    private static final String TEST_STATUS_PUBLISHED = "published";
    private static final String TEST_STATUS_ENDED = "ended";
    private static final String SUBMISSION_STATUS_SUBMITTED = "submitted";
    private static final String SUBMISSION_STATUS_GRADED = "graded";
    private static final String QUESTION_TYPE_SINGLE_CHOICE = "single_choice";
    private static final String QUESTION_TYPE_FILL_BLANK = "fill_blank";
    private static final String QUESTION_TYPE_SHORT_ANSWER = "short_answer";
    private static final String APPEAL_STATUS_PENDING = "pending";
    private static final Set<String> QUESTION_TYPES = Set.of(
            QUESTION_TYPE_SINGLE_CHOICE,
            QUESTION_TYPE_FILL_BLANK,
            QUESTION_TYPE_SHORT_ANSWER);

    private final TestMapper testMapper;
    private final TestQuestionMapper testQuestionMapper;
    private final TestQuestionOptionMapper testQuestionOptionMapper;
    private final TestSubmissionMapper testSubmissionMapper;
    private final TestSubmissionAnswerMapper testSubmissionAnswerMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;

    public TestPageDataDTO<TestVO> getTeacherTests(UserEntity teacher, TestQuery query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        List<String> courseIds = courseMapper.selectList(new LambdaQueryWrapper<CourseEntity>()
                        .eq(CourseEntity::getTeacherId, teacher.getId())
                        .select(CourseEntity::getId))
                .stream()
                .map(CourseEntity::getId)
                .toList();
        if (courseIds.isEmpty()) {
            return toPageData(Page.<TestEntity>of(page, pageSize), Collections.emptyList(), page, pageSize);
        }

        LambdaQueryWrapper<TestEntity> wrapper = new LambdaQueryWrapper<TestEntity>()
                .in(TestEntity::getCourseId, courseIds)
                .orderByDesc(TestEntity::getCreatedAt, TestEntity::getId);
        applyQueryFilters(wrapper, query);

        IPage<TestEntity> pageResult = testMapper.selectPage(Page.of(page, pageSize), wrapper);
        return toPageData(pageResult, buildTestViews(pageResult.getRecords(), true, null), page, pageSize);
    }

    public TestVO getTeacherTestDetail(UserEntity teacher, String testId) {
        return buildTestView(ensureOwnedTest(teacher.getId(), testId), true, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestVO createTeacherTest(UserEntity teacher, TestSaveRequestDTO request) {
        CourseEntity course = ensureOwnedCourse(teacher.getId(), request.getCourseId());
        validateTestSaveRequest(request);

        LocalDateTime now = TimeUtils.nowUtc();
        TestEntity test = new TestEntity();
        test.setId(UUID.randomUUID().toString());
        test.setCourseId(course.getId());
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setDuration(request.getDuration());
        test.setTotalScore(sumQuestionScores(request.getQuestions()));
        test.setShowAnswer(Boolean.TRUE.equals(request.getShowAnswer()));
        test.setStatus(TEST_STATUS_DRAFT);
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        testMapper.insert(test);
        saveQuestions(test.getId(), request.getQuestions(), now);
        return buildTestView(testMapper.selectById(test.getId()), true, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestVO updateTeacherTest(UserEntity teacher, String testId, TestSaveRequestDTO request) {
        TestEntity existing = ensureOwnedTest(teacher.getId(), testId);
        ensureEditable(existing);
        ensureOwnedCourse(teacher.getId(), request.getCourseId());
        validateTestSaveRequest(request);

        LocalDateTime now = TimeUtils.nowUtc();
        existing.setCourseId(request.getCourseId());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setDuration(request.getDuration());
        existing.setTotalScore(sumQuestionScores(request.getQuestions()));
        existing.setShowAnswer(Boolean.TRUE.equals(request.getShowAnswer()));
        existing.setUpdatedAt(now);
        testMapper.updateById(existing);

        deleteQuestionsAndOptions(existing.getId());
        saveQuestions(existing.getId(), request.getQuestions(), now);
        return buildTestView(testMapper.selectById(existing.getId()), true, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestVO publishTeacherTest(UserEntity teacher, String testId) {
        TestEntity test = ensureOwnedTest(teacher.getId(), testId);
        if (TEST_STATUS_PUBLISHED.equals(test.getStatus())) {
            return buildTestView(test, true, null);
        }
        if (!TEST_STATUS_DRAFT.equals(test.getStatus())) {
            throw new BusinessException(409, "当前测试状态不可发布");
        }
        test.setStatus(TEST_STATUS_PUBLISHED);
        test.setUpdatedAt(TimeUtils.nowUtc());
        testMapper.updateById(test);
        return buildTestView(testMapper.selectById(test.getId()), true, null);
    }

    public List<TestSubmissionVO> getTeacherSubmissions(UserEntity teacher, String testId) {
        ensureOwnedTest(teacher.getId(), testId);
        return buildSubmissionViewsByTestIds(List.of(testId)).getOrDefault(testId, new ArrayList<>());
    }

    @Transactional(rollbackFor = Exception.class)
    public TestSubmissionVO gradeSubmission(UserEntity teacher, String submissionId, TestGradeRequestDTO request) {
        TestSubmissionEntity submission = testSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(404, "提交不存在");
        }
        TestEntity test = ensureOwnedTest(teacher.getId(), submission.getTestId());
        Map<String, TestQuestionEntity> questionMap = listQuestions(test.getId()).stream()
                .collect(Collectors.toMap(TestQuestionEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<TestSubmissionAnswerEntity> currentAnswers = listSubmissionAnswers(List.of(submission.getId()));
        Map<String, TestSubmissionAnswerEntity> answerMap = currentAnswers.stream()
                .collect(Collectors.toMap(TestSubmissionAnswerEntity::getQuestionId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        for (TestGradeAnswerRequestDTO gradeAnswer : request.getAnswers()) {
            TestQuestionEntity question = questionMap.get(gradeAnswer.getQuestionId());
            if (question == null) {
                throw new BusinessException(404, "题目不存在");
            }
            TestSubmissionAnswerEntity answer = answerMap.get(question.getId());
            if (answer == null) {
                answer = new TestSubmissionAnswerEntity();
                answer.setId(UUID.randomUUID().toString());
                answer.setSubmissionId(submission.getId());
                answer.setQuestionId(question.getId());
                answer.setCreatedAt(TimeUtils.nowUtc());
                currentAnswers.add(answer);
            }
            answer.setScore(Math.max(0, gradeAnswer.getScore()));
            answer.setFeedback(gradeAnswer.getFeedback());
            answer.setIsCorrect(answer.getScore() >= question.getScore());
            answer.setUpdatedAt(TimeUtils.nowUtc());
            if (testSubmissionAnswerMapper.selectById(answer.getId()) == null) {
                testSubmissionAnswerMapper.insert(answer);
            } else {
                testSubmissionAnswerMapper.updateById(answer);
            }
            answerMap.put(question.getId(), answer);
        }

        recalculateSubmission(submission, questionMap, currentAnswers, true);
        testSubmissionMapper.updateById(submission);
        return buildSubmissionView(submission, currentAnswers);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestBatchGradeResultVO batchGradeObjective(UserEntity teacher, String testId) {
        TestEntity test = ensureOwnedTest(teacher.getId(), testId);
        List<TestQuestionEntity> questions = listQuestions(test.getId());
        Map<String, TestQuestionEntity> questionMap = questions.stream()
                .collect(Collectors.toMap(TestQuestionEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<TestSubmissionEntity> submissions = testSubmissionMapper.selectList(new LambdaQueryWrapper<TestSubmissionEntity>()
                .eq(TestSubmissionEntity::getTestId, testId)
                .orderByDesc(TestSubmissionEntity::getSubmittedAt, TestSubmissionEntity::getCreatedAt));
        Map<String, List<TestSubmissionAnswerEntity>> submissionAnswerMap = listSubmissionAnswersBySubmissionIds(submissions.stream().map(TestSubmissionEntity::getId).toList());

        for (TestSubmissionEntity submission : submissions) {
            List<TestSubmissionAnswerEntity> answers = submissionAnswerMap.getOrDefault(submission.getId(), new ArrayList<>());
            autoGradeObjectiveAnswers(questionMap, answers);
            for (TestSubmissionAnswerEntity answer : answers) {
                testSubmissionAnswerMapper.updateById(answer);
            }
            recalculateSubmission(submission, questionMap, answers, false);
            testSubmissionMapper.updateById(submission);
        }

        TestBatchGradeResultVO result = new TestBatchGradeResultVO();
        result.setTestId(testId);
        result.setSubmissions(buildSubmissionViews(submissions, submissionAnswerMap));
        return result;
    }

    public TestStatisticsVO getTeacherStatistics(UserEntity teacher, String testId) {
        ensureOwnedTest(teacher.getId(), testId);
        List<TestSubmissionEntity> submissions = testSubmissionMapper.selectList(new LambdaQueryWrapper<TestSubmissionEntity>()
                .eq(TestSubmissionEntity::getTestId, testId));
        List<TestQuestionEntity> questions = listQuestions(testId);
        Map<String, List<TestSubmissionAnswerEntity>> answerMap = listSubmissionAnswersBySubmissionIds(submissions.stream().map(TestSubmissionEntity::getId).toList());

        TestStatisticsVO statistics = new TestStatisticsVO();
        statistics.setTestId(testId);
        statistics.setTotalSubmissions(submissions.size());
        statistics.setAverageScore(roundDouble(submissions.stream().mapToInt(item -> defaultInt(item.getTotalScore())).average().orElse(0)));
        statistics.setHighestScore(submissions.stream().map(TestSubmissionEntity::getTotalScore).filter(Objects::nonNull).max(Integer::compareTo).orElse(0));
        statistics.setLowestScore(submissions.stream().map(TestSubmissionEntity::getTotalScore).filter(Objects::nonNull).min(Integer::compareTo).orElse(0));
        statistics.setPassRate(submissions.isEmpty() ? 0D : roundDouble(submissions.stream().filter(item -> defaultInt(item.getTotalScore()) >= 60).count() * 100D / submissions.size()));

        List<TestQuestionStatVO> questionStats = new ArrayList<>();
        List<TestWrongDistributionVO> wrongDistribution = new ArrayList<>();
        for (TestQuestionEntity question : questions) {
            List<TestSubmissionAnswerEntity> questionAnswers = answerMap.values().stream()
                    .flatMap(List::stream)
                    .filter(item -> question.getId().equals(item.getQuestionId()))
                    .toList();
            long correctCount = questionAnswers.stream().filter(item -> Boolean.TRUE.equals(item.getIsCorrect())).count();
            int totalCount = submissions.size();

            TestQuestionStatVO questionStat = new TestQuestionStatVO();
            questionStat.setQuestionId(question.getId());
            questionStat.setCorrectCount(Math.toIntExact(correctCount));
            questionStat.setWrongCount(Math.max(totalCount - Math.toIntExact(correctCount), 0));
            questionStat.setCorrectRate(totalCount == 0 ? 0D : roundDouble(correctCount * 100D / totalCount));
            questionStat.setAverageScore(roundDouble(questionAnswers.stream().mapToInt(item -> defaultInt(item.getScore())).average().orElse(0)));
            questionStats.add(questionStat);

            TestWrongDistributionVO distribution = new TestWrongDistributionVO();
            distribution.setQuestionId(question.getId());
            distribution.setContent(question.getContent());
            distribution.setWrongRate(totalCount == 0 ? 0D : roundDouble((totalCount - correctCount) * 100D / totalCount));
            wrongDistribution.add(distribution);
        }
        wrongDistribution.sort(Comparator.comparing(TestWrongDistributionVO::getWrongRate, Comparator.nullsLast(Double::compareTo)).reversed());
        statistics.setQuestionStats(questionStats);
        statistics.setWrongDistribution(wrongDistribution);
        statistics.setLearningBrief(buildLearningBrief(statistics));
        statistics.setAdaptiveRecommendations(buildRecommendations(wrongDistribution));
        return statistics;
    }

    public TestPageDataDTO<TestVO> getStudentTests(UserEntity student, TestQuery query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        LambdaQueryWrapper<TestEntity> wrapper = new LambdaQueryWrapper<TestEntity>()
                .ne(TestEntity::getStatus, TEST_STATUS_DRAFT)
                .apply("course_id in (select cs.course_id from course_students cs where cs.student_id = {0})", student.getId())
                .orderByDesc(TestEntity::getCreatedAt, TestEntity::getId);
        applyQueryFilters(wrapper, query);

        IPage<TestEntity> pageResult = testMapper.selectPage(Page.of(page, pageSize), wrapper);
        return toPageData(pageResult, buildTestViews(pageResult.getRecords(), false, student.getId()), page, pageSize);
    }

    public TestVO getStudentTestDetail(UserEntity student, String testId) {
        return buildTestView(ensureStudentAccessibleTest(student.getId(), testId), false, student.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public TestSubmissionVO submitStudentTest(UserEntity student, String testId, TestSubmitRequestDTO request) {
        TestEntity test = ensureStudentAccessibleTest(student.getId(), testId);
        if (!TEST_STATUS_PUBLISHED.equals(test.getStatus())) {
            throw new BusinessException(409, "当前测试不可提交");
        }

        List<TestQuestionEntity> questions = listQuestions(testId);
        Map<String, TestQuestionEntity> questionMap = questions.stream()
                .collect(Collectors.toMap(TestQuestionEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        validateSubmissionRequest(request, questionMap);

        LocalDateTime now = TimeUtils.nowUtc();
        TestSubmissionEntity submission = testSubmissionMapper.selectOne(new LambdaQueryWrapper<TestSubmissionEntity>()
                .eq(TestSubmissionEntity::getTestId, testId)
                .eq(TestSubmissionEntity::getStudentId, student.getId())
                .last("LIMIT 1"));
        if (submission != null && SUBMISSION_STATUS_GRADED.equals(submission.getStatus())) {
            throw new BusinessException(409, "已批改提交不可重复覆盖");
        }
        if (submission == null) {
            submission = new TestSubmissionEntity();
            submission.setId(UUID.randomUUID().toString());
            submission.setTestId(testId);
            submission.setStudentId(student.getId());
            submission.setCreatedAt(now);
            testSubmissionMapper.insert(submission);
        } else {
            testSubmissionAnswerMapper.delete(new LambdaQueryWrapper<TestSubmissionAnswerEntity>()
                    .eq(TestSubmissionAnswerEntity::getSubmissionId, submission.getId()));
        }

        List<TestSubmissionAnswerEntity> answers = buildSubmissionAnswers(submission.getId(), request.getAnswers(), questionMap, now);
        answers.forEach(testSubmissionAnswerMapper::insert);
        recalculateSubmission(submission, questionMap, answers, false);
        submission.setSubmittedAt(now);
        submission.setUpdatedAt(now);
        testSubmissionMapper.updateById(submission);
        return buildSubmissionView(submission, answers);
    }

    public TestSubmissionVO getStudentSubmission(UserEntity student, String testId) {
        ensureStudentAccessibleTest(student.getId(), testId);
        TestSubmissionEntity submission = testSubmissionMapper.selectOne(new LambdaQueryWrapper<TestSubmissionEntity>()
                .eq(TestSubmissionEntity::getTestId, testId)
                .eq(TestSubmissionEntity::getStudentId, student.getId())
                .last("LIMIT 1"));
        if (submission == null) {
            return null;
        }
        return buildSubmissionView(submission, listSubmissionAnswers(List.of(submission.getId())));
    }

    @Transactional(rollbackFor = Exception.class)
    public TestSubmissionVO appealSubmission(UserEntity student, String submissionId, SubmissionAppealRequestDTO request) {
        TestSubmissionEntity submission = testSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(404, "提交不存在");
        }
        if (!student.getId().equals(submission.getStudentId())) {
            throw new BusinessException(403, "无权申诉该提交");
        }
        if (!SUBMISSION_STATUS_GRADED.equals(submission.getStatus())) {
            throw new BusinessException(422, "仅已批改提交允许申诉");
        }
        if (StringUtils.hasText(submission.getAppealStatus())) {
            throw new BusinessException(422, "当前提交已发起申诉");
        }
        submission.setAppealReason(request.getReason());
        submission.setAppealStatus(APPEAL_STATUS_PENDING);
        submission.setUpdatedAt(TimeUtils.nowUtc());
        testSubmissionMapper.updateById(submission);
        return buildSubmissionView(submission, listSubmissionAnswers(List.of(submission.getId())));
    }

    private void applyQueryFilters(LambdaQueryWrapper<TestEntity> wrapper, TestQuery query) {
        if (StringUtils.hasText(query.getCourseId())) {
            wrapper.eq(TestEntity::getCourseId, query.getCourseId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(condition -> condition
                    .like(TestEntity::getTitle, query.getKeyword())
                    .or()
                    .like(TestEntity::getDescription, query.getKeyword()));
        }
    }

    private TestPageDataDTO<TestVO> toPageData(IPage<TestEntity> pageResult, List<TestVO> list, long page, long pageSize) {
        TestPageDataDTO<TestVO> response = new TestPageDataDTO<>();
        response.setList(list);
        response.setTotal(pageResult.getTotal());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages(pageResult.getPages());
        return response;
    }

    private CourseEntity ensureOwnedCourse(String teacherId, String courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "无权操作该课程下的测试");
        }
        return course;
    }

    private TestEntity ensureOwnedTest(String teacherId, String testId) {
        TestEntity test = testMapper.selectById(testId);
        if (test == null) {
            throw new BusinessException(404, "测试不存在");
        }
        CourseEntity course = courseMapper.selectById(test.getCourseId());
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "无权访问该测试");
        }
        return test;
    }

    private TestEntity ensureStudentAccessibleTest(String studentId, String testId) {
        TestEntity test = testMapper.selectById(testId);
        if (test == null) {
            throw new BusinessException(404, "测试不存在");
        }
        if (TEST_STATUS_DRAFT.equals(test.getStatus())) {
            throw new BusinessException(403, "无权访问该测试");
        }
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudentEntity>()
                .eq(CourseStudentEntity::getCourseId, test.getCourseId())
                .eq(CourseStudentEntity::getStudentId, studentId));
        if (count == null || count < 1) {
            throw new BusinessException(403, "无权访问该测试");
        }
        return test;
    }

    private void ensureEditable(TestEntity test) {
        if (!TEST_STATUS_DRAFT.equals(test.getStatus())) {
            throw new BusinessException(409, "测试已发布不可修改");
        }
    }

    private void validateTestSaveRequest(TestSaveRequestDTO request) {
        if (CollectionUtils.isEmpty(request.getQuestions())) {
            throw new BusinessException(422, "questions至少包含1题");
        }
        for (TestQuestionRequestDTO question : request.getQuestions()) {
            if (!QUESTION_TYPES.contains(question.getType())) {
                throw new BusinessException(422, "题目类型不支持");
            }
            if (QUESTION_TYPE_SINGLE_CHOICE.equals(question.getType()) && CollectionUtils.isEmpty(question.getOptions())) {
                throw new BusinessException(422, "单选题必须配置选项");
            }
        }
    }

    private int sumQuestionScores(List<TestQuestionRequestDTO> questions) {
        return questions.stream().mapToInt(TestQuestionRequestDTO::getScore).sum();
    }

    private void saveQuestions(String testId, List<TestQuestionRequestDTO> questions, LocalDateTime now) {
        for (int i = 0; i < questions.size(); i++) {
            TestQuestionRequestDTO request = questions.get(i);
            TestQuestionEntity question = new TestQuestionEntity();
            question.setId(UUID.randomUUID().toString());
            question.setTestId(testId);
            question.setType(request.getType());
            question.setContent(request.getContent());
            question.setAnswer(request.getAnswer());
            question.setScore(request.getScore());
            question.setSortOrder(i + 1);
            question.setAnalysis(request.getAnalysis());
            question.setCreatedAt(now);
            testQuestionMapper.insert(question);
            saveQuestionOptions(question.getId(), request.getOptions());
        }
    }

    private void saveQuestionOptions(String questionId, List<TestQuestionOptionRequestDTO> options) {
        if (CollectionUtils.isEmpty(options)) {
            return;
        }
        for (int i = 0; i < options.size(); i++) {
            TestQuestionOptionRequestDTO optionRequest = options.get(i);
            TestQuestionOptionEntity option = new TestQuestionOptionEntity();
            option.setId(UUID.randomUUID().toString());
            option.setQuestionId(questionId);
            option.setLabel(optionRequest.getLabel());
            option.setContent(optionRequest.getContent());
            option.setSortOrder(i + 1);
            testQuestionOptionMapper.insert(option);
        }
    }

    private void deleteQuestionsAndOptions(String testId) {
        List<TestQuestionEntity> questions = listQuestions(testId);
        if (questions.isEmpty()) {
            return;
        }
        List<String> questionIds = questions.stream().map(TestQuestionEntity::getId).toList();
        testQuestionOptionMapper.delete(new LambdaQueryWrapper<TestQuestionOptionEntity>()
                .in(TestQuestionOptionEntity::getQuestionId, questionIds));
        testQuestionMapper.delete(new LambdaQueryWrapper<TestQuestionEntity>()
                .eq(TestQuestionEntity::getTestId, testId));
    }

    private List<TestQuestionEntity> listQuestions(String testId) {
        return testQuestionMapper.selectList(new LambdaQueryWrapper<TestQuestionEntity>()
                .eq(TestQuestionEntity::getTestId, testId)
                .orderByAsc(TestQuestionEntity::getSortOrder, TestQuestionEntity::getCreatedAt));
    }

    private List<TestVO> buildTestViews(List<TestEntity> tests, boolean teacherView, String studentId) {
        if (CollectionUtils.isEmpty(tests)) {
            return Collections.emptyList();
        }
        List<String> courseIds = tests.stream().map(TestEntity::getCourseId).distinct().toList();
        Map<String, String> courseNameMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(CourseEntity::getId, CourseEntity::getName, (left, right) -> left, LinkedHashMap::new));
        List<String> testIds = tests.stream().map(TestEntity::getId).toList();
        Map<String, List<TestQuestionEntity>> questionMap = listQuestionsByTestIds(testIds);
        List<String> questionIds = questionMap.values().stream().flatMap(List::stream).map(TestQuestionEntity::getId).toList();
        Map<String, List<TestQuestionOptionEntity>> optionMap = listQuestionOptionsByQuestionIds(questionIds);
        Map<String, List<TestSubmissionVO>> submissionMap = teacherView
                ? buildSubmissionViewsByTestIds(testIds)
                : buildSubmissionViewsByTestIdsAndStudent(testIds, studentId);

        List<TestVO> views = new ArrayList<>();
        for (TestEntity test : tests) {
            List<TestSubmissionVO> submissions = submissionMap.getOrDefault(test.getId(), new ArrayList<>());
            boolean includeAnswer = teacherView || canStudentSeeAnswer(test, submissions.isEmpty() ? null : submissions.get(0));
            views.add(toTestView(test,
                    courseNameMap.get(test.getCourseId()),
                    buildQuestionViews(questionMap.getOrDefault(test.getId(), new ArrayList<>()), optionMap, includeAnswer),
                    submissions));
        }
        return views;
    }

    private TestVO buildTestView(TestEntity test, boolean teacherView, String studentId) {
        return buildTestViews(List.of(test), teacherView, studentId).get(0);
    }

    private Map<String, List<TestQuestionEntity>> listQuestionsByTestIds(List<String> testIds) {
        if (CollectionUtils.isEmpty(testIds)) {
            return Collections.emptyMap();
        }
        return testQuestionMapper.selectList(new LambdaQueryWrapper<TestQuestionEntity>()
                        .in(TestQuestionEntity::getTestId, testIds)
                        .orderByAsc(TestQuestionEntity::getSortOrder, TestQuestionEntity::getCreatedAt))
                .stream()
                .collect(Collectors.groupingBy(TestQuestionEntity::getTestId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<TestQuestionOptionEntity>> listQuestionOptionsByQuestionIds(List<String> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        return testQuestionOptionMapper.selectList(new LambdaQueryWrapper<TestQuestionOptionEntity>()
                        .in(TestQuestionOptionEntity::getQuestionId, questionIds)
                        .orderByAsc(TestQuestionOptionEntity::getSortOrder, TestQuestionOptionEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(TestQuestionOptionEntity::getQuestionId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<TestQuestionVO> buildQuestionViews(List<TestQuestionEntity> questions,
                                                    Map<String, List<TestQuestionOptionEntity>> optionMap,
                                                    boolean includeAnswer) {
        List<TestQuestionVO> views = new ArrayList<>();
        for (TestQuestionEntity question : questions) {
            TestQuestionVO view = new TestQuestionVO();
            view.setId(question.getId());
            view.setTestId(question.getTestId());
            view.setType(question.getType());
            view.setContent(question.getContent());
            view.setAnswer(includeAnswer ? question.getAnswer() : null);
            view.setScore(question.getScore());
            view.setOrder(question.getSortOrder());
            view.setAnalysis(includeAnswer ? question.getAnalysis() : null);
            view.setOptions(optionMap.getOrDefault(question.getId(), new ArrayList<>()).stream().map(this::toOptionView).toList());
            views.add(view);
        }
        return views;
    }

    private TestQuestionOptionVO toOptionView(TestQuestionOptionEntity option) {
        TestQuestionOptionVO view = new TestQuestionOptionVO();
        view.setId(option.getId());
        view.setLabel(option.getLabel());
        view.setContent(option.getContent());
        return view;
    }

    private Map<String, List<TestSubmissionVO>> buildSubmissionViewsByTestIds(List<String> testIds) {
        if (CollectionUtils.isEmpty(testIds)) {
            return Collections.emptyMap();
        }
        List<TestSubmissionEntity> submissions = testSubmissionMapper.selectList(new LambdaQueryWrapper<TestSubmissionEntity>()
                .in(TestSubmissionEntity::getTestId, testIds)
                .orderByDesc(TestSubmissionEntity::getSubmittedAt, TestSubmissionEntity::getCreatedAt));
        Map<String, List<TestSubmissionAnswerEntity>> answerMap = listSubmissionAnswersBySubmissionIds(submissions.stream().map(TestSubmissionEntity::getId).toList());
        return buildSubmissionViews(submissions, answerMap).stream()
                .collect(Collectors.groupingBy(TestSubmissionVO::getTestId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<TestSubmissionVO>> buildSubmissionViewsByTestIdsAndStudent(List<String> testIds, String studentId) {
        if (CollectionUtils.isEmpty(testIds) || !StringUtils.hasText(studentId)) {
            return Collections.emptyMap();
        }
        List<TestSubmissionEntity> submissions = testSubmissionMapper.selectList(new LambdaQueryWrapper<TestSubmissionEntity>()
                .in(TestSubmissionEntity::getTestId, testIds)
                .eq(TestSubmissionEntity::getStudentId, studentId)
                .orderByDesc(TestSubmissionEntity::getSubmittedAt, TestSubmissionEntity::getCreatedAt));
        Map<String, List<TestSubmissionAnswerEntity>> answerMap = listSubmissionAnswersBySubmissionIds(submissions.stream().map(TestSubmissionEntity::getId).toList());
        return buildSubmissionViews(submissions, answerMap).stream()
                .collect(Collectors.groupingBy(TestSubmissionVO::getTestId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<TestSubmissionAnswerEntity>> listSubmissionAnswersBySubmissionIds(List<String> submissionIds) {
        if (CollectionUtils.isEmpty(submissionIds)) {
            return Collections.emptyMap();
        }
        return testSubmissionAnswerMapper.selectList(new LambdaQueryWrapper<TestSubmissionAnswerEntity>()
                        .in(TestSubmissionAnswerEntity::getSubmissionId, submissionIds)
                        .orderByAsc(TestSubmissionAnswerEntity::getCreatedAt, TestSubmissionAnswerEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(TestSubmissionAnswerEntity::getSubmissionId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<TestSubmissionAnswerEntity> listSubmissionAnswers(List<String> submissionIds) {
        if (CollectionUtils.isEmpty(submissionIds)) {
            return Collections.emptyList();
        }
        return testSubmissionAnswerMapper.selectList(new LambdaQueryWrapper<TestSubmissionAnswerEntity>()
                .in(TestSubmissionAnswerEntity::getSubmissionId, submissionIds)
                .orderByAsc(TestSubmissionAnswerEntity::getCreatedAt, TestSubmissionAnswerEntity::getId));
    }

    private List<TestSubmissionVO> buildSubmissionViews(List<TestSubmissionEntity> submissions,
                                                        Map<String, List<TestSubmissionAnswerEntity>> answerMap) {
        if (CollectionUtils.isEmpty(submissions)) {
            return Collections.emptyList();
        }
        List<String> studentIds = submissions.stream().map(TestSubmissionEntity::getStudentId).distinct().toList();
        Map<String, UserEntity> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, StudentProfileEntity> profileMap = studentProfileMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(StudentProfileEntity::getStudentId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<TestSubmissionVO> views = new ArrayList<>();
        for (TestSubmissionEntity submission : submissions) {
            UserEntity user = userMap.get(submission.getStudentId());
            StudentProfileEntity profile = profileMap.get(submission.getStudentId());
            views.add(toSubmissionView(submission,
                    answerMap.getOrDefault(submission.getId(), new ArrayList<>()),
                    user == null ? null : user.getRealName(),
                    profile == null ? null : profile.getStudentNo()));
        }
        return views;
    }

    private TestSubmissionVO buildSubmissionView(TestSubmissionEntity submission, List<TestSubmissionAnswerEntity> answers) {
        UserEntity user = userMapper.selectById(submission.getStudentId());
        StudentProfileEntity profile = studentProfileMapper.selectById(submission.getStudentId());
        return toSubmissionView(submission,
                answers,
                user == null ? null : user.getRealName(),
                profile == null ? null : profile.getStudentNo());
    }

    private TestSubmissionVO toSubmissionView(TestSubmissionEntity submission,
                                              List<TestSubmissionAnswerEntity> answers,
                                              String studentName,
                                              String studentNo) {
        TestSubmissionVO view = new TestSubmissionVO();
        view.setId(submission.getId());
        view.setTestId(submission.getTestId());
        view.setStudentId(submission.getStudentId());
        view.setStudentName(studentName);
        view.setStudentNo(studentNo);
        view.setAnswers(answers.stream().map(this::toSubmissionAnswerView).toList());
        view.setTotalScore(submission.getTotalScore());
        view.setStatus(submission.getStatus());
        view.setSubmittedAt(TimeUtils.toIsoUtc(submission.getSubmittedAt()));
        view.setGradedAt(TimeUtils.toIsoUtc(submission.getGradedAt()));
        view.setAppealReason(submission.getAppealReason());
        view.setAppealStatus(submission.getAppealStatus());
        view.setAnalysisSummary(submission.getAnalysisSummary());
        view.setCreatedAt(TimeUtils.toIsoUtc(submission.getCreatedAt()));
        return view;
    }

    private SubmissionAnswerVO toSubmissionAnswerView(TestSubmissionAnswerEntity answer) {
        SubmissionAnswerVO view = new SubmissionAnswerVO();
        view.setQuestionId(answer.getQuestionId());
        view.setAnswer(answer.getAnswer());
        view.setScore(answer.getScore());
        view.setFeedback(answer.getFeedback());
        view.setIsCorrect(answer.getIsCorrect());
        return view;
    }

    private TestVO toTestView(TestEntity test,
                              String courseName,
                              List<TestQuestionVO> questions,
                              List<TestSubmissionVO> submissions) {
        TestVO view = new TestVO();
        view.setId(test.getId());
        view.setCourseId(test.getCourseId());
        view.setCourseName(courseName);
        view.setTitle(test.getTitle());
        view.setDescription(test.getDescription());
        view.setDuration(test.getDuration());
        view.setTotalScore(test.getTotalScore());
        view.setShowAnswer(test.getShowAnswer());
        view.setStatus(test.getStatus());
        view.setQuestions(questions);
        view.setSubmissions(submissions);
        view.setCreatedAt(TimeUtils.toIsoUtc(test.getCreatedAt()));
        view.setUpdatedAt(TimeUtils.toIsoUtc(test.getUpdatedAt()));
        return view;
    }

    private boolean canStudentSeeAnswer(TestEntity test, TestSubmissionVO submission) {
        return Boolean.TRUE.equals(test.getShowAnswer())
                && (submission != null || TEST_STATUS_ENDED.equals(test.getStatus()));
    }

    private void validateSubmissionRequest(TestSubmitRequestDTO request, Map<String, TestQuestionEntity> questionMap) {
        Set<String> visited = new LinkedHashSet<>();
        for (TestSubmitAnswerRequestDTO answer : request.getAnswers()) {
            if (!questionMap.containsKey(answer.getQuestionId())) {
                throw new BusinessException(404, "题目不存在");
            }
            if (!visited.add(answer.getQuestionId())) {
                throw new BusinessException(422, "题目答案重复提交");
            }
        }
    }

    private List<TestSubmissionAnswerEntity> buildSubmissionAnswers(String submissionId,
                                                                    List<TestSubmitAnswerRequestDTO> requests,
                                                                    Map<String, TestQuestionEntity> questionMap,
                                                                    LocalDateTime now) {
        List<TestSubmissionAnswerEntity> entities = new ArrayList<>();
        for (TestQuestionEntity question : questionMap.values()) {
            TestSubmitAnswerRequestDTO request = requests.stream()
                    .filter(item -> question.getId().equals(item.getQuestionId()))
                    .findFirst()
                    .orElse(null);
            String answerText = request == null ? null : request.getAnswer();
            AutoGradeResult autoGrade = autoGrade(question, answerText);

            TestSubmissionAnswerEntity entity = new TestSubmissionAnswerEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setSubmissionId(submissionId);
            entity.setQuestionId(question.getId());
            entity.setAnswer(answerText);
            entity.setScore(autoGrade.score());
            entity.setFeedback(autoGrade.feedback());
            entity.setIsCorrect(autoGrade.isCorrect());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entities.add(entity);
        }
        return entities;
    }

    private AutoGradeResult autoGrade(TestQuestionEntity question, String answer) {
        if (QUESTION_TYPE_SHORT_ANSWER.equals(question.getType())) {
            return new AutoGradeResult(null, "主观题待教师批改", null);
        }
        if (!StringUtils.hasText(answer)) {
            return new AutoGradeResult(0, "未作答", false);
        }
        boolean correct = Objects.equals(normalizeAnswer(question.getAnswer()), normalizeAnswer(answer));
        return new AutoGradeResult(correct ? question.getScore() : 0, correct ? "回答正确" : "回答错误", correct);
    }

    private void autoGradeObjectiveAnswers(Map<String, TestQuestionEntity> questionMap, List<TestSubmissionAnswerEntity> answers) {
        for (TestSubmissionAnswerEntity answer : answers) {
            TestQuestionEntity question = questionMap.get(answer.getQuestionId());
            if (question == null || QUESTION_TYPE_SHORT_ANSWER.equals(question.getType())) {
                continue;
            }
            AutoGradeResult result = autoGrade(question, answer.getAnswer());
            answer.setScore(result.score());
            answer.setFeedback(result.feedback());
            answer.setIsCorrect(result.isCorrect());
            answer.setUpdatedAt(TimeUtils.nowUtc());
        }
    }

    private void recalculateSubmission(TestSubmissionEntity submission,
                                       Map<String, TestQuestionEntity> questionMap,
                                       List<TestSubmissionAnswerEntity> answers,
                                       boolean forceGraded) {
        int totalScore = answers.stream().mapToInt(item -> defaultInt(item.getScore())).sum();
        boolean hasUngradedShortAnswer = answers.stream().anyMatch(item -> {
            TestQuestionEntity question = questionMap.get(item.getQuestionId());
            return question != null && QUESTION_TYPE_SHORT_ANSWER.equals(question.getType()) && item.getScore() == null;
        });

        submission.setTotalScore(totalScore);
        submission.setStatus(forceGraded || !hasUngradedShortAnswer ? SUBMISSION_STATUS_GRADED : SUBMISSION_STATUS_SUBMITTED);
        submission.setGradedAt(SUBMISSION_STATUS_GRADED.equals(submission.getStatus()) ? TimeUtils.nowUtc() : null);
        submission.setAnalysisSummary(buildAnalysisSummary(submission.getStatus(), totalScore, hasUngradedShortAnswer));
        submission.setUpdatedAt(TimeUtils.nowUtc());
    }

    private String buildAnalysisSummary(String status, int totalScore, boolean hasUngradedShortAnswer) {
        if (hasUngradedShortAnswer && !SUBMISSION_STATUS_GRADED.equals(status)) {
            return "客观题已自动批改，主观题待教师批改。";
        }
        if (totalScore >= 90) {
            return "整体作答优秀，知识掌握较扎实。";
        }
        if (totalScore >= 60) {
            return "整体作答较稳定，仍有部分知识点可继续巩固。";
        }
        return "基础题仍需加强，建议针对薄弱知识点继续练习。";
    }

    private String buildLearningBrief(TestStatisticsVO statistics) {
        double averageScore = statistics.getAverageScore() == null ? 0D : statistics.getAverageScore();
        if (averageScore >= 85) {
            return "学生整体掌握较好，测试目标达成度较高。";
        }
        if (averageScore >= 60) {
            return "学生整体基础较稳，但部分题型仍需针对性训练。";
        }
        return "学生整体掌握偏弱，建议先回归基础知识再做专项练习。";
    }

    private List<String> buildRecommendations(List<TestWrongDistributionVO> wrongDistributions) {
        if (CollectionUtils.isEmpty(wrongDistributions)) {
            return List.of("保持当前学习节奏，继续巩固核心知识点");
        }
        return wrongDistributions.stream()
                .filter(item -> item.getWrongRate() != null && item.getWrongRate() > 0)
                .limit(3)
                .map(item -> "重点复习题目相关知识点：" + abbreviate(item.getContent(), 24))
                .toList();
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String normalizeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return null;
        }
        return answer.trim().replace("\r", "").replace("\n", "").toLowerCase(Locale.ROOT);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double roundDouble(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record AutoGradeResult(Integer score, String feedback, Boolean isCorrect) {
    }
}
