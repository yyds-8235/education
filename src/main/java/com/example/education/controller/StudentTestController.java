
package com.example.education.controller;
import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.SubmissionAppealRequestDTO;
import com.example.education.pojo.dto.TestPageDataDTO;
import com.example.education.pojo.dto.TestSubmitRequestDTO;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.query.TestQuery;
import com.example.education.pojo.vo.TestSubmissionVO;
import com.example.education.pojo.vo.TestVO;
import com.example.education.service.AuthService;
import com.example.education.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentTestController {

    private final AuthService authService;
    private final TestService testService;

    @GetMapping("/student/tests")
    public ApiResponse<TestPageDataDTO<TestVO>> getTests(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "page", required = false) Long page,
            @RequestParam(value = "pageSize", required = false) Long pageSize,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        UserEntity student = authService.requireStudent(token);
        TestQuery query = new TestQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setCourseId(courseId);
        query.setKeyword(keyword);
        return ApiResponse.success(testService.getStudentTests(student, query));
    }

    @GetMapping("/student/tests/{testId}")
    public ApiResponse<TestVO> getTestDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity student = authService.requireStudent(token);
        return ApiResponse.success(testService.getStudentTestDetail(student, testId));
    }

    @PostMapping("/student/tests/{testId}/submit")
    public ApiResponse<TestSubmissionVO> submitTest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId,
            @Valid @RequestBody TestSubmitRequestDTO request) {
        UserEntity student = authService.requireStudent(token);
        return ApiResponse.success(testService.submitStudentTest(student, testId, request));
    }

    @GetMapping("/student/tests/{testId}/submission")
    public ApiResponse<TestSubmissionVO> getSubmission(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String testId) {
        UserEntity student = authService.requireStudent(token);
        return ApiResponse.success(testService.getStudentSubmission(student, testId));
    }

    @PostMapping("/student/test-submissions/{submissionId}/appeal")
    public ApiResponse<TestSubmissionVO> appealSubmission(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String submissionId,
            @Valid @RequestBody SubmissionAppealRequestDTO request) {
        UserEntity student = authService.requireStudent(token);
        return ApiResponse.success(testService.appealSubmission(student, submissionId, request));
    }
}
