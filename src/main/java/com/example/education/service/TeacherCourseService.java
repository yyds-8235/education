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
import com.example.education.pojo.dto.AddCourseStudentsRequestDTO;
import com.example.education.pojo.dto.CourseChapterRequestDTO;
import com.example.education.pojo.dto.CoursePageDataDTO;
import com.example.education.pojo.dto.CourseResourceRequestDTO;
import com.example.education.pojo.dto.CourseSaveRequestDTO;
import com.example.education.pojo.entity.CourseChapterEntity;
import com.example.education.pojo.entity.CourseEntity;
import com.example.education.pojo.entity.CourseResourceEntity;
import com.example.education.pojo.entity.CourseStudentEntity;
import com.example.education.pojo.entity.StudentProfileEntity;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.model.CourseSelectableStudentRow;
import com.example.education.pojo.model.CourseStudentRow;
import com.example.education.pojo.query.CourseQuery;
import com.example.education.pojo.vo.CourseChapterVO;
import com.example.education.pojo.vo.CourseResourceVO;
import com.example.education.pojo.vo.CourseSelectableStudentVO;
import com.example.education.pojo.vo.CourseStudentBatchResultVO;
import com.example.education.pojo.vo.CourseStudentVO;
import com.example.education.pojo.vo.CourseUploadResourceVO;
import com.example.education.pojo.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherCourseService {

    private static final Set<String> COURSE_VISIBILITIES = Set.of("public", "private");
    private static final Set<String> COURSE_STATUSES = Set.of("draft", "active", "archived");
    private static final Set<String> RESOURCE_TYPES = Set.of("video", "ppt", "word", "pdf", "other");

    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseResourceMapper courseResourceMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;
    private final MinioStorageService minioStorageService;

    public CoursePageDataDTO getCourses(UserEntity teacher, CourseQuery query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<CourseEntity> pageRequest = Page.of(page, pageSize);
        LambdaQueryWrapper<CourseEntity> wrapper = new LambdaQueryWrapper<CourseEntity>()
                .eq(CourseEntity::getTeacherId, teacher.getId())
                .orderByDesc(CourseEntity::getCreatedAt);

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

        IPage<CourseEntity> pageResult = courseMapper.selectPage(pageRequest, wrapper);

        CoursePageDataDTO response = new CoursePageDataDTO();
        response.setList(pageResult.getRecords().stream()
                .map(course -> toCourseView(course, teacher.getRealName(), false))
                .toList());
        response.setTotal(pageResult.getTotal());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages(pageResult.getPages());
        return response;
    }

    public CourseVO getCourseDetail(UserEntity teacher, String courseId) {
        CourseEntity course = ensureOwnedCourse(teacher.getId(), courseId);
        return buildCourseDetail(course, teacher.getRealName());
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseVO createCourse(UserEntity teacher, CourseSaveRequestDTO request) {
        validateCourseRequest(request, teacher.getId());

        LocalDateTime now = TimeUtils.nowUtc();
        String courseId = UUID.randomUUID().toString();

        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setGrade(request.getGrade());
        course.setClassName(request.getClazz());
        course.setSubject(request.getSubject());
        course.setTeacherId(teacher.getId());
        course.setVisibility(request.getVisibility());
        course.setCoverImage(request.getCoverImage());
        course.setStudentCount(0);
        course.setStatus(request.getStatus());
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        courseMapper.insert(course);

        saveChaptersAndResources(courseId, teacher.getId(), request.getChapters(), now);
        return buildCourseDetail(courseMapper.selectById(courseId), teacher.getRealName());
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseVO updateCourse(UserEntity teacher, String courseId, CourseSaveRequestDTO request) {
        CourseEntity existing = ensureOwnedCourse(teacher.getId(), courseId);
        validateCourseRequest(request, teacher.getId());

        deleteObsoleteResourceObjects(existing.getId(), request.getChapters());

        LocalDateTime now = TimeUtils.nowUtc();
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setGrade(request.getGrade());
        existing.setClassName(request.getClazz());
        existing.setSubject(request.getSubject());
        existing.setVisibility(request.getVisibility());
        existing.setCoverImage(request.getCoverImage());
        existing.setStatus(request.getStatus());
        existing.setUpdatedAt(now);
        courseMapper.updateById(existing);

        deleteCourseResources(existing.getId());
        courseChapterMapper.delete(new LambdaQueryWrapper<CourseChapterEntity>()
                .eq(CourseChapterEntity::getCourseId, existing.getId()));
        saveChaptersAndResources(existing.getId(), teacher.getId(), request.getChapters(), now);

        return buildCourseDetail(courseMapper.selectById(existing.getId()), teacher.getRealName());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> deleteCourse(UserEntity teacher, String courseId) {
        CourseEntity course = ensureOwnedCourse(teacher.getId(), courseId);

        for (CourseResourceEntity resource : listCourseResources(courseId)) {
            minioStorageService.removeObject(resource.getBucketName(), resource.getObjectKey());
        }

        courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudentEntity>()
                .eq(CourseStudentEntity::getCourseId, courseId));
        deleteCourseResources(courseId);
        courseChapterMapper.delete(new LambdaQueryWrapper<CourseChapterEntity>()
                .eq(CourseChapterEntity::getCourseId, courseId));
        courseMapper.deleteById(course.getId());

        return Collections.singletonMap("id", courseId);
    }

    public List<CourseStudentVO> getCourseStudents(UserEntity teacher, String courseId) {
        ensureOwnedCourse(teacher.getId(), courseId);
        return courseStudentMapper.selectCourseStudents(courseId).stream()
                .map(this::toCourseStudentView)
                .toList();
    }

    public List<CourseSelectableStudentVO> getCandidateStudents(UserEntity teacher, String courseId, String keyword) {
        ensureOwnedCourse(teacher.getId(), courseId);
        return courseStudentMapper.selectCandidateStudents(courseId, keyword).stream()
                .map(this::toCourseSelectableStudentView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseStudentBatchResultVO addStudents(UserEntity teacher, String courseId, AddCourseStudentsRequestDTO request) {
        ensureOwnedCourse(teacher.getId(), courseId);

        LinkedHashSet<String> requestedIds = request.getStudentIds().stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            throw new BusinessException(422, "studentIds不能为空");
        }

        List<UserEntity> students = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getId, requestedIds)
                .eq(UserEntity::getRole, "student")
                .ne(UserEntity::getStatus, "inactive"));
        if (students.size() != requestedIds.size()) {
            throw new BusinessException(404, "学生不存在");
        }

        List<StudentProfileEntity> profiles = studentProfileMapper.selectList(new LambdaQueryWrapper<StudentProfileEntity>()
                .in(StudentProfileEntity::getStudentId, requestedIds));
        if (profiles.size() != requestedIds.size()) {
            throw new BusinessException(404, "学生不存在");
        }

        Map<String, StudentProfileEntity> profileMap = profiles.stream()
                .collect(Collectors.toMap(StudentProfileEntity::getStudentId, Function.identity()));
        Set<String> existingStudentIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudentEntity>()
                        .eq(CourseStudentEntity::getCourseId, courseId)
                        .in(CourseStudentEntity::getStudentId, requestedIds))
                .stream()
                .map(CourseStudentEntity::getStudentId)
                .collect(Collectors.toSet());

        LocalDateTime now = TimeUtils.nowUtc();
        List<String> insertedStudentIds = new ArrayList<>();
        for (String studentId : requestedIds) {
            if (existingStudentIds.contains(studentId)) {
                continue;
            }
            StudentProfileEntity profile = profileMap.get(studentId);
            CourseStudentEntity entity = new CourseStudentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setCourseId(courseId);
            entity.setStudentId(studentId);
            entity.setStudentNo(profile.getStudentNo());
            entity.setJoinedAt(now);
            entity.setProgress(BigDecimal.ZERO);
            courseStudentMapper.insert(entity);
            insertedStudentIds.add(studentId);
        }

        refreshStudentCount(courseId);

        CourseStudentBatchResultVO response = new CourseStudentBatchResultVO();
        response.setCourseId(courseId);
        if (!insertedStudentIds.isEmpty()) {
            Set<String> insertedIdSet = new LinkedHashSet<>(insertedStudentIds);
            response.setStudents(courseStudentMapper.selectCourseStudents(courseId).stream()
                    .filter(item -> insertedIdSet.contains(item.getStudentId()))
                    .map(this::toCourseStudentView)
                    .toList());
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> removeStudent(UserEntity teacher, String courseId, String studentId) {
        ensureOwnedCourse(teacher.getId(), courseId);
        courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudentEntity>()
                .eq(CourseStudentEntity::getCourseId, courseId)
                .eq(CourseStudentEntity::getStudentId, studentId));
        refreshStudentCount(courseId);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("courseId", courseId);
        response.put("studentId", studentId);
        return response;
    }

    public CourseUploadResourceVO uploadCourseResource(UserEntity teacher, MultipartFile file) {
        MinioStorageService.CourseResourceUploadResult uploadResult =
                minioStorageService.uploadCourseResource(file, teacher.getId());

        CourseUploadResourceVO response = new CourseUploadResourceVO();
        response.setId("upload-" + UUID.randomUUID());
        response.setName(uploadResult.originalFilename());
        response.setType(uploadResult.type());
        response.setUrl(uploadResult.url());
        response.setBucketName(uploadResult.bucketName());
        response.setObjectKey(uploadResult.objectKey());
        response.setSize(uploadResult.size());
        return response;
    }

    private void validateCourseRequest(CourseSaveRequestDTO request, String teacherId) {
        if (!COURSE_VISIBILITIES.contains(request.getVisibility())) {
            throw new BusinessException(422, "visibility取值非法");
        }
        if (!COURSE_STATUSES.contains(request.getStatus())) {
            throw new BusinessException(422, "status取值非法");
        }
        if (request.getChapters() == null) {
            request.setChapters(new ArrayList<>());
        }

        for (CourseChapterRequestDTO chapter : request.getChapters()) {
            if (chapter.getResources() == null) {
                chapter.setResources(new ArrayList<>());
            }
            for (CourseResourceRequestDTO resource : chapter.getResources()) {
//                if (!RESOURCE_TYPES.contains(resource.getType())) {
//                    throw new BusinessException(422, "resource.type取值非法");
//                }
                boolean hasBucket = StringUtils.hasText(resource.getBucketName());
                boolean hasObjectKey = StringUtils.hasText(resource.getObjectKey());
                if (hasBucket != hasObjectKey) {
                    throw new BusinessException(422, "resource.bucketName和resource.objectKey必须同时传递");
                }
                if (hasObjectKey && !resource.getObjectKey().startsWith("courses/" + teacherId + "/")) {
                    throw new BusinessException(403, "资源不属于当前教师");
                }
            }
        }
    }

    private CourseEntity ensureOwnedCourse(String teacherId, String courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "课程不属于当前教师");
        }
        return course;
    }

    private CourseVO buildCourseDetail(CourseEntity course, String teacherName) {
        CourseVO view = toCourseView(course, teacherName, true);
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

    private CourseVO toCourseView(CourseEntity course, String teacherName, boolean withEmptyChapters) {
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
        view.setStatus(course.getStatus());
        view.setCreatedAt(TimeUtils.toIsoUtc(course.getCreatedAt()));
        view.setUpdatedAt(TimeUtils.toIsoUtc(course.getUpdatedAt()));
        List<CourseChapterEntity> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapterEntity>()
                .eq(CourseChapterEntity::getCourseId, course.getId())
                .orderByAsc(CourseChapterEntity::getSortOrder, CourseChapterEntity::getCreatedAt));
        List<CourseChapterVO> chapterViews = new ArrayList<>();
        for (CourseChapterEntity chapter : chapters) {
            CourseChapterVO chapterView = new CourseChapterVO();
            chapterView.setId(chapter.getId());
            chapterViews.add(chapterView);
        }
        view.setChapters(chapterViews);
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

    private CourseStudentVO toCourseStudentView(CourseStudentRow row) {
        CourseStudentVO view = new CourseStudentVO();
        view.setId(row.getId());
        view.setCourseId(row.getCourseId());
        view.setStudentId(row.getStudentId());
        view.setStudentName(row.getStudentName());
        view.setStudentNo(row.getStudentNo());
        view.setJoinedAt(TimeUtils.toIsoUtc(row.getJoinedAt()));
        view.setProgress(row.getProgress());
        return view;
    }

    private CourseSelectableStudentVO toCourseSelectableStudentView(CourseSelectableStudentRow row) {
        CourseSelectableStudentVO view = new CourseSelectableStudentVO();
        view.setId(row.getId());
        view.setUsername(row.getUsername());
        view.setRealName(row.getRealName());
        view.setStudentNo(row.getStudentNo());
        view.setGrade(row.getGrade());
        view.setClazz(row.getClassName());
        return view;
    }

    private void saveChaptersAndResources(String courseId,
                                          String teacherId,
                                          List<CourseChapterRequestDTO> chapters,
                                          LocalDateTime now) {
        if (CollectionUtils.isEmpty(chapters)) {
            return;
        }

        for (int chapterIndex = 0; chapterIndex < chapters.size(); chapterIndex++) {
            CourseChapterRequestDTO chapterRequest = chapters.get(chapterIndex);
            String chapterId = UUID.randomUUID().toString();

            CourseChapterEntity chapter = new CourseChapterEntity();
            chapter.setId(chapterId);
            chapter.setCourseId(courseId);
            chapter.setTitle(chapterRequest.getTitle());
            chapter.setDescription(chapterRequest.getDescription());
            chapter.setSortOrder(chapterIndex + 1);
            chapter.setCreatedAt(now);
            courseChapterMapper.insert(chapter);

            List<CourseResourceRequestDTO> resources = chapterRequest.getResources();
            if (CollectionUtils.isEmpty(resources)) {
                continue;
            }

            for (int resourceIndex = 0; resourceIndex < resources.size(); resourceIndex++) {
                CourseResourceRequestDTO resourceRequest = resources.get(resourceIndex);
                if (StringUtils.hasText(resourceRequest.getObjectKey())
                        && !resourceRequest.getObjectKey().startsWith("courses/" + teacherId + "/")) {
                    throw new BusinessException(403, "资源不属于当前教师");
                }

                CourseResourceEntity resource = new CourseResourceEntity();
                resource.setId(UUID.randomUUID().toString());
                resource.setChapterId(chapterId);
                resource.setName(resourceRequest.getName());
                resource.setType(resourceRequest.getType());
                resource.setUrl(resourceRequest.getUrl());
                resource.setBucketName(resourceRequest.getBucketName());
                resource.setObjectKey(resourceRequest.getObjectKey());
                resource.setSize(resourceRequest.getSize());
                resource.setDuration(resourceRequest.getDuration());
                resource.setSortOrder(resourceIndex + 1);
                resource.setCreatedAt(now);
                courseResourceMapper.insert(resource);
            }
        }
    }

    private void deleteObsoleteResourceObjects(String courseId, List<CourseChapterRequestDTO> chapters) {
        List<CourseResourceEntity> existingResources = listCourseResources(courseId);
        if (existingResources.isEmpty()) {
            return;
        }

        Set<String> retainedKeys = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(chapters)) {
            for (CourseChapterRequestDTO chapter : chapters) {
                if (CollectionUtils.isEmpty(chapter.getResources())) {
                    continue;
                }
                for (CourseResourceRequestDTO resource : chapter.getResources()) {
                    if (StringUtils.hasText(resource.getBucketName()) && StringUtils.hasText(resource.getObjectKey())) {
                        retainedKeys.add(buildResourceIdentity(resource.getBucketName(), resource.getObjectKey()));
                    }
                }
            }
        }

        for (CourseResourceEntity resource : existingResources) {
            String identity = buildResourceIdentity(resource.getBucketName(), resource.getObjectKey());
            if (StringUtils.hasText(resource.getBucketName())
                    && StringUtils.hasText(resource.getObjectKey())
                    && !retainedKeys.contains(identity)) {
                minioStorageService.removeObject(resource.getBucketName(), resource.getObjectKey());
            }
        }
    }

    private List<CourseResourceEntity> listCourseResources(String courseId) {
        List<CourseChapterEntity> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapterEntity>()
                .eq(CourseChapterEntity::getCourseId, courseId));
        if (CollectionUtils.isEmpty(chapters)) {
            return Collections.emptyList();
        }
        List<String> chapterIds = chapters.stream().map(CourseChapterEntity::getId).toList();
        return courseResourceMapper.selectList(new LambdaQueryWrapper<CourseResourceEntity>()
                .in(CourseResourceEntity::getChapterId, chapterIds));
    }

    private void deleteCourseResources(String courseId) {
        List<CourseChapterEntity> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapterEntity>()
                .eq(CourseChapterEntity::getCourseId, courseId));
        if (CollectionUtils.isEmpty(chapters)) {
            return;
        }
        List<String> chapterIds = chapters.stream().map(CourseChapterEntity::getId).toList();
        courseResourceMapper.delete(new LambdaQueryWrapper<CourseResourceEntity>()
                .in(CourseResourceEntity::getChapterId, chapterIds));
    }

    private void refreshStudentCount(String courseId) {
        long total = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudentEntity>()
                .eq(CourseStudentEntity::getCourseId, courseId));
        courseMapper.update(null, new LambdaUpdateWrapper<CourseEntity>()
                .eq(CourseEntity::getId, courseId)
                .set(CourseEntity::getStudentCount, Math.toIntExact(total))
                .set(CourseEntity::getUpdatedAt, TimeUtils.nowUtc()));
    }

    private String buildResourceIdentity(String bucketName, String objectKey) {
        return bucketName + "::" + objectKey;
    }
}
