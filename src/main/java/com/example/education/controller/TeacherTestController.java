package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.TestGradeRequestDTO;
import com.example.education.pojo.dto.TestPageDataDTO;
import com.example.education.pojo.dto.TestSaveRequestDTO;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.query.TestQuery;
import com.example.education.pojo.vo.TestBatchGradeResultVO;
import com.example.education.pojo.vo.TestStatisticsVO;
import com.example.education.pojo.vo.TestSubmissionVO;
import com.example.education.pojo.vo.TestVO;
import com.example.education.service.AuthService;
import com.example.education.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeacherTestController {

    private final AuthService authService;
    private final TestService testService;

    @GetMapping("/teacher/tests")
    public ApiResponse<TestPageDataDTO<TestVO>> getTests(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "page", required = false) Long page,
            @RequestParam(value = "pageSize", required = false) Long pageSize,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        UserEntity teacher = authService.requireTeacher(token);
        TestQuery query = new TestQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setCourseId(courseId);
        query.setKeyword(keyword);
        return ApiResponse.success(testService.getTeacherTests(teacher, query));
    }

    @GetMapping("/teacher/tests/{testId}")
    public ApiResponse<TestVO> getTestDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.getTeacherTestDetail(teacher, testId));
    }

    @PostMapping("/teacher/tests")
    public ApiResponse<TestVO> createTest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody TestSaveRequestDTO request) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.createTeacherTest(teacher, request));
    }

    @PutMapping("/teacher/tests/{testId}")
    public ApiResponse<TestVO> updateTest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId,
            @Valid @RequestBody TestSaveRequestDTO request) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.updateTeacherTest(teacher, testId, request));
    }

    @PostMapping("/teacher/tests/{testId}/publish")
    public ApiResponse<TestVO> publishTest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.publishTeacherTest(teacher, testId));
    }

    @GetMapping("/teacher/tests/{testId}/submissions")
    public ApiResponse<List<TestSubmissionVO>> getSubmissions(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.getTeacherSubmissions(teacher, testId));
    }

    @PutMapping("/teacher/test-submissions/{submissionId}/grade")
    public ApiResponse<TestSubmissionVO> gradeSubmission(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String submissionId,
            @Valid @RequestBody TestGradeRequestDTO request) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.gradeSubmission(teacher, submissionId, request));
    }

    @PostMapping("/teacher/tests/{testId}/batch-grade-objective")
    public ApiResponse<TestBatchGradeResultVO> batchGradeObjective(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.batchGradeObjective(teacher, testId));
    }

    @GetMapping("/teacher/tests/{testId}/statistics")
    public ApiResponse<TestStatisticsVO> getStatistics(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity teacher = authService.requireTeacher(token);
        return ApiResponse.success(testService.getTeacherStatistics(teacher, testId));
    }
}
