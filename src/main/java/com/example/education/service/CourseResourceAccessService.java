package com.example.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.education.common.exception.BusinessException;
import com.example.education.mapper.CourseChapterMapper;
import com.example.education.mapper.CourseMapper;
import com.example.education.mapper.CourseResourceMapper;
import com.example.education.mapper.CourseStudentMapper;
import com.example.education.pojo.entity.CourseChapterEntity;
import com.example.education.pojo.entity.CourseEntity;
import com.example.education.pojo.entity.CourseResourceEntity;
import com.example.education.pojo.entity.CourseStudentEntity;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.vo.CourseResourceAccessVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CourseResourceAccessService {

    private static final int URL_EXPIRES_IN_SECONDS = 600;

    private final CourseResourceMapper courseResourceMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final MinioStorageService minioStorageService;

    public CourseResourceAccessVO getPreviewUrl(UserEntity user, String resourceId) {
        ResourceContext context = getAccessibleResource(user, resourceId);
        String url = minioStorageService.generatePreviewUrl(
                context.resource().getBucketName(),
                context.resource().getObjectKey(),
                context.resource().getName(),
                context.resource().getType(),
                URL_EXPIRES_IN_SECONDS);
        return toAccessView(context.resource(), url);
    }

    public CourseResourceAccessVO getDownloadUrl(UserEntity user, String resourceId) {
        ResourceContext context = getAccessibleResource(user, resourceId);
        String url = minioStorageService.generateDownloadUrl(
                context.resource().getBucketName(),
                context.resource().getObjectKey(),
                context.resource().getName(),
                URL_EXPIRES_IN_SECONDS);
        return toAccessView(context.resource(), url);
    }

    private ResourceContext getAccessibleResource(UserEntity user, String resourceId) {
        CourseResourceEntity resource = courseResourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "课程资源不存在");
        }
        if (!StringUtils.hasText(resource.getBucketName()) || !StringUtils.hasText(resource.getObjectKey())) {
            throw new BusinessException(404, "课程资源文件不存在");
        }

        CourseChapterEntity chapter = courseChapterMapper.selectById(resource.getChapterId());
        if (chapter == null) {
            throw new BusinessException(404, "课程资源不存在");
        }

        CourseEntity course = courseMapper.selectById(chapter.getCourseId());
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }

        ensureUserCanAccessCourse(user, course.getId(), course.getTeacherId());
        return new ResourceContext(resource, chapter, course);
    }

    private void ensureUserCanAccessCourse(UserEntity user, String courseId, String teacherId) {
        if ("teacher".equals(user.getRole())) {
            if (!teacherId.equals(user.getId())) {
                throw new BusinessException(403, "无权访问该课程资源");
            }
            return;
        }

        if ("student".equals(user.getRole())) {
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudentEntity>()
                    .eq(CourseStudentEntity::getCourseId, courseId)
                    .eq(CourseStudentEntity::getStudentId, user.getId()));
            if (count == null || count < 1) {
                throw new BusinessException(403, "无权访问该课程资源");
            }
            return;
        }

        throw new BusinessException(403, "无权访问该课程资源");
    }

    private CourseResourceAccessVO toAccessView(CourseResourceEntity resource, String url) {
        CourseResourceAccessVO view = new CourseResourceAccessVO();
        view.setResourceId(resource.getId());
        view.setFileName(resource.getName());
        view.setUrl(url);
        view.setExpiresIn(URL_EXPIRES_IN_SECONDS);
        return view;
    }

    private record ResourceContext(
            CourseResourceEntity resource,
            CourseChapterEntity chapter,
            CourseEntity course) {
    }
}
