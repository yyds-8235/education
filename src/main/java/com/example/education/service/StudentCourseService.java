package com.example.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.education.common.exception.BusinessException;
import com.example.education.common.util.TimeUtils;
import com.example.education.mapper.CourseChapterMapper;
import com.example.education.mapper.CourseMapper;
import com.example.education.mapper.CourseResourceMapper;
import com.example.education.mapper.CourseStudentMapper;
import com.example.education.mapper.StudentProfileMapper;
import com.example.education.mapper.UserMapper;
import com.example.education.pojo.dto.CoursePageDataDTO;
import com.example.education.pojo.entity.CourseChapterEntity;
import com.example.education.pojo.entity.CourseEntity;
import com.example.education.pojo.entity.CourseResourceEntity;
import com.example.education.pojo.entity.CourseStudentEntity;
import com.example.education.pojo.entity.StudentProfileEntity;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.query.CourseQuery;
import com.example.education.pojo.vo.CourseChapterVO;
import com.example.education.pojo.vo.CourseResourceVO;
import com.example.education.pojo.vo.CourseStudentVO;
import com.example.education.pojo.vo.CourseVO;
import com.example.education.pojo.vo.StudentJoinCourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentCourseService {

    private static final String COURSE_SCOPE_ALL = "all";
    private static final String COURSE_SCOPE_JOINED = "joined";
    private static final String COURSE_SCOPE_DISCOVER = "discover";
    private static final String COURSE_VISIBILITY_PUBLIC = "public";
    private static final String JOINED_EXISTS_SQL = "EXISTS (SELECT 1 FROM course_students cs WHERE cs.course_id = courses.id AND cs.student_id = {0})";
    private static final String JOINED_NOT_EXISTS_SQL = "NOT EXISTS (SELECT 1 FROM course_students cs WHERE cs.course_id = courses.id AND cs.student_id = {0})";

    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseResourceMapper courseResourceMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;

    public CoursePageDataDTO getCourses(UserEntity student, CourseQuery query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        String scope = normalizeScope(query.getScope());

        Page<CourseEntity> pageRequest = Page.of(page, pageSize);
        LambdaQueryWrapper<CourseEntity> wrapper = new LambdaQueryWrapper<CourseEntity>()
                .orderByDesc(CourseEntity::getCreatedAt);

        applyCommonFilters(wrapper, query);
        applyScopeFilter(wrapper, scope, student.getId());

        IPage<CourseEntity> pageResult = courseMapper.selectPage(pageRequest, wrapper);
        Map<String, String> teacherNameMap = loadTeacherNames(pageResult.getRecords());
        Set<String> joinedCourseIds = loadJoinedCourseIds(student.getId(), pageResult.getRecords());

        CoursePageDataDTO response = new CoursePageDataDTO();
        response.setList(pageResult.getRecords().stream()
                .map(course -> toCourseView(course,
                        teacherNameMap.get(course.getTeacherId()),
                        joinedCourseIds.contains(course.getId())))
                .toList());
        response.setTotal(pageResult.getTotal());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages(pageResult.getPages());
        return response;
    }

    public CourseVO getCourseDetail(UserEntity student, String courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }

        boolean joined = hasJoinedCourse(student.getId(), courseId);
        if (!joined && !COURSE_VISIBILITY_PUBLIC.equals(course.getVisibility())) {
            throw new BusinessException(403, "无权查看该课程");
        }

        String teacherName = loadTeacherNames(List.of(course)).get(course.getTeacherId());
        return buildCourseDetail(course, teacherName, joined);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentJoinCourseVO joinCourse(UserEntity student, String courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!COURSE_VISIBILITY_PUBLIC.equals(course.getVisibility())) {
            throw new BusinessException(400, "仅公开课程允许加入");
        }
        if (hasJoinedCourse(student.getId(), courseId)) {
            throw new BusinessException(400, "当前学生已加入该课程");
        }

        StudentProfileEntity studentProfile = studentProfileMapper.selectById(student.getId());
        if (studentProfile == null || !StringUtils.hasText(studentProfile.getStudentNo())) {
            throw new BusinessException(400, "学生档案不存在或学号缺失");
        }

        LocalDateTime now = TimeUtils.nowUtc();
        CourseStudentEntity relation = new CourseStudentEntity();
        relation.setId(UUID.randomUUID().toString());
        relation.setCourseId(courseId);
        relation.setStudentId(student.getId());
        relation.setStudentNo(studentProfile.getStudentNo());
        relation.setJoinedAt(now);
        relation.setProgress(BigDecimal.ZERO);

        try {
            courseStudentMapper.insert(relation);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "当前学生已加入该课程");
        }

        refreshStudentCount(courseId);

        StudentJoinCourseVO response = new StudentJoinCourseVO();
        response.setCourseId(courseId);
        response.setStudent(toCourseStudentView(relation, student.getRealName()));
        return response;
    }

    private void applyCommonFilters(LambdaQueryWrapper<CourseEntity> wrapper, CourseQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(condition -> condition
                    .like(CourseEntity::getName, query.getKeyword())
                    .or()
                    .like(CourseEntity::getDescription, query.getKeyword())
                    .or()
                    .like(CourseEntity::getSubject, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getGrade())) {
            wrapper.eq(CourseEntity::getGrade, query.getGrade());
        }
        if (StringUtils.hasText(query.getClassName())) {
            wrapper.eq(CourseEntity::getClassName, query.getClassName());
        }
        if (StringUtils.hasText(query.getSubject())) {
            wrapper.eq(CourseEntity::getSubject, query.getSubject());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CourseEntity::getStatus, query.getStatus());
        }
    }

    private void applyScopeFilter(LambdaQueryWrapper<CourseEntity> wrapper, String scope, String studentId) {
        if (COURSE_SCOPE_JOINED.equals(scope)) {
            wrapper.apply(JOINED_EXISTS_SQL, studentId);
            return;
        }
        if (COURSE_SCOPE_DISCOVER.equals(scope)) {
            wrapper.eq(CourseEntity::getVisibility, COURSE_VISIBILITY_PUBLIC)
                    .apply(JOINED_NOT_EXISTS_SQL, studentId);
            return;
        }
        wrapper.and(condition -> condition
                .apply(JOINED_EXISTS_SQL, studentId)
                .or(publicCondition -> publicCondition
                        .eq(CourseEntity::getVisibility, COURSE_VISIBILITY_PUBLIC)
                        .apply(JOINED_NOT_EXISTS_SQL, studentId)));
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return COURSE_SCOPE_ALL;
        }

        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        if (COURSE_SCOPE_ALL.equals(normalized)
                || COURSE_SCOPE_JOINED.equals(normalized)
                || COURSE_SCOPE_DISCOVER.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(422, "scope 参数不合法");
    }

    private Set<String> loadJoinedCourseIds(String studentId, List<CourseEntity> courses) {
        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptySet();
        }

        List<String> courseIds = courses.stream().map(CourseEntity::getId).toList();
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudentEntity>()
                        .eq(CourseStudentEntity::getStudentId, studentId)
                        .in(CourseStudentEntity::getCourseId, courseIds))
                .stream()
                .map(CourseStudentEntity::getCourseId)
                .collect(Collectors.toSet());
    }

    private boolean hasJoinedCourse(String studentId, String courseId) {
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudentEntity>()
                .eq(CourseStudentEntity::getStudentId, studentId)
                .eq(CourseStudentEntity::getCourseId, courseId));
        return count != null && count > 0;
    }

    private Map<String, String> loadTeacherNames(List<CourseEntity> courses) {
        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptyMap();
        }

        List<String> teacherIds = courses.stream()
                .map(CourseEntity::getTeacherId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(UserEntity::getId,
                        user -> StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private CourseVO buildCourseDetail(CourseEntity course, String teacherName, boolean joined) {
        CourseVO view = toCourseView(course, teacherName, joined);
        List<CourseChapterEntity> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapterEntity>()
                .eq(CourseChapterEntity::getCourseId, course.getId())
                .orderByAsc(CourseChapterEntity::getSortOrder, CourseChapterEntity::getCreatedAt));

        if (CollectionUtils.isEmpty(chapters)) {
            return view;
        }

        List<String> chapterIds = chapters.stream().map(CourseChapterEntity::getId).toList();
        Map<String, List<CourseResourceVO>> resourceMap = courseResourceMapper.selectList(new LambdaQueryWrapper<CourseResourceEntity>()
                        .in(CourseResourceEntity::getChapterId, chapterIds)
                        .orderByAsc(CourseResourceEntity::getSortOrder, CourseResourceEntity::getCreatedAt))
                .stream()
                .map(this::toCourseResourceView)
                .collect(Collectors.groupingBy(CourseResourceVO::getChapterId, LinkedHashMap::new, Collectors.toList()));

        List<CourseChapterVO> chapterViews = new ArrayList<>();
        for (CourseChapterEntity chapter : chapters) {
            CourseChapterVO chapterView = new CourseChapterVO();
            chapterView.setId(chapter.getId());
            chapterView.setCourseId(chapter.getCourseId());
            chapterView.setTitle(chapter.getTitle());
            chapterView.setDescription(chapter.getDescription());
            chapterView.setOrder(chapter.getSortOrder());
            chapterView.setCreatedAt(TimeUtils.toIsoUtc(chapter.getCreatedAt()));
            chapterView.setResources(resourceMap.getOrDefault(chapter.getId(), new ArrayList<>()));
            chapterViews.add(chapterView);
        }
        view.setChapters(chapterViews);
        return view;
    }

    private CourseVO toCourseView(CourseEntity course, String teacherName, boolean joined) {
        CourseVO view = new CourseVO();
        view.setId(course.getId());
        view.setName(course.getName());
        view.setDescription(course.getDescription());
        view.setGrade(course.getGrade());
        view.setClazz(course.getClassName());
        view.setSubject(course.getSubject());
        view.setTeacherId(course.getTeacherId());
        view.setTeacherName(StringUtils.hasText(teacherName) ? teacherName : null);
        view.setVisibility(course.getVisibility());
        view.setCoverImage(course.getCoverImage());
        view.setStudentCount(course.getStudentCount());
        view.setJoined(joined);
        view.setStatus(course.getStatus());
        view.setCreatedAt(TimeUtils.toIsoUtc(course.getCreatedAt()));
        view.setUpdatedAt(TimeUtils.toIsoUtc(course.getUpdatedAt()));
        view.setChapters(new ArrayList<>());
        return view;
    }

    private CourseResourceVO toCourseResourceView(CourseResourceEntity resource) {
        CourseResourceVO view = new CourseResourceVO();
        view.setId(resource.getId());
        view.setChapterId(resource.getChapterId());
        view.setName(resource.getName());
        view.setType(resource.getType());
        view.setUrl(resource.getUrl());
        view.setBucketName(resource.getBucketName());
        view.setObjectKey(resource.getObjectKey());
        view.setSize(resource.getSize());
        view.setDuration(resource.getDuration());
        view.setOrder(resource.getSortOrder());
        view.setCreatedAt(TimeUtils.toIsoUtc(resource.getCreatedAt()));
        return view;
    }

    private CourseStudentVO toCourseStudentView(CourseStudentEntity relation, String studentName) {
        CourseStudentVO view = new CourseStudentVO();
        view.setId(relation.getId());
        view.setCourseId(relation.getCourseId());
        view.setStudentId(relation.getStudentId());
        view.setStudentName(studentName);
        view.setStudentNo(relation.getStudentNo());
        view.setJoinedAt(TimeUtils.toIsoUtc(relation.getJoinedAt()));
        view.setProgress(relation.getProgress());
        return view;
    }

    private void refreshStudentCount(String courseId) {
        long total = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudentEntity>()
                .eq(CourseStudentEntity::getCourseId, courseId));
        courseMapper.update(null, new LambdaUpdateWrapper<CourseEntity>()
                .eq(CourseEntity::getId, courseId)
                .set(CourseEntity::getStudentCount, Math.toIntExact(total))
                .set(CourseEntity::getUpdatedAt, TimeUtils.nowUtc()));
    }
}
