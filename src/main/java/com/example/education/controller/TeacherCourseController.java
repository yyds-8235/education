package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.AddCourseStudentsRequestDTO;
import com.example.education.pojo.dto.CoursePageDataDTO;
import com.example.education.pojo.dto.CourseSaveRequestDTO;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.query.CourseQuery;
import com.example.education.pojo.vo.CourseSelectableStudentVO;
import com.example.education.pojo.vo.CourseStudentBatchResultVO;
import com.example.education.pojo.vo.CourseStudentVO;
import com.example.education.pojo.vo.CourseUploadResourceVO;
import com.example.education.pojo.vo.CourseVO;
import com.example.education.service.AuthService;
import com.example.education.service.TeacherCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TeacherCourseController {

    private final AuthService authService;
    private final TeacherCourseService teacherCourseService;

    @GetMapping("/teacher/courses")
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
        UserEntity teacher = authService.requireTeacher(token);

        CourseQuery query = new CourseQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        query.setGrade(grade);
        query.setClassName(className);
        query.setSubject(subject);
        query.setStatus(status);
        query.setScope(scope);

        return ApiResponse.success(teacherCourseService.getCourses(teacher, query));
    }

    @GetMapping("/teacher/courses/{courseId}")
    public ApiResponse<CourseVO> getCourseDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.getCourseDetail(teacher, courseId));
    }

    @PostMapping("/teacher/courses")
    public ApiResponse<CourseVO> createCourse(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CourseSaveRequestDTO request) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.createCourse(teacher, request));
    }

    @PutMapping("/teacher/courses/{courseId}")
    public ApiResponse<CourseVO> updateCourse(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId,
            @Valid @RequestBody CourseSaveRequestDTO request) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.updateCourse(teacher, courseId, request));
    }

    @DeleteMapping("/teacher/courses/{courseId}")
    public ApiResponse<Map<String, String>> deleteCourse(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.deleteCourse(teacher, courseId));
    }

    @GetMapping("/teacher/courses/{courseId}/students")
    public ApiResponse<List<CourseStudentVO>> getCourseStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.getCourseStudents(teacher, courseId));
    }

    @GetMapping("/teacher/courses/{courseId}/candidate-students")
    public ApiResponse<List<CourseSelectableStudentVO>> getCandidateStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.getCandidateStudents(teacher, courseId, keyword));
    }

    @PostMapping("/teacher/courses/{courseId}/students")
    public ApiResponse<CourseStudentBatchResultVO> addStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId,
            @Valid @RequestBody AddCourseStudentsRequestDTO request) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.addStudents(teacher, courseId, request));
    }

    @DeleteMapping("/teacher/courses/{courseId}/students/{studentId}")
    public ApiResponse<Map<String, String>> removeStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("courseId") String courseId,
            @PathVariable("studentId") String studentId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.removeStudent(teacher, courseId, studentId));
    }

    @PostMapping(value = "/teacher/course-resources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourseUploadResourceVO> uploadCourseResource(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestPart("file") MultipartFile file) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(teacherCourseService.uploadCourseResource(teacher, file));
    }
}
