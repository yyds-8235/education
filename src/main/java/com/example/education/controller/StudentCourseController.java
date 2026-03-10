package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.CoursePageDataDTO;
import com.example.education.pojo.query.CourseQuery;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.vo.CourseVO;
import com.example.education.pojo.vo.StudentJoinCourseVO;
import com.example.education.service.AuthService;
import com.example.education.service.StudentCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentCourseController {

    private final AuthService authService;
    private final StudentCourseService studentCourseService;

    @GetMapping("/student/courses")
    public ApiResponse<CoursePageDataDTO> getCourses(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "page", required = false) Long page,
            @RequestParam(value = "pageSize", required = false) Long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "class", required = false) String className,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "scope", required = false) String scope) {
        UserEntity student = authService.requireStudent(token);

        CourseQuery query = new CourseQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        query.setGrade(grade);
        query.setClassName(className);
        query.setSubject(subject);
        query.setStatus(status);
        query.setScope(scope);

        return ApiResponse.success(studentCourseService.getCourses(student, query));
    }

    @GetMapping("/student/courses/{courseId}")
    public ApiResponse<CourseVO> getCourseDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId) {
        UserEntity student = authService.requireStudent(token);
        return ApiResponse.success(studentCourseService.getCourseDetail(student, courseId));
    }

    @PostMapping("/student/courses/{courseId}/join")
    public ApiResponse<StudentJoinCourseVO> joinCourse(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId) {
        UserEntity student = authService.requireStudent(token);
        return ApiResponse.success(studentCourseService.joinCourse(student, courseId));
    }
}

